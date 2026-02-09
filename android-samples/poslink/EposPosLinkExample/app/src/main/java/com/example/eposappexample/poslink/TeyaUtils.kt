package com.example.eposappexample.poslink

import android.util.Log
import com.example.eposappexample.poslink.models.Product
import com.teya.sdkutilities.Logger
import com.teya.unifiedepossdk.PaymentStateSubscription
import com.teya.unifiedepossdk.PrintStateDetails
import com.teya.unifiedepossdk.PrintingStatusSubscription
import com.teya.unifiedepossdk.TeyaPosLinkSDK
import com.teya.unifiedepossdk.models.Align
import com.teya.unifiedepossdk.models.ReceiptRow
import com.teya.unifiedepossdk.models.RowElement
import com.teya.unifiedepossdk.models.TableHeaderCell
import com.teya.unifiedepossdk.models.TableRow
import com.teya.unifiedepossdk.models.Template
import com.teya.unifiedepossdk.poslink.PosLinkSDK
import com.teya.unifiedepossdk.poslink.TeyaPosLinkPaymentInProgressUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object TeyaUtils {

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

    fun makePayment(amount: Int, tip: Int?) {
        val paymentSubscription = teyaPosLinkSDK.transactionsApi?.makePayment(
            transactionId = UUID.randomUUID()
                .toString(), // or pass whatever identifier you already have for the payment you're about to make
            amount = amount, // the total amount to be paid including the tip, in the smallest unit of the currency (e.g., cents).
            currency = "GBP", // The ISO 4217 currency code (e.g., "GBP", "EUR").
            tip = tip // An optional tip amount, in the smallest unit of the currency.
        )

        paymentSubscription?.subscribe(
            object : PaymentStateSubscription.PaymentStateChangeListener {
                override fun onPaymentStateChanged(state: PaymentStateSubscription.PaymentStateDetails) {
                    Log.d("SDK", "new state = $state, is it a final state = ${state.isFinal}")
                }
            }
        )

        paymentSubscription?.subscribe(
            TeyaPosLinkPaymentInProgressUi(
                autoDismissOnFinalStateAfterMs = 2000, // Configurable. Time in ms before the UI auto-dismisses after a final payment state.
                onDismiss = { // Optional callback invoked after dismissing the UI with the current PaymentStateDetails.
                    Log.d("SDK", "Payment UI dismissed with payment state details: $it")
                }
            )
        )
    }

    fun printReceipt(products: List<Product>, tip: Double) {
        teyaPosLinkSDK.printingApi?.printCustomTemplate(
            buildCustomPrintTemplate(products, tip)
        )?.subscribe(
            object : PrintingStatusSubscription.Listener {
                override fun onPrintingStateChanged(printStateDetails: PrintStateDetails) {
                    Log.d("SDK", "Printing state changed: $printStateDetails")
                }
            }
        )
    }

    private fun buildCustomPrintTemplate(products: List<Product>, tip: Double) = Template(
        listOf(
            ReceiptRow.Items(
                items = listOf(
                    RowElement.Text(
                        text = "CUSTOMER RECEIPT",
                        align = Align.Left,
                        bold = true
                    ),
                    RowElement.Text(
                        text = SimpleDateFormat(
                            "dd/MM/yy · HH:mm",
                            Locale.getDefault()
                        ).format(Date()),
                        align = Align.Right,
                        bold = true
                    ),
                )
            ),

            ReceiptRow.Spacer,
            ReceiptRow.Divider,

            ReceiptRow.Table(
                headerCells = listOf(
                    TableHeaderCell(
                        element = RowElement.Text(
                            text = "ITEM",
                            bold = true,
                            align = Align.Left
                        ),
                        1f
                    ),
                    TableHeaderCell(
                        element = RowElement.Text(
                            text = "PRICE",
                            bold = true,
                            align = Align.Right
                        ),
                        1f
                    ),
                ),
                rows = products.map { product ->
                    TableRow(
                        cells = listOf(
                            RowElement.Text(
                                text = "${product.quantity}x ${product.name.uppercase()}",
                                align = Align.Left,
                                bold = true
                            ),
                            RowElement.Text(
                                text = formatPrice(product.price * product.quantity),
                                align = Align.Right,
                                bold = true
                            ),
                        )
                    )
                }
            ),

            ReceiptRow.Divider,

            ReceiptRow.Items(
                items = listOf(
                    RowElement.Text(
                        text = "TIP",
                        align = Align.Left,
                        bold = true
                    ),
                    RowElement.Text(
                        text = formatPrice(tip),
                        align = Align.Right,
                        bold = true
                    )
                )
            ),
            ReceiptRow.Items(
                items = listOf(
                    RowElement.Text(
                        text = "TOTAL",
                        align = Align.Left,
                        bold = true
                    ),
                    RowElement.Text(
                        text = formatPrice(products.sumOf { it.price * it.quantity } + tip),
                        align = Align.Right,
                        bold = true
                    )
                )
            ),

            ReceiptRow.Item(
                RowElement.QrCode(
                    url = "https://teya.com",
                    align = Align.Center
                )
            ),

            ReceiptRow.Spacer,
            ReceiptRow.Spacer,

            ReceiptRow.Item(
                RowElement.Text(
                    text = "Thank you",
                    align = Align.Center,
                    bold = true
                )
            ),
        )
    )
}