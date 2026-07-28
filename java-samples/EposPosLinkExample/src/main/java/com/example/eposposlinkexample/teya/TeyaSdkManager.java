package com.example.eposposlinkexample.teya;

import com.example.eposposlinkexample.models.ProductItem;

import com.teya.sdkutilities.Logger;
import com.teya.unifiedepossdk.PaymentStateDetails;
import com.teya.unifiedepossdk.TeyaPosLinkSDK;
import com.teya.unifiedepossdk.models.GatewayPaymentId;
import com.teya.unifiedepossdk.models.TransactionType;
import com.teya.unifiedepossdk.poslink.PosLinkSDK;
import com.teya.unifiedepossdk.poslink.TabEventListener;
import com.teya.unifiedepossdk.poslink.TeyaPosLinkPaymentInProgressUi;
import com.teya.unifiedepossdk.poslink.models.tabs.Tab;
import com.teya.unifiedepossdk.poslink.models.tabs.TabId;
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentContext;
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentMethod;
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class TeyaSdkManager {

    public static final String CURRENCY_CODE = "GBP";

    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";

    private static final System.Logger LOG = System.getLogger("TeyaSdk");

    private static final TeyaSdkManager INSTANCE = new TeyaSdkManager();

    private final BooleanProperty ready = new SimpleBooleanProperty(false);

    private final PosLinkSDK teyaPosLinkSDK = TeyaPosLinkSDK.init(
            new PosLinkSDK.AuthConfig.Managed(CLIENT_ID, CLIENT_SECRET),
            false,
            null,
            new LoggerImpl());

    private TeyaSdkManager() {
    }

    public static TeyaSdkManager getInstance() {
        return INSTANCE;
    }

    /**
     * True once {@link #setup()} has linked a device. The UI binds to this to enable actions.
     */
    public BooleanProperty readyProperty() {
        return ready;
    }

    // ---- Set up ----

    public void setup() {
        teyaPosLinkSDK.setup(
                failure -> {
                    LOG.log(System.Logger.Level.ERROR, "Failed to set up the SDK: " + failure);
                    Platform.runLater(() -> ready.set(false));
                    return Unit.INSTANCE;
                },
                () -> {
                    LOG.log(System.Logger.Level.INFO, "SDK setup successful");
                    Platform.runLater(() -> ready.set(true));
                    return Unit.INSTANCE;
                });
    }

    public void clearUserAuth() {
        teyaPosLinkSDK.clearUserAuth();
        ready.set(false);
    }

    public void clearDeviceLink() {
        teyaPosLinkSDK.clearDeviceLink();
        ready.set(false);
    }

    static final class LoggerImpl implements Logger {

        @Override
        public void d(@NotNull String message) {
            LOG.log(System.Logger.Level.DEBUG, message);
        }

        @Override
        public void i(@NotNull String message) {
            LOG.log(System.Logger.Level.INFO, message);
        }

        @Override
        public void w(@NotNull String message) {
            LOG.log(System.Logger.Level.WARNING, message);
        }

        @Override
        public void e(@NotNull String message) {
            LOG.log(System.Logger.Level.ERROR, message);
        }
    }

    // ---- Sale ----

    public void makePayment(int amountMinor, Integer tipMinor, Consumer<PaymentStateDetails> onStateChanged) {
        teyaPosLinkSDK.getTransactionsApi()
                .makePayment(UUID.randomUUID().toString(), amountMinor, CURRENCY_CODE, tipMinor, null, null)
                .subscribe(state -> {
                    TransactionRecorder.recordPaymentIfFinal(state, TransactionType.Payment, false, false);
                    onStateChanged.accept(state);
                })
                .subscribe(TeyaPosLinkPaymentInProgressUi.create());
    }

    public void printReceipt(List<ProductItem> items, int tipMinor) {
        teyaPosLinkSDK.getPrintingApi()
                .printCustomTemplate(PrintUtils.buildReceiptTemplate(items, tipMinor))
                .subscribe(details -> LOG.log(System.Logger.Level.DEBUG, "Printing state: " + details));
    }

    // ---- Refund ----

    public void refundPayment(String gatewayPaymentId, int amountMinor, String currency, Runnable onSettled) {
        teyaPosLinkSDK.getTransactionsApi()
                .refundPayment(new GatewayPaymentId(gatewayPaymentId), amountMinor, currency, null)
                .subscribe(refundResult -> {
                    LOG.log(System.Logger.Level.DEBUG, "Refund result: " + refundResult);
                    TransactionRecorder.recordRefund(refundResult, gatewayPaymentId, amountMinor, currency);
                    onSettled.run();
                });
    }

    // ---- Pay at Table ----

    public void setPayAtTableEnabled(boolean enable, Runnable onSuccess, Consumer<String> onFailure) {
        teyaPosLinkSDK.getTabsApi().setPayAtTableEnabledOnStore(
                enable,
                () -> {
                    onSuccess.run();
                    return Unit.INSTANCE;
                },
                failure -> {
                    onFailure.accept(String.valueOf(failure));
                    return Unit.INSTANCE;
                });
    }

    public void openTab(String tabId, String tabName, Consumer<Tab> onSuccess, Consumer<String> onFailure) {
        teyaPosLinkSDK.getTabsApi().openTab(
                new TabId(tabId),
                tabName,
                CURRENCY_CODE,
                tab -> {
                    onSuccess.accept(tab);
                    return Unit.INSTANCE;
                },
                failure -> {
                    onFailure.accept(String.valueOf(failure));
                    return Unit.INSTANCE;
                });
    }

    public void listTabs(Consumer<List<TabSummary>> onSuccess, Consumer<String> onFailure) {
        teyaPosLinkSDK.getTabsApi().listTabs(
                null,
                null,
                null,
                null,
                page -> {
                    onSuccess.accept(page.getItems());
                    return Unit.INSTANCE;
                },
                failure -> {
                    onFailure.accept(String.valueOf(failure));
                    return Unit.INSTANCE;
                });
    }

    public void getTab(String tabId, Consumer<Tab> onSuccess, Consumer<String> onFailure) {
        teyaPosLinkSDK.getTabsApi().getTab(
                new TabId(tabId),
                tab -> {
                    onSuccess.accept(tab);
                    return Unit.INSTANCE;
                },
                failure -> {
                    onFailure.accept(String.valueOf(failure));
                    return Unit.INSTANCE;
                });
    }

    public void closeTab(String tabId, Runnable onSuccess, Consumer<String> onFailure) {
        teyaPosLinkSDK.getTabsApi().closeTab(
                new TabId(tabId),
                () -> {
                    onSuccess.run();
                    return Unit.INSTANCE;
                },
                failure -> {
                    onFailure.accept(String.valueOf(failure));
                    return Unit.INSTANCE;
                });
    }

    public void respondToBillRequest(String tabId, String tabName, String terminalId, int totalAmountMinor,
                                     List<ProductItem> billItems, Runnable onSuccess, Consumer<String> onFailure) {
        teyaPosLinkSDK.getTabsApi().respondToBillRequest(
                new TabId(tabId),
                terminalId,
                totalAmountMinor,
                CURRENCY_CODE,
                PrintUtils.buildBillTemplate(tabName, billItems, totalAmountMinor),
                null,
                () -> {
                    onSuccess.run();
                    return Unit.INSTANCE;
                },
                failure -> {
                    onFailure.accept(String.valueOf(failure));
                    return Unit.INSTANCE;
                });
    }

    public void makeTabPayment(TabPaymentContext tabContext, int amountMinor, String currency) {
        teyaPosLinkSDK.getTransactionsApi()
                .makePayment(UUID.randomUUID().toString(), amountMinor, currency, null, null, tabContext)
                .subscribe(state -> {
                    LOG.log(System.Logger.Level.DEBUG, "Tab payment state: " + state);
                    TransactionRecorder.recordPaymentIfFinal(state, TransactionType.Payment, true,
                            tabContext.getMethod() == TabPaymentMethod.CASH);
                });
    }

    public void subscribeToTabEvents(TabEventListener listener) {
        teyaPosLinkSDK.getTabsApi().getTabEvents().subscribe(listener);
    }
}
