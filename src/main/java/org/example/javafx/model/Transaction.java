package org.example.javafx.model;

import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Model representing a blockchain transaction
 * Using Lombok @Data for getters, setters, toString, equals, hashCode
 */
@Data
public class Transaction {
    private String hash;
    private String from;
    private String to;
    private BigDecimal value;
    private Long blockNumber;
    private Long timestamp;
    private String status;
    private BigDecimal gasUsed;
    private BigDecimal gasPrice;
    private String input; 
    
    /**
     * Method to determine transaction type using switch expression
     * Based on input data and function signatures
     */
    public String getTransactionType() {
        // contract creation
        if (to == null || to.isEmpty()) {
            return "Contract Creation";
        }
        
        // check if it's a simple ETH transfer
        if (input == null || input.isEmpty() || input.equals("0x") || input.equals("0x0")) {
            return "Transfer";
        }
        
        // cchecking input data for common function signatures using switch expression
        if (input != null && input.length() >= 10) {
            String functionSig = input.substring(0, 10).toLowerCase();
            
            return switch (functionSig) {
                // ERC20 and common token functions
                case "0xa9059cbb" -> "Transfer"; // transfer(address,uint256)
                case "0x23b872dd" -> "Transfer From"; // transferFrom(address,address,uint256)
                case "0x095ea7b3" -> "Approval"; // approve(address,uint256)
                case "0x40c10f19" -> "Mint"; // mint(address,uint256)
                case "0x42966c68" -> "Burn"; // burn(uint256)
                case "0x1249c58b" -> "Burn From"; // burnFrom(address,uint256)
                case "0x3a4b66f1" -> "Claim"; // claim()
                // DEX functions
                case "0x7ff36ab5", "0x38ed1739", "0x8803dbee" -> "Swap"; // swap functions
                case "0x02751cec", "0x2195995c" -> "Remove Liquidity"; // removeLiquidity functions
                case "0xe8e33700", "0xf305d719" -> "Add Liquidity"; // addLiquidity functions
                // Other functions
                case "0x2e1a7d4d" -> "Withdraw"; // withdraw(uint256)
                case "0xb6b55f25" -> "Deposit"; // deposit(uint256)
                case "0x379607f5" -> "Stake"; // stake/claimReward
                case "0x2e17de78" -> "Unstake"; // unstake(uint256)
                default -> {

                    // if value > 0 it's likely a transfer with contract interaction
                    if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                        yield "Contract Call";
                    }
                    yield "Contract Call";
                }
            };
        }
        
        // default is just contract interaction
        return "Contract Call";
    }
    
    public String getFormattedTime() {
        if (timestamp != null) {
            return formatRelativeTime(timestamp);
        }
        return "N/A";
    }
    
    private String formatRelativeTime(long timestamp) {
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;
        
        return switch ((int) (diff / 60)) {
            case 0 -> "Just now";
            default -> {
                if (diff < 3600) {
                    long minutes = diff / 60;
                    yield minutes + (minutes == 1 ? " minute ago" : " minutes ago");
                } else if (diff < 86400) {
                    long hours = diff / 3600;
                    yield hours + (hours == 1 ? " hour ago" : " hours ago");
                } else {
                    long days = diff / 86400;
                    yield days + (days == 1 ? " day ago" : " days ago");
                }
            }
        };
    }
    
    public String getFormattedValue() {
        if (value != null) {
            return value.divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP) + " ETH";
        }
        return "0 ETH";
    }
}
