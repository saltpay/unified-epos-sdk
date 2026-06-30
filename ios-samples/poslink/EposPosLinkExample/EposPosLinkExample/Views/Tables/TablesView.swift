import SwiftUI
import Lemonade
import TeyaUnifiedEposSDK

struct TablesView: View {
    @Bindable var viewModel: TablesViewModel

    var body: some View {
        NavigationStack {
            TablesListContent(viewModel: viewModel)
                .navigationDestination(isPresented: detailPresented) {
                    TableDetailsView(viewModel: viewModel)
                }
        }
    }

    private var detailPresented: Binding<Bool> {
        Binding(
            get: { viewModel.selectedTab != nil },
            set: { isActive in if !isActive { viewModel.closeTableDetails() } }
        )
    }
}

private struct TablesListContent: View {
    @Bindable var viewModel: TablesViewModel

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: 12)]

    var body: some View {
        VStack(spacing: 0) {
            LemonadeUi.Switch(
                checked: viewModel.patEnabled,
                onCheckedChange: { viewModel.setPayAtTableEnabled($0) },
                label: "Pay at Table on store"
            )
            .padding(16)

            if viewModel.openTabs.isEmpty {
                Spacer()
                LemonadeUi.Text(
                    "No open tables yet.\nTap + to add one.",
                    textStyle: LemonadeTypography.shared.bodyMediumRegular,
                    textAlign: .center,
                    color: LemonadeTheme.colors.content.contentSecondary
                )
                Spacer()
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(viewModel.openTabs, id: \.tabId.value) { tab in
                            TableTile(
                                tab: tab,
                                totalMinor: viewModel.tabTotalMinor(tab.tabId.value),
                                onTap: { viewModel.openTableDetails(tab.tabId) }
                            )
                        }
                    }
                    .padding(16)
                }
            }
        }
        .lemonadeTopBar(label: "Pay at Table") {
            ToolbarItem(placement: .topBarTrailing) {
                LemonadeUi.IconButton(
                    icon: .arrowsRepeat,
                    contentDescription: "Refresh tabs",
                    onClick: { viewModel.getTabs() }
                )
            }
            ToolbarItem(placement: .topBarTrailing) {
                LemonadeUi.IconButton(
                    icon: .plus,
                    contentDescription: "Add table",
                    onClick: { viewModel.showAddTable() }
                )
            }
        }
        .sheet(isPresented: $viewModel.showAddTableDialog) {
            AddTableSheet(viewModel: viewModel)
        }
    }
}

private struct TableTile: View {
    let tab: TeyaTabSummary
    let totalMinor: Int
    let onTap: () -> Void

    var body: some View {
        LemonadeUi.Card(contentPadding: .medium, background: .elevated) {
            VStack(alignment: .leading, spacing: 4) {
                LemonadeUi.Text(tab.tabName, textStyle: LemonadeTypography.shared.headingXSmall)
                LemonadeUi.Text(
                    PriceUtils.formatMinor(totalMinor),
                    textStyle: LemonadeTypography.shared.bodyMediumRegular,
                    color: LemonadeTheme.colors.content.contentBrand
                )
                StatusTag(status: tab.status)
                if let terminalId = tab.showingBillTerminalId {
                    LemonadeUi.Tag(label: "Bill on \(terminalId)", voice: .info)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 120, alignment: .topLeading)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

private struct AddTableSheet: View {
    @Bindable var viewModel: TablesViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            LemonadeUi.Text("Add table", textStyle: LemonadeTypography.shared.headingSmall)

            LemonadeUi.TextField(
                input: Binding(
                    get: { viewModel.tabNameInput },
                    set: { viewModel.updateTabName($0) }
                ),
                label: "Table name (e.g. Table 5)"
            )

            LemonadeUi.Button(
                label: "Open table",
                onClick: { viewModel.openTab() },
                size: .large,
                enabled: viewModel.canOpenTab
            )
            .frame(maxWidth: .infinity)

            LemonadeUi.Button(
                label: "Cancel",
                onClick: { viewModel.dismissAddTable() },
                variant: .neutral,
                type: .ghost,
                size: .large
            )
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .frame(maxHeight: .infinity, alignment: .top)
        .presentationDetents([.medium])
    }
}
