package com.finnexus.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class MarketMath {
    private MarketMath() {}

    public static BigDecimal sma(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = values.size() - period; i < values.size(); i++) {
            sum = sum.add(values.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
    }

    public static BigDecimal rsi(List<BigDecimal> values, int period) {
        if (values.size() <= period) {
            return BigDecimal.ZERO;
        }
        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;
        for (int i = values.size() - period; i < values.size(); i++) {
            BigDecimal change = values.get(i).subtract(values.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gain = gain.add(change);
            } else {
                loss = loss.add(change.abs());
            }
        }
        if (loss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal rs = gain.divide(loss, 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 6, RoundingMode.HALF_UP));
    }
}
