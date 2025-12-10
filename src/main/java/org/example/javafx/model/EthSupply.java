package org.example.javafx.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Record representing Ethereum supply information from CoinGecko API
 * Using record for immutable data structure
 */
public record EthSupply(
        BigDecimal circulatingSupply,
        BigDecimal totalSupply,
        BigDecimal maxSupply
) {
    private static final DecimalFormat df = new DecimalFormat("#,###");
    
    /**
     * Get formatted circulating supply
     */
    public String getFormattedCirculatingSupply() {
        if (circulatingSupply != null) {
            return df.format(circulatingSupply) + " ETH";
        }
        return "N/A";
    }
    
    /**
     * Get formatted total supply
     */
    public String getFormattedTotalSupply() {
        if (totalSupply != null) {
            return df.format(totalSupply) + " ETH";
        }
        return "N/A";
    }
    
    /**
     * Get formatted max supply (or "Unlimited" if null)
     */
    public String getFormattedMaxSupply() {
        if (maxSupply != null) {
            return df.format(maxSupply) + " ETH";
        }
        return "Unlimited";
    }
    
    /**
     * Get short formatted supply (for display in card)
     */
    public String getFormattedShortSupply() {
        if (circulatingSupply != null) {
            var billions = circulatingSupply.divide(BigDecimal.valueOf(1_000_000_000), 2, java.math.RoundingMode.HALF_UP);
            return billions + "B ETH";
        }
        return "N/A";
    }
}

