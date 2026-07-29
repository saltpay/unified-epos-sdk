package com.example.eposposlinkexample.ui.tables;

import com.example.eposposlinkexample.models.Product;
import com.example.eposposlinkexample.models.ProductItem;
import com.example.eposposlinkexample.teya.TeyaSdkManager;
import com.example.eposposlinkexample.ui.StatusTags;
import com.example.eposposlinkexample.util.PriceUtils;

import com.teya.unifiedepossdk.poslink.TabEventListener;
import com.teya.unifiedepossdk.poslink.models.tabs.Tab;
import com.teya.unifiedepossdk.poslink.models.tabs.TabBillRequest;
import com.teya.unifiedepossdk.poslink.models.tabs.TabCompletion;
import com.teya.unifiedepossdk.poslink.models.tabs.TabId;
import com.teya.unifiedepossdk.poslink.models.tabs.TabPayRequest;
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentDetail;
import com.teya.unifiedepossdk.poslink.models.tabs.TabStatus;
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class PayAtTableController implements TabEventListener {

    private static final System.Logger LOG = System.getLogger("PayAtTable");

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(PayAtTableController.class);
    private static final String PAT_ENABLED_KEY = "pat_enabled";

    @FXML private VBox listPane;
    @FXML private CheckBox patToggle;
    @FXML private Button refreshListButton;
    @FXML private FlowPane tilesPane;
    @FXML private Label noTablesLabel;
    @FXML private Button addTableButton;

    @FXML private VBox detailsPane;
    @FXML private Button backButton;
    @FXML private Label detailNameLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Button refreshDetailButton;
    @FXML private HBox billRow;
    @FXML private Label detailBillTerminalLabel;
    @FXML private Label paymentInProgressLabel;
    @FXML private VBox detailItems;
    @FXML private Button closeTableButton;
    @FXML private Button addItemsButton;
    @FXML private Label paymentsCountLabel;
    @FXML private Button viewPaymentsButton;
    @FXML private Label detailTotalLabel;

    private final TeyaSdkManager sdk = TeyaSdkManager.getInstance();
    private final Map<String, TabSummary> tabsById = new LinkedHashMap<>();
    private final Map<String, List<ProductItem>> basketsByTab = new LinkedHashMap<>();

    private String selectedTabId;
    private Tab selectedTabDetail;
    private boolean suppressToggle;

    @FXML
    private void initialize() {
        addTableButton.disableProperty().bind(sdk.readyProperty().not());

        patToggle.setSelected(PREFERENCES.getBoolean(PAT_ENABLED_KEY, false));
        patToggle.selectedProperty().addListener((observable, previous, current) -> {
            if (suppressToggle) {
                return;
            }
            sdk.setPayAtTableEnabled(current,
                    () -> PREFERENCES.putBoolean(PAT_ENABLED_KEY, current),
                    error -> Platform.runLater(() -> {
                        LOG.log(System.Logger.Level.ERROR, "setPayAtTableEnabled failed: " + error);
                        suppressToggle = true;
                        patToggle.setSelected(previous);
                        suppressToggle = false;
                    }));
        });

        refreshListButton.setOnAction(event -> refreshTabs());
        addTableButton.setOnAction(event -> addTable());
        backButton.setOnAction(event -> closeDetails());
        refreshDetailButton.setOnAction(event -> refreshSelected());
        closeTableButton.setOnAction(event -> closeTab());
        addItemsButton.setOnAction(event -> addItems());
        viewPaymentsButton.setOnAction(event -> viewPayments());

        sdk.subscribeToTabEvents(this);
        showList();
        rebuildTiles();

        sdk.readyProperty().addListener((observable, previous, current) -> {
            if (current) {
                refreshTabs();
            }
        });
    }

    // ---- List ----

    private void refreshTabs() {
        sdk.listTabs(tabs -> Platform.runLater(() -> {
            tabsById.clear();
            for (TabSummary tab : tabs) {
                tabsById.put(tab.getTabId().getValue(), tab);
                basketsByTab.computeIfAbsent(tab.getTabId().getValue(), key -> freshBasket());
            }
            rebuildTiles();
        }), error -> LOG.log(System.Logger.Level.ERROR, "listTabs failed: " + error));
    }

    private void addTable() {
        new AddTableDialog().showAndWait().ifPresent(name -> {
            String tabId = "tab-" + System.currentTimeMillis();
            sdk.openTab(tabId, name, tab -> Platform.runLater(() -> {
                String id = tab.getTabId().getValue();
                basketsByTab.put(id, freshBasket());
                tabsById.put(id, toSummary(tab));
                rebuildTiles();
            }), error -> LOG.log(System.Logger.Level.ERROR, "openTab failed: " + error));
        });
    }

    private void rebuildTiles() {
        tilesPane.getChildren().clear();
        for (TabSummary tab : tabsById.values()) {
            tilesPane.getChildren().add(createTile(tab));
        }
        boolean empty = tabsById.isEmpty();
        noTablesLabel.setVisible(empty);
        noTablesLabel.setManaged(empty);
    }

    private VBox createTile(TabSummary tab) {
        Label name = new Label(tab.getTabName());
        name.setStyle("-fx-font-weight: bold;");
        Label total = new Label(PriceUtils.formatMinor(basketTotalMinor(tab.getTabId().getValue())));
        total.setStyle("-fx-text-fill: #1447e6;");

        VBox tile = new VBox(6, name, total, StatusTags.forTab(tab.getStatus()));
        if (tab.getShowingBillTerminalId() != null) {
            tile.getChildren().add(StatusTags.billTag(tab.getShowingBillTerminalId()));
        }
        tile.setPrefWidth(170);
        tile.setStyle("-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: #dddbd7; -fx-border-radius: 8; -fx-padding: 12;");
        tile.setOnMouseClicked(event -> openDetails(tab.getTabId().getValue()));
        return tile;
    }

    // ---- Details ----

    private void openDetails(String tabId) {
        selectedTabId = tabId;
        selectedTabDetail = null;
        showDetails();
        rebuildDetails();
        refreshSelected();
    }

    private void closeDetails() {
        selectedTabId = null;
        selectedTabDetail = null;
        showList();
        rebuildTiles();
    }

    private void refreshSelected() {
        if (selectedTabId != null) {
            refreshTab(selectedTabId);
        }
    }

    private void closeTab() {
        if (selectedTabId == null) {
            return;
        }
        String tabId = selectedTabId;
        sdk.closeTab(tabId, () -> Platform.runLater(() -> handleTabClosed(tabId)),
                error -> LOG.log(System.Logger.Level.ERROR, "closeTab failed: " + error));
    }

    private void addItems() {
        List<ProductItem> basket = basketsByTab.get(selectedTabId);
        if (basket == null) {
            return;
        }
        new ProductCatalogueDialog(basket).showAndWait();
        rebuildDetails();
        rebuildTiles();
    }

    private void viewPayments() {
        if (selectedTabDetail != null) {
            new PaymentsDialog(selectedTabDetail).showAndWait();
        }
    }

    private void rebuildDetails() {
        if (selectedTabId == null) {
            return;
        }
        TabSummary summary = tabsById.get(selectedTabId);
        detailNameLabel.setText(summary != null ? summary.getTabName() : "");
        detailStatusLabel.setText(summary != null ? summary.getStatus().name() : "");

        String billTerminal = summary != null ? summary.getShowingBillTerminalId() : null;
        billRow.setVisible(billTerminal != null);
        billRow.setManaged(billTerminal != null);
        if (billTerminal != null) {
            detailBillTerminalLabel.setText(billTerminal);
        }

        boolean inProgress = selectedTabDetail != null && selectedTabDetail.getPaymentRequests() != null
                && selectedTabDetail.getPaymentRequests().stream().anyMatch(payment -> !payment.getStatus().isFinal());
        paymentInProgressLabel.setVisible(inProgress);
        paymentInProgressLabel.setManaged(inProgress);

        detailItems.getChildren().clear();
        List<ProductItem> basket = basketsByTab.get(selectedTabId);
        if (basket != null) {
            for (ProductItem item : basket) {
                if (item.getQuantity() > 0) {
                    detailItems.getChildren().add(createDetailRow(item));
                }
            }
        }

        int paymentCount = selectedTabDetail != null && selectedTabDetail.getPaymentRequests() != null
                ? selectedTabDetail.getPaymentRequests().size() : 0;
        paymentsCountLabel.setText(paymentCount == 0 ? "No payments yet" : "Payments (" + paymentCount + ")");
        viewPaymentsButton.setVisible(paymentCount > 0);
        viewPaymentsButton.setManaged(paymentCount > 0);

        detailTotalLabel.setText(PriceUtils.formatMinor(basketTotalMinor(selectedTabId)));
    }

    private HBox createDetailRow(ProductItem item) {
        Label name = new Label(item.product().name());
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button minus = new Button("−");
        minus.setOnAction(event -> {
            item.remove();
            rebuildDetails();
            rebuildTiles();
        });
        Label quantity = new Label(String.valueOf(item.getQuantity()));
        Button plus = new Button("+");
        plus.setOnAction(event -> {
            item.add();
            rebuildDetails();
            rebuildTiles();
        });
        Label lineTotal = new Label(PriceUtils.formatPrice(item.lineTotal()));

        HBox row = new HBox(8, name, spacer, minus, quantity, plus, lineTotal);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    // ---- Tab events (raised by the SDK, may arrive off the UI thread) ----

    @Override
    public void onShowBillRequested(TabBillRequest request) {
        String tabId = request.getTabId().getValue();
        Platform.runLater(() -> {
            List<ProductItem> basket = basketsByTab.getOrDefault(tabId, List.of());
            sdk.respondToBillRequest(tabId, tabName(tabId), request.getTerminalId(), basketTotalMinor(tabId),
                    basket,
                    () -> LOG.log(System.Logger.Level.DEBUG, "Bill shown on terminal " + request.getTerminalId()),
                    error -> LOG.log(System.Logger.Level.ERROR, "respondToBillRequest failed: " + error));
            refreshTab(tabId);
        });
    }

    @Override
    public void onPayRequested(TabPayRequest request) {
        String tabId = request.getTabContext().getTabId().getValue();
        Platform.runLater(() -> {
            sdk.makeTabPayment(request.getTabContext(), request.getAmount(), request.getCurrency());
            refreshTab(tabId);
        });
    }

    @Override
    public void onTabCompleted(TabCompletion completion) {
        String tabId = completion.getTabId().getValue();
        Platform.runLater(() -> sdk.closeTab(tabId, () -> Platform.runLater(() -> handleTabClosed(tabId)),
                error -> LOG.log(System.Logger.Level.ERROR, "closeTab failed: " + error)));
    }

    @Override
    public void onPaymentProgress(TabPaymentDetail detail) {
        Platform.runLater(() -> refreshTab(detail.getTabId().getValue()));
    }

    @Override
    public void onPaymentCompleted(TabPaymentDetail detail) {
        Platform.runLater(() -> refreshTab(detail.getTabId().getValue()));
    }

    @Override
    public void onTabPaused(TabId tabId) {
        Platform.runLater(() -> refreshTab(tabId.getValue()));
    }

    @Override
    public void onTabResumed(TabId tabId) {
        Platform.runLater(() -> refreshTab(tabId.getValue()));
    }

    @Override
    public void onBillHidden(TabId tabId) {
        Platform.runLater(() -> refreshTab(tabId.getValue()));
    }

    // ---- Helpers ----

    private void refreshTab(String tabId) {
        sdk.getTab(tabId, tab -> Platform.runLater(() -> {
            String id = tab.getTabId().getValue();
            if (tab.getStatus() == TabStatus.CLOSED) {
                handleTabClosed(id);
                return;
            }
            tabsById.put(id, toSummary(tab));
            if (id.equals(selectedTabId)) {
                selectedTabDetail = tab;
                rebuildDetails();
            }
            rebuildTiles();
        }), error -> LOG.log(System.Logger.Level.ERROR, "getTab failed: " + error));
    }

    private static TabSummary toSummary(Tab tab) {
        return new TabSummary(
                tab.getTabId(),
                tab.getTabName(),
                tab.getStatus(),
                tab.getTotalAmount(),
                tab.getTotalPaid(),
                tab.getRemaining(),
                tab.getCurrency(),
                tab.getShowingBillTerminalId());
    }

    private String tabName(String tabId) {
        TabSummary summary = tabsById.get(tabId);
        return summary != null ? summary.getTabName() : tabId;
    }

    private void handleTabClosed(String tabId) {
        boolean wasSelected = tabId.equals(selectedTabId);
        tabsById.remove(tabId);
        basketsByTab.remove(tabId);
        if (wasSelected) {
            closeDetails();
        } else {
            rebuildTiles();
        }
    }

    private int basketTotalMinor(String tabId) {
        List<ProductItem> basket = basketsByTab.get(tabId);
        if (basket == null) {
            return 0;
        }
        int total = 0;
        for (ProductItem item : basket) {
            total += PriceUtils.toMinorUnits(item.lineTotal());
        }
        return total;
    }

    private List<ProductItem> freshBasket() {
        List<ProductItem> items = new ArrayList<>();
        for (Product product : Product.getProducts()) {
            items.add(new ProductItem(product));
        }
        return items;
    }

    private void showList() {
        listPane.setVisible(true);
        listPane.setManaged(true);
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
    }

    private void showDetails() {
        listPane.setVisible(false);
        listPane.setManaged(false);
        detailsPane.setVisible(true);
        detailsPane.setManaged(true);
    }
}
