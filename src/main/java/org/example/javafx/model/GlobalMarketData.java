package org.example.javafx.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Record representing global cryptocurrency market data from CoinGecko
 * Using record for immutable data structure
 */
public record GlobalMarketData(
        BigDecimal totalMarketCap,
        BigDecimal totalVolume24h,
        BigDecimal marketCapChangePercent24h,
        Integer activeCryptocurrencies,
        Integer markets
) {
    private static final DecimalFormat df = new DecimalFormat("#,###.##");
    
    public String getFormattedTotalMarketCap() {
        if (totalMarketCap != null) {
            var trillions = totalMarketCap.divide(BigDecimal.valueOf(1_000_000_000_000L), 2, java.math.RoundingMode.HALF_UP);
            return "$" + trillions + "T";
        }
        return "N/A";
    }
    
    public String getFormattedTotalVolume() {
        if (totalVolume24h != null) {
            var billions = totalVolume24h.divide(BigDecimal.valueOf(1_000_000_000L), 2, java.math.RoundingMode.HALF_UP);
            return "$" + billions + "B";
        }
        return "N/A";
    }
    
    public String getFormattedMarketCapChange() {
        if (marketCapChangePercent24h != null) {
            String sign = marketCapChangePercent24h.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + marketCapChangePercent24h.setScale(2, java.math.RoundingMode.HALF_UP) + "%";
        }
        return "0.00%";
    }
}

