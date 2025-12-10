package org.example.javafx.model;

import java.math.BigDecimal;

/**
 * Record representing blockchain statistics
 * Using record for immutable data structure
 */
public record BlockchainStats(
        Long latestBlock,
        BigDecimal totalTransactions,
        BigDecimal averageGasPrice,
        BigDecimal totalValueTransferred
) {
}
