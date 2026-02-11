package com.finnexus.service.impl;

import com.finnexus.domain.dto.CreateOrderRequest;
import com.finnexus.domain.dto.OrderResponse;
import com.finnexus.domain.dto.QuoteDto;
import com.finnexus.domain.entity.Order;
import com.finnexus.domain.entity.Trade;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.entity.Wallet;
import com.finnexus.domain.enums.OrderSide;
import com.finnexus.domain.enums.OrderStatus;
import com.finnexus.domain.enums.OrderType;
import com.finnexus.domain.enums.TradeStatus;
import com.finnexus.exception.BadRequestException;
import com.finnexus.exception.NotFoundException;
import com.finnexus.repository.OrderRepository;
import com.finnexus.repository.TradeRepository;
import com.finnexus.repository.UserRepository;
import com.finnexus.repository.WalletRepository;
import com.finnexus.service.MarketDataService;
import com.finnexus.service.OrderService;
import com.finnexus.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal LEVERAGE = new BigDecimal("50");

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MarketDataService marketDataService;

    public OrderServiceImpl(OrderRepository orderRepository, TradeRepository tradeRepository,
                            UserRepository userRepository, WalletRepository walletRepository,
                            MarketDataService marketDataService) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.marketDataService = marketDataService;
    }

    @Override
    public OrderResponse placeOrder(CreateOrderRequest request) {
        User user = getCurrentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        OrderSide side = OrderSide.valueOf(request.getSide().toUpperCase());
        OrderType type = OrderType.valueOf(request.getType().toUpperCase());

        if (type == OrderType.LIMIT && request.getPrice() == null) {
            throw new BadRequestException("Limit order requires price");
        }

        BigDecimal price = type == OrderType.MARKET
                ? marketDataService.getQuote(request.getSymbol()).getPrice()
                : request.getPrice();

        BigDecimal requiredMargin = price.multiply(request.getQuantity())
                .divide(LEVERAGE, 4, RoundingMode.HALF_UP);

        BigDecimal available = wallet.getBalance().subtract(wallet.getMarginUsed());
        if (available.compareTo(requiredMargin) < 0) {
            throw new BadRequestException("Insufficient margin");
        }

        Order order = new Order();
        order.setUser(user);
        order.setSymbol(request.getSymbol().toUpperCase());
        order.setSide(side);
        order.setType(type);
        order.setPrice(type == OrderType.LIMIT ? request.getPrice() : null);
        order.setQuantity(request.getQuantity());

        if (type == OrderType.MARKET) {
            executeOrder(order, price, wallet, requiredMargin);
            orderRepository.save(order);
            createTrade(order);
        } else {
            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
        }

        walletRepository.save(wallet);

        return toResponse(order);
    }

    @Override
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized order access");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        return toResponse(order);
    }

    @Override
    public List<OrderResponse> getOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void processPendingOrders() {
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        for (Order order : pending) {
            QuoteDto quote = marketDataService.getQuote(order.getSymbol());
            boolean shouldExecute = (order.getSide() == OrderSide.BUY && quote.getPrice().compareTo(order.getPrice()) <= 0)
                    || (order.getSide() == OrderSide.SELL && quote.getPrice().compareTo(order.getPrice()) >= 0);
            if (shouldExecute) {
                Wallet wallet = walletRepository.findByUserId(order.getUser().getId())
                        .orElseThrow(() -> new NotFoundException("Wallet not found"));
                BigDecimal requiredMargin = quote.getPrice().multiply(order.getQuantity())
                        .divide(LEVERAGE, 4, RoundingMode.HALF_UP);
                BigDecimal available = wallet.getBalance().subtract(wallet.getMarginUsed());
                if (available.compareTo(requiredMargin) < 0) {
                    order.setStatus(OrderStatus.REJECTED);
                    orderRepository.save(order);
                } else {
                    executeOrder(order, quote.getPrice(), wallet, requiredMargin);
                    orderRepository.save(order);
                    createTrade(order);
                    walletRepository.save(wallet);
                }
            }
        }
    }

    private void executeOrder(Order order, BigDecimal executedPrice, Wallet wallet, BigDecimal requiredMargin) {
        order.setExecutedPrice(executedPrice);
        order.setExecutedAt(Instant.now());
        order.setStatus(OrderStatus.EXECUTED);

        wallet.setMarginUsed(wallet.getMarginUsed().add(requiredMargin));
    }

    private void createTrade(Order order) {
        Trade trade = new Trade();
        trade.setUser(order.getUser());
        trade.setOrder(order);
        trade.setSymbol(order.getSymbol());
        trade.setSide(order.getSide());
        trade.setEntryPrice(order.getExecutedPrice());
        trade.setQuantity(order.getQuantity());
        trade.setStatus(TradeStatus.OPEN);
        trade.setOpenedAt(Instant.now());
        tradeRepository.save(trade);
    }

    private User getCurrentUser() {
        String username = SecurityUtil.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private OrderResponse toResponse(Order order) {
        BigDecimal investedAmount = null;
        BigDecimal currentPrice = null;
        BigDecimal pnl = null;

        if (order.getStatus() == OrderStatus.EXECUTED && order.getExecutedPrice() != null) {
            investedAmount = order.getExecutedPrice().multiply(order.getQuantity())
                    .setScale(4, RoundingMode.HALF_UP);
            currentPrice = marketDataService.getQuote(order.getSymbol()).getPrice();
            pnl = calculatePnl(order.getSide(), order.getExecutedPrice(), currentPrice, order.getQuantity());
        } else if (order.getStatus() == OrderStatus.PENDING && order.getPrice() != null) {
            investedAmount = order.getPrice().multiply(order.getQuantity())
                    .setScale(4, RoundingMode.HALF_UP);
        }

        return new OrderResponse(
                order.getId(),
                order.getSymbol(),
                order.getSide().name(),
                order.getType().name(),
                order.getStatus().name(),
                order.getPrice(),
                order.getExecutedPrice(),
                order.getQuantity(),
                investedAmount,
                currentPrice,
                pnl,
                order.getCreatedAt(),
                order.getExecutedAt()
        );
    }

    private BigDecimal calculatePnl(OrderSide side, BigDecimal entry, BigDecimal current, BigDecimal quantity) {
        if (entry == null || current == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = side == OrderSide.BUY ? current.subtract(entry) : entry.subtract(current);
        return diff.multiply(quantity).setScale(4, RoundingMode.HALF_UP);
    }
}
