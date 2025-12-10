package org.example.javafx.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.javafx.model.Block;
import org.example.javafx.model.EthPrice;
import org.example.javafx.model.Transaction;
import org.example.javafx.service.BlockchainService;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class DashboardController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label ethPriceLabel;
    @FXML private Label ethChangeLabel;
    @FXML private Label ethSupplyLabel;
    @FXML private Label marketCapLabel;
    @FXML private Label tpsLabel;
    @FXML private Label latestBlockLabel;
    
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> txHashColumn;
    @FXML private TableColumn<Transaction, String> txTypeColumn;
    @FXML private TableColumn<Transaction, String> txFromColumn;
    @FXML private TableColumn<Transaction, String> txToColumn;
    @FXML private TableColumn<Transaction, String> txValueColumn;
    @FXML private TableColumn<Transaction, String> txBlockColumn;
    @FXML private TableColumn<Transaction, String> txTimeColumn;
    
    @FXML private TableView<Block> blockTable;
    @FXML private TableColumn<Block, Long> blockNumberColumn;
    @FXML private TableColumn<Block, Integer> blockTxsColumn;
    @FXML private TableColumn<Block, String> blockGasColumn;
    @FXML private TableColumn<Block, String> blockTimeColumn;
    
    @FXML private VBox txSkeletonLoader;
    @FXML private VBox blockSkeletonLoader;
    @FXML private javafx.scene.shape.Rectangle ethSupplySkeleton;
    @FXML private javafx.scene.shape.Rectangle marketCapSkeleton;
    @FXML private javafx.scene.shape.Rectangle tpsSkeleton;
    @FXML private javafx.scene.shape.Rectangle latestBlockSkeleton;
    
    // labels for the global market data
    @FXML private Label globalMarketCapLabel;
    @FXML private Label globalVolumeLabel;
    @FXML private Label globalMarketCapChangeLabel;
    @FXML private Label activeCryptosLabel;
    @FXML private Label marketsLabel;
    
    @FXML private javafx.scene.shape.Rectangle globalMarketCapSkeleton;
    @FXML private javafx.scene.shape.Rectangle globalVolumeSkeleton;
    @FXML private javafx.scene.shape.Rectangle globalMarketCapChangeSkeleton;
    @FXML private javafx.scene.shape.Rectangle activeCryptosSkeleton;
    @FXML private javafx.scene.shape.Rectangle marketsSkeleton;
    
    @FXML private BorderPane root;
    
    private BlockchainService blockchainService;
    private ObservableList<Transaction> transactionList;
    private ObservableList<Block> blockList;
    private DecimalFormat df = new DecimalFormat("#,###.##");
    
    // cache to avoid refetching when navigating back
    private static DashboardController cachedInstance;
    private static Scene cachedScene;
    private long lastDataLoadTime = 0;
    private static final long DATA_CACHE_DURATION_MS = 30000; // 30 seconds cache duration
    
    // auto-refresh timer
    private javafx.animation.Timeline refreshTimer;
    private static final long REFRESH_INTERVAL_SECONDS = 20; // refresh everything every 20 seconds
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            blockchainService = new BlockchainService();
        } catch (org.example.javafx.exception.BlockchainException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize BlockchainService", e);
        }
        transactionList = FXCollections.observableArrayList();
        blockList = FXCollections.observableArrayList();
        
        // cache this instance for reuse
        cachedInstance = this;
        
        // initialize skeleton loaders visibility
        setupSkeletonAnimations();
        
        // setup transaction table with copy buttons
        txHashColumn.setCellFactory(column -> createCopyableCell(tx -> tx.getHash() != null ? tx.getHash() : "N/A"));
        txHashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));
        
        txTypeColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx != null ? tx.getTransactionType() : "N/A");
        });
        
        txFromColumn.setCellFactory(column -> createCopyableCell(tx -> tx.getFrom() != null ? tx.getFrom() : "N/A"));
        txFromColumn.setCellValueFactory(new PropertyValueFactory<>("from"));
        
        txToColumn.setCellFactory(column -> createCopyableCell(tx -> tx.getTo() != null ? tx.getTo() : "N/A"));
        txToColumn.setCellValueFactory(new PropertyValueFactory<>("to"));
        
        txValueColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFormattedValue());
        });
        
        txBlockColumn.setCellFactory(column -> createCopyableCell(tx -> tx.getBlockNumber() != null ? tx.getBlockNumber().toString() : "N/A"));
        txBlockColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getBlockNumber() != null ? tx.getBlockNumber().toString() : "N/A");
        });
        
        txTimeColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFormattedTime());
        });
        transactionTable.setItems(transactionList);
        
        // setup block table with copy buttons
        blockNumberColumn.setCellFactory(column -> createCopyableBlockCell(block -> block.getNumber() != null ? block.getNumber().toString() : "N/A"));
        blockNumberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        
        blockTxsColumn.setCellValueFactory(new PropertyValueFactory<>("transactionCount"));
        
        blockGasColumn.setCellValueFactory(cellData -> {
            Block block = cellData.getValue();
            if (block.getGasUsed() != null && block.getGasLimit() != null) {
                double percent = block.getGasUsed().divide(block.getGasLimit(), 4, java.math.RoundingMode.HALF_UP).multiply(new java.math.BigDecimal(100)).doubleValue();
                return new javafx.beans.property.SimpleStringProperty(df.format(percent) + "%");
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        blockTimeColumn.setCellValueFactory(cellData -> {
            Block block = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(block.getFormattedTime());
        });
        blockTable.setItems(blockList);
        
        // setup search
        searchButton.setOnAction(event -> performSearch());
        searchField.setOnAction(event -> performSearch());
        
        // make table rows clickable
        transactionTable.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailView(row.getItem().getHash(), "transaction");
                }
            });
            return row;
        });
        
        blockTable.setRowFactory(tv -> {
            TableRow<Block> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailView(row.getItem().getNumber().toString(), "block");
                }
            });
            return row;
        });
        
        // load dashboard data only if needed
        if (shouldRefreshData()) {
            loadDashboard();
        } else {
            System.out.println("[DashboardController] Using cached data, skipping refetch");
        }
        
        // setup auto-refresh timer (every 10 seconds)
        setupAutoRefresh();
    }
    
    /**
     * Sets up an auto-refresh timer that refreshes all dashboard data every 20 seconds
     */
    private void setupAutoRefresh() {
        refreshTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(REFRESH_INTERVAL_SECONDS),
                event -> {
                    System.out.println("[DashboardController] Auto-refreshing dashboard data...");
                    loadDashboard();
                }
            )
        );
        refreshTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        refreshTimer.play();
    }
    
    /**
     * Stops the auto-refresh timer (useful when navigating away from dashboard)
     */
    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
    
    private boolean shouldRefreshData() {
        long now = System.currentTimeMillis();
        boolean shouldRefresh = (now - lastDataLoadTime) > DATA_CACHE_DURATION_MS || lastDataLoadTime == 0;
        if (shouldRefresh) {
            lastDataLoadTime = now;
        }
        return shouldRefresh;
    }
    
    private void loadDashboard() {
        // always refresh when called (either from timer or initial load)
        lastDataLoadTime = System.currentTimeMillis();
        
        // load ETH price
        Platform.runLater(() -> {
            ethPriceLabel.setText("ETH: Chargement...");
            marketCapSkeleton.setVisible(true);
            marketCapLabel.setVisible(false);
        });
        
        // Promise (ts: Promise) to load the ETH price asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                EthPrice ethPrice = blockchainService.getEthPrice();
                // using lambda expression to update the UI
                Platform.runLater(() -> {
                    // update the UI with the ETH price
                    ethPriceLabel.setText("ETH: " + ethPrice.getFormattedPrice());
                    String change = ethPrice.getFormattedChange();
                    ethChangeLabel.setText(change);
                    // apply CSS classes for styling and remove inline styles first
                    ethChangeLabel.getStyleClass().removeAll("price-positive", "price-negative");
                    ethChangeLabel.setStyle(""); // clear any inline styles

                    // if the ETH price is positive, add the price-positive class, otherwise add the price-negative class
                    if (ethPrice.priceChangePercent24h() != null && 
                        ethPrice.priceChangePercent24h().compareTo(java.math.BigDecimal.ZERO) >= 0) {
                        ethChangeLabel.getStyleClass().add("price-positive");
                    } else {
                        ethChangeLabel.getStyleClass().add("price-negative");
                    }
                    
                    // if the market cap is not null, update the UI with the market cap 
                    if (ethPrice.marketCap() != null) {
                        marketCapSkeleton.setVisible(false);
                        marketCapLabel.setVisible(true);
                        marketCapLabel.setText("$" + df.format(ethPrice.marketCap()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ethPriceLabel.setText("ETH: Erreur");
                    marketCapSkeleton.setVisible(false);
                    marketCapLabel.setVisible(true);
                    marketCapLabel.setText("Erreur");
                });
            }
        });
        
        // load latest block
        Platform.runLater(() -> {
            latestBlockSkeleton.setVisible(true);
            latestBlockLabel.setVisible(false);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                Long latestBlock = blockchainService.getLatestBlockNumber();
                Platform.runLater(() -> {
                    latestBlockSkeleton.setVisible(false);
                    latestBlockLabel.setVisible(true);
                    if (latestBlock != null) {
                        latestBlockLabel.setText(df.format(latestBlock));
                    } else {
                        latestBlockLabel.setText("Erreur");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    latestBlockSkeleton.setVisible(false);
                    latestBlockLabel.setVisible(true);
                    latestBlockLabel.setText("Erreur");
                });
            }
        });
        
        // load TPS
        Platform.runLater(() -> {
            tpsSkeleton.setVisible(true);
            tpsLabel.setVisible(false);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[DashboardController] Calculating TPS...");
                Double tps = blockchainService.calculateTPS();
                System.out.println("[DashboardController] TPS result: " + tps);
                Platform.runLater(() -> {
                    tpsSkeleton.setVisible(false);
                    tpsLabel.setVisible(true);
                    if (tps != null && tps > 0) {
                        System.out.println("[DashboardController] Setting TPS label to: " + df.format(tps) + " TPS");
                        tpsLabel.setText(df.format(tps) + " TPS");
                    } else {
                        System.err.println("[DashboardController] TPS calculation returned 0 or null");
                        tpsLabel.setText("N/A");
                    }
                });
            } catch (Exception e) {
                System.err.println("[DashboardController] ERROR calculating TPS: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    tpsSkeleton.setVisible(false);
                    tpsLabel.setVisible(true);
                    tpsLabel.setText("Erreur");
                });
            }
        });
        
        // load ETH supply
        Platform.runLater(() -> {
            ethSupplySkeleton.setVisible(true);
            ethSupplyLabel.setVisible(false);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                var supply = blockchainService.getEthSupplyDetails();
                Platform.runLater(() -> {
                    ethSupplySkeleton.setVisible(false);
                    ethSupplyLabel.setVisible(true);
                    // Display circulating supply with more detail
                    var supplyText = supply.getFormattedShortSupply();
                    if (supply.circulatingSupply() != null && supply.totalSupply() != null) {
                        // Show both circulating and total if available
                        var totalText = supply.getFormattedTotalSupply();
                        ethSupplyLabel.setText(supplyText + " / " + totalText);
                    } else {
                        ethSupplyLabel.setText(supplyText);
                    }
                });
            } catch (Exception e) {
                System.err.println("[DashboardController] ERROR fetching ETH supply: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    ethSupplySkeleton.setVisible(false);
                    ethSupplyLabel.setVisible(true);
                    ethSupplyLabel.setText("Erreur");
                });
            }
        });
        
        // load transactions
        loadTransactions();
        
        // load blocks
        loadBlocks();
        
        // load global market data
        loadGlobalMarketData();
    }
    
    private void loadTransactions() {
        // show skeleton loader with 15 rows
        Platform.runLater(() -> {
            txSkeletonLoader.getChildren().clear();
            for (int i = 0; i < 15; i++) {
                HBox row = new HBox(10);
                row.getChildren().addAll(
                    createSkeletonRect(200, 20),
                    createSkeletonRect(150, 20),
                    createSkeletonRect(100, 20),
                    createSkeletonRect(80, 20),
                    createSkeletonRect(120, 20)
                );
                txSkeletonLoader.getChildren().add(row);
            }
            txSkeletonLoader.setVisible(true);
            transactionTable.setVisible(false);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[DashboardController] Loading latest transactions...");
                List<Transaction> txs = blockchainService.getLatestTransactions(20);
                System.out.println("[DashboardController] Received " + txs.size() + " transactions");
                Platform.runLater(() -> {
                    txSkeletonLoader.setVisible(false);
                    transactionTable.setVisible(true);
                    transactionList.clear();
                    transactionList.addAll(txs);
                    System.out.println("[DashboardController] Added " + transactionList.size() + " transactions to table");
                });
            } catch (Exception e) {
                System.err.println("[DashboardController] ERROR loading transactions: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    txSkeletonLoader.setVisible(false);
                    transactionTable.setVisible(true);
                    // Error loading transactions
                });
            }
        });
    }
    
    private javafx.scene.shape.Rectangle createSkeletonRect(double width, double height) {
        javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(width, height);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        rect.getStyleClass().add("skeleton-shimmer");
        
        // add shimmer animation
        javafx.animation.FadeTransition fadeTransition = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(1500), rect);
        fadeTransition.setFromValue(0.4);
        fadeTransition.setToValue(0.8);
        fadeTransition.setCycleCount(javafx.animation.Animation.INDEFINITE);
        fadeTransition.setAutoReverse(true);
        fadeTransition.play();
        
        return rect;
    }
    
    private void setupSkeletonAnimations() {
        // animate metric skeletons
        animateSkeleton(ethSupplySkeleton);
        animateSkeleton(marketCapSkeleton);
        animateSkeleton(tpsSkeleton);
        animateSkeleton(latestBlockSkeleton);
    }
    
    private void animateSkeleton(javafx.scene.shape.Rectangle skeleton) {
        if (skeleton != null) {
            javafx.animation.FadeTransition fadeTransition = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(1500), skeleton);
            fadeTransition.setFromValue(0.4);
            fadeTransition.setToValue(0.8);
            fadeTransition.setCycleCount(javafx.animation.Animation.INDEFINITE);
            fadeTransition.setAutoReverse(true);
            fadeTransition.play();
        }
    }
    
    private void loadBlocks() {
        // show skeleton loader with 15 rows
        Platform.runLater(() -> {
            blockSkeletonLoader.getChildren().clear();
            for (int i = 0; i < 15; i++) {
                HBox row = new HBox(10);
                row.getChildren().addAll(
                    createSkeletonRect(100, 20),
                    createSkeletonRect(80, 20),
                    createSkeletonRect(100, 20),
                    createSkeletonRect(120, 20)
                );
                blockSkeletonLoader.getChildren().add(row);
            }
            blockSkeletonLoader.setVisible(true);
            blockTable.setVisible(false);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[DashboardController] Loading latest blocks...");
                List<Block> blocks = blockchainService.getLatestBlocks(20);
                System.out.println("[DashboardController] Received " + blocks.size() + " blocks");
                Platform.runLater(() -> {
                    blockSkeletonLoader.setVisible(false);
                    blockTable.setVisible(true);
                    blockList.clear();
                    blockList.addAll(blocks);
                    System.out.println("[DashboardController] Added " + blockList.size() + " blocks to table");
                });
            } catch (Exception e) {
                System.err.println("[DashboardController] ERROR loading blocks: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    blockSkeletonLoader.setVisible(false);
                    blockTable.setVisible(true);
                    // Error loading blocks
                });
            }
        });
    }
    
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        
        // determine search type and show detail view
        // use if-else for boolean conditions (cant use switch because it requires constant values)
        if (query.startsWith("0x") && query.length() == 66) {
            showDetailView(query, "transaction");
        } else if (query.startsWith("0x") && query.length() == 42) {
            showDetailView(query, "address");
        } else if (query.matches("\\d+")) {
            showDetailView(query, "block");
        } else {
            showError("Invalid search format. Use:\n- Transaction hash (0x...)\n- Address (0x...)\n- Block number");
        }
    }
    
    public void showDetailView(String query, String type) {
        try {
            // stop auto-refresh when navigating away
            stopAutoRefresh();
            
            // store current scene before navigating away
            if (root != null && root.getScene() != null) {
                cachedScene = root.getScene();
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/javafx/detail-view.fxml"));
            Parent detailView = loader.load();
            DetailViewController controller = loader.getController();
            controller.loadDetails(query, type, blockchainService);

            // pass reference to this dashboard controller for navigation back
            controller.setDashboardController(this);
            
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(detailView, 1200, 800));
        } catch (IOException e) {
            System.err.println("[DashboardController] Error loading detail view: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading detail view: " + e.getMessage());
        }
    }
    
    // method to navigate back to this dashboard without reloading
    public void showDashboard() {
        showDashboard(null);
    }
    
    // method to navigate back to this dashboard without reloading (with optional stage parameter)
    public void showDashboard(Stage passedStage) {
        Platform.runLater(() -> {
            // restart auto-refresh when returning to dashboard
            if (refreshTimer == null || refreshTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                setupAutoRefresh();
            }
            
            // get the current stage - prefer passed stage, then try cached scene, then root's scene
            Stage stage = passedStage;
            if (stage == null) {
                if (cachedScene != null) {
                    stage = (Stage) cachedScene.getWindow();
                } else if (root != null && root.getScene() != null) {
                    stage = (Stage) root.getScene().getWindow();
                }
            }
            
            final Stage finalStage = stage;
            if (finalStage != null) {
                // use cached scene if available, otherwise use root's scene
                Scene sceneToShow = cachedScene != null ? cachedScene : (root != null && root.getScene() != null ? root.getScene() : null);

                if (sceneToShow != null) {
                    System.out.println("[DashboardController] Showing dashboard scene");
                    finalStage.setScene(sceneToShow);

                } else {
                    System.out.println("[DashboardController] No scene available, creating new dashboard");
                    // fallback: reload dashboard
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/javafx/dashboard.fxml"));
                        Parent dashboard = loader.load();
                        finalStage.setScene(new Scene(dashboard, 1200, 800));
                    } catch (IOException e) {
                        System.err.println("[DashboardController] Error reloading dashboard: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.err.println("[DashboardController] No stage available!");
            }
        });
    }
    
    // call this when the dashboard scene is first created
    public void setScene(Scene scene) {
        cachedScene = scene;
    }
    
    // static method to get or create dashboard instance
    public static DashboardController getOrCreateInstance() {
        if (cachedInstance != null && cachedInstance.root != null) {
            return cachedInstance;
        }
        return null; // return null because it will be created by fxml loader
    }
    
    // method to create a copyable transaction cell
    private TableCell<Transaction, String> createCopyableCell(java.util.function.Function<Transaction, String> valueExtractor) {
        return new TableCell<Transaction, String>() {
            private final Button copyButton = new Button("📋");
            private final HBox container = new HBox(5);
            
            {
                copyButton.setStyle("-fx-background-color: transparent; -fx-padding: 2px 5px; -fx-cursor: hand;");
                copyButton.setOnAction(event -> {
                    Transaction tx = getTableView().getItems().get(getIndex());
                    if (tx != null) {
                        String value = valueExtractor.apply(tx);
                        copyToClipboard(value);
                    }
                });
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label label = new Label(item);
                    label.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
                    container.getChildren().setAll(label, copyButton);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    // method to create a copyable block cell
    private TableCell<Block, Long> createCopyableBlockCell(java.util.function.Function<Block, String> valueExtractor) {
        return new TableCell<Block, Long>() {
            private final Button copyButton = new Button("📋");
            private final HBox container = new HBox(5);
            
            {
                copyButton.setStyle("-fx-background-color: transparent; -fx-padding: 2px 5px; -fx-cursor: hand;");
                copyButton.setOnAction(event -> {
                    Block block = getTableView().getItems().get(getIndex());
                    if (block != null) {
                        String value = valueExtractor.apply(block);
                        copyToClipboard(value);
                    }
                });
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
            
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {

                    String value = valueExtractor.apply(getTableView().getItems().get(getIndex()));
                    Label label = new Label(value);
                    label.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);

                    container.getChildren().setAll(label, copyButton);

                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    // method to copy text to clipboard
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        showToast("Copied to clipboard!");
    }
    
    // method to show a toast notification
    private void showToast(String message) {
        Platform.runLater(() -> {
            if (root == null || root.getScene() == null) return;
            
            // use popup for toast notification
            javafx.stage.Popup popup = new javafx.stage.Popup();
            popup.setAutoHide(true);
            popup.setAutoFix(true);
            
            Label toast = new Label(message);
            toast.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-text-fill: white; -fx-padding: 12px 24px; -fx-background-radius: 8px; -fx-font-size: 14px; -fx-font-weight: bold;");
            
            popup.getContent().add(toast);
            
            // show popup at bottom center
            Stage stage = (Stage) root.getScene().getWindow();
            
            // animate in
            toast.setOpacity(0);
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            // position and show popup
            popup.show(stage);
            // position at bottom center after showing (toast width is now known)
            javafx.application.Platform.runLater(() -> {
                double x = stage.getX() + (stage.getWidth() / 2) - (toast.getWidth() / 2);
                double y = stage.getY() + stage.getHeight() - 100;
                popup.setX(x);
                popup.setY(y);
            });
            
            // auto-hide after 2 seconds
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(event -> {
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(fadeEvent -> popup.hide());
                fadeOut.play();
            });
            pause.play();
        });
    }
    

    // method to show an error alert
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void loadGlobalMarketData() {
        // update UI threads to display skeletons while its loading
        Platform.runLater(() -> {
            globalMarketCapSkeleton.setVisible(true);
            globalVolumeSkeleton.setVisible(true);
            globalMarketCapChangeSkeleton.setVisible(true);
            activeCryptosSkeleton.setVisible(true);
            marketsSkeleton.setVisible(true);
            globalMarketCapLabel.setVisible(false);
            globalVolumeLabel.setVisible(false);
            globalMarketCapChangeLabel.setVisible(false);
            activeCryptosLabel.setVisible(false);
            marketsLabel.setVisible(false);
        });
        
        CompletableFuture.runAsync(() -> {
            try {
                var globalData = blockchainService.getGlobalMarketData();
                Platform.runLater(() -> {
                    // updating ui thread to replace skeletons to display data
                    globalMarketCapSkeleton.setVisible(false);
                    globalVolumeSkeleton.setVisible(false);
                    globalMarketCapChangeSkeleton.setVisible(false);
                    activeCryptosSkeleton.setVisible(false);
                    marketsSkeleton.setVisible(false);
                    
                    globalMarketCapLabel.setVisible(true);
                    globalVolumeLabel.setVisible(true);
                    globalMarketCapChangeLabel.setVisible(true);
                    activeCryptosLabel.setVisible(true);
                    marketsLabel.setVisible(true);
                    
                    globalMarketCapLabel.setText(globalData.getFormattedTotalMarketCap());
                    globalVolumeLabel.setText(globalData.getFormattedTotalVolume());
                    globalMarketCapChangeLabel.setText(globalData.getFormattedMarketCapChange());
                    
                    // apply color styling to market cap change
                    globalMarketCapChangeLabel.getStyleClass().removeAll("price-positive", "price-negative");
                    if (globalData.marketCapChangePercent24h() != null && 
                        globalData.marketCapChangePercent24h().compareTo(java.math.BigDecimal.ZERO) >= 0) {
                        globalMarketCapChangeLabel.getStyleClass().add("price-positive");
                    } else {
                        globalMarketCapChangeLabel.getStyleClass().add("price-negative");
                    }
                    
                    if (globalData.activeCryptocurrencies() != null) {
                        activeCryptosLabel.setText(df.format(globalData.activeCryptocurrencies()));
                    } else {
                        activeCryptosLabel.setText("N/A");
                    }
                    
                    if (globalData.markets() != null) {
                        marketsLabel.setText(df.format(globalData.markets()));
                    } else {
                        marketsLabel.setText("N/A");
                    }
                });
            } catch (Exception e) {
                System.err.println("[DashboardController] ERROR loading global market data: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    globalMarketCapSkeleton.setVisible(false);
                    globalVolumeSkeleton.setVisible(false);
                    globalMarketCapChangeSkeleton.setVisible(false);
                    activeCryptosSkeleton.setVisible(false);
                    marketsSkeleton.setVisible(false);
                    
                    globalMarketCapLabel.setVisible(true);
                    globalVolumeLabel.setVisible(true);
                    globalMarketCapChangeLabel.setVisible(true);
                    activeCryptosLabel.setVisible(true);
                    marketsLabel.setVisible(true);
                    
                    globalMarketCapLabel.setText("Erreur");
                    globalVolumeLabel.setText("Erreur");
                    globalMarketCapChangeLabel.setText("Erreur");
                    activeCryptosLabel.setText("Erreur");
                    marketsLabel.setText("Erreur");
                });
            }
        });
    }
}

