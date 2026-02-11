package com.finnexus.service.impl;

import com.finnexus.domain.dto.CandleDto;
import com.finnexus.domain.dto.RoboStrategyRequest;
import com.finnexus.domain.dto.RoboStrategyResponse;
import com.finnexus.domain.entity.Order;
import com.finnexus.domain.entity.RoboStrategy;
import com.finnexus.domain.entity.Trade;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.entity.Wallet;
import com.finnexus.domain.enums.OrderSide;
import com.finnexus.domain.enums.OrderStatus;
import com.finnexus.domain.enums.OrderType;
import com.finnexus.domain.enums.StrategyType;
import com.finnexus.domain.enums.Timeframe;
import com.finnexus.domain.enums.TradeStatus;
import com.finnexus.exception.BadRequestException;
import com.finnexus.exception.NotFoundException;
import com.finnexus.repository.OrderRepository;
import com.finnexus.repository.RoboStrategyRepository;
import com.finnexus.repository.TradeRepository;
import com.finnexus.repository.UserRepository;
import com.finnexus.repository.WalletRepository;
import com.finnexus.service.MarketDataService;
import com.finnexus.service.RoboStrategyService;
import com.finnexus.util.MarketMath;
import com.finnexus.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoboStrategyServiceImpl implements RoboStrategyService {
    private static final BigDecimal LEVERAGE = new BigDecimal("50");
    private static final BigDecimal DEFAULT_QTY = new BigDecimal("1.0");

    private final RoboStrategyRepository strategyRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;

    public RoboStrategyServiceImpl(RoboStrategyRepository strategyRepository, UserRepository userRepository,
                                   MarketDataService marketDataService, OrderRepository orderRepository,
                                   TradeRepository tradeRepository, WalletRepository walletRepository) {
        this.strategyRepository = strategyRepository;
        this.userRepository = userRepository;
        this.marketDataService = marketDataService;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public RoboStrategyResponse create(RoboStrategyRequest request) {
        User user = getCurrentUser();
        RoboStrategy strategy = new RoboStrategy();
        applyRequest(strategy, request);
        strategy.setUser(user);
        strategyRepository.save(strategy);
        return toResponse(strategy);
    }

    @Override
    public RoboStrategyResponse update(Long id, RoboStrategyRequest request) {
        User user = getCurrentUser();
        RoboStrategy strategy = strategyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Strategy not found"));
        if (!strategy.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized strategy access");
        }
        applyRequest(strategy, request);
        strategyRepository.save(strategy);
        return toResponse(strategy);
    }

    @Override
    public void delete(Long id) {
        User user = getCurrentUser();
        RoboStrategy strategy = strategyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Strategy not found"));
        if (!strategy.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized strategy access");
        }
        strategyRepository.delete(strategy);
    }

    @Override
    public List<RoboStrategyResponse> list() {
        User user = getCurrentUser();
        return strategyRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void runStrategies() {
        User user = getCurrentUser();
        List<RoboStrategy> strategies = strategyRepository.findByUserId(user.getId());
        for (RoboStrategy strategy : strategies) {
            if (!strategy.isEnabled()) {
                continue;
            }
            List<CandleDto> candles = marketDataService.getCandles(strategy.getSymbol(), strategy.getTimeframe(), 60);
            List<BigDecimal> closes = candles.stream().map(CandleDto::getClose).toList();
            String signal = evaluateSignal(strategy, closes);
            if (signal.equals("HOLD")) {
                continue;
            }
            if (signal.equals(strategy.getLastSignal())) {
                continue;
            }
            placeAutoOrder(strategy, signal);
            strategy.setLastSignal(signal);
            strategyRepository.save(strategy);
        }
    }

    private void placeAutoOrder(RoboStrategy strategy, String signal) {
        User user = strategy.getUser();
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        BigDecimal price = marketDataService.getQuote(strategy.getSymbol()).getPrice();
        BigDecimal requiredMargin = price.multiply(DEFAULT_QTY)
                .divide(LEVERAGE, 4, RoundingMode.HALF_UP);
        BigDecimal available = wallet.getBalance().subtract(wallet.getMarginUsed());
        if (available.compareTo(requiredMargin) < 0) {
            return;
        }

        Order order = new Order();
        order.setUser(user);
        order.setSymbol(strategy.getSymbol());
        order.setSide(signal.equals("BUY") ? OrderSide.BUY : OrderSide.SELL);
        order.setType(OrderType.MARKET);
        order.setQuantity(DEFAULT_QTY);
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutedPrice(price);
        order.setExecutedAt(Instant.now());
        orderRepository.save(order);

        Trade trade = new Trade();
        trade.setUser(user);
        trade.setOrder(order);
        trade.setSymbol(order.getSymbol());
        trade.setSide(order.getSide());
        trade.setEntryPrice(price);
        trade.setQuantity(DEFAULT_QTY);
        trade.setStatus(TradeStatus.OPEN);
        trade.setOpenedAt(Instant.now());
        tradeRepository.save(trade);

        wallet.setMarginUsed(wallet.getMarginUsed().add(requiredMargin));
        walletRepository.save(wallet);
    }

    private String evaluateSignal(RoboStrategy strategy, List<BigDecimal> closes) {
        if (strategy.getType() == StrategyType.MOVING_AVERAGE_CROSSOVER) {
            int fast = strategy.getFastPeriod() != null ? strategy.getFastPeriod() : 5;
            int slow = strategy.getSlowPeriod() != null ? strategy.getSlowPeriod() : 20;
            if (closes.size() < slow + 2) {
                return "HOLD";
            }
            BigDecimal prevFast = MarketMath.sma(closes.subList(0, closes.size() - 1), fast);
            BigDecimal prevSlow = MarketMath.sma(closes.subList(0, closes.size() - 1), slow);
            BigDecimal currFast = MarketMath.sma(closes, fast);
            BigDecimal currSlow = MarketMath.sma(closes, slow);
            boolean crossedUp = prevFast.compareTo(prevSlow) <= 0 && currFast.compareTo(currSlow) > 0;
            boolean crossedDown = prevFast.compareTo(prevSlow) >= 0 && currFast.compareTo(currSlow) < 0;
            if (crossedUp) {
                return "BUY";
            }
            if (crossedDown) {
                return "SELL";
            }
            return "HOLD";
        }

        if (strategy.getType() == StrategyType.RSI) {
            int period = strategy.getRsiPeriod() != null ? strategy.getRsiPeriod() : 14;
            int overbought = strategy.getRsiOverbought() != null ? strategy.getRsiOverbought() : 70;
            int oversold = strategy.getRsiOversold() != null ? strategy.getRsiOversold() : 30;
            BigDecimal rsi = MarketMath.rsi(closes, period);
            if (rsi.compareTo(BigDecimal.valueOf(oversold)) <= 0) {
                return "BUY";
            }
            if (rsi.compareTo(BigDecimal.valueOf(overbought)) >= 0) {
                return "SELL";
            }
        }
        return "HOLD";
    }

    private void applyRequest(RoboStrategy strategy, RoboStrategyRequest request) {
        strategy.setType(StrategyType.valueOf(request.getType().toUpperCase()));
        strategy.setSymbol(request.getSymbol().toUpperCase());
        strategy.setTimeframe(Timeframe.valueOf(request.getTimeframe().toUpperCase()));
        strategy.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        strategy.setFastPeriod(request.getFastPeriod());
        strategy.setSlowPeriod(request.getSlowPeriod());
        strategy.setRsiPeriod(request.getRsiPeriod());
        strategy.setRsiOverbought(request.getRsiOverbought());
        strategy.setRsiOversold(request.getRsiOversold());
    }

    private RoboStrategyResponse toResponse(RoboStrategy strategy) {
        return new RoboStrategyResponse(
                strategy.getId(),
                strategy.getType().name(),
                strategy.getSymbol(),
                strategy.getTimeframe().name(),
                strategy.isEnabled(),
                strategy.getFastPeriod(),
                strategy.getSlowPeriod(),
                strategy.getRsiPeriod(),
                strategy.getRsiOverbought(),
                strategy.getRsiOversold(),
                strategy.getLastSignal()
        );
    }

    private User getCurrentUser() {
        String username = SecurityUtil.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
