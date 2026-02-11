package com.finnexus.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RoboStrategyRequest {
    @NotBlank
    private String type;

    @NotBlank
    private String symbol;

    @NotBlank
    private String timeframe;

    @NotNull
    private Boolean enabled;

    private Integer fastPeriod;
    private Integer slowPeriod;
    private Integer rsiPeriod;
    private Integer rsiOverbought;
    private Integer rsiOversold;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFastPeriod() {
        return fastPeriod;
    }

    public void setFastPeriod(Integer fastPeriod) {
        this.fastPeriod = fastPeriod;
    }

    public Integer getSlowPeriod() {
        return slowPeriod;
    }

    public void setSlowPeriod(Integer slowPeriod) {
        this.slowPeriod = slowPeriod;
    }

    public Integer getRsiPeriod() {
        return rsiPeriod;
    }

    public void setRsiPeriod(Integer rsiPeriod) {
        this.rsiPeriod = rsiPeriod;
    }

    public Integer getRsiOverbought() {
        return rsiOverbought;
    }

    public void setRsiOverbought(Integer rsiOverbought) {
        this.rsiOverbought = rsiOverbought;
    }

    public Integer getRsiOversold() {
        return rsiOversold;
    }

    public void setRsiOversold(Integer rsiOversold) {
        this.rsiOversold = rsiOversold;
    }
}
