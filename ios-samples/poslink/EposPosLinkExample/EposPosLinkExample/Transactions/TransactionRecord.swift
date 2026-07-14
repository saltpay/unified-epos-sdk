import Foundation
import TeyaUnifiedEposSDK

struct TransactionRecord: Identifiable, Equatable {
    let id: String
    let type: TeyaTransactionType
    let isSuccess: Bool
    let amountMinor: Int
    let currency: String
    let gatewayPaymentId: String?
    let timestamp: Int64
    var isRefunded: Bool = false

    var isRefundable: Bool {
        type == .payment && isSuccess && !isRefunded && gatewayPaymentId != nil
    }
}
