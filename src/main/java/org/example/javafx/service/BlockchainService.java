package org.example.javafx.service;

import org.example.javafx.exception.BlockchainException;
import org.example.javafx.model.*;
import org.example.javafx.repository.BlockchainRepository;
import org.example.javafx.repository.EtherscanRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer that uses BlockchainRepository
 * Provides synchronous wrappers for async repository methods
 * Similar pattern to WeatherRepository in the course
 */
public class BlockchainService {
    
    private final BlockchainRepository repository;
    
    public BlockchainService() throws BlockchainException {
        this.repository = new EtherscanRepository();
    }
    
    // Synchronous wrappers for backward compatibility with controllers
    
    public Long getLatestBlockNumber() throws Exception {
        return repository.getLatestBlockNumber().get();
    }
    
    public Block getBlockByNumber(Long blockNumber) throws Exception {
        return repository.getBlockByNumber(blockNumber).get();
    }
    
    public List<Block> getLatestBlocks(int count) throws Exception {
        return repository.getLatestBlocks(count).get();
    }
    
    public Transaction getTransactionByHash(String hash) throws Exception {
        return repository.getTransactionByHash(hash).get();
    }
    
    public List<Transaction> getLatestTransactions(int count) throws Exception {
        return repository.getLatestTransactions(count).get();
    }
    
    public Address getAddressInfo(String address) throws Exception {
        return repository.getAddressDetails(address).get();
    }
    
    public List<Transaction> getRecentTransactions(String address, int count) throws Exception {
        return repository.getAddressTransactions(address, count).get();
    }
    
    public EthPrice getEthPrice() throws Exception {
        return repository.getEthPrice().get();
    }
    
    public Double calculateTPS() throws Exception {
        try {
            var tps = repository.calculateTPS().get();
            System.out.println("[BlockchainService] TPS calculated: " + tps);
            return tps;
        } catch (Exception e) {
            System.err.println("[BlockchainService] Error calculating TPS: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public String getEthSupply() throws Exception {
        var supply = repository.getEthSupply().get();
        return supply.getFormattedShortSupply();
    }
    
    public org.example.javafx.model.EthSupply getEthSupplyDetails() throws Exception {
        return repository.getEthSupply().get();
    }
    
    public BlockchainStats getBlockchainStats() throws Exception {
        var latestBlock = getLatestBlockNumber();
        return new BlockchainStats(latestBlock, null, null, null);
    }
    
    // Async methods for better performance
    
    public CompletableFuture<Long> getLatestBlockNumberAsync() throws BlockchainException {
        return repository.getLatestBlockNumber();
    }
    
    public CompletableFuture<EthPrice> getEthPriceAsync() throws BlockchainException {
        return repository.getEthPrice();
    }
    
    public CompletableFuture<List<Block>> getLatestBlocksAsync(int count) throws BlockchainException {
        return repository.getLatestBlocks(count);
    }
    
    public CompletableFuture<List<Transaction>> getLatestTransactionsAsync(int count) throws BlockchainException {
        return repository.getLatestTransactions(count);
    }
    
    public CompletableFuture<Double> calculateTPSAsync() throws BlockchainException {
        return repository.calculateTPS();
    }
    
    public List<TrendingCoin> getTrendingCoins() throws Exception {
        return repository.getTrendingCoins().get();
    }
    
    public List<TrendingCoin> getTopCoins(int limit) throws Exception {
        return repository.getTopCoins(limit).get();
    }
    
    public GlobalMarketData getGlobalMarketData() throws Exception {
        return repository.getGlobalMarketData().get();
    }
    
    public CompletableFuture<List<TrendingCoin>> getTrendingCoinsAsync() throws BlockchainException {
        return repository.getTrendingCoins();
    }
    
    public CompletableFuture<List<TrendingCoin>> getTopCoinsAsync(int limit) throws BlockchainException {
        return repository.getTopCoins(limit);
    }
    
    public CompletableFuture<GlobalMarketData> getGlobalMarketDataAsync() throws BlockchainException {
        return repository.getGlobalMarketData();
    }
}
