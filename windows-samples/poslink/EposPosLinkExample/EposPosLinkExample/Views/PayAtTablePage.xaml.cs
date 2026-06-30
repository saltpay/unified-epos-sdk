using EposPosLinkExample.ViewModels;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using System;

namespace EposPosLinkExample.Views;

public sealed partial class PayAtTablePage : Page
{
    public TablesViewModel ViewModel { get; } = new();

    public PayAtTablePage()
    {
        InitializeComponent();
        NavigationCacheMode = NavigationCacheMode.Required;
    }

    private void PatToggle_Toggled(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        if (sender is ToggleSwitch sw && sw.IsOn != ViewModel.PatEnabled)
        {
            ViewModel.SetPatEnabledCommand.Execute(sw.IsOn);
        }
    }

    private void Tile_ItemClick(object sender, ItemClickEventArgs e)
    {
        if (e.ClickedItem is TableTile tile)
        {
            ViewModel.OpenTableDetailsCommand.Execute(tile.TabId);
        }
    }

    private async void AddTable_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        var dialog = new AddTableDialog { XamlRoot = this.XamlRoot };
        var result = await dialog.ShowAsync();
        if (result == ContentDialogResult.Primary)
        {
            await ViewModel.OpenTab(dialog.TableName);
        }
    }

    private async void AddItems_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        var dialog = new ProductCatalogueDialog(ViewModel) { XamlRoot = this.XamlRoot };
        await dialog.ShowAsync();
    }

    private async void CloseTable_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        await ViewModel.CloseTabCommand.ExecuteAsync(null);
    }

    private async void ViewPayments_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        if (ViewModel.SelectedTabDetail is { } tab)
        {
            var dialog = new PaymentsDialog(tab) { XamlRoot = this.XamlRoot };
            await dialog.ShowAsync();
        }
    }
}
