package org.example.javafx.model;

import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Model representing a blockchain address
 * Using Lombok @Data for getters, setters, toString, equals, hashCode
 */
@Data
public class Address {
    private String address;
    private BigDecimal balance;
    private Integer transactionCount;
    
    public String getFormattedBalance() {
        if (balance != null) {
            return balance.divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP) + " ETH";
        }
        return "0 ETH";
    }
}
