package com.finnexus.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CandleDto {
    private Instant time;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    public CandleDto(Instant time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public Instant getTime() {
        return time;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }
}
