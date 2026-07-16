package com.example.eposappexample.poslink.teya

import android.util.Log
import com.example.eposappexample.poslink.CURRENCY_CODE
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.transactions.TransactionRecorder
import com.teya.sdkutilities.Logger
import com.teya.unifiedepossdk.PaymentStateDetails
import com.teya.unifiedepossdk.PaymentStateSubscription
import com.teya.unifiedepossdk.PrintStateDetails
import com.teya.unifiedepossdk.PrintingStatusSubscription
import com.teya.unifiedepossdk.RefundResultDetails
import com.teya.unifiedepossdk.RefundResultSubscription
import com.teya.unifiedepossdk.TeyaPosLinkSDK
import com.teya.unifiedepossdk.models.GatewayPaymentId
import com.teya.unifiedepossdk.models.TransactionType
import com.teya.unifiedepossdk.poslink.PosLinkSDK
import com.teya.unifiedepossdk.poslink.PosLinkTabsApi
import com.teya.unifiedepossdk.poslink.TabEventListener
import com.teya.unifiedepossdk.poslink.TeyaPosLinkPaymentInProgressUi
import com.teya.unifiedepossdk.poslink.models.tabs.Tab
import com.teya.unifiedepossdk.poslink.models.tabs.TabId
import com.teya.unifiedepossdk.poslink.models.tabs.TabPage
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentContext
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentMethod
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary
import java.util.UUID

object TeyaUtils {

    // ---- Set up ----

    val teyaPosLinkSDK = TeyaPosLinkSDK(
        isProductionEnv = false, // Set to true for production
        authConfig = PosLinkSDK.AuthConfig.Managed(
            clientId = "",  // Replace with your Client ID
            clientSecret = ""  // Replace with your Client Secret
        ),
        eposInstanceId = null,  // Optional: identifier for your ePOS app instance
        logger = LoggerImpl()   // Optional: your custom logger implementation
    )

    class LoggerImpl : Logger {
        override fun d(message: String) {
            Log.d("SDK", message)
        }

        override fun i(message: String) {
            Log.i("SDK", message)
        }

        override fun w(message: String) {
            Log.w("SDK", message)
        }

        override fun e(message: String) {
            Log.e("SDK", message)
        }
    }

    fun setUp() {
        teyaPosLinkSDK.setup(
            onFailure = {
                Log.e("SDK", "Failed to initialize TeyaPosLinkSDK: $it")
            },
            onSuccess = {
                Log.d("SDK", "TeyaPosLinkSDK initialized successfully")
            }
        )
    }

    fun clearUserAuth() {
        teyaPosLinkSDK.clearUserAuth()
    }

    fun clearDeviceLink() {
        teyaPosLinkSDK.clearDeviceLink()
    }

    // ---- Sale ----

    fun makePayment(amount: Int, tip: Int?) {
        val paymentSubscription = teyaPosLinkSDK.transactionsApi.makePayment(
            transactionId = UUID.randomUUID()
                .toString(), // or pass whatever identifier you already have for the payment you're about to make
            amount = amount, // the total amount to be paid including the tip, in the smallest unit of the currency (e.g., cents).
            currency = CURRENCY_CODE, // The ISO 4217 currency code (e.g., "GBP", "EUR").
            tip = tip // An optional tip amount, in the smallest unit of the currency.
        )

        paymentSubscription.subscribe(
            object : PaymentStateSubscription.PaymentStateChangeListener {
                override fun onPaymentStateChanged(state: PaymentStateDetails) {
                    Log.d("SDK", "new state = $state, is it a final state = ${state.isFinal}")
                    TransactionRecorder.recordPaymentIfFinal(state, TransactionType.Payment)
                }
            }
        )

        paymentSubscription.subscribe(
            TeyaPosLinkPaymentInProgressUi(
                autoDismissOnFinalStateAfterMs = 2000, // Configurable. Time in ms before the UI auto-dismisses after a final payment state.
                onDismiss = { // Optional callback invoked after dismissing the UI with the current PaymentStateDetails.
                    Log.d("SDK", "Payment UI dismissed with payment state details: $it")
                }
            )
        )
    }

    fun printReceipt(products: List<Product>, tip: Double) {
        teyaPosLinkSDK.printingApi.printCustomTemplate(
            PrintUtils.buildCustomPrintTemplate(products, tip)
        ).subscribe(
            object : PrintingStatusSubscription.Listener {
                override fun onPrintingStateChanged(printStateDetails: PrintStateDetails) {
                    Log.d("SDK", "Printing state changed: $printStateDetails")
                }
            }
        )
    }

    // ---- Refund ----

