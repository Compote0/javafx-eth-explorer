package org.example.javafx.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.example.javafx.model.Address;
import org.example.javafx.model.Block;
import org.example.javafx.model.BlockchainStats;
import org.example.javafx.model.Transaction;
import org.example.javafx.service.BlockchainService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class BlockchainExplorerController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label latestBlockLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label averageGasPriceLabel;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> hashColumn;
    @FXML private TableColumn<Transaction, String> fromColumn;
    @FXML private TableColumn<Transaction, String> toColumn;
    @FXML private TableColumn<Transaction, String> valueColumn;
    @FXML private TableColumn<Transaction, Long> blockColumn;
    @FXML private TableColumn<Transaction, String> timeColumn;
    @FXML private VBox resultBox;
    @FXML private TextArea resultTextArea;
    @FXML private ProgressIndicator loadingIndicator;
    
    private BlockchainService blockchainService;
    // observable list to store the transactions
    private ObservableList<Transaction> transactionList;
    
    @Override
    // method to initialize the controller
    public void initialize(URL location, ResourceBundle resources) {
        try {
            blockchainService = new BlockchainService();
        } catch (org.example.javafx.exception.BlockchainException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize BlockchainService", e);
        }
        transactionList = FXCollections.observableArrayList();
        
        // set table columns
        hashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));
        fromColumn.setCellValueFactory(new PropertyValueFactory<>("from"));
        toColumn.setCellValueFactory(new PropertyValueFactory<>("to"));

        valueColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFormattedValue());
        });

        blockColumn.setCellValueFactory(new PropertyValueFactory<>("blockNumber"));

        timeColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFormattedTime());
        });
        
        transactionTable.setItems(transactionList);
        
        // set search button
        searchButton.setOnAction(e -> performSearch());
        searchField.setOnAction(e -> performSearch());
        
        // load initial data
        loadDashboard();
    }
    
    // method to load the dashboard
    private void loadDashboard() {
        loadingIndicator.setVisible(true);
        CompletableFuture.runAsync(() -> {
            try {
                BlockchainStats stats = blockchainService.getBlockchainStats();
                List<Transaction> recentTxs = blockchainService.getLatestTransactions(20);
                
                Platform.runLater(() -> {
                    if (stats != null) {
                        latestBlockLabel.setText("Latest Block: " + (stats.latestBlock() != null ? stats.latestBlock().toString() : "N/A"));
                    }
                    transactionList.clear();
                    transactionList.addAll(recentTxs);
                    loadingIndicator.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error loading dashboard: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                });
            }
        });
    }
    
    // method to search a query by address, transaction hash, or block number
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showError("Please enter a search query");
            return;
        }
        
        loadingIndicator.setVisible(true);
        resultBox.setVisible(false);
        

        // Promise (ts: Promise) to perform the search asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // determine search type
                if (query.startsWith("0x") && query.length() == 66) {
                    // transaction hash
                    Transaction tx = blockchainService.getTransactionByHash(query);
                    Platform.runLater(() -> displayTransaction(tx));
                } else if (query.startsWith("0x") && query.length() == 42) {
                    // address
                    Address address = blockchainService.getAddressInfo(query);
                    List<Transaction> txs = blockchainService.getRecentTransactions(query, 20);
                    Platform.runLater(() -> displayAddress(address, txs));
                } else if (query.matches("\\d+")) {
                    // block number
                    Block block = blockchainService.getBlockByNumber(Long.parseLong(query));
                    Platform.runLater(() -> displayBlock(block));
                } else {
                    Platform.runLater(() -> {
                        showError("Invalid search format. Use:\n- Transaction hash (0x...)\n- Address (0x...)\n- Block number");
                        loadingIndicator.setVisible(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Search error: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                });
            }
        });
    }
    
    // method to display a transaction details screen
    private void displayTransaction(Transaction tx) {
        if (tx == null) {
            showError("Transaction not found");
            loadingIndicator.setVisible(false);
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== TRANSACTION ===\n\n");
        sb.append("Hash: ").append(tx.getHash()).append("\n");
        sb.append("From: ").append(tx.getFrom()).append("\n");
        sb.append("To: ").append(tx.getTo()).append("\n");
        sb.append("Value: ").append(tx.getFormattedValue()).append("\n");
        sb.append("Block: ").append(tx.getBlockNumber()).append("\n");
        sb.append("Time: ").append(tx.getFormattedTime()).append("\n");
        sb.append("Gas Used: ").append(tx.getGasUsed()).append("\n");
        sb.append("Gas Price: ").append(tx.getGasPrice()).append("\n");
        
        resultTextArea.setText(sb.toString());
        resultBox.setVisible(true);
        loadingIndicator.setVisible(false);
    }
    
    // method to display an address details screen
    private void displayAddress(Address address, List<Transaction> txs) {
        if (address == null) {
            showError("Address not found");
            loadingIndicator.setVisible(false);
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== ADDRESS ===\n\n");
        sb.append("Address: ").append(address.getAddress()).append("\n");
        sb.append("Balance: ").append(address.getFormattedBalance()).append("\n");
        sb.append("Transaction Count: ").append(address.getTransactionCount()).append("\n");
        sb.append("\n=== RECENT TRANSACTIONS ===\n\n");
        
        resultTextArea.setText(sb.toString());
        resultBox.setVisible(true);
        transactionList.clear();
        transactionList.addAll(txs);
        loadingIndicator.setVisible(false);
    }
    
    // method to display a block details screen
    private void displayBlock(Block block) {
        if (block == null) {
            showError("Block not found");
            loadingIndicator.setVisible(false);
            return;
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("=== BLOCK ===\n\n");
        stringBuilder.append("Number: ").append(block.getNumber()).append("\n");
        stringBuilder.append("Hash: ").append(block.getHash()).append("\n");
        stringBuilder.append("Time: ").append(block.getFormattedTime()).append("\n");
        stringBuilder.append("Transactions: ").append(block.getTransactionCount()).append("\n");
        stringBuilder.append("Gas Used: ").append(block.getGasUsed()).append("\n");
        stringBuilder.append("Gas Limit: ").append(block.getGasLimit()).append("\n");
        stringBuilder.append("Miner: ").append(block.getMiner()).append("\n");
        
        resultTextArea.setText(stringBuilder.toString());
        resultBox.setVisible(true);
        loadingIndicator.setVisible(false);
    }
    
    // method to show an error alert
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

