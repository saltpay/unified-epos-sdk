using System.Text.Json.Serialization;

namespace EposPosLinkExample.Models
{
    public class UnreferencedRefundStateChange
    {
        [JsonPropertyName("transactionId")]
        public required string TransactionId { get; set; }

        [JsonPropertyName("amount")]
        public int Amount { get; set; }

        [JsonPropertyName("currency")]
        public required string Currency { get; set; }

        [JsonPropertyName("gatewayRefundId")]
        public string? GatewayRefundId { get; set; }

        [JsonPropertyName("state")]
        [JsonConverter(typeof(JsonStringEnumConverter))]
        public MakePaymentStateChange.PaymentState State { get; set; }

        [JsonPropertyName("inProgressState")]
        [JsonConverter(typeof(JsonStringEnumConverter))]
        public MakePaymentStateChange.InProgressState? ProgressState { get; set; }

        [JsonPropertyName("isFinal")]
        public bool IsFinal { get; set; }

        [JsonPropertyName("reason")]
        [JsonConverter(typeof(JsonStringEnumConverter))]
        public MakePaymentStateChange.PaymentStateReason? Reason { get; set; }

        [JsonPropertyName("transactionTimestamp")]
        public long? TransactionTimestamp { get; set; }

        [JsonPropertyName("debugErrorMessage")]
        public string? DebugErrorMessage { get; set; }

        public bool IsSuccess => State == MakePaymentStateChange.PaymentState.SUCCESSFUL && GatewayRefundId != null;

        public override string ToString() =>
            $"UnreferencedRefundStateChange {{ TransactionId: {TransactionId}, GatewayRefundId: {GatewayRefundId}, " +
            $"State: {State}, ProgressState: {ProgressState}, Amount: {Amount}, Currency: {Currency}, " +
            $"IsFinal: {IsFinal}, Reason: {Reason}, TransactionTimestamp: {TransactionTimestamp}, " +
            $"DebugErrorMessage: {DebugErrorMessage} }}";
    }
}
