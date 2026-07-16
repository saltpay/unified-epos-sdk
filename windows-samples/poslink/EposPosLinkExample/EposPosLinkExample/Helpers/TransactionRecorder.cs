using System;
using EposPosLinkExample.Models;

namespace EposPosLinkExample.Helpers
{
    public class TransactionRecorder
    {
        private readonly TransactionStore _transactionStore = TransactionStore.Instance;

        public void RecordPaymentIfFinal(MakePaymentStateChange state, TransactionType type, bool isTab = false, bool isCash = false)
        {
            if (!state.IsFinal) return;

            _transactionStore.Upsert(new TransactionRecord(
                Id: state.TransactionId,
                Type: type,
                IsSuccess: state.State == MakePaymentStateChange.PaymentState.SUCCESSFUL,
                AmountMinor: state.Amount,
                Currency: state.Currency,
                GatewayPaymentId: state.GatewayPaymentId,
                Timestamp: state.TransactionTimestamp ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds())
            {
                IsCashPayment = isCash,
                IsTabPayment = isTab
            });
        }

        public void RecordRefund(RefundResult result, string gatewayPaymentId, int refundAmount, string currency)
        {
            _transactionStore.Upsert(new TransactionRecord(
                Id: result.GatewayRefundId ?? Guid.NewGuid().ToString(),
                Type: TransactionType.Refund,
                IsSuccess: result.IsSuccess,
                AmountMinor: refundAmount,
                Currency: currency,
                GatewayPaymentId: null,
                Timestamp: DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()));

            if (result.IsSuccess)
            {
                _transactionStore.MarkRefunded(gatewayPaymentId);
            }
        }
    }
}