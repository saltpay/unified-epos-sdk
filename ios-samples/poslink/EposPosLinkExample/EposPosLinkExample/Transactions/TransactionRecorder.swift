import Foundation
import TeyaUnifiedEposSDK

enum TransactionRecorder {

    static func recordPaymentIfFinal(
        state: TeyaPaymentStateDetails,
        type: TeyaTransactionType,
        isTab: Bool = false,
        isCash: Bool = false
    ) {
        guard state.isFinal else { return }
        TransactionStore.shared.upsert(
            TransactionRecord(
                id: state.eposTransactionId,
                type: type,
                isSuccess: state.state == .successful,
                amountMinor: Int(state.amount),
                currency: state.currency,
                gatewayPaymentId: state.gatewayPaymentId?.id,
                timestamp: state.transactionTimestamp?.asInt64 ?? Int64(Date().timeIntervalSince1970 * 1000),
                isCashPayment: isCash,
                isTabPayment: isTab
            )
        )
    }

    static func recordRefund(
        refundResult: TeyaRefundResultDetails,
        gatewayPaymentId: String,
        amountMinor: Int32,
        currency: String
    ) {
        TransactionStore.shared.upsert(
            TransactionRecord(
                id: refundResult.gatewayRefundId?.id ?? UUID().uuidString,
                type: .refund,
                isSuccess: refundResult.result == TeyaRefundResult.success,
                amountMinor: Int(amountMinor),
                currency: currency,
                gatewayPaymentId: nil,
                timestamp: Int64(Date().timeIntervalSince1970 * 1000)
            )
        )
        if refundResult.result == TeyaRefundResult.success {
            TransactionStore.shared.markRefunded(gatewayPaymentId: gatewayPaymentId)
        }
    }
}