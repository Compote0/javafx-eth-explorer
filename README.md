# Blockchain Explorer Dashboard

A JavaFX application for exploring blockchain data with real-time transaction feeds and search functionality.

## Features

- **Search Functionality**

- Search by wallet address (0x...)
- Search by transaction hash
- Search by block number

- **Dashboard**

- Latest block information
- Real-time transaction feed
- Blockchain statistics

- **Transaction Feed**
- View recent transactions
- Display transaction details (hash, from, to, value, block, time)

## Setup

1. **Get an Etherscan API Key** (optional but recommended for higher rate limits):

   - Visit https://etherscan.io/apis
   - Sign up for a free account
   - Get your API key
   - Update `BlockchainService.java` line 20: `private static final String API_KEY = "YourApiKeyToken";`

2. **Run the application**:
   ```bash
   ./gradlew run
   ```

## Usage

1. **Search for a transaction**: Enter a transaction hash (starts with 0x, 66 characters)
2. **Search for an address**: Enter a wallet address (starts with 0x, 42 characters)
3. **Search for a block**: Enter a block number (numeric)

The transaction feed automatically displays recent transactions from the blockchain.

## API

This application uses the Etherscan API:

- Free tier: 5 calls/second
- Public endpoints available without API key (limited)
- For production use, get a free API key from Etherscan

## Technologies

- JavaFX 21
- Java 17
- Gson for JSON parsing
- Java HTTP Client

## Patterns Implemented

### 1. **Lombok @Data Annotations**

- **Location**: `Transaction.java`, `Block.java`, `Address.java`
- **Pattern**: Using Lombok to reduce boilerplate code (getters, setters, toString, equals, hashCode)
- **Course Reference**: Similar to `TempBean.java`, `WindBean.java`, `WeatherBean.java` in the course

```java
@Data
public class Transaction {
    private String hash;
    // ... fields
    // lombok generates getters, setters, toString, equals, hashCode automatically
}
```

### 2. **Records**

- **Location**: `EthPrice.java`, `BlockchainStats.java`
- **Pattern**: Using Java records for immutable data structures
- **Course Reference**: Modern Java feature (Java 14+)

```java
public record EthPrice(
    BigDecimal price,
    BigDecimal priceChange24h,
    // ... other fields
) {
    // records automatically provide:
    // - Immutable fields
    // - Constructor
    // - Getters (fieldName() not getFieldName())
    // - toString, equals, hashCode
}
```

### 3. **Sealed Classes**

- **Location**: `ApiResponse.java`
- **Pattern**: Using sealed classes to restrict inheritance
- **Course Reference**: Modern Java feature (Java 17+)

```java
public sealed class ApiResponse
        permits ApiResponse.Success, ApiResponse.Error {

    public static final class Success extends ApiResponse { ... }
    public static final class Error extends ApiResponse { ... }
}
```

### 4. **Repository Pattern**

- **Location**: `BlockchainRepository.java` (interface), `EtherscanRepository.java` (implementation)
- **Pattern**: Abstracting data access behind an interface
- **Course Reference**: Similar to `WeatherRepository.java` in the course

```java
public interface BlockchainRepository {
    CompletableFuture<Long> getLatestBlockNumber() throws BlockchainException;
    // ... other methods
}

public class EtherscanRepository implements BlockchainRepository {
    // Implementation
}
```

### 5. **Custom Exceptions with Inheritance**

- **Location**: `exception/` package
- **Pattern**: Creating exception hierarchy using inheritance
- **Course Reference**: Standard Java exception handling pattern

```java
public class BlockchainException extends Exception { ... }
public class ApiException extends BlockchainException { ... }
public class RateLimitException extends ApiException { ... }
```

### 6. **Switch Expressions**

- **Location**: `Transaction.getTransactionType()`
- **Pattern**: Using modern switch expressions instead of if-else chains
- **Course Reference**: Modern Java feature (Java 14+)

```java
return switch (functionSig) {
    case "0xa9059cbb" -> "Transfer";
    case "0x23b872dd" -> "Transfer From";
    case "0x095ea7b3" -> "Approval";
    default -> "Contract Call";
};
```

### 7. **Thread-Safe Operations**

- **Location**: `EtherscanRepository.java`
- **Pattern**: Using `AtomicLong` and `synchronized` for thread-safe rate limiting
- **Course Reference**: Similar to thread patterns in `ExoThread.java`

```java
private final AtomicLong lastRequestTime = new AtomicLong(0);

private synchronized void enforceRateLimit() throws InterruptedException {
    // Thread-safe rate limiting
}
```

### 8. **Lambda Expressions**

- **Location**: Throughout the codebase
- **Pattern**: Using lambda expressions for async operations and callbacks
- **Course Reference**: Similar to `WeatherUI.java` using lambdas for button actions

```java
CompletableFuture.supplyAsync(() -> {
    // Async operation
    return result;
});
```

### 9. **CompletableFuture for Async Operations**

- **Location**: `BlockchainRepository` interface and `EtherscanRepository`
- **Pattern**: Using CompletableFuture for non-blocking API calls
- **Course Reference**: Modern Java concurrency patterns

```java
CompletableFuture<Long> getLatestBlockNumber() throws BlockchainException;
```

### 10. **var Keyword**

- **Location**: Throughout `EtherscanRepository.java`
- **Pattern**: Using `var` for local variable type inference
- **Course Reference**: Modern Java feature (Java 10+)

```java
var url = String.format(...);
var response = makeRequest(url);
var json = gson.fromJson(response, JsonObject.class);
```

### 11. **Text Blocks**

- **Location**: `EtherscanRepository.java` (URL construction)
- **Pattern**: Using String.format for multi-line strings (text blocks could be used)
- **Course Reference**: Modern Java feature (Java 15+)

### 12. **Service Layer Pattern**

- **Location**: `BlockchainService.java`
- **Pattern**: Service layer that wraps repository, provides synchronous wrappers
- **Course Reference**: Similar to how services wrap repositories in the course

## Architecture Comparison

### Course Architecture:

```
Controller → Service → Repository → API/Database
```

### Our Architecture:

```
Controller → Service → Repository (interface) → EtherscanRepository (implementation) → HTTP API
```

## Why These Patterns?

1. **Lombok**: Reduces boilerplate, matches course style
2. **Records**: Modern Java, perfect for immutable data
3. **Sealed Classes**: Type safety, restricts inheritance
4. **Repository Pattern**: Abstraction, testability, matches course pattern
5. **Custom Exceptions**: Better error handling, shows inheritance understanding
6. **Switch Expressions**: Modern Java, cleaner than if-else
7. **Thread-Safe**: Shows understanding of concurrency
8. **CompletableFuture**: Modern async programming
9. **var**: Modern Java, reduces verbosity
10. **Service Layer**: Matches course architecture

## Testing

```bash
./gradlew build
```

```bash
./gradlew run
```
