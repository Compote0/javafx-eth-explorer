package org.example.javafx.exception;

/**
 * @title RateLimitException
 * @notice Exception for API rate limit errors (HTTP 429 - Too Many Requests)
 * @dev Extends ApiException and automatically sets status code to 429.
 *      This is the most specific exception in the hierarchy.
 * 
 * @description
 * RateLimitException is thrown when an API rate limit is exceeded.
 * For Etherscan API, the rate limit is 3 requests per second.
 * This exception allows for specific handling of rate limit scenarios,
 * such as implementing retry logic with exponential backoff.
 * 
 * @inheritance
 * Exception
 *     └── BlockchainException
 *         └── ApiException
 *             └── RateLimitException (this class)
 * 
 * @rateLimitInfo
 * - Etherscan API: Maximum 5 requests per second
 * - Status Code: Always 429 (Too Many Requests)
 * - Handling: Wait for rate limit delay before retrying
 * 
 * @usage
 * Use this exception when the API returns HTTP 429 status code,
 * indicating that too many requests have been made in a short period.
 * 
 * @example
 * // Detecting rate limit error
 * if (response.statusCode() == 429) {
 *     throw new RateLimitException("Rate limit exceeded");
 * }
 * 
 * // Handling rate limit with retry logic
 * try {
 *     makeRequest(url);
 * } catch (RateLimitException e) {
 *     // Wait for rate limit delay
 *     Thread.sleep(RATE_LIMIT_DELAY);
 *     // Retry the request
 *     makeRequest(url);
 * }
 * 
 * // Specific rate limit handling vs generic API errors
 * try {
 *     fetchBlockchainData();
 * } catch (RateLimitException e) {
 *     // Handle rate limit specifically - wait and retry
 *     handleRateLimit(e);
 * } catch (ApiException e) {
 *     // Handle other API errors
 *     handleApiError(e);
 * } catch (BlockchainException e) {
 *     // Handle other blockchain errors
 *     handleGenericError(e);
 * }
 * 
 * @see ApiException For other HTTP API errors
 * @see BlockchainException For generic blockchain errors
 * 
 */
public class RateLimitException extends ApiException {
    
    /**
     * @notice Creates a new RateLimitException with the specified message
     * @dev Automatically sets HTTP status code to 429 (Too Many Requests)
     * @param message The error message describing the rate limit error
     */
    public RateLimitException(String message) {
        super(message, 429);
    }
    
    /**
     * @notice Creates a new RateLimitException with message and cause
     * @dev Automatically sets HTTP status code to 429 (Too Many Requests)
     * @param message The error message describing the rate limit error
     * @param cause The underlying exception that caused this rate limit error
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, 429, cause);
    }
}
