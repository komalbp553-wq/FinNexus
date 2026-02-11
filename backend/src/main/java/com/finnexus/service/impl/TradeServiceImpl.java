package com.finnexus.service.impl;

import com.finnexus.domain.dto.CloseTradeRequest;
import com.finnexus.domain.dto.TradeResponse;
import com.finnexus.domain.entity.Trade;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.entity.Wallet;
import com.finnexus.domain.enums.OrderSide;
import com.finnexus.domain.enums.TradeStatus;
import com.finnexus.exception.BadRequestException;
import com.finnexus.exception.NotFoundException;
import com.finnexus.repository.TradeRepository;
import com.finnexus.repository.UserRepository;
import com.finnexus.repository.WalletRepository;
import com.finnexus.service.TradeService;
import com.finnexus.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class TradeServiceImpl implements TradeService {
    private static final BigDecimal LEVERAGE = new BigDecimal("50");

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public TradeServiceImpl(TradeRepository tradeRepository, UserRepository userRepository, WalletRepository walletRepository) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public List<TradeResponse> getTrades() {
        User user = getCurrentUser();
        return tradeRepository.findByUserIdOrderByOpenedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TradeResponse closeTrade(Long tradeId, CloseTradeRequest request) {
        User user = getCurrentUser();
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NotFoundException("Trade not found"));
        if (!trade.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized trade access");
        }
        if (trade.getStatus() != TradeStatus.OPEN) {
            throw new BadRequestException("Trade already closed");
        }

        BigDecimal exitPrice = request.getExitPrice();
        BigDecimal pnl = calculatePnl(trade.getSide(), trade.getEntryPrice(), exitPrice, trade.getQuantity());
        trade.setExitPrice(exitPrice);
        trade.setPnl(pnl);
        trade.setStatus(TradeStatus.CLOSED);
        trade.setClosedAt(Instant.now());

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        BigDecimal marginToRelease = trade.getEntryPrice().multiply(trade.getQuantity())
                .divide(LEVERAGE, 4, RoundingMode.HALF_UP);
        wallet.setMarginUsed(wallet.getMarginUsed().subtract(marginToRelease));
        wallet.setBalance(wallet.getBalance().add(pnl));
        wallet.setEquity(wallet.getBalance());

        tradeRepository.save(trade);
        walletRepository.save(wallet);

        return toResponse(trade);
    }

    private BigDecimal calculatePnl(OrderSide side, BigDecimal entry, BigDecimal exit, BigDecimal quantity) {
        BigDecimal diff = side == OrderSide.BUY ? exit.subtract(entry) : entry.subtract(exit);
        return diff.multiply(quantity).setScale(4, RoundingMode.HALF_UP);
    }

    private User getCurrentUser() {
        String username = SecurityUtil.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private TradeResponse toResponse(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getOrder().getId(),
                trade.getSymbol(),
                trade.getSide().name(),
                trade.getStatus().name(),
                trade.getEntryPrice(),
                trade.getExitPrice(),
                trade.getQuantity(),
                trade.getPnl(),
                trade.getOpenedAt(),
                trade.getClosedAt()
        );
    }
}
