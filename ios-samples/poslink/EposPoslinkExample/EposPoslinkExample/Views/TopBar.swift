import SwiftUI

struct TopBar: ToolbarContent {
    let onClearUserAuth: () -> Void
    let onClearDeviceLink: () -> Void

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Button("Clear User Auth") {
                    onClearUserAuth()
                }
                Button("Clear Device Link") {
                    onClearDeviceLink()
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
        }
    }
}
