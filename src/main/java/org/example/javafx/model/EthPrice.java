package org.example.javafx.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Record representing Ethereum price information from CoinGecko API
 * Using record for immutable data structure
 */
public record EthPrice(
        BigDecimal price,
        BigDecimal priceChange24h,
        BigDecimal priceChangePercent24h,
        BigDecimal marketCap,
        BigDecimal volume24h
) {
    public String getFormattedPrice() {
        if (price != null) {
            return "$" + price.setScale(2, RoundingMode.HALF_UP);
        }
        return "$0.00";
    }
    
    public String getFormattedChange() {
        if (priceChangePercent24h != null) {
            String sign = priceChangePercent24h.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + priceChangePercent24h.setScale(2, RoundingMode.HALF_UP) + "%";
        }
        return "0.00%";
    }
}
