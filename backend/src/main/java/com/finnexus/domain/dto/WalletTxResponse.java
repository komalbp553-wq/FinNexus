package com.finnexus.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class WalletTxResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private String note;
    private Instant createdAt;

    public WalletTxResponse(Long id, String type, BigDecimal amount, String note, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
