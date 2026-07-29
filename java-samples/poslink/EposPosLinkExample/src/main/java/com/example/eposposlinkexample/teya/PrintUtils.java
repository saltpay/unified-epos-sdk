package com.example.eposposlinkexample.teya;

import com.example.eposposlinkexample.models.ProductItem;
import com.example.eposposlinkexample.util.PriceUtils;

import com.teya.unifiedepossdk.models.Align;
import com.teya.unifiedepossdk.models.ReceiptRow;
import com.teya.unifiedepossdk.models.RowElement;
import com.teya.unifiedepossdk.models.TableHeaderCell;
import com.teya.unifiedepossdk.models.TableRow;
import com.teya.unifiedepossdk.models.Template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class PrintUtils {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yy · HH:mm");

    private PrintUtils() {
    }

    static Template buildReceiptTemplate(List<ProductItem> items, int tipMinor) {
        return new Template(List.of(
                new ReceiptRow.Items(List.of(
                        new RowElement.Text("CUSTOMER RECEIPT", true, Align.Left),
                        new RowElement.Text(LocalDateTime.now().format(TIMESTAMP), true, Align.Right))),
                ReceiptRow.Spacer.INSTANCE,
                ReceiptRow.Divider.INSTANCE,
                itemsTable(items),
                ReceiptRow.Divider.INSTANCE,
                amountRow("TIP", tipMinor),
                amountRow("TOTAL", totalMinor(items) + tipMinor),
                new ReceiptRow.Item(new RowElement.QrCode("https://teya.com", Align.Center)),
                ReceiptRow.Spacer.INSTANCE,
                ReceiptRow.Spacer.INSTANCE,
                new ReceiptRow.Item(new RowElement.Text("Thank you", true, Align.Center))));
    }

    static Template buildBillTemplate(String title, List<ProductItem> items, int totalMinor) {
        return new Template(List.of(
                new ReceiptRow.Item(new RowElement.Text("BILL", true, Align.Center)),
                new ReceiptRow.Item(new RowElement.Text(title, false, Align.Center)),
                ReceiptRow.Spacer.INSTANCE,
                ReceiptRow.Divider.INSTANCE,
                itemsTable(items),
                ReceiptRow.Divider.INSTANCE,
                amountRow("TOTAL", totalMinor)));
    }

    private static ReceiptRow.Table itemsTable(List<ProductItem> items) {
        List<TableHeaderCell> header = List.of(
                new TableHeaderCell(new RowElement.Text("ITEM", true, Align.Left), 1f),
                new TableHeaderCell(new RowElement.Text("PRICE", true, Align.Right), 1f));

        List<TableRow> rows = items.stream()
                .filter(item -> item.getQuantity() > 0)
                .map(item -> new TableRow(List.of(
                        new RowElement.Text(
                                item.getQuantity() + "x " + item.product().name().toUpperCase(), false, Align.Left),
                        new RowElement.Text(PriceUtils.formatPrice(item.lineTotal()), false, Align.Right))))
                .toList();

        return new ReceiptRow.Table(header, rows);
    }

    private static ReceiptRow.Items amountRow(String label, int amountMinor) {
        return new ReceiptRow.Items(List.of(
                new RowElement.Text(label, true, Align.Left),
                new RowElement.Text(PriceUtils.formatMinor(amountMinor), true, Align.Right)));
    }

    private static int totalMinor(List<ProductItem> items) {
        return items.stream().mapToInt(item -> PriceUtils.toMinorUnits(item.lineTotal())).sum();
    }
}
