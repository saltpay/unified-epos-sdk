using System;
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

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(PayButtonText))]
    public partial bool UnreferencedRefund { get; set; }

    public string ItemCountText => $"{ItemCount} item{(ItemCount != 1 ? "s" : "")}";
    public string SubtotalText => $"Subtotal: {PriceUtils.FormatPrice(Subtotal)}";
    public decimal TipAmount => decimal.TryParse(TipInput, NumberStyles.Any, CultureInfo.InvariantCulture, out var v) ? v : 0m;
    public decimal Total => Subtotal + TipAmount;
    public string PayButtonText =>
        UnreferencedRefund ? $"Refund {PriceUtils.FormatPrice(Total)}" : $"Pay {PriceUtils.FormatPrice(Total)}";
    public bool IsPayEnabled => ItemCount > 0 && IsSdkReady;

    private readonly TeyaSdkManager _teyaSdkManager = TeyaSdkManager.Instance;

    public SaleViewModel()
    {
        TipInput = "";

        Products = new ObservableCollection<ProductItem>(
            Product.GetProducts().Select(p => new ProductItem(p.Id, p.Name, p.Price, p.Emoji))
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
        if (UnreferencedRefund)
        {
            var refund = await _teyaSdkManager.MakeUnreferencedRefund(
                id: Guid.NewGuid().ToString(),
                amount: decimal.ToInt32(Total * 100),
                currency: "GBP"
            );

            Debug.WriteLine($"Unreferenced refund response: {refund}");

            if (refund.State == MakePaymentStateChange.PaymentState.SUCCESSFUL)
            {
                ClearBasket();
            }
            return;
        }

        var response = await _teyaSdkManager.MakePayment(
           id: Guid.NewGuid().ToString(),
           totalAmount: decimal.ToInt32(Total * 100),
           tipAmount: decimal.ToInt32(TipAmount * 100)
        );

        Debug.WriteLine($"Payment response: {response}");

        if (response.State == MakePaymentStateChange.PaymentState.SUCCESSFUL)
        {
            ClearBasket();
        }
    }

    [RelayCommand]
    private async Task PrintReceipt()
    {
        var template = ReceiptTemplateBuilder.BuildSaleReceiptTemplate(Products, TipAmount, Total);
        var response = await _teyaSdkManager.PrintCustomTemplate(template);
        Debug.WriteLine($"Printing response: {response}");
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
