using System;
using System.Collections.Generic;
using System.Linq;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models.Tabs;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace EposPosLinkExample.Views;

public sealed partial class PaymentsDialog : ContentDialog
{
    public PaymentsDialog(Tab tab)
    {
        InitializeComponent();
        PaidText.Text = PriceUtils.FormatMinor(tab.TotalPaid ?? 0);
        if (tab.Remaining is int remaining)
        {
            RemainingText.Text = PriceUtils.FormatMinor(remaining);
        }
        else
        {
            RemainingRow.Visibility = Visibility.Collapsed;
        }
        PaymentsList.ItemsSource = tab.PaymentRequests.Select(p => new PaymentRow(p)).ToList();
    }
}

public sealed class PaymentRow
{
    public PaymentState State { get; }
    public string AmountText { get; }
    public string Descriptor { get; }
    public string TipText { get; }
    public Visibility TipVisibility { get; }
    public string TimestampText { get; }
    public Visibility TimestampVisibility { get; }

    public PaymentRow(PaymentRequestSummary p)
    {
        State = p.Status;
        AmountText = PriceUtils.FormatMinor(p.Amount);

        var parts = new List<string>();
        if (p.Method is TabPaymentMethod m) parts.Add(Capitalize(m.ToString()));
        parts.Add(Capitalize(p.Type.ToString()));
        Descriptor = string.Join(" · ", parts);

        if (p.Tip is int tip && tip > 0)
        {
            TipText = $"Tip: {PriceUtils.FormatMinor(tip)}";
            TipVisibility = Visibility.Visible;
        }
        else
        {
            TipText = "";
            TipVisibility = Visibility.Collapsed;
        }

        if (p.TimestampEpochMillis is long ms)
        {
            TimestampText = DateTimeOffset.FromUnixTimeMilliseconds(ms).LocalDateTime.ToString("dd/MM/yy · HH:mm");
            TimestampVisibility = Visibility.Visible;
        }
        else
        {
            TimestampText = "";
            TimestampVisibility = Visibility.Collapsed;
        }
    }

    private static string Capitalize(string s) =>
        string.IsNullOrEmpty(s) ? s : char.ToUpper(s[0]) + s.Substring(1).ToLower();
}
