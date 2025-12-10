package org.example.javafx.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.javafx.model.Address;
import org.example.javafx.model.Block;
import org.example.javafx.model.Transaction;
import org.example.javafx.service.BlockchainService;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class DetailViewController implements Initializable {
    
    @FXML private Button backButton;
    @FXML private Label detailTitle;
    @FXML private VBox detailContent;
    @FXML private BorderPane detailRoot;
    
    private BlockchainService blockchainService;
    private DecimalFormat df = new DecimalFormat("#,###.##");
    private DashboardController dashboardController;
    private ObservableList<Transaction> addressTransactionList = FXCollections.observableArrayList();
    
    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ensure button is clickable and has proper cursor
        backButton.setDisable(false);
        backButton.setCursor(javafx.scene.Cursor.HAND);
        
        backButton.setOnAction(e -> {
            System.out.println("[DetailViewController] Back button clicked!");
            // get the stage from the current detail view
            Stage stage = null;
            if (detailRoot != null && detailRoot.getScene() != null) {
                stage = (Stage) detailRoot.getScene().getWindow();
            }
            
            // try to reuse existing dashboard controller if available
            if (dashboardController != null && stage != null) {
                System.out.println("[DetailViewController] Using cached dashboard controller");
                dashboardController.showDashboard(stage);
            } else {
                System.out.println("[DetailViewController] Creating new dashboard");
                // fallback: create new dashboard (will refetch data)
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/javafx/dashboard.fxml"));
                    Parent dashboard = loader.load();
                    if (stage != null) {
                        stage.setScene(new Scene(dashboard, 1200, 800));
                    } else {
                        System.err.println("[DetailViewController] No stage available for navigation!");
                    }
                } catch (IOException ex) {
                    System.err.println("[DetailViewController] Error loading dashboard: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }
    
    // method to load the details
    public void loadDetails(String query, String type, BlockchainService service) {
        this.blockchainService = service;
        
        CompletableFuture.runAsync(() -> {
            try {
                if ("transaction".equals(type)) {
                    loadTransactionDetails(query);
                } else if ("address".equals(type)) {
                    loadAddressDetails(query);
                } else if ("block".equals(type)) {
                    loadBlockDetails(query);
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addLabel("Error: " + e.getMessage());
                });
            }
        });
    }
    
    // method to load the transaction details
    private void loadTransactionDetails(String txHash) {
        try {
            Transaction tx = blockchainService.getTransactionByHash(txHash);
            Platform.runLater(() -> {
                detailTitle.setText("Transaction: " + (txHash.length() > 20 ? txHash.substring(0, 20) + "..." : txHash));
                detailContent.getChildren().clear();
                
                if (tx != null) {
                    addCopyableLabel("Hash: ", tx.getHash());
                    addCopyableLabel("From: ", tx.getFrom());
                    addCopyableLabel("To: ", tx.getTo());
                    addLabel("Value: " + tx.getFormattedValue());
                    addCopyableLabel("Block: ", tx.getBlockNumber() != null ? tx.getBlockNumber().toString() : null);
                    addLabel("Time: " + tx.getFormattedTime());
                    if (tx.getGasUsed() != null) {
                        addLabel("Gas Used: " + df.format(tx.getGasUsed()));
                    }
                    if (tx.getGasPrice() != null) {
                        addLabel("Gas Price: " + df.format(tx.getGasPrice()));
                    }
                } else {
                    addLabel("Transaction not found");
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                addLabel("Error loading transaction: " + e.getMessage());
            });
        }
    }
    
    // method to load the address details
    private void loadAddressDetails(String address) {
        try {
            Address addr = blockchainService.getAddressInfo(address);
            List<Transaction> txs = blockchainService.getRecentTransactions(address, 10);
            
            Platform.runLater(() -> {
                detailTitle.setText("Address: " + (address.length() > 20 ? address.substring(0, 20) + "..." : address));
                detailContent.getChildren().clear();
                
                if (addr != null) {
                    addCopyableLabel("Address: ", addr.getAddress());
                    addLabel("Balance: " + addr.getFormattedBalance());
                    addLabel("Transaction Count: " + (addr.getTransactionCount() != null ? addr.getTransactionCount() : "N/A"));
                    
                    if (!txs.isEmpty()) {
                        addLabel("\nRecent Transactions (10 most recent):");
                        // Create and add transaction table
                        TableView<Transaction> txTable = createTransactionTable();
                        addressTransactionList.clear();
                        addressTransactionList.addAll(txs);
                        txTable.setItems(addressTransactionList);
                        detailContent.getChildren().add(txTable);
                    } else {
                        addLabel("\nNo recent transactions found.");
                    }
                } else {
                    addLabel("Address not found");
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                addLabel("Error loading address: " + e.getMessage());
            });
        }
    }
    
    // method to create the transaction table
    private TableView<Transaction> createTransactionTable() {
        TableView<Transaction> table = new TableView<>();
        table.setPrefHeight(400);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // hash column with copy button
        TableColumn<Transaction, String> hashColumn = new TableColumn<>("Hash");
        hashColumn.setCellFactory(column -> createCopyableTableCell(tx -> tx.getHash() != null ? tx.getHash() : "N/A"));
        hashColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getHash() != null ? tx.getHash() : "N/A");
        });
        hashColumn.setPrefWidth(200);
        
        // type column
        TableColumn<Transaction, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx != null ? tx.getTransactionType() : "N/A");
        });
        typeColumn.setPrefWidth(120);
        
        // From column with copy button
        TableColumn<Transaction, String> fromColumn = new TableColumn<>("From");
        fromColumn.setCellFactory(column -> createCopyableTableCell(tx -> tx.getFrom() != null ? tx.getFrom() : "N/A"));
        fromColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFrom() != null ? tx.getFrom() : "N/A");
        });
        fromColumn.setPrefWidth(150);
        
        // To column with copy button
        TableColumn<Transaction, String> toColumn = new TableColumn<>("To");
        toColumn.setCellFactory(column -> createCopyableTableCell(tx -> tx.getTo() != null ? tx.getTo() : "N/A"));
        toColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getTo() != null ? tx.getTo() : "N/A");
        });
        toColumn.setPrefWidth(150);
        
        // Value column
        TableColumn<Transaction, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFormattedValue());
        });
        valueColumn.setPrefWidth(120);
        
        // Block column with copy button
        TableColumn<Transaction, String> blockColumn = new TableColumn<>("Block");
        blockColumn.setCellFactory(column -> createCopyableTableCell(tx -> tx.getBlockNumber() != null ? tx.getBlockNumber().toString() : "N/A"));
        blockColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getBlockNumber() != null ? tx.getBlockNumber().toString() : "N/A");
        });
        blockColumn.setPrefWidth(100);
        
        // Time column
        TableColumn<Transaction, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(cellData -> {
            Transaction tx = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(tx.getFormattedTime());
        });
        timeColumn.setPrefWidth(150);
        
        table.getColumns().addAll(hashColumn, typeColumn, fromColumn, toColumn, valueColumn, blockColumn, timeColumn);
        
        // Make rows clickable to view transaction details
        table.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Transaction tx = row.getItem();
                    if (tx != null && tx.getHash() != null) {
                        // Navigate to transaction detail view
                        if (dashboardController != null) {
                            Platform.runLater(() -> {
                                dashboardController.showDetailView(tx.getHash(), "transaction");
                            });
                        }
                    }
                }
            });
            return row;
        });
        
        return table;
    }
    
    // method to create the copyable table cell
    private TableCell<Transaction, String> createCopyableTableCell(java.util.function.Function<Transaction, String> valueExtractor) {
        return new TableCell<Transaction, String>() {
            private final Button copyButton = new Button("📋");
            private final HBox container = new HBox(5);
            
            {
                copyButton.setStyle("-fx-background-color: transparent; -fx-padding: 2px 5px; -fx-cursor: hand;");
                copyButton.setOnAction(e -> {
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
    
    // method to load the block details
    private void loadBlockDetails(String blockNumber) {
        try {
            Block block = blockchainService.getBlockByNumber(Long.parseLong(blockNumber));
            Platform.runLater(() -> {
                detailTitle.setText("Block: " + blockNumber);
                detailContent.getChildren().clear();
                
                if (block != null) {
                    addCopyableLabel("Block Number: ", block.getNumber() != null ? block.getNumber().toString() : null);
                    addCopyableLabel("Hash: ", block.getHash());
                    addLabel("Time: " + block.getFormattedTime());
                    addLabel("Transactions: " + (block.getTransactionCount() != null ? block.getTransactionCount() : "N/A"));
                    if (block.getGasUsed() != null) {
                        addLabel("Gas Used: " + df.format(block.getGasUsed()));
                    }
                    if (block.getGasLimit() != null) {
                        addLabel("Gas Limit: " + df.format(block.getGasLimit()));
                    }
                    addCopyableLabel("Miner: ", block.getMiner());
                } else {
                    addLabel("Block not found");
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                addLabel("Error loading block: " + e.getMessage());
            });
        }
    }
    
    // method to add a label
    private void addLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-padding: 5px;");
        detailContent.getChildren().add(label);
    }
    
    // method to add a copyable label
    private void addCopyableLabel(String prefix, String value) {
        addCopyableLabel(prefix, value, value);
    }
    
    // method to add a copyable label
    private void addCopyableLabel(String prefix, String displayValue, String copyValue) {
        HBox container = new HBox(5);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label label = new Label(prefix + (displayValue != null ? displayValue : "N/A"));
        label.setWrapText(true);
        label.setStyle("-fx-padding: 5px;");
        
        if (copyValue != null && !copyValue.equals("N/A")) {
            Button copyButton = new Button("📋");
            copyButton.setStyle("-fx-background-color: transparent; -fx-padding: 2px 5px; -fx-cursor: hand;");
            copyButton.setOnAction(e -> copyToClipboard(copyValue));
            container.getChildren().addAll(label, copyButton);
        } else {
            container.getChildren().add(label);
        }
        
        detailContent.getChildren().add(container);
    }
    
    // method to copy text to clipboard
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        showToast("Copied to clipboard!");
    }
    
    // method to show a toast
    private void showToast(String message) {
        Platform.runLater(() -> {
            if (detailRoot == null || detailRoot.getScene() == null) return;
            
            // use popup for toast notification
            javafx.stage.Popup popup = new javafx.stage.Popup();
            popup.setAutoHide(true);
            popup.setAutoFix(true);
            
            Label toast = new Label(message);
            toast.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-text-fill: white; -fx-padding: 12px 24px; -fx-background-radius: 8px; -fx-font-size: 14px; -fx-font-weight: bold;");
            
            popup.getContent().add(toast);
            
            // show popup at bottom center
            Stage stage = (Stage) detailRoot.getScene().getWindow();
            
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
            pause.setOnFinished(e -> {
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(event -> popup.hide());
                fadeOut.play();
            });
            pause.play();
        });
    }
}

