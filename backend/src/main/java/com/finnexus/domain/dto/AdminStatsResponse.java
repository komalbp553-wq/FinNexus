package com.finnexus.domain.dto;

public class AdminStatsResponse {
    private long totalUsers;
    private long totalTrades;
    private long totalOrders;
    private long openTrades;

    public AdminStatsResponse(long totalUsers, long totalTrades, long totalOrders, long openTrades) {
        this.totalUsers = totalUsers;
        this.totalTrades = totalTrades;
        this.totalOrders = totalOrders;
        this.openTrades = openTrades;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalTrades() {
        return totalTrades;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getOpenTrades() {
        return openTrades;
    }
}
