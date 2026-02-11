package com.finnexus.domain.dto;

import java.math.BigDecimal;

public class PnlResponse {
    private BigDecimal openPnl;
    private BigDecimal closedPnl;
    private BigDecimal dailyPnl;
    private BigDecimal weeklyPnl;
    private BigDecimal roi;
    private BigDecimal accuracy;

    public PnlResponse(BigDecimal openPnl, BigDecimal closedPnl, BigDecimal dailyPnl, BigDecimal weeklyPnl,
                       BigDecimal roi, BigDecimal accuracy) {
        this.openPnl = openPnl;
        this.closedPnl = closedPnl;
        this.dailyPnl = dailyPnl;
        this.weeklyPnl = weeklyPnl;
        this.roi = roi;
        this.accuracy = accuracy;
    }

    public BigDecimal getOpenPnl() {
        return openPnl;
    }

    public BigDecimal getClosedPnl() {
        return closedPnl;
    }

    public BigDecimal getDailyPnl() {
        return dailyPnl;
    }

    public BigDecimal getWeeklyPnl() {
        return weeklyPnl;
    }

    public BigDecimal getRoi() {
        return roi;
    }

    public BigDecimal getAccuracy() {
        return accuracy;
    }
}
