package com.finnexus.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class QuoteDto {
    private String symbol;
    private BigDecimal price;
    private Instant time;

    public QuoteDto(String symbol, BigDecimal price, Instant time) {
        this.symbol = symbol;
        this.price = price;
        this.time = time;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getTime() {
        return time;
    }
}
