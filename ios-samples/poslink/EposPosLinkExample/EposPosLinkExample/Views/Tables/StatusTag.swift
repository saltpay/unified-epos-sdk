import SwiftUI
import Lemonade
import TeyaUnifiedEposSDK

struct StatusTag: View {
    let status: TeyaTabStatus

    private var label: String {
        if status == .open { return "Open" }
        if status == .paying { return "Paying" }
        if status == .paused { return "Paused" }
        if status == .completed { return "Completed" }
        if status == .closed { return "Closed" }
        return "Unknown"
    }

    private var voice: TagVoice {
        if status == .open || status == .completed { return .positive }
        if status == .paying { return .info }
        if status == .paused { return .warning }
        return .neutral
    }

    var body: some View {
        LemonadeUi.Tag(label: label, voice: voice)
    }
}

struct PaymentStatusTag: View {
    let state: TeyaPaymentState

    private var label: String {
        if state == .successful { return "Paid" }
        if state == .canceled { return "Canceled" }
        if state == .processingfailed || state == .communicationfailed { return "Failed" }
        return state.name
    }

    private var voice: TagVoice {
        if state == .successful { return .positive }
        if state == .canceled { return .neutral }
        if state == .processingfailed || state == .communicationfailed { return .critical }
        return .info
    }

    var body: some View {
        LemonadeUi.Tag(label: label, voice: voice)
    }
}
