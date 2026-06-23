package com.example.eposappexample.poslink.teya

import com.example.eposappexample.poslink.formatMinor
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.models.Product
import com.teya.unifiedepossdk.models.Align
import com.teya.unifiedepossdk.models.ReceiptRow
import com.teya.unifiedepossdk.models.RowElement
import com.teya.unifiedepossdk.models.TableHeaderCell
import com.teya.unifiedepossdk.models.TableRow
import com.teya.unifiedepossdk.models.Template
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintUtils {
    fun buildCustomPrintTemplate(products: List<Product>, tip: Double) = Template(
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

    fun buildTableBillTemplate(tab: TabSummary, items: List<Product>, totalMinor: Int) =
        Template(
            listOf(
                ReceiptRow.Item(
                    RowElement.Text(text = "BILL", align = Align.Center, bold = true)
                ),
                ReceiptRow.Item(
                    RowElement.Text(text = tab.tabName, align = Align.Center)
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
                    rows = items.map { product ->
                        TableRow(
                            cells = listOf(
                                RowElement.Text(
                                    text = "${product.quantity}x ${product.name.uppercase()}",
                                    align = Align.Left
                                ),
                                RowElement.Text(
                                    text = formatPrice(product.price * product.quantity),
                                    align = Align.Right
                                ),
                            )
                        )
                    }
                ),
                ReceiptRow.Divider,
                ReceiptRow.Items(
                    items = listOf(
                        RowElement.Text(text = "TOTAL", align = Align.Left, bold = true),
                        RowElement.Text(
                            text = formatMinor(totalMinor),
                            align = Align.Right,
                            bold = true
                        ),
                    )
                ),
            )
        )
}