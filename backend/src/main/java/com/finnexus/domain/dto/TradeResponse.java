package com.finnexus.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TradeResponse {
    private Long id;
    private Long orderId;
    private String symbol;
    private String side;
    private String status;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private BigDecimal quantity;
    private BigDecimal pnl;
    private Instant openedAt;
    private Instant closedAt;

    public TradeResponse(Long id, Long orderId, String symbol, String side, String status,
                         BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal quantity,
                         BigDecimal pnl, Instant openedAt, Instant closedAt) {
        this.id = id;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.status = status;
        this.entryPrice = entryPrice;
        this.exitPrice = exitPrice;
        this.quantity = quantity;
        this.pnl = pnl;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public BigDecimal getExitPrice() {
        return exitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPnl() {
        return pnl;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
