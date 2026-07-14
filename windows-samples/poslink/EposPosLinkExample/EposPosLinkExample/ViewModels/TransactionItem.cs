using System;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models;
using Microsoft.UI.Xaml;

namespace EposPosLinkExample.ViewModels;

public partial class TransactionItem : ObservableObject
{
    private readonly TransactionRecord _record;
    private readonly TeyaSdkManager _sdk = TeyaSdkManager.Instance;

    public TransactionItem(TransactionRecord record)
    {
        _record = record;
    }

    public string Id => _record.Id;

    public string AmountFormatted => PriceUtils.FormatMinor(_record.AmountMinor);
    public string TypeLabel => _record.Type == TransactionType.Payment ? "Payment" : "Refund";
    public string TimestampFormatted =>
        DateTimeOffset.FromUnixTimeMilliseconds(_record.Timestamp).LocalDateTime.ToString("HH:mm • dd MMM yyyy");

    // The status tag binds this through the StatusTo* converters (same path as Pay at Table).
    public TransactionRecord Status => _record;

    public Visibility RefundVisibility => _record.IsRefundable ? Visibility.Visible : Visibility.Collapsed;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(RefundButtonText))]
    [NotifyCanExecuteChangedFor(nameof(RefundCommand))]
    public partial bool IsRefunding { get; private set; }

    public string RefundButtonText => IsRefunding ? "Refunding..." : "Refund";

    private bool CanRefund() => _record.IsRefundable && !IsRefunding;

    [RelayCommand(CanExecute = nameof(CanRefund))]
    private async Task Refund()
    {
        if (_record.GatewayPaymentId is not { } gatewayPaymentId) return;

        IsRefunding = true;
        try
        {
            await _sdk.RefundPayment(gatewayPaymentId, _record.AmountMinor, _record.Currency);
        }
        finally
        {
            IsRefunding = false;
        }
    }
}
