package org.example.javafx.repository;

import org.example.javafx.exception.BlockchainException;
import org.example.javafx.model.Address;
import org.example.javafx.model.Block;
import org.example.javafx.model.EthPrice;
import org.example.javafx.model.EthSupply;
import org.example.javafx.model.GlobalMarketData;
import org.example.javafx.model.TrendingCoin;
import org.example.javafx.model.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for blockchain data access
 * Using generics and abstract methods (interface pattern)
 * Similar to WeatherRepository in the course
 */
public interface BlockchainRepository {
    
    /**
     * Get latest block number
     * @return CompletableFuture with block number
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<Long> getLatestBlockNumber() throws BlockchainException;
    
    /**
     * Get block by number
     * @param blockNumber the block number
     * @return CompletableFuture with Block
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<Block> getBlockByNumber(Long blockNumber) throws BlockchainException;
    
    /**
     * Get latest blocks
     * @param count number of blocks to fetch
     * @return CompletableFuture with list of blocks
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<List<Block>> getLatestBlocks(int count) throws BlockchainException;
    
    /**
     * Get transaction by hash
     * @param hash transaction hash
     * @return CompletableFuture with Transaction
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<Transaction> getTransactionByHash(String hash) throws BlockchainException;
    
    /**
     * Get latest transactions
     * @param count number of transactions to fetch
     * @return CompletableFuture with list of transactions
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<List<Transaction>> getLatestTransactions(int count) throws BlockchainException;
    
    /**
     * Get address details
     * @param address the address hash
     * @return CompletableFuture with Address
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<Address> getAddressDetails(String address) throws BlockchainException;
    
    /**
     * Get transactions for an address
     * @param address the address hash
     * @param count number of transactions to fetch
     * @return CompletableFuture with list of transactions
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<List<Transaction>> getAddressTransactions(String address, int count) throws BlockchainException;
    
    /**
     * Get ETH price from CoinGecko
     * @return CompletableFuture with EthPrice
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<EthPrice> getEthPrice() throws BlockchainException;
    
    /**
     * Calculate transactions per second (TPS)
     * @return CompletableFuture with TPS value
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<Double> calculateTPS() throws BlockchainException;
    
    /**
     * Get ETH supply information from CoinGecko
     * @return CompletableFuture with EthSupply containing circulating, total, and max supply
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<EthSupply> getEthSupply() throws BlockchainException;
    
    /**
     * Get trending cryptocurrencies from CoinGecko
     * @return CompletableFuture with list of trending coins
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<List<TrendingCoin>> getTrendingCoins() throws BlockchainException;
    
    /**
     * Get top cryptocurrencies by market cap from CoinGecko
     * @param limit Number of coins to fetch (default: 10)
     * @return CompletableFuture with list of top coins
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<List<TrendingCoin>> getTopCoins(int limit) throws BlockchainException;
    
    /**
     * Get global cryptocurrency market data from CoinGecko
     * @return CompletableFuture with GlobalMarketData
     * @throws BlockchainException if API call fails
     */
    CompletableFuture<GlobalMarketData> getGlobalMarketData() throws BlockchainException;
}

