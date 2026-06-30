import SwiftUI
import Lemonade

@main
struct EposPoslinkExampleApp: App {
    init() {
        LemonadeFonts.registerFonts()
        TeyaService.shared.setUp()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
