package com.finnexus.domain.dto;

public class RoboStrategyResponse {
    private Long id;
    private String type;
    private String symbol;
    private String timeframe;
    private boolean enabled;
    private Integer fastPeriod;
    private Integer slowPeriod;
    private Integer rsiPeriod;
    private Integer rsiOverbought;
    private Integer rsiOversold;
    private String lastSignal;

    public RoboStrategyResponse(Long id, String type, String symbol, String timeframe, boolean enabled,
                                Integer fastPeriod, Integer slowPeriod, Integer rsiPeriod,
                                Integer rsiOverbought, Integer rsiOversold, String lastSignal) {
        this.id = id;
        this.type = type;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.enabled = enabled;
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.rsiPeriod = rsiPeriod;
        this.rsiOverbought = rsiOverbought;
        this.rsiOversold = rsiOversold;
        this.lastSignal = lastSignal;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getFastPeriod() {
        return fastPeriod;
    }

    public Integer getSlowPeriod() {
        return slowPeriod;
    }

    public Integer getRsiPeriod() {
        return rsiPeriod;
    }

    public Integer getRsiOverbought() {
        return rsiOverbought;
    }

    public Integer getRsiOversold() {
        return rsiOversold;
    }

    public String getLastSignal() {
        return lastSignal;
    }
}