    fun refundPayment(
        gatewayPaymentId: String,
        amountMinor: Int,
        currency: String,
        onSettled: () -> Unit,
    ) {
        teyaPosLinkSDK.transactionsApi.refundPayment(
            paymentId = GatewayPaymentId(gatewayPaymentId),
            amount = amountMinor,
            currency = currency,
        ).subscribe(
            object : RefundResultSubscription.RefundResultListener {
                override fun onRefundResult(refundResult: RefundResultDetails) {
                    Log.d("SDK", "Refund result: $refundResult")
                    TransactionRecorder.recordRefund(
                        refundResult = refundResult,
                        gatewayPaymentId = gatewayPaymentId,
                        amountMinor = amountMinor,
                        currency = currency,
                    )
                    onSettled()
                }
            }
        )
    }

    fun makeUnreferencedRefund(amount: Int) {
        val refundSubscription = teyaPosLinkSDK.transactionsApi.makeUnreferencedRefund(
            transactionId = UUID.randomUUID().toString(),
            amount = amount,
            currency = CURRENCY_CODE,
        )

        refundSubscription.subscribe(
            object : PaymentStateSubscription.PaymentStateChangeListener {
                override fun onPaymentStateChanged(state: PaymentStateDetails) {
                    Log.d("SDK", "unreferenced refund state = $state, final = ${state.isFinal}")
                    TransactionRecorder.recordPaymentIfFinal(state, TransactionType.Refund)
                }
            }
        )

        refundSubscription.subscribe(
            TeyaPosLinkPaymentInProgressUi(
                autoDismissOnFinalStateAfterMs = 2000,
                onDismiss = {
                    Log.d("SDK", "Unreferenced refund UI dismissed with details: $it")
                }
            )
        )
    }

    // ---- Pay at Table ----

    /** Enables or disables Pay at Table for the linked store. */
    fun setPayAtTableEnabled(
        enable: Boolean,
        onSuccess: () -> Unit,
        onFailure: (PosLinkTabsApi.TabOperationFailure) -> Unit
    ) {
        teyaPosLinkSDK.tabsApi.setPayAtTableEnabledOnStore(enable, onSuccess, onFailure)
    }

    fun openTab(
        tabId: String,
        tabName: String,
        onSuccess: (Tab) -> Unit,
        onFailure: (PosLinkTabsApi.TabOperationFailure) -> Unit
    ) {
        teyaPosLinkSDK.tabsApi.openTab(
            tabId = TabId(tabId),
            tabName = tabName,
            currency = CURRENCY_CODE,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun listTabs(
        onSuccess: (TabPage) -> Unit,
        onFailure: (PosLinkTabsApi.TabOperationFailure) -> Unit
    ) {
        teyaPosLinkSDK.tabsApi.listTabs(onSuccess = onSuccess, onFailure = onFailure)
    }

    fun getTab(
        tabId: TabId,
        onSuccess: (Tab) -> Unit,
        onFailure: (PosLinkTabsApi.TabOperationFailure) -> Unit
    ) {
        teyaPosLinkSDK.tabsApi.getTab(tabId, onSuccess, onFailure)
    }

    fun closeTab(
        tabId: TabId,
        onSuccess: () -> Unit,
        onFailure: (PosLinkTabsApi.TabOperationFailure) -> Unit
    ) {
        teyaPosLinkSDK.tabsApi.closeTab(tabId, onSuccess, onFailure)
    }

    fun subscribeToTabEvents(listener: TabEventListener) {
        teyaPosLinkSDK.tabsApi.tabEvents.subscribe(listener)
    }

    fun unsubscribeFromTabEvents(listener: TabEventListener) {
        teyaPosLinkSDK.tabsApi.tabEvents.unsubscribe(listener)
    }

    /** Responds to a SHOW_BILL_REQUEST by sending the bill back to the requesting terminal. */
    fun respondToBillRequest(
        tab: TabSummary,
        terminalId: String,
        totalAmountMinor: Int,
        billItems: List<Product>,
        onSuccess: () -> Unit,
        onFailure: (PosLinkTabsApi.ShowBillFailure) -> Unit
    ) {
        teyaPosLinkSDK.tabsApi.respondToBillRequest(
            tabId = tab.tabId,
            terminalId = terminalId,
            totalAmount = totalAmountMinor,
            currency = CURRENCY_CODE,
            printModel = PrintUtils.buildTableBillTemplate(tab, billItems, totalAmountMinor),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /** Responds to a PAY_REQUEST by starting a tab-tagged payment and recording its final state. */
    fun makeTabPayment(tabContext: TabPaymentContext, amount: Int, currency: String) {
        val subscription = teyaPosLinkSDK.transactionsApi.makePayment(
            transactionId = UUID.randomUUID().toString(),
            amount = amount,
            currency = currency,
            tip = null,
            tabContext = tabContext
        )
        subscription.subscribe(
            object : PaymentStateSubscription.PaymentStateChangeListener {
                override fun onPaymentStateChanged(state: PaymentStateDetails) {
                    Log.d("SDK", "Tab payment state = $state, final = ${state.isFinal}")
                    TransactionRecorder.recordPaymentIfFinal(
                        state,
                        TransactionType.Payment,
                        isTab = true,
                        isCash = tabContext.method == TabPaymentMethod.CASH,
                    )
                }
            }
        )
    }
}