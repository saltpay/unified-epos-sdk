import Foundation

@Observable
final class TransactionStore {
    static let shared = TransactionStore()

    private init() {}

    private(set) var transactions: [TransactionRecord] = []

    func upsert(_ record: TransactionRecord) {
        DispatchQueue.main.async {
            self.transactions = [record] + self.transactions.filter { $0.id != record.id }
        }
    }

    func markRefunded(gatewayPaymentId: String) {
        DispatchQueue.main.async {
            self.transactions = self.transactions.map { record in
                // Only payments carry a non-nil gatewayPaymentId, so matching on it is enough.
                guard record.gatewayPaymentId == gatewayPaymentId else { return record }
                var updated = record
                updated.isRefunded = true
                return updated
            }
        }
    }
}
