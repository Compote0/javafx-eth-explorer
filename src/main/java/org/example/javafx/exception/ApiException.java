package org.example.javafx.exception;

/**
 * @title ApiException
 * @notice Exception for API-related errors with HTTP status codes
 * @dev Extends BlockchainException to provide more specific error handling
 *      for HTTP/API communication issues.
 * 
 * @description
 * ApiException is used for errors that occur during API communication,
 * such as HTTP errors (404, 500, etc.). It extends BlockchainException
 * to maintain the exception hierarchy while adding HTTP status code
 * information for more precise error handling.
 * 
 * @inheritance
 * Exception
 *     └── BlockchainException
 *         └── ApiException (this class)
 *             └── RateLimitException
 * 
 * @features
 * - Stores HTTP status code (e.g., 400, 404, 500)
 * - Provides getStatusCode() method for error handling
 * - Maintains exception chain with cause
 * 
 * @usage
 * Use this exception when API calls fail with HTTP error status codes.
 * For rate limit errors specifically, use RateLimitException instead.
 * 
 * @example
 * // HTTP 404 error
 * if (response.statusCode() == 404) {
 *     throw new ApiException("Resource not found", 404);
 * }
 * 
 * // HTTP 500 error with cause
 * if (response.statusCode() == 500) {
 *     throw new ApiException("Server error", 500, serverException);
 * }
 * 
 * // Catching API errors
 * try {
 *     makeRequest(url);
 * } catch (ApiException e) {
 *     int statusCode = e.getStatusCode();
 *     if (statusCode == 404) {
 *         // Handle not found
 *     } else if (statusCode == 500) {
 *         // Handle server error
 *     }
 * }
 * 
 * @see RateLimitException For rate limit specific errors (HTTP 429)
 * @see BlockchainException For generic blockchain errors
 * 
 */
public class ApiException extends BlockchainException {
    
    /**
     * @notice The HTTP status code associated with this API error
     * @dev Common status codes:
     *      - 400: Bad Request
     *      - 401: Unauthorized
     *      - 404: Not Found
     *      - 429: Too Many Requests (use RateLimitException instead)
     *      - 500: Internal Server Error
     */
    private final int statusCode;
    
    /**
     * @notice Creates a new ApiException with the specified message and status code
     * @param message The error message describing the API error
     * @param statusCode The HTTP status code (e.g., 404, 500)
     */
    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    /**
     * @notice Creates a new ApiException with message, status code, and cause
     * @param message The error message describing the API error
     * @param statusCode The HTTP status code (e.g., 404, 500)
     * @param cause The underlying exception that caused this API error
     */
    public ApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    /**
     * @notice Gets the HTTP status code associated with this API error
     * @return The HTTP status code (e.g., 404, 500, 429)
     */
    public int getStatusCode() {
        return statusCode;
    }
}
