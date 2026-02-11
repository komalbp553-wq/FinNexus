package com.finnexus.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderResponse {
    private Long id;
    private String symbol;
    private String side;
    private String type;
    private String status;
    private BigDecimal price;
    private BigDecimal executedPrice;
    private BigDecimal quantity;
    private BigDecimal investedAmount;
    private BigDecimal currentPrice;
    private BigDecimal pnl;
    private Instant createdAt;
    private Instant executedAt;

    public OrderResponse(Long id, String symbol, String side, String type, String status,
                         BigDecimal price, BigDecimal executedPrice, BigDecimal quantity,
                         BigDecimal investedAmount, BigDecimal currentPrice, BigDecimal pnl,
                         Instant createdAt, Instant executedAt) {
        this.id = id;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.status = status;
        this.price = price;
        this.executedPrice = executedPrice;
        this.quantity = quantity;
        this.investedAmount = investedAmount;
        this.currentPrice = currentPrice;
        this.pnl = pnl;
        this.createdAt = createdAt;
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getExecutedPrice() {
        return executedPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPnl() {
        return pnl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
