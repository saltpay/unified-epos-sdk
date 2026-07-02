import SwiftUI
import Lemonade

struct RootView: View {
    @State private var selectedTab = 0
    @State private var tablesViewModel = TablesViewModel()

    private let tabs: [LemonadeTabButtonProperties] = [
        .labelAndIcon("Sale", icon: .basket),
        .labelAndIcon("Pay at Table", icon: .forkKnife),
    ]

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                MainView()
                    .opacity(selectedTab == 0 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 0)

                TablesView(viewModel: tablesViewModel)
                    .opacity(selectedTab == 1 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 1)
            }

            Divider()

            LemonadeUi.SegmentedControl(
                properties: tabs,
                selectedTab: selectedTab,
                onTabSelected: { selectedTab = $0 }
            )
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .background(LemonadeTheme.colors.background.bgDefault)
    }
}
