package com.finvigil.transaction.dto;

import java.math.BigDecimal;

public class VelocityStats {

    private final int count;
    private final BigDecimal totalAmount;

    public VelocityStats(int count, BigDecimal totalAmount) {
        this.count = count;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public int getCount() {
        return count;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
