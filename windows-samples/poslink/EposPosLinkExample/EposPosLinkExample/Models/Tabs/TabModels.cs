using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace EposPosLinkExample.Models.Tabs;

public enum TabStatus { OPEN, PAYING, PAUSED, COMPLETED, CLOSED, UNKNOWN }

public enum TabPaymentMethod { CARD, CASH }

public enum TabPaymentType { FULL, SPLIT }

public enum PaymentState { PENDING, NEW, IN_PROGRESS, CANCELLING, CANCELED, SUCCESSFUL, PROCESSING_FAILED, COMMUNICATION_FAILED }

public static class PaymentStateExtensions
{
    public static bool IsFinal(this PaymentState state) =>
        state is PaymentState.SUCCESSFUL
            or PaymentState.CANCELED
            or PaymentState.PROCESSING_FAILED
            or PaymentState.COMMUNICATION_FAILED;
}

public record TabSummary(
    [property: JsonPropertyName("tabId")] string TabId,
    [property: JsonPropertyName("tabName")] string Name,
    [property: JsonPropertyName("status")][property: JsonConverter(typeof(JsonStringEnumConverter))] TabStatus Status,
    [property: JsonPropertyName("totalAmount")] int TotalAmount,
    [property: JsonPropertyName("totalPaid")] int? TotalPaid,
    [property: JsonPropertyName("remaining")] int? Remaining,
    [property: JsonPropertyName("currency")] string Currency,
    [property: JsonPropertyName("showingBillTerminalId")] string? ShowingBillTerminalId);

public record PaymentRequestSummary(
    [property: JsonPropertyName("amount")] int Amount,
    [property: JsonPropertyName("tip")] int? Tip,
    [property: JsonPropertyName("method")][property: JsonConverter(typeof(JsonStringEnumConverter))] TabPaymentMethod? Method,
    [property: JsonPropertyName("type")][property: JsonConverter(typeof(JsonStringEnumConverter))] TabPaymentType Type,
    [property: JsonPropertyName("status")][property: JsonConverter(typeof(JsonStringEnumConverter))] PaymentState Status,
    [property: JsonPropertyName("timestampEpochMillis")] long? TimestampEpochMillis);

public record Tab(
    [property: JsonPropertyName("tabId")] string TabId,
    [property: JsonPropertyName("tabName")] string Name,
    [property: JsonPropertyName("status")][property: JsonConverter(typeof(JsonStringEnumConverter))] TabStatus Status,
    [property: JsonPropertyName("totalAmount")] int TotalAmount,
    [property: JsonPropertyName("totalPaid")] int? TotalPaid,
    [property: JsonPropertyName("remaining")] int? Remaining,
    [property: JsonPropertyName("currency")] string Currency,
    [property: JsonPropertyName("showingBillTerminalId")] string? ShowingBillTerminalId,
    [property: JsonPropertyName("paymentRequests")] IReadOnlyList<PaymentRequestSummary>? PaymentRequests)
{
    public TabSummary ToSummary() =>
        new(TabId, Name, Status, TotalAmount, TotalPaid, Remaining, Currency, ShowingBillTerminalId);
}
