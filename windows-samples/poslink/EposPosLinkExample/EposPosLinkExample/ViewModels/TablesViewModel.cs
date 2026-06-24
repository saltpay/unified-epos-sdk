using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models;
using EposPosLinkExample.Models.Tabs;

namespace EposPosLinkExample.ViewModels;

public partial class TablesViewModel : ObservableObject
{
    private readonly TeyaSdkManager _sdk = TeyaSdkManager.Instance;
    private readonly Dictionary<string, ObservableCollection<TabProductItem>> _itemsByTab = new();

    public ObservableCollection<TableTile> Tiles { get; } = new();

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
    public partial Tab? SelectedTabDetail { get; private set; }

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
    public TabStatus SelectedTabStatus => SelectedTab?.Status ?? TabStatus.Unknown;
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
        _sdk.ReadyChanged += OnReadyChanged;
        _sdk.SubscribeToTabEvents();
        _ = RefreshTabs();
    }

    private void OnReadyChanged(object? sender, EventArgs e) => IsSdkReady = _sdk.IsReady;

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
}
