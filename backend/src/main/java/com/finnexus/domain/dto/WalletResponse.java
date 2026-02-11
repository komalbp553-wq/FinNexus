package com.finnexus.domain.dto;

import java.math.BigDecimal;

public class WalletResponse {
    private BigDecimal balance;
    private BigDecimal equity;
    private BigDecimal marginUsed;

    public WalletResponse(BigDecimal balance, BigDecimal equity, BigDecimal marginUsed) {
        this.balance = balance;
        this.equity = equity;
        this.marginUsed = marginUsed;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getEquity() {
        return equity;
    }

    public BigDecimal getMarginUsed() {
        return marginUsed;
    }
}
