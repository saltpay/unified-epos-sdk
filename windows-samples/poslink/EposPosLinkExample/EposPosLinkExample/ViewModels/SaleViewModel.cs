using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Globalization;
using System.Linq;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models;

namespace EposPosLinkExample.ViewModels;

public partial class SaleViewModel : ObservableObject
{
    public ObservableCollection<ProductItem> Products { get; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(TipAmount))]
    [NotifyPropertyChangedFor(nameof(Total))]
    [NotifyPropertyChangedFor(nameof(PayButtonText))]
    public partial string TipInput { get; set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsPayEnabled))]
    public partial bool IsSdkReady { get; private set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(ItemCountText))]
    [NotifyPropertyChangedFor(nameof(IsPayEnabled))]
    public partial int ItemCount { get; private set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(SubtotalText))]
    [NotifyPropertyChangedFor(nameof(Total))]
    [NotifyPropertyChangedFor(nameof(PayButtonText))]
    public partial decimal Subtotal { get; private set; }

    public string ItemCountText => $"{ItemCount} item{(ItemCount != 1 ? "s" : "")}";
    public string SubtotalText => $"Subtotal: {PriceUtils.FormatPrice(Subtotal)}";
    public decimal TipAmount => decimal.TryParse(TipInput, NumberStyles.Any, CultureInfo.InvariantCulture, out var v) ? v : 0m;
    public decimal Total => Subtotal + TipAmount;
    public string PayButtonText => $"Pay {PriceUtils.FormatPrice(Total)}";
    public bool IsPayEnabled => ItemCount > 0 && IsSdkReady;

    private readonly TeyaSdkManager _teyaSdkManager = TeyaSdkManager.Instance;

    public SaleViewModel()
    {
        TipInput = "";

        Products = new ObservableCollection<ProductItem>(
            Product.GetProducts().Select(p => new ProductItem(p.Id, p.Name, p.Price))
        );

        foreach (var product in Products)
        {
            product.PropertyChanged += (s, e) =>
            {
                if (e.PropertyName == nameof(ProductItem.Quantity))
                    RefreshTotals();
            };
        }

        IsSdkReady = _teyaSdkManager.IsReady;
        _teyaSdkManager.ReadyChanged += (s, e) => IsSdkReady = _teyaSdkManager.IsReady;
    }

    [RelayCommand]
    private async Task Pay()
    {
        var response = await _teyaSdkManager.MakePayment(
           id: Guid.NewGuid().ToString(),
           totalAmount: decimal.ToInt32(Total * 100),
           tipAmount: decimal.ToInt32(TipAmount * 100)
        );

        Debug.WriteLine($"Payment response: {response}");

        if (response.State == Models.MakePaymentStateChange.PaymentState.SUCCESSFUL)
        {
            ClearBasket();
        }
    }

    [RelayCommand]
    private async Task PrintReceipt()
    {
        var template = BuildCustomPrintTemplate();
        var response = await _teyaSdkManager.PrintCustomTemplate(template);
        Debug.WriteLine($"Printing response: {response}");
    }

    private Models.PrintTemplate BuildCustomPrintTemplate()
    {
        var productsInBasket = Products.Where(p => p.Quantity > 0).ToList();

        var rows = new List<Models.ReceiptRow>
        {
            new Models.ReceiptRowItems
            {
                Items = new List<Models.RowElement>
                {
                    new Models.RowElementText { Text = "CUSTOMER RECEIPT", Align = Models.Align.LEFT, Bold = true },
                    new Models.RowElementText { Text = DateTime.Now.ToString("dd/MM/yy · HH:mm"), Align = Models.Align.RIGHT, Bold = true }
                }
            },
            new Models.ReceiptRowSpacer(),
            new Models.ReceiptRowDivider(),
        };

        foreach (var p in productsInBasket)
        {
            rows.Add(new Models.ReceiptRowItems
            {
                Items = new List<Models.RowElement>
                {
                    new Models.RowElementText { Text = $"{p.Quantity}x {p.Name.ToUpper()}", Align = Models.Align.LEFT, Bold = true },
                    new Models.RowElementText { Text = PriceUtils.FormatPrice(p.Price * p.Quantity), Align = Models.Align.RIGHT, Bold = true }
                }
            });
        }

        rows.AddRange(new Models.ReceiptRow[]
        {
            new Models.ReceiptRowDivider(),
            new Models.ReceiptRowItems
            {
                Items = new List<Models.RowElement>
                {
                    new Models.RowElementText { Text = "TIP", Align = Models.Align.LEFT, Bold = true },
                    new Models.RowElementText { Text = PriceUtils.FormatPrice(TipAmount), Align = Models.Align.RIGHT, Bold = true }
                }
            },
            new Models.ReceiptRowItems
            {
                Items = new List<Models.RowElement>
                {
                    new Models.RowElementText { Text = "TOTAL", Align = Models.Align.LEFT, Bold = true },
                    new Models.RowElementText { Text = PriceUtils.FormatPrice(Total), Align = Models.Align.RIGHT, Bold = true }
                }
            },
            new Models.ReceiptRowItem
            {
                Item = new Models.RowElementQrCode { Url = "https://teya.com", Align = Models.Align.CENTER }
            },
            new Models.ReceiptRowSpacer(),
            new Models.ReceiptRowSpacer(),
            new Models.ReceiptRowItem
            {
                Item = new Models.RowElementText { Text = "Thank you", Align = Models.Align.CENTER, Bold = true }
            }
        });

        return new Models.PrintTemplate { Rows = rows };
    }

    private void RefreshTotals()
    {
        ItemCount = Products.Sum(p => p.Quantity);
        Subtotal = Products.Sum(p => p.Price * p.Quantity);
    }

    private void ClearBasket()
    {
        foreach (var product in Products)
        {
            product.Quantity = 0;
        }
        TipInput = "";
    }
}
