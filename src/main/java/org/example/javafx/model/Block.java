package org.example.javafx.model;

import lombok.Data;
import java.util.List;

/**
 * Model representing a blockchain block
 * Using Lombok @Data for getters, setters, toString, equals, hashCode
 */
@Data
public class Block {
    private Long number;
    private String hash;
    private Long timestamp;
    private Integer transactionCount;
    private java.math.BigDecimal gasUsed;
    private java.math.BigDecimal gasLimit;
    private String miner;
    private List<Transaction> transactions;
    
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
}
