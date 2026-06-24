using System.Collections.Generic;

namespace EposPosLinkExample.Models.Tabs;

public enum TabStatus { Open, Paying, Paused, Completed, Closed, Unknown }

public enum TabPaymentMethod { Card, Mobile, Cash, Unknown }

public enum TabPaymentType { Sale, Refund }

public enum PaymentState { Pending, InProgress, Successful, Canceled, ProcessingFailed, CommunicationFailed, Unknown }

public static class PaymentStateExtensions
{
    public static bool IsFinal(this PaymentState state) =>
        state is PaymentState.Successful
            or PaymentState.Canceled
            or PaymentState.ProcessingFailed
            or PaymentState.CommunicationFailed;
}

public record TabSummary(
    string TabId,
    string Name,
    TabStatus Status,
    int TotalAmount,
    int? TotalPaid,
    int? Remaining,
    string Currency,
    string? ShowingBillTerminalId);

public record PaymentRequestSummary(
    int Amount,
    int? Tip,
    TabPaymentMethod? Method,
    TabPaymentType Type,
    PaymentState Status,
    long? TimestampEpochMillis);

public record Tab(
    string TabId,
    string Name,
    TabStatus Status,
    int TotalAmount,
    int? TotalPaid,
    int? Remaining,
    string Currency,
    string? ShowingBillTerminalId,
    IReadOnlyList<PaymentRequestSummary> PaymentRequests)
{
    public TabSummary ToSummary() =>
        new(TabId, Name, Status, TotalAmount, TotalPaid, Remaining, Currency, ShowingBillTerminalId);
}
