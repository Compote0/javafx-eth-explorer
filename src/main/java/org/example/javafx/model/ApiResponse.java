package org.example.javafx.model;

/**
 * Sealed class hierarchy for API responses
 * Using sealed classes to restrict inheritance
 */
public sealed class ApiResponse 
        permits ApiResponse.Success, ApiResponse.Error {
    
    /**
     * Success response with data
     */
    public static final class Success extends ApiResponse {
        private final String data;
        
        public Success(String data) {
            this.data = data;
        }
        
        public String getData() {
            return data;
        }
    }
    
    /**
     * Error response with message
     */
    public static final class Error extends ApiResponse {
        private final String message;
        private final String code;
        
        public Error(String message, String code) {
            this.message = message;
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getCode() {
            return code;
        }
    }
}

