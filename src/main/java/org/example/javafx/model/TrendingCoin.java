package org.example.javafx.model;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Model representing a trending cryptocurrency from CoinGecko
 * Using Lombok @Data for getters, setters, toString, equals, hashCode
 */
@Data
public class TrendingCoin {
    private String id;
    private String name;
    private String symbol;
    private Integer marketCapRank;
    private BigDecimal price;
    private BigDecimal priceChange24h;
    private BigDecimal priceChangePercent24h;
    private BigDecimal marketCap;
    private BigDecimal volume24h;
    private String imageUrl;

    // method to get the correct formatted price with dollar symbol
    public String getFormattedPrice() {
        if (price != null) {
            if (price.compareTo(BigDecimal.ONE) < 0) {
                return "$" + price.setScale(4, java.math.RoundingMode.HALF_UP);
            }
            return "$" + price.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return "$0.00";
    }

    // method to get the formatted 24h price change with a + or - sign and two decimals
    public String getFormattedChange() {
        if (priceChangePercent24h != null) {
            String sign = priceChangePercent24h.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + priceChangePercent24h.setScale(2, java.math.RoundingMode.HALF_UP) + "%";
        }
        return "0.00%";
    }

    // method to get the correct market cap format
    public String getFormattedMarketCap() {
        if (marketCap != null) {
            if (marketCap.compareTo(BigDecimal.valueOf(1_000_000_000)) >= 0) {
                return "$" + marketCap.divide(BigDecimal.valueOf(1_000_000_000), 2, java.math.RoundingMode.HALF_UP) + "B";
            } else if (marketCap.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
                return "$" + marketCap.divide(BigDecimal.valueOf(1_000_000), 2, java.math.RoundingMode.HALF_UP) + "M";
            }
            return "$" + marketCap.setScale(0, java.math.RoundingMode.HALF_UP);
        }
        return "$0";
    }
}

