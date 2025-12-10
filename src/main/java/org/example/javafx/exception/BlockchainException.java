package org.example.javafx.exception;

/**
 * @title BlockchainException
 * @notice Base exception class for all blockchain-related errors
 * @dev This is the root exception in the blockchain exception hierarchy.
 *      All blockchain-specific exceptions extend from this class.
 * 
 * @description
 * BlockchainException serves as the foundation for the exception hierarchy,
 * providing a common base for all blockchain-related error handling.
 * This demonstrates the inheritance pattern where more specific exceptions
 * (ApiException, RateLimitException) extend from this base class.
 * 
 * @inheritance
 * Exception (Java standard)
 *     └── BlockchainException (this class)
 *         └── ApiException
 *             └── RateLimitException
 * 
 * @usage
 * Use this exception for generic blockchain errors that don't fit into
 * more specific categories (API errors, rate limits, etc.).
 * 
 * @example
 * // Generic blockchain error
 * throw new BlockchainException("Invalid blockchain configuration");
 * 
 * // With cause (wrapping another exception)
 * throw new BlockchainException("Failed to load config", ioException);
 * 
 * // Catching all blockchain exceptions
 * try {
 *     blockchainService.getLatestBlock();
 * } catch (BlockchainException e) {
 *     // Catches BlockchainException, ApiException, AND RateLimitException
 *     logger.error("Blockchain error: " + e.getMessage());
 * }
 * 
 */
public class BlockchainException extends Exception {
    
    /**
     * @notice Creates a new BlockchainException with the specified message
     * @param message The error message describing what went wrong
     */
    public BlockchainException(String message) {
        super(message);
    }
    
    /**
     * @notice Creates a new BlockchainException with the specified message and cause
     * @param message The error message describing what went wrong
     * @param cause The underlying exception that caused this error
     */
    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
    }
}
