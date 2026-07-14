import Foundation

@Observable
final class TransactionHistoryViewModel {

    private(set) var refundingIds: Set<String> = []

    func refund(_ record: TransactionRecord) {
        guard let paymentId = record.gatewayPaymentId else { return }
        guard !refundingIds.contains(record.id) else { return }

        refundingIds.insert(record.id)
        TeyaService.shared.refundPayment(
            gatewayPaymentId: paymentId,
            amountMinor: Int32(record.amountMinor),
            currency: record.currency,
            onSettled: { [weak self] in
                DispatchQueue.main.async {
                    self?.refundingIds.remove(record.id)
                }
            }
        )
    }
}
