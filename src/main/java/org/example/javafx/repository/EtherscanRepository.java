package org.example.javafx.repository;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.javafx.exception.ApiException;
import org.example.javafx.exception.BlockchainException;
import org.example.javafx.exception.RateLimitException;
import org.example.javafx.model.Address;
import org.example.javafx.model.Block;
import org.example.javafx.model.EthPrice;
import org.example.javafx.model.EthSupply;
import org.example.javafx.model.GlobalMarketData;
import org.example.javafx.model.TrendingCoin;
import org.example.javafx.model.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository implementation for Etherscan API
 * Implements BlockchainRepository interface
 * Thread-safe with synchronized rate limiting
 * Similar pattern to WeatherRepository in the course
 */
public class EtherscanRepository implements BlockchainRepository {
    
    // API Configuration - loaded from config.properties
    private final String ETHERSCAN_API_URL;
    private final String API_KEY;
    private final String CHAIN_ID;
    private final String COINGECKO_API_URL;
    private final String COINGECKO_API_KEY;
    private final long RATE_LIMIT_DELAY;
    private final long INITIAL_DELAY;
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    // thread-safe rate limiting using AtomicLong
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    
    public EtherscanRepository() throws BlockchainException {
        // load configuration from config.properties
        var config = loadConfig();
        
        // initialize API configuration with fallback defaults
        this.ETHERSCAN_API_URL = config.getProperty("etherscan.api.url", "https://api.etherscan.io/v2/api");
        this.API_KEY = config.getProperty("etherscan.api.key", "");
        this.CHAIN_ID = config.getProperty("etherscan.chain.id", "1");
        this.COINGECKO_API_URL = config.getProperty("coingecko.api.url", "https://api.coingecko.com/api/v3");
        this.COINGECKO_API_KEY = config.getProperty("coingecko.api.key", "");
        this.RATE_LIMIT_DELAY = Long.parseLong(config.getProperty("api.rate.limit.delay", "400"));
        this.INITIAL_DELAY = Long.parseLong(config.getProperty("api.initial.delay", "100"));
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        
        // warn if API key is missing
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("[EtherscanRepository] WARNING: Etherscan API key is not configured in config.properties!");
            System.err.println("[EtherscanRepository] Please add your API key to src/main/resources/config.properties");
        }
    }
    
    /**
     * Loads configuration from config.properties file
     * Falls back to defaults if file is not found
     */
    private Properties loadConfig() throws BlockchainException {
        var config = new Properties();
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (inputStream != null) {
                config.load(inputStream);
                System.out.println("[EtherscanRepository] Configuration loaded from config.properties");
                inputStream.close();
            } else {
                System.err.println("[EtherscanRepository] WARNING: config.properties not found, using default values");
            }
        } catch (IOException e) {
            throw new BlockchainException("Error loading config.properties: " + e.getMessage(), e);
        }
        return config;
    }
    
    /**
     * Safely parse a JSON value that might be a number or formatted string
     * Handles cases where API returns formatted strings like "$39,362,461"
     */
    private BigDecimal safeParseBigDecimal(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            if (element.isJsonPrimitive()) {
                var primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return BigDecimal.valueOf(primitive.getAsDouble());
                } else if (primitive.isString()) {
                    // remove formatting characters ($, ",", etc.)
                    var cleaned = primitive.getAsString()
                            .replace("$", "")
                            .replace(",", "")
                            .replace(" ", "")
                            .trim();
                    if (cleaned.isEmpty() || cleaned.equals("null")) {
                        return null;
                    }
                    return new BigDecimal(cleaned);
                }
            }
        } catch (Exception e) {
            System.err.println("[EtherscanRepository] Error parsing number: " + element + " - " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Thread-safe rate limiting
     * Ensures minimum delay between API calls
     */
    private synchronized void enforceRateLimit() throws InterruptedException {
        var now = System.currentTimeMillis();
        var lastTime = lastRequestTime.get();
        var timeSinceLastRequest = now - lastTime;
        
        if (timeSinceLastRequest < RATE_LIMIT_DELAY) {
            var sleepTime = RATE_LIMIT_DELAY - timeSinceLastRequest;
            Thread.sleep(sleepTime);
        }
        
        lastRequestTime.set(System.currentTimeMillis());
    }
    
    /**
     * Makes HTTP request with error handling and retry logic for rate limits
     * Uses text blocks for URL construction
     * @param url The API URL to request
     * @param maxRetries Maximum number of retries for rate limit errors (default: 3)
     * @return The response body as a string
     * @throws BlockchainException if the request fails after all retries
     */
    private String makeRequest(String url) throws BlockchainException {
        return makeRequest(url, 3);
    }
    
    /**
     * Makes HTTP request with retry logic for rate limit errors
     * @param url The API URL to request
     * @param maxRetries Maximum number of retries for rate limit errors
     * @return The response body as a string
     * @throws BlockchainException if the request fails after all retries
     */
    private String makeRequest(String url, int maxRetries) throws BlockchainException {
        int retryCount = 0;
        long baseWaitTime = 1000; // starting with 1 second wait
        
        while (retryCount <= maxRetries) {
            try {
                enforceRateLimit();
                
                System.out.println("[EtherscanRepository] Making HTTP request to: " + url + (retryCount > 0 ? " (retry " + retryCount + ")" : ""));
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("[EtherscanRepository] HTTP Status Code: " + response.statusCode());
                
                if (response.statusCode() == 429) {
                    throw new RateLimitException("Rate limit exceeded (HTTP 429)");
                }
                
                if (response.statusCode() != 200) {
                    throw new ApiException("HTTP Error: " + response.statusCode(), response.statusCode());
                }
                
                var body = response.body();
                
                if (body == null) {
                    throw new BlockchainException("Empty response from API");
                }
                
                // checking for API errors in the response
                if (body.contains("\"status\":\"0\"") || body.contains("\"status\":0")) {
                    var json = gson.fromJson(body, JsonObject.class);
                    var message = json.has("message") ? json.get("message").getAsString() : "API returned error";
                    var result = json.has("result") ? json.get("result").getAsString() : "";
                    
                    // checking if it's a rate limit error
                    if (message.toLowerCase().contains("rate limit") || 
                        message.toLowerCase().contains("max calls per sec") ||
                        result.toLowerCase().contains("rate limit")) {

                        // if this is a rate limit error, we retry after waiting
                        if (retryCount < maxRetries) {
                            // the wait time doubles with each retry to reduce repeated failed requests
                            long waitTime = baseWaitTime * (long) Math.pow(2, retryCount);
                            System.err.println("[EtherscanRepository] Rate limit detected: " + message + ". Waiting " + waitTime + "ms before retry " + (retryCount + 1) + "/" + maxRetries);


                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new BlockchainException("Retry interrupted", e);
                            }
                            retryCount++;
                            // retry the request
                            continue;
                        } else {
                            throw new RateLimitException("Rate limit exceeded after " + maxRetries + " retries: " + message);
                        }
                    }
                    
                    // do not throw for "No transactions found" bc that's a valid response
                    if (!message.contains("No transactions found") && 
                        !message.contains("No record found") && 
                        !message.equals("OK")) {
                        throw new ApiException("API Error: " + message + (result.isEmpty() ? "" : " - " + result), 400);
                    }
                }
                
                // checking for JSON-RPC errors
                if (body.contains("\"error\"")) {
                    var json = gson.fromJson(body, JsonObject.class);
                    if (json.has("error")) {
                        var error = json.getAsJsonObject("error");
                        var message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                        
                        // checking if it's a rate limit error
                        if (message.toLowerCase().contains("rate limit") || 
                            message.toLowerCase().contains("max calls per sec")) {

                            if (retryCount < maxRetries) {
                                long waitTime = baseWaitTime * (long) Math.pow(2, retryCount);
                                System.err.println("[EtherscanRepository] Rate limit detected: " + message + ". Waiting " + waitTime + "ms before retry " + (retryCount + 1) + "/" + maxRetries);
                                try {
                                    Thread.sleep(waitTime);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new BlockchainException("Retry interrupted", e);
                                }
                                retryCount++;
                                continue;
                            } else {
                                throw new RateLimitException("Rate limit exceeded after " + maxRetries + " retries: " + message);
                            }
                        }
                        
                        throw new ApiException("JSON-RPC Error: " + message, 400);
                    }
                }
                
                return body;
                
            } catch (RateLimitException e) {
                // handle rate limit exception
                if (retryCount < maxRetries) {
                    long waitTime = baseWaitTime * (long) Math.pow(2, retryCount);
                    System.err.println("[EtherscanRepository] Rate limit exception: " + e.getMessage() + ". Waiting " + waitTime + "ms before retry " + (retryCount + 1) + "/" + maxRetries);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BlockchainException("Retry interrupted", ie);
                    }
                    retryCount++;
                    continue;
                } else {

                    // rethrow if max retries is reached
                    throw e;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BlockchainException("Request interrupted", e);
            } catch (IOException e) {
                throw new BlockchainException("IO Error: " + e.getMessage(), e);
            }
        }
        
        // just in case but theoretically will not reach this case
        throw new BlockchainException("Request failed after " + maxRetries + " retries");
    }
    
    @Override
    public CompletableFuture<Long> getLatestBlockNumber() throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(INITIAL_DELAY);
                var url = String.format(
                    "%s?module=proxy&action=eth_blockNumber&chainid=%s&apikey=%s",
                    ETHERSCAN_API_URL, CHAIN_ID, API_KEY
                );
                var response = makeRequest(url);
                var json = gson.fromJson(response, JsonObject.class);
                
                if (json.has("result")) {
                    var hex = json.get("result").getAsString();
                    if (hex != null && hex.startsWith("0x") && hex.length() > 2) {
                        return Long.parseLong(hex.substring(2), 16);
                    }
                }
                throw new BlockchainException("Invalid API response");
            } catch (BlockchainException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(new BlockchainException("Request interrupted", e));
            }
        });
    }
    
    @Override
    public CompletableFuture<Block> getBlockByNumber(Long blockNumber) throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var hexBlock = "0x" + Long.toHexString(blockNumber);
                var url = String.format(
                    "%s?module=proxy&action=eth_getBlockByNumber&chainid=%s&tag=%s&boolean=true&apikey=%s",
                    ETHERSCAN_API_URL, CHAIN_ID, hexBlock, API_KEY
                );
                var response = makeRequest(url);
                var json = gson.fromJson(response, JsonObject.class);
                
                if (json.has("result") && !json.get("result").isJsonNull()) {
                    var result = json.getAsJsonObject("result");
                    var block = new Block();
                    block.setNumber(blockNumber);
                    block.setHash(result.has("hash") ? result.get("hash").getAsString() : null);
                    
                    if (result.has("timestamp")) {
                        var timestampHex = result.get("timestamp").getAsString();
                        if (timestampHex != null && timestampHex.startsWith("0x")) {
                            try {
                                block.setTimestamp(Long.parseLong(timestampHex.substring(2), 16));
                            } catch (NumberFormatException e) {
                                // Ignore invalid timestamp
                            }
                        }
                    }
                    
                    if (result.has("gasUsed")) {
                        var gasUsedHex = result.get("gasUsed").getAsString().substring(2);
                        block.setGasUsed(new BigDecimal(new java.math.BigInteger(gasUsedHex, 16)));
                    }
                    if (result.has("gasLimit")) {
                        var gasLimitHex = result.get("gasLimit").getAsString().substring(2);
                        block.setGasLimit(new BigDecimal(new java.math.BigInteger(gasLimitHex, 16)));
                    }
                    block.setMiner(result.has("miner") ? result.get("miner").getAsString() : null);
                    
                    if (result.has("transactions")) {
                        var txs = result.getAsJsonArray("transactions");
                        block.setTransactionCount(txs.size());
                    }
                    
                    return block;
                }
                return null;
            } catch (BlockchainException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Block>> getLatestBlocks(int count) throws BlockchainException {
        return getLatestBlockNumber()
            .thenCompose(latestBlock -> {
                var blocks = new ArrayList<Block>();
                // limit to avoid rate limiting :
                var actualCount = Math.min(count, 10);
                
                var futures = new ArrayList<CompletableFuture<Block>>();
                for (long i = latestBlock; i > latestBlock - actualCount && i > 0; i--) {
                    try {
                        futures.add(getBlockByNumber(i));
                    } catch (BlockchainException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        futures.forEach(f -> {
                            try {
                                var block = f.get();
                                if (block != null) {
                                    blocks.add(block);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return blocks;
                    });
            });
    }
    
    @Override
    public CompletableFuture<Transaction> getTransactionByHash(String hash) throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = String.format(
                    "%s?module=proxy&action=eth_getTransactionByHash&chainid=%s&txhash=%s&apikey=%s",
                    ETHERSCAN_API_URL, CHAIN_ID, hash, API_KEY
                );
                var response = makeRequest(url);
                var json = gson.fromJson(response, JsonObject.class);
                
                if (json.has("result") && !json.get("result").isJsonNull()) {
                    var result = json.getAsJsonObject("result");
                    var tx = new Transaction();
                    tx.setHash(result.has("hash") ? result.get("hash").getAsString() : null);
                    tx.setFrom(result.has("from") ? result.get("from").getAsString() : null);
                    tx.setTo(result.has("to") ? result.get("to").getAsString() : null);
                    
                    Long blockNumber = null;
                    if (result.has("blockNumber")) {
                        var blockHex = result.get("blockNumber").getAsString();
                        if (blockHex != null && blockHex.startsWith("0x")) {
                            try {
                                blockNumber = Long.parseLong(blockHex.substring(2), 16);
                                tx.setBlockNumber(blockNumber);
                            } catch (NumberFormatException e) {
                                // Ignore invalid block number
                            }
                        }
                    }
                    
                    if (result.has("value")) {
                        var valueHex = result.get("value").getAsString();
                        if (valueHex != null && valueHex.startsWith("0x")) {
                            try {
                                tx.setValue(new BigDecimal(new java.math.BigInteger(valueHex.substring(2), 16)));
                            } catch (NumberFormatException e) {
                                // Ignore invalid value
                            }
                        }
                    }
                    
                    if (result.has("input")) {
                        tx.setInput(result.get("input").getAsString());
                    }
                    if (result.has("gas")) {
                        var gasHex = result.get("gas").getAsString();
                        if (gasHex != null && gasHex.startsWith("0x")) {
                            try {
                                tx.setGasUsed(new BigDecimal(new java.math.BigInteger(gasHex.substring(2), 16)));
                            } catch (NumberFormatException e) {
                                // Ignore invalid gas
                            }
                        }
                    }
                    if (result.has("gasPrice")) {
                        var gasPriceHex = result.get("gasPrice").getAsString();
                        if (gasPriceHex != null && gasPriceHex.startsWith("0x")) {
                            try {
                                tx.setGasPrice(new BigDecimal(new java.math.BigInteger(gasPriceHex.substring(2), 16)));
                            } catch (NumberFormatException e) {
                                // Ignore invalid gas price
                            }
                        }
                    }
                    
                    // Get timestamp from block if block number is available
                    if (blockNumber != null) {
                        try {
                            var blockFuture = getBlockByNumber(blockNumber);
                            var block = blockFuture.get();
                            if (block != null && block.getTimestamp() != null) {
                                tx.setTimestamp(block.getTimestamp());
                            }
                        } catch (Exception e) {
                            // Continue without timestamp
                        }
                    }
                    
                    return tx;
                }
                return null;
            } catch (BlockchainException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(new BlockchainException("Error fetching transaction", e));
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Transaction>> getLatestTransactions(int count) throws BlockchainException {
        // Use a well-known address to get recent transactions as a feed
        var feedAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7"; // USDT contract (very active)
        return getAddressTransactions(feedAddress, count);
    }
    
    @Override
    public CompletableFuture<Address> getAddressDetails(String address) throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = String.format(
                    "%s?module=account&action=balance&chainid=%s&address=%s&tag=latest&apikey=%s",
                    ETHERSCAN_API_URL, CHAIN_ID, address, API_KEY
                );
                var response = makeRequest(url);
                var json = gson.fromJson(response, JsonObject.class);
                
                var addr = new Address();
                addr.setAddress(address);
                
                if (json.has("result")) {
                    var balanceStr = json.get("result").getAsString();
                    try {
                        addr.setBalance(new BigDecimal(balanceStr));
                    } catch (NumberFormatException e) {
                        throw new BlockchainException("Invalid balance response: " + balanceStr);
                    }
                }
                
                // Get transaction count
                try {
                    var txCountUrl = String.format(
                        "%s?module=proxy&action=eth_getTransactionCount&chainid=%s&address=%s&tag=latest&apikey=%s",
                        ETHERSCAN_API_URL, CHAIN_ID, address, API_KEY
                    );
                    var txCountResponse = makeRequest(txCountUrl);
                    var txCountJson = gson.fromJson(txCountResponse, JsonObject.class);
                    
                    if (txCountJson.has("result")) {
                        var hex = txCountJson.get("result").getAsString();
                        if (hex != null && hex.startsWith("0x")) {
                            try {
                                addr.setTransactionCount(Integer.parseInt(hex.substring(2), 16));
                            } catch (NumberFormatException e) {
                                // Ignore invalid tx count
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore errors getting tx count
                }
                
                return addr;
            } catch (BlockchainException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Transaction>> getAddressTransactions(String address, int count) throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = String.format(
                    "%s?module=account&action=txlist&chainid=%s&address=%s&startblock=0&endblock=99999999&page=1&offset=%d&sort=desc&apikey=%s",
                    ETHERSCAN_API_URL, CHAIN_ID, address, count, API_KEY
                );
                var response = makeRequest(url);
                var json = gson.fromJson(response, JsonObject.class);
                
                var transactions = new ArrayList<Transaction>();
                if (json.has("result")) {
                    if (json.get("result").isJsonArray()) {
                        var results = json.getAsJsonArray("result");
                        for (int i = 0; i < Math.min(results.size(), count); i++) {
                            var txJson = results.get(i).getAsJsonObject();
                            var tx = new Transaction();
                            tx.setHash(txJson.has("hash") ? txJson.get("hash").getAsString() : null);
                            tx.setFrom(txJson.has("from") ? txJson.get("from").getAsString() : null);
                            tx.setTo(txJson.has("to") ? txJson.get("to").getAsString() : null);
                            tx.setValue(txJson.has("value") ? new BigDecimal(txJson.get("value").getAsString()) : null);
                            tx.setBlockNumber(txJson.has("blockNumber") ? Long.parseLong(txJson.get("blockNumber").getAsString()) : null);
                            tx.setTimestamp(txJson.has("timeStamp") ? Long.parseLong(txJson.get("timeStamp").getAsString()) : null);
                            tx.setGasUsed(txJson.has("gasUsed") ? new BigDecimal(txJson.get("gasUsed").getAsString()) : null);
                            tx.setGasPrice(txJson.has("gasPrice") ? new BigDecimal(txJson.get("gasPrice").getAsString()) : null);
                            tx.setInput(txJson.has("input") ? txJson.get("input").getAsString() : (txJson.has("methodId") ? txJson.get("methodId").getAsString() : null));
                            transactions.add(tx);
                        }
                    }
                }
                return transactions;
            } catch (BlockchainException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<EthPrice> getEthPrice() throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = COINGECKO_API_URL + "/simple/price?ids=ethereum&vs_currencies=usd&include_24hr_change=true&include_market_cap=true&include_24hr_vol=true";
                if (!COINGECKO_API_KEY.isEmpty()) {
                    url += "&x_cg_demo_api_key=" + COINGECKO_API_KEY;
                }
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET();
                
                // add API key as header
                if (!COINGECKO_API_KEY.isEmpty()) {
                    requestBuilder.header("x-cg-demo-api-key", COINGECKO_API_KEY);
                }
                
                var request = requestBuilder.build();
                
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var body = response.body();
                var json = gson.fromJson(body, JsonObject.class);
                
                if (json.has("ethereum")) {
                    var eth = json.getAsJsonObject("ethereum");
                    var price = eth.has("usd") ? BigDecimal.valueOf(eth.get("usd").getAsDouble()) : BigDecimal.ZERO;
                    var priceChangePercent24h = eth.has("usd_24h_change") ? BigDecimal.valueOf(eth.get("usd_24h_change").getAsDouble()) : BigDecimal.ZERO;

                    // calculate priceChange24h from percentage
                    var priceChange24h = priceChangePercent24h.multiply(price).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    var marketCap = eth.has("usd_market_cap") ? BigDecimal.valueOf(eth.get("usd_market_cap").getAsDouble()) : BigDecimal.ZERO;
                    var volume24h = eth.has("usd_24h_vol") ? BigDecimal.valueOf(eth.get("usd_24h_vol").getAsDouble()) : BigDecimal.ZERO;
                    
                    return new EthPrice(price, priceChange24h, priceChangePercent24h, marketCap, volume24h);
                }
                throw new BlockchainException("Invalid CoinGecko response");
            } catch (Exception e) {
                throw new RuntimeException(new BlockchainException("Error fetching ETH price", e));
            }
        });
    }
    
    @Override
    public CompletableFuture<Double> calculateTPS() throws BlockchainException {
        return getLatestBlockNumber()
            .thenCompose(latestBlock -> {
                try {
                    Thread.sleep(INITIAL_DELAY * 5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return CompletableFuture.completedFuture(0.0);
                }
                try {
                    return getBlockByNumber(latestBlock)
                        .thenApply(block -> {
                            if (block == null || block.getTransactionCount() == null) {
                                System.err.println("[EtherscanRepository] Block is null or has no transaction count");
                                return 0.0;
                            }
                            // average block time on Ethereum is ~12 seconds
                            var tps = block.getTransactionCount().doubleValue() / 12.0;
                            System.out.println("[EtherscanRepository] Calculated TPS: " + tps + " (from " + block.getTransactionCount() + " transactions)");
                            return tps;
                        })
                        .exceptionally(throwable -> {
                            System.err.println("[EtherscanRepository] Error calculating TPS: " + throwable.getMessage());
                            throwable.printStackTrace();
                            return 0.0;
                        });
                } catch (BlockchainException e) {
                    System.err.println("[EtherscanRepository] BlockchainException in calculateTPS: " + e.getMessage());
                    e.printStackTrace();
                    return CompletableFuture.completedFuture(0.0);
                }
            })
            .exceptionally(throwable -> {
                System.err.println("[EtherscanRepository] Error getting latest block for TPS: " + throwable.getMessage());
                throwable.printStackTrace();
                return 0.0;
            });
    }
    
    @Override
    public CompletableFuture<EthSupply> getEthSupply() throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // using CoinGecko coins endpoint for detailed supply information
                var url = COINGECKO_API_URL + "/coins/ethereum";
                if (!COINGECKO_API_KEY.isEmpty()) {
                    url += "?x_cg_demo_api_key=" + COINGECKO_API_KEY;
                }
                
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET();
                
                // add API key as header
                if (!COINGECKO_API_KEY.isEmpty()) {
                    requestBuilder.header("x-cg-demo-api-key", COINGECKO_API_KEY);
                }
                
                var request = requestBuilder.build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var body = response.body();
                var json = gson.fromJson(body, JsonObject.class);
                
                if (json.has("market_data")) {
                    var marketData = json.getAsJsonObject("market_data");
                    
                    var circulatingSupply = marketData.has("circulating_supply") 
                        ? BigDecimal.valueOf(marketData.get("circulating_supply").getAsDouble())
                        : null;
                    
                    var totalSupply = marketData.has("total_supply") 
                        ? BigDecimal.valueOf(marketData.get("total_supply").getAsDouble())
                        : null;
                    
                    var maxSupply = marketData.has("max_supply") && !marketData.get("max_supply").isJsonNull()
                        ? BigDecimal.valueOf(marketData.get("max_supply").getAsDouble())
                        : null;
                    
                    return new EthSupply(circulatingSupply, totalSupply, maxSupply);
                }
                throw new BlockchainException("Invalid CoinGecko supply response");
            } catch (Exception e) {
                throw new RuntimeException(new BlockchainException("Error fetching ETH supply", e));
            }
            });
    }
    
    @Override
    public CompletableFuture<List<TrendingCoin>> getTrendingCoins() throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = COINGECKO_API_URL + "/search/trending";
                if (!COINGECKO_API_KEY.isEmpty()) {
                    url += "?x_cg_demo_api_key=" + COINGECKO_API_KEY;
                }
                
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET();
                
                if (!COINGECKO_API_KEY.isEmpty()) {
                    requestBuilder.header("x-cg-demo-api-key", COINGECKO_API_KEY);
                }
                
                var request = requestBuilder.build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var body = response.body();
                var json = gson.fromJson(body, JsonObject.class);
                
                var trendingCoins = new ArrayList<TrendingCoin>();
                if (json.has("coins")) {
                    var coins = json.getAsJsonArray("coins");

                    // limit to top 10 trending coins to avoid too many API calls
                    int maxTrending = Math.min(coins.size(), 10);
                    for (int i = 0; i < maxTrending; i++) {
                        var coinElement = coins.get(i);
                        var coinObj = coinElement.getAsJsonObject();
                        var item = coinObj.getAsJsonObject("item");
                        
                        var trendingCoin = new TrendingCoin();
                        trendingCoin.setId(item.has("id") ? item.get("id").getAsString() : null);
                        trendingCoin.setName(item.has("name") ? item.get("name").getAsString() : null);
                        trendingCoin.setSymbol(item.has("symbol") ? item.get("symbol").getAsString().toUpperCase() : null);
                        trendingCoin.setMarketCapRank(item.has("market_cap_rank") ? item.get("market_cap_rank").getAsInt() : null);
                        trendingCoin.setImageUrl(item.has("small") ? item.get("small").getAsString() : null);
                        
                        // always fetch price data from markets endpoint for trending coins
                        //bc the trending endpoint doesn't always have complete price data
                        if (trendingCoin.getId() != null) {
                            try {
                                var marketUrl = COINGECKO_API_URL + "/coins/markets?vs_currency=usd&ids=" + trendingCoin.getId() + "&per_page=1";
                                if (!COINGECKO_API_KEY.isEmpty()) {
                                    marketUrl += "&x_cg_demo_api_key=" + COINGECKO_API_KEY;
                                }
                                
                                var marketRequestBuilder = HttpRequest.newBuilder()
                                        .uri(URI.create(marketUrl))
                                        .timeout(Duration.ofSeconds(10))
                                        .GET();
                                
                                if (!COINGECKO_API_KEY.isEmpty()) {
                                    marketRequestBuilder.header("x-cg-demo-api-key", COINGECKO_API_KEY);
                                }
                                
                                var marketResponse = httpClient.send(marketRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                                var marketBody = marketResponse.body();
                                var marketArray = gson.fromJson(marketBody, JsonArray.class);
                                if (marketArray.size() > 0) {
                                    var marketData = marketArray.get(0).getAsJsonObject();
                                    trendingCoin.setPrice(marketData.has("current_price") && !marketData.get("current_price").isJsonNull() 
                                        ? safeParseBigDecimal(marketData.get("current_price")) : null);
                                    trendingCoin.setPriceChange24h(marketData.has("price_change_24h") && !marketData.get("price_change_24h").isJsonNull()
                                        ? safeParseBigDecimal(marketData.get("price_change_24h")) : null);
                                    trendingCoin.setPriceChangePercent24h(marketData.has("price_change_percentage_24h") && !marketData.get("price_change_percentage_24h").isJsonNull()
                                        ? safeParseBigDecimal(marketData.get("price_change_percentage_24h")) : null);
                                    trendingCoin.setMarketCap(marketData.has("market_cap") && !marketData.get("market_cap").isJsonNull()
                                        ? safeParseBigDecimal(marketData.get("market_cap")) : null);
                                    trendingCoin.setVolume24h(marketData.has("total_volume") && !marketData.get("total_volume").isJsonNull()
                                        ? safeParseBigDecimal(marketData.get("total_volume")) : null);
                                    // update market cap rank from market data if available
                                    if (marketData.has("market_cap_rank") && !marketData.get("market_cap_rank").isJsonNull()) {
                                        trendingCoin.setMarketCapRank(marketData.get("market_cap_rank").getAsInt());
                                    }
                                }
                            } catch (Exception e) {
                                // if fetching market data fails, continue with what we have
                                System.err.println("[EtherscanRepository] Could not fetch market data for " + trendingCoin.getId() + ": " + e.getMessage());
                            }
                        }
                        
                        trendingCoins.add(trendingCoin);
                    }
                }
                return trendingCoins;
            } catch (Exception e) {
                throw new RuntimeException(new BlockchainException("Error fetching trending coins", e));
            }
        });
    }
    
    @Override
    public CompletableFuture<List<TrendingCoin>> getTopCoins(int limit) throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = COINGECKO_API_URL + "/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=" + limit + "&page=1&sparkline=false";
                if (!COINGECKO_API_KEY.isEmpty()) {
                    url += "&x_cg_demo_api_key=" + COINGECKO_API_KEY;
                }
                
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET();
                
                if (!COINGECKO_API_KEY.isEmpty()) {
                    requestBuilder.header("x-cg-demo-api-key", COINGECKO_API_KEY);
                }
                
                var request = requestBuilder.build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var body = response.body();
                var jsonArray = gson.fromJson(body, JsonArray.class);
                
                var topCoins = new ArrayList<TrendingCoin>();
                for (var element : jsonArray) {
                    var coin = element.getAsJsonObject();
                    
                    var trendingCoin = new TrendingCoin();

                    trendingCoin.setId(coin.has("id") ? coin.get("id").getAsString() : null);

                    trendingCoin.setName(coin.has("name") ? coin.get("name").getAsString() : null);

                    trendingCoin.setSymbol(coin.has("symbol") ? coin.get("symbol").getAsString().toUpperCase() : null);

                    trendingCoin.setMarketCapRank(coin.has("market_cap_rank") && !coin.get("market_cap_rank").isJsonNull() 
                        ? coin.get("market_cap_rank").getAsInt() : null);

                    trendingCoin.setPrice(coin.has("current_price") && !coin.get("current_price").isJsonNull()
                        ? safeParseBigDecimal(coin.get("current_price")) : null);

                    trendingCoin.setPriceChange24h(coin.has("price_change_24h") && !coin.get("price_change_24h").isJsonNull()
                        ? safeParseBigDecimal(coin.get("price_change_24h")) : null);

                    trendingCoin.setPriceChangePercent24h(coin.has("price_change_percentage_24h") && !coin.get("price_change_percentage_24h").isJsonNull()
                        ? safeParseBigDecimal(coin.get("price_change_percentage_24h")) : null);

                    trendingCoin.setMarketCap(coin.has("market_cap") && !coin.get("market_cap").isJsonNull()
                        ? safeParseBigDecimal(coin.get("market_cap")) : null);

                    trendingCoin.setVolume24h(coin.has("total_volume") && !coin.get("total_volume").isJsonNull()
                        ? safeParseBigDecimal(coin.get("total_volume")) : null);

                    trendingCoin.setImageUrl(coin.has("image") ? coin.get("image").getAsString() : null);
                    
                    topCoins.add(trendingCoin);
                }
                return topCoins;
            } catch (Exception e) {
                throw new RuntimeException(new BlockchainException("Error fetching top coins", e));
            }
        });
    }
    
    @Override
    public CompletableFuture<GlobalMarketData> getGlobalMarketData() throws BlockchainException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = COINGECKO_API_URL + "/global";
                if (!COINGECKO_API_KEY.isEmpty()) {
                    url += "?x_cg_demo_api_key=" + COINGECKO_API_KEY;
                }
                
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET();
                
                if (!COINGECKO_API_KEY.isEmpty()) {
                    requestBuilder.header("x-cg-demo-api-key", COINGECKO_API_KEY);
                }
                
                var request = requestBuilder.build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var body = response.body();
                var json = gson.fromJson(body, JsonObject.class);
                
                if (json.has("data")) {
                    var data = json.getAsJsonObject("data");
                    var totalMarketCap = data.has("total_market_cap") 
                        ? data.getAsJsonObject("total_market_cap").has("usd")
                            ? BigDecimal.valueOf(data.getAsJsonObject("total_market_cap").get("usd").getAsDouble())
                            : null
                        : null;
                    
                    var totalVolume = data.has("total_volume") 
                        ? data.getAsJsonObject("total_volume").has("usd")
                            ? BigDecimal.valueOf(data.getAsJsonObject("total_volume").get("usd").getAsDouble())
                            : null
                        : null;
                    
                    var marketCapChange = data.has("market_cap_change_percentage_24h_usd")
                        ? BigDecimal.valueOf(data.get("market_cap_change_percentage_24h_usd").getAsDouble())
                        : null;
                    
                    var activeCryptos = data.has("active_cryptocurrencies")
                        ? data.get("active_cryptocurrencies").getAsInt()
                        : null;
                    
                    var markets = data.has("markets")
                        ? data.get("markets").getAsInt()
                        : null;
                    
                    return new GlobalMarketData(totalMarketCap, totalVolume, marketCapChange, activeCryptos, markets);
                }
                throw new BlockchainException("Invalid CoinGecko global data response");
            } catch (Exception e) {
                throw new RuntimeException(new BlockchainException("Error fetching global market data", e));
            }
        });
    }
}

