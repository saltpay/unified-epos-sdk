using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models;
using EposPosLinkExample.Models.Tabs;
using Microsoft.UI.Dispatching;

namespace EposPosLinkExample.ViewModels;

public partial class TablesViewModel : ObservableObject
{
    private readonly TeyaSdkManager _sdk = TeyaSdkManager.Instance;
    private readonly DispatcherQueue _dispatcherQueue = DispatcherQueue.GetForCurrentThread();
    private readonly Dictionary<string, ObservableCollection<TabProductItem>> _itemsByTab = new();

    public ObservableCollection<TableTile> Tiles { get; } = new();

    public bool HasNoTables => Tiles.Count == 0;

    [ObservableProperty]
    public partial bool PatEnabled { get; private set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsTableSelected))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    public partial string? SelectedTabId { get; private set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(SelectedPaidMinor))]
    [NotifyPropertyChangedFor(nameof(HasPayments))]
    [NotifyPropertyChangedFor(nameof(SelectedPayments))]
    [NotifyPropertyChangedFor(nameof(PaymentsCountText))]
    [NotifyPropertyChangedFor(nameof(IsPaymentInProgress))]
    public partial Tab? SelectedTabDetail { get; private set; }

    public bool IsPaymentInProgress =>
        SelectedTabDetail?.PaymentRequests?.Any(p => !p.Status.IsFinal()) == true;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(SelectedTabName))]
    [NotifyPropertyChangedFor(nameof(SelectedTabStatus))]
    [NotifyPropertyChangedFor(nameof(SelectedShowingBillTerminalId))]
    public partial TabSummary? SelectedTab { get; private set; }

    [ObservableProperty]
    public partial bool IsSdkReady { get; private set; }

    public bool IsTableSelected => SelectedTabId != null;
    public bool IsListVisible => SelectedTabId == null;

    public string SelectedTabName => SelectedTab?.Name ?? "";
    public TabStatus SelectedTabStatus => SelectedTab?.Status ?? TabStatus.UNKNOWN;
    public string? SelectedShowingBillTerminalId => SelectedTab?.ShowingBillTerminalId;

    public ObservableCollection<TabProductItem> SelectedTabItems =>
        SelectedTabId != null && _itemsByTab.TryGetValue(SelectedTabId, out var items)
            ? items
            : EmptyItems;

    private static readonly ObservableCollection<TabProductItem> EmptyItems = new();

    public string SelectedTabTotalFormatted => PriceUtils.FormatMinor(SelectedTabTotalMinor());

    public int? SelectedPaidMinor => SelectedTabDetail?.TotalPaid ?? 0;
    public IReadOnlyList<PaymentRequestSummary> SelectedPayments =>
        SelectedTabDetail?.PaymentRequests ?? Array.Empty<PaymentRequestSummary>();
    public bool HasPayments => SelectedPayments.Count > 0;
    public string PaymentsCountText => HasPayments ? $"Payments ({SelectedPayments.Count})" : "No payments yet";

    public TablesViewModel()
    {
        PatEnabled = PreferencesHelper.GetPatEnabled();
        IsSdkReady = _sdk.IsReady;
        Tiles.CollectionChanged += (_, _) => OnPropertyChanged(nameof(HasNoTables));
        _sdk.ReadyChanged += OnReadyChanged;
        _sdk.TabEventReceived += OnTabEvent;
        _ = _sdk.SubscribeToTabEvents();
        _ = RefreshTabs();
    }

    private void OnReadyChanged(object? sender, EventArgs e)
    {
        IsSdkReady = _sdk.IsReady;
        if (_sdk.IsReady)
        {
            _ = _sdk.SubscribeToTabEvents();
            _ = RefreshTabs();
        }
    }

    private int SelectedTabTotalMinor() => TabTotalMinor(SelectedTabId);

    private int TabTotalMinor(string? tabId)
    {
        if (tabId == null || !_itemsByTab.TryGetValue(tabId, out var items)) return 0;
        return items.Sum(i => PriceUtils.ToMinorUnits(i.Price * i.Quantity));
    }

    [RelayCommand]
    private async Task RefreshTabs()
    {
        var tabs = await _sdk.ListTabs();
        Tiles.Clear();
        foreach (var t in tabs)
        {
            if (!_itemsByTab.ContainsKey(t.TabId))
                _itemsByTab[t.TabId] = new ObservableCollection<TabProductItem>();
            Tiles.Add(new TableTile(t.TabId, t.Name, t.Status, TabTotalMinor(t.TabId), t.ShowingBillTerminalId));
        }
    }

    [RelayCommand]
    private async Task SetPatEnabled(bool enable)
    {
        var ok = await _sdk.SetPayAtTableEnabledOnStore(enable);
        if (ok)
        {
            PatEnabled = enable;
            PreferencesHelper.SetPatEnabled(enable);
        }
    }

    public async Task OpenTab(string name)
    {
        if (string.IsNullOrWhiteSpace(name)) return;
        var tabId = $"tab-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";
        var tab = await _sdk.OpenTab(tabId, name, "GBP");
        _itemsByTab[tab.TabId] = new ObservableCollection<TabProductItem>();
        Tiles.Add(new TableTile(tab.TabId, tab.Name, tab.Status, 0, tab.ShowingBillTerminalId));
    }

    [RelayCommand]
    private async Task OpenTableDetails(string tabId)
    {
        SelectedTab = Tiles.Where(t => t.TabId == tabId)
            .Select(t => new TabSummary(t.TabId, t.Name, t.Status, t.TotalMinor, null, null, "GBP", t.ShowingBillTerminalId))
            .FirstOrDefault();
        SelectedTabId = tabId;
        SelectedTabDetail = null;
        OnPropertyChanged(nameof(SelectedTabItems));
        OnPropertyChanged(nameof(SelectedTabTotalFormatted));
        await RefreshSelectedTabDetail();
    }

    [RelayCommand]
    private void CloseTableDetails()
    {
        SelectedTabId = null;
        SelectedTab = null;
        SelectedTabDetail = null;
    }

    [RelayCommand]
    private async Task RefreshSelectedTabDetail()
    {
        if (SelectedTabId == null) return;
        var tab = await _sdk.GetTab(SelectedTabId);
        SelectedTabDetail = tab;
        SelectedTab = tab.ToSummary() with { TotalAmount = TabTotalMinor(tab.TabId) };
        UpdateTile(tab);
    }

    [RelayCommand]
    private async Task CloseTab()
    {
        if (SelectedTabId == null) return;
        var tabId = SelectedTabId;
        await _sdk.CloseTab(tabId);
        var tile = Tiles.FirstOrDefault(t => t.TabId == tabId);
        if (tile != null) Tiles.Remove(tile);
        _itemsByTab.Remove(tabId);
        CloseTableDetails();
    }

    public void SetProductQuantity(int id, string name, decimal price, string emoji, int quantity)
    {
        if (SelectedTabId == null) return;
        var items = _itemsByTab[SelectedTabId];
        var existing = items.FirstOrDefault(i => i.Id == id);
        if (quantity <= 0)
        {
            if (existing != null)
            {
                existing.QuantityChanged -= OnItemQuantityChanged;
                items.Remove(existing);
            }
        }
        else if (existing != null)
        {
            existing.Quantity = quantity;
        }
        else
        {
            var item = new TabProductItem(id, name, price, emoji) { Quantity = quantity };
            item.QuantityChanged += OnItemQuantityChanged;
            items.Add(item);
        }
        OnSelectedTotalsChanged();
    }

    private void OnItemQuantityChanged(object? sender, EventArgs e)
    {
        if (sender is TabProductItem item && item.Quantity == 0 && SelectedTabId != null)
        {
            item.QuantityChanged -= OnItemQuantityChanged;
            _itemsByTab[SelectedTabId].Remove(item);
        }
        OnSelectedTotalsChanged();
    }

    private void OnSelectedTotalsChanged()
    {
        OnPropertyChanged(nameof(SelectedTabTotalFormatted));
        if (SelectedTabId != null) UpdateTileTotal(SelectedTabId);
    }

    private void UpdateTileTotal(string tabId)
    {
        var tile = Tiles.FirstOrDefault(t => t.TabId == tabId);
        if (tile != null) tile.TotalMinor = TabTotalMinor(tabId);
    }

    private void UpdateTile(Tab tab)
    {
        var tile = Tiles.FirstOrDefault(t => t.TabId == tab.TabId);
        if (tile != null)
        {
            tile.Status = tab.Status;
            tile.TotalMinor = TabTotalMinor(tab.TabId);
            tile.ShowingBillTerminalId = tab.ShowingBillTerminalId;
        }
    }

    public List<TabProductItem> BuildCatalogue()
    {
        var items = SelectedTabId != null && _itemsByTab.TryGetValue(SelectedTabId, out var basket)
            ? basket
            : new ObservableCollection<TabProductItem>();
        return Product.GetProducts()
            .Select(p =>
            {
                var inBasket = items.FirstOrDefault(i => i.Id == p.Id);
                return new TabProductItem(p.Id, p.Name, p.Price, p.Emoji) { Quantity = inBasket?.Quantity ?? 0 };
            })
            .ToList();
    }

    // ---- Tab event handling ----

    private void OnTabEvent(object? sender, TabEventArgs e)
    {
        _dispatcherQueue.TryEnqueue(() => HandleTabEvent(e));
    }

    private void HandleTabEvent(TabEventArgs e)
    {
        try
        {
            switch (e.Method)
            {
                case "onShowBillRequested":
                    _ = HandleShowBillRequested(e.Params);
                    break;
                case "onPayRequested":
                    _ = HandlePayRequested(e.Params);
                    break;
                case "onPaymentProgress":
                    var progressTabId = GetPaymentString(e.Params, "tabId");
                    if (progressTabId != null) _ = RefreshTab(progressTabId);
                    break;
                case "onPaymentCompleted":
                    var payCompletedTabId = GetPaymentString(e.Params, "tabId");
                    if (payCompletedTabId != null) _ = RefreshTab(payCompletedTabId);
                    break;
                case "onTabPaused":
                case "onTabResumed":
                case "onBillHidden":
                    var tabId = GetString(e.Params, "tabId");
                    if (tabId != null) _ = RefreshTab(tabId);
                    break;
                case "onTabCompleted":
                    var completedTabId = GetString(e.Params, "tabId");
                    if (completedTabId != null) _ = HandleTabCompleted(completedTabId);
                    break;
                case "onConnectionStateChange":
                    Debug.WriteLine($"Connection state changed: {e.Params}");
                    break;
                case "onUnsubscribed":
                    Debug.WriteLine("Unsubscribed from tab events");
                    break;
                default:
                    Debug.WriteLine($"Unknown tab event: {e.Method}");
                    break;
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Error handling tab event {e.Method}: {ex.Message}");
        }
    }

    private async Task HandleShowBillRequested(JsonElement parameters)
    {
        var tabId = GetString(parameters, "tabId");
        var terminalId = GetString(parameters, "terminalId");
        if (tabId == null || terminalId == null) return;

        var tile = Tiles.FirstOrDefault(t => t.TabId == tabId);
        if (tile == null)
        {
            Debug.WriteLine($"Tab not found for bill request: {tabId}");
            return;
        }

        var items = _itemsByTab.TryGetValue(tabId, out var itemList) ? itemList.ToList() : new List<TabProductItem>();
        var totalMinor = TabTotalMinor(tabId);
        var printTemplate = BuildBillTemplate(tile.Name, items, totalMinor);

        try
        {
            await _sdk.RespondToBillRequest(tabId, terminalId, totalMinor, "GBP", printTemplate);
            Debug.WriteLine($"respondToBillRequest success for tab {tabId}");
            await RefreshTab(tabId);
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"respondToBillRequest failed: {ex.Message}");
        }
    }

    private async Task HandlePayRequested(JsonElement parameters)
    {
        var tabId = GetString(parameters, "tabId");
        var terminalId = GetString(parameters, "terminalId");
        var amount = GetInt(parameters, "amount");
        var currency = GetString(parameters, "currency") ?? "GBP";
        var type = GetString(parameters, "paymentType") ?? "Sale";
        var method = GetString(parameters, "paymentMethod") ?? "Card";

        if (tabId == null || terminalId == null || amount == null) return;

        try
        {
            await _sdk.MakeTabPayment(tabId, terminalId, amount.Value, currency, type, method);
            Debug.WriteLine($"makeTabPayment success for tab {tabId}");
            await RefreshTab(tabId);
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"makeTabPayment failed: {ex.Message}");
        }
    }

    private async Task HandleTabCompleted(string tabId)
    {
        Debug.WriteLine($"Tab completed: {tabId}");
        try
        {
            await _sdk.CloseTab(tabId);
            var tile = Tiles.FirstOrDefault(t => t.TabId == tabId);
            if (tile != null) Tiles.Remove(tile);
            _itemsByTab.Remove(tabId);
            if (SelectedTabId == tabId) CloseTableDetails();
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"closeTab after completion failed: {ex.Message}");
        }
    }

    private async Task RefreshTab(string tabId)
    {
        try
        {
            var tab = await _sdk.GetTab(tabId);

            if (tab.Status is TabStatus.CLOSED)
            {
                var tile = Tiles.FirstOrDefault(t => t.TabId == tabId);
                if (tile != null) Tiles.Remove(tile);
                _itemsByTab.Remove(tabId);
                if (tabId == SelectedTabId) CloseTableDetails();
                return;
            }

            UpdateTile(tab);
            if (tabId == SelectedTabId)
            {
                SelectedTabDetail = tab;
                SelectedTab = tab.ToSummary() with { TotalAmount = TabTotalMinor(tab.TabId) };
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"RefreshTab({tabId}) failed: {ex.Message}");
        }
    }

    private static PrintTemplate BuildBillTemplate(string tabName, IReadOnlyList<TabProductItem> items, int totalMinor)
    {
        var rows = new List<ReceiptRow>
        {
            new ReceiptRowItem
            {
                Item = new RowElementText { Text = "BILL", Align = Align.CENTER, Bold = true }
            },
            new ReceiptRowItem
            {
                Item = new RowElementText { Text = tabName, Align = Align.CENTER }
            },
            new ReceiptRowSpacer(),
            new ReceiptRowDivider(),
        };

        foreach (var item in items)
        {
            rows.Add(new ReceiptRowItems
            {
                Items = new List<RowElement>
                {
                    new RowElementText { Text = $"{item.Quantity}x {item.Name.ToUpper()}", Align = Align.LEFT },
                    new RowElementText { Text = PriceUtils.FormatPrice(item.Price * item.Quantity), Align = Align.RIGHT }
                }
            });
        }

        rows.Add(new ReceiptRowDivider());
        rows.Add(new ReceiptRowItems
        {
            Items = new List<RowElement>
            {
                new RowElementText { Text = "TOTAL", Align = Align.LEFT, Bold = true },
                new RowElementText { Text = PriceUtils.FormatMinor(totalMinor), Align = Align.RIGHT, Bold = true }
            }
        });

        return new PrintTemplate { Rows = rows };
    }

    private static string? GetPaymentString(JsonElement element, string propertyName) =>
        element.TryGetProperty("payment", out var payment) ? GetString(payment, propertyName) : null;

    private static string? GetString(JsonElement element, string propertyName) =>
        element.TryGetProperty(propertyName, out var prop) && prop.ValueKind == JsonValueKind.String
            ? prop.GetString()
            : null;

    private static int? GetInt(JsonElement element, string propertyName) =>
        element.TryGetProperty(propertyName, out var prop) && prop.ValueKind == JsonValueKind.Number
            ? prop.GetInt32()
            : null;
}
