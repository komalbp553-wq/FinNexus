package com.finnexus.domain.entity;

import com.finnexus.domain.enums.StrategyType;
import com.finnexus.domain.enums.Timeframe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "robo_strategies")
public class RoboStrategy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StrategyType type;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, length = 15)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Timeframe timeframe = Timeframe.M15;

    @Column
    private Integer fastPeriod;

    @Column
    private Integer slowPeriod;

    @Column
    private Integer rsiPeriod;

    @Column
    private Integer rsiOverbought;

    @Column
    private Integer rsiOversold;

    @Column(length = 10)
    private String lastSignal;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public StrategyType getType() {
        return type;
    }

    public void setType(StrategyType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
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

    public String getLastSignal() {
        return lastSignal;
    }

    public void setLastSignal(String lastSignal) {
        this.lastSignal = lastSignal;
    }
}
