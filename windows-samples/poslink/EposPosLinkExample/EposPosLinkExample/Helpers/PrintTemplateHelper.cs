using System;
using System.Collections.Generic;
using System.Linq;
using EposPosLinkExample.Models;
using EposPosLinkExample.ViewModels;

namespace EposPosLinkExample.Helpers;

public static class ReceiptTemplateBuilder
{
    public static PrintTemplate BuildSaleReceiptTemplate(IEnumerable<ProductItem> products, decimal tipAmount, decimal total)
    {
        var productsInBasket = products.Where(p => p.Quantity > 0).ToList();

        var rows = new List<ReceiptRow>
        {
            new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = "CUSTOMER RECEIPT", Align = Align.LEFT, Bold = true },
                    new RowElementText { Text = DateTime.Now.ToString("dd/MM/yy · HH:mm"), Align = Align.RIGHT, Bold = true }
                }
            },
            new ReceiptRowSpacer(),
            new ReceiptRowDivider(),
        };

        foreach (var p in productsInBasket)
        {
            rows.Add(new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = $"{p.Quantity}x {p.Name.ToUpper()}", Align = Align.LEFT, Bold = true },
                    new RowElementText { Text = PriceUtils.FormatPrice(p.Price * p.Quantity), Align = Align.RIGHT, Bold = true }
                }
            });
        }

        rows.AddRange(new ReceiptRow[]
        {
            new ReceiptRowDivider(),
            new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = "TIP", Align = Align.LEFT, Bold = true },
                    new RowElementText { Text = PriceUtils.FormatPrice(tipAmount), Align = Align.RIGHT, Bold = true }
                }
            },
            new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = "TOTAL", Align = Align.LEFT, Bold = true },
                    new RowElementText { Text = PriceUtils.FormatPrice(total), Align = Align.RIGHT, Bold = true }
                }
            },
            new ReceiptRowItem
            {
                Item = new RowElementQrCode { Url = "https://teya.com", Align = Align.CENTER }
            },
            new ReceiptRowSpacer(),
            new ReceiptRowSpacer(),
            new ReceiptRowItem
            {
                Item = new RowElementText { Text = "Thank you", Align = Align.CENTER, Bold = true }
            }
        });

        return new PrintTemplate { Rows = rows };
    }

    public static PrintTemplate BuildTableBillTemplate(string tabName, IReadOnlyList<TabProductItem> items, int totalMinor)
    {
        var rows = new List<ReceiptRow>
        {
            new ReceiptRowItem
            {
                Item = new RowElementText { Text = "BILL", Align = Align.CENTER, Bold = true }
            },
            new ReceiptRowItem
            {
                Item = new RowElementText { Text = tabName, Align = Align.CENTER }
            },
            new ReceiptRowSpacer(),
            new ReceiptRowDivider(),
        };

        foreach (var item in items)
        {
            rows.Add(new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = $"{item.Quantity}x {item.Name.ToUpper()}", Align = Align.LEFT },
                    new RowElementText { Text = PriceUtils.FormatPrice(item.Price * item.Quantity), Align = Align.RIGHT }
                }
            });
        }

        rows.Add(new ReceiptRowDivider());
        rows.Add(new ReceiptRowItems
        {
            Items = new List<RowElement>
            {
                new RowElementText { Text = "TOTAL", Align = Align.LEFT, Bold = true },
                new RowElementText { Text = PriceUtils.FormatMinor(totalMinor), Align = Align.RIGHT, Bold = true }
            }
        });

        return new PrintTemplate { Rows = rows };
    }
}