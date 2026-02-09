package com.example.eposappexample.aio

import android.util.Log
import com.example.eposappexample.aio.models.Product
import com.teya.sdkutilities.Logger
import com.teya.unifiedepossdk.PaymentStateSubscription
import com.teya.unifiedepossdk.PrintStateDetails
import com.teya.unifiedepossdk.PrintingStatusSubscription
import com.teya.unifiedepossdk.RefundResultSubscription
import com.teya.unifiedepossdk.TeyaCommonTransactionsApi
import com.teya.unifiedepossdk.aio.AllInOneSDK
import com.teya.unifiedepossdk.aio.MissedResponseListener
import com.teya.unifiedepossdk.aio.TeyaAllInOneSDK
import com.teya.unifiedepossdk.models.Align
import com.teya.unifiedepossdk.models.GatewayPaymentId
import com.teya.unifiedepossdk.models.GatewayTransactionId
import com.teya.unifiedepossdk.models.ReceiptRow
import com.teya.unifiedepossdk.models.RowElement
import com.teya.unifiedepossdk.models.TableHeaderCell
import com.teya.unifiedepossdk.models.TableRow
import com.teya.unifiedepossdk.models.Template
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object TeyaUtils {

    val allInOneSDK: AllInOneSDK = TeyaAllInOneSDK(
        onMissedResponseListener = object : MissedResponseListener {
            override fun onMissedPaymentResponse(
                eposTransactionId: String,
                finalState: PaymentStateSubscription.PaymentStateDetails
            ) {
                Log.d("SDK", "Missed payment response: $eposTransactionId, state: $finalState")
            }

            override fun onMissedReceiptResponse(
                gatewayTransactionId: GatewayTransactionId,
                result: PrintStateDetails
            ) {
                Log.d("SDK", "Missed receipt response: $gatewayTransactionId, result: $result")
            }

            override fun onMissedCustomReceiptResponse(
                customReceiptId: String,
                result: PrintStateDetails
            ) {
                Log.d("SDK", "Missed custom receipt response: $customReceiptId, result: $result")
            }

            override fun onMissedRefundResponse(
                gatewayPaymentId: GatewayPaymentId,
                result: RefundResultSubscription.ResultDetails
            ) {
                Log.d("SDK", "Missed refund response: $gatewayPaymentId, result: $result")
            }
        },
        logger = LoggerImpl(), // Optional: your custom logger implementation
        eposInstanceId = null // Optional: identifier for your ePOS app instance
    )

    private var transactionsApi: TeyaCommonTransactionsApi? = null

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
        if (!allInOneSDK.isAvailable) {
            Log.e("SDK", "Teya Payments App is not installed")
            return
        }

        transactionsApi = allInOneSDK.setupTransactionsApi()
        if (transactionsApi != null) {
            Log.d("SDK", "TeyaAllInOneSDK transactions API set up successfully")
        } else {
            Log.e("SDK", "Failed to set up TeyaAllInOneSDK transactions API")
        }
    }

    fun makePayment(amount: Int, tip: Int?) {
        val api = transactionsApi
        if (api == null) {
            Log.e("SDK", "Transactions API not set up")
            return
        }

        val paymentSubscription = api.makePayment(
            transactionId = UUID.randomUUID()
                .toString(), // or pass whatever identifier you already have for the payment you're about to make
            amount = amount, // the total amount to be paid including the tip, in the smallest unit of the currency (e.g., cents).
            currency = "GBP", // The ISO 4217 currency code (e.g., "GBP", "EUR").
            tip = tip // An optional tip amount, in the smallest unit of the currency.
        )

        paymentSubscription.subscribe(
            object : PaymentStateSubscription.PaymentStateChangeListener {
                override fun onPaymentStateChanged(state: PaymentStateSubscription.PaymentStateDetails) {
                    Log.d("SDK", "new state = $state, is it a final state = ${state.isFinal}")
                }
            }
        )
    }

    fun printReceipt(products: List<Product>, tip: Double) {
        allInOneSDK.printingApi?.printCustomTemplate(
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
                        text = SimpleDateFormat("dd/MM/yy · HH:mm", Locale.getDefault()).format(Date()),
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
