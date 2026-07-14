namespace EposPosLinkExample.Models
{
    public enum TransactionType { Payment, Refund }

    public record TransactionRecord(
        string Id,
        TransactionType Type,
        bool IsSuccess,
        int AmountMinor,
        string Currency,
        string? GatewayPaymentId,
        long Timestamp)
    {
        public bool IsRefunded { get; init; }

        public bool IsRefundable => Type == TransactionType.Payment && IsSuccess && !IsRefunded && GatewayPaymentId != null;
    }
}
