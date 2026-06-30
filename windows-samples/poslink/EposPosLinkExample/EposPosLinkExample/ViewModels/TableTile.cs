using CommunityToolkit.Mvvm.ComponentModel;
using EposPosLinkExample.Helpers;
using EposPosLinkExample.Models.Tabs;
using Microsoft.UI.Xaml;

namespace EposPosLinkExample.ViewModels;

public partial class TableTile : ObservableObject
{
    public string TabId { get; }
    public string Name { get; }

    [ObservableProperty]
    public partial TabStatus Status { get; set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(TotalFormatted))]
    public partial int TotalMinor { get; set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(BillTagText))]
    [NotifyPropertyChangedFor(nameof(BillTagVisibility))]
    public partial string? ShowingBillTerminalId { get; set; }

    public string TotalFormatted => PriceUtils.FormatMinor(TotalMinor);
    public string BillTagText => $"Bill on {ShowingBillTerminalId}";
    public Visibility BillTagVisibility => ShowingBillTerminalId == null ? Visibility.Collapsed : Visibility.Visible;

    public TableTile(string tabId, string name, TabStatus status, int totalMinor, string? showingBillTerminalId)
    {
        TabId = tabId;
        Name = name;
        Status = status;
        TotalMinor = totalMinor;
        ShowingBillTerminalId = showingBillTerminalId;
    }
}
