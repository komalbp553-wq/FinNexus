package com.finnexus.service.impl;

import com.finnexus.domain.dto.PnlResponse;
import com.finnexus.domain.entity.Trade;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.enums.OrderSide;
import com.finnexus.domain.enums.TradeStatus;
import com.finnexus.exception.NotFoundException;
import com.finnexus.repository.TradeRepository;
import com.finnexus.repository.UserRepository;
import com.finnexus.service.MarketDataService;
import com.finnexus.service.PnlService;
import com.finnexus.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PnlServiceImpl implements PnlService {
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10000.00");

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;

    public PnlServiceImpl(TradeRepository tradeRepository, UserRepository userRepository,
                          MarketDataService marketDataService) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.marketDataService = marketDataService;
    }

    @Override
    public PnlResponse getPnlSummary() {
        User user = getCurrentUser();
        List<Trade> openTrades = tradeRepository.findByUserIdAndStatus(user.getId(), TradeStatus.OPEN);
        List<Trade> closedTrades = tradeRepository.findByUserIdAndStatus(user.getId(), TradeStatus.CLOSED);

        BigDecimal openPnl = openTrades.stream()
                .map(this::calculateUnrealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal closedPnl = closedTrades.stream()
                .map(trade -> trade.getPnl() != null ? trade.getPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant dayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        BigDecimal dailyPnl = closedTrades.stream()
                .filter(t -> t.getClosedAt() != null && t.getClosedAt().isAfter(dayAgo))
                .map(t -> t.getPnl() != null ? t.getPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weeklyPnl = closedTrades.stream()
                .filter(t -> t.getClosedAt() != null && t.getClosedAt().isAfter(weekAgo))
                .map(t -> t.getPnl() != null ? t.getPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long wins = closedTrades.stream().filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) > 0).count();
        BigDecimal accuracy = closedTrades.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(wins * 100.0 / closedTrades.size()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal roi = closedPnl.divide(INITIAL_BALANCE, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new PnlResponse(openPnl.setScale(4, RoundingMode.HALF_UP),
                closedPnl.setScale(4, RoundingMode.HALF_UP),
                dailyPnl.setScale(4, RoundingMode.HALF_UP),
                weeklyPnl.setScale(4, RoundingMode.HALF_UP),
                roi.setScale(2, RoundingMode.HALF_UP),
                accuracy);
    }

    private BigDecimal calculateUnrealizedPnl(Trade trade) {
        BigDecimal currentPrice = marketDataService.getQuote(trade.getSymbol()).getPrice();
        BigDecimal diff = trade.getSide() == OrderSide.BUY
                ? currentPrice.subtract(trade.getEntryPrice())
                : trade.getEntryPrice().subtract(currentPrice);
        return diff.multiply(trade.getQuantity());
    }

    private User getCurrentUser() {
        String username = SecurityUtil.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
