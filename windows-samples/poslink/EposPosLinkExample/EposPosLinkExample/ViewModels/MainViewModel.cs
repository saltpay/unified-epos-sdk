using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Globalization;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models;

namespace EposPosLinkExample.ViewModels;

public partial class MainViewModel : ObservableObject
{
    public ObservableCollection<ProductItem> Products { get; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(TipAmount))]
    [NotifyPropertyChangedFor(nameof(Total))]
    [NotifyPropertyChangedFor(nameof(PayButtonText))]
    public partial string TipInput { get; set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsPayEnabled))]
    [NotifyPropertyChangedFor(nameof(IsSdkSettingUp))]
    public partial bool IsSdkReady { get; private set; }

    [ObservableProperty]
    public partial string SdkStatusMessage { get; private set; } = "";

    public bool IsSdkSettingUp => !IsSdkReady;

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

    private readonly TeyaSdkManager _teyaSdkManager = new();

    public MainViewModel()
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

        _ = StartAndSetupTeyaSdk();
    }

    private async Task StartAndSetupTeyaSdk()
    {
        SdkStatusMessage = "Starting SDK process...";
        _teyaSdkManager.StartProcess();

        SdkStatusMessage = "Initializing SDK...";
        await _teyaSdkManager.Initialize();

        SdkStatusMessage = "Setting up SDK...";
        var setupResponse = await _teyaSdkManager.Setup();
        Debug.WriteLine($"{setupResponse}");

        if (setupResponse.TryGetProperty("response", out JsonElement response) && response.GetString() == "SUCCESS")
        {
            Debug.WriteLine("SDK setup completed successfully.");
            SdkStatusMessage = "SDK ready";
            IsSdkReady = true;
        }
        else
        {
            Debug.WriteLine("SDK setup did not complete successfully.");
            SdkStatusMessage = "SDK setup failed";
        }
    }

    [RelayCommand]
    private async Task Pay()
    {
        var response = await _teyaSdkManager.MakePayment(
           id: Guid.NewGuid().ToString(),
           totalAmount: Decimal.ToInt32(Total * 100),
           tipAmount: Decimal.ToInt32(TipAmount * 100)
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
        var template = BuildCustomPrintTemplate();
        var response = await _teyaSdkManager.PrintCustomTemplate(template);
        Debug.WriteLine($"Printing response: {response}");
    }

    private PrintTemplate BuildCustomPrintTemplate()
    {
        var productsInBasket = Products.Where(p => p.Quantity > 0).ToList();

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
                    new RowElementText { Text = PriceUtils.FormatPrice(TipAmount), Align = Align.RIGHT, Bold = true }
                }
            },
            new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = "TOTAL", Align = Align.LEFT, Bold = true },
                    new RowElementText { Text = PriceUtils.FormatPrice(Total), Align = Align.RIGHT, Bold = true }
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

    [RelayCommand]
    private async Task ClearUserAuth()
    {
        await _teyaSdkManager.ClearUserAuth();
        await _teyaSdkManager.Setup();
    }

    [RelayCommand]
    private async Task ClearDeviceLink()
    {
        await _teyaSdkManager.ClearDeviceLink();
        await _teyaSdkManager.Setup();
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
