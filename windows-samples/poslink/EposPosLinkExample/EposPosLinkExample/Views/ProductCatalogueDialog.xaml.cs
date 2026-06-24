using System.Collections.Generic;
using EposPosLinkExample.ViewModels;
using Microsoft.UI.Xaml.Controls;

namespace EposPosLinkExample.Views;

public sealed partial class ProductCatalogueDialog : ContentDialog
{
    private readonly TablesViewModel _viewModel;

    public ProductCatalogueDialog(TablesViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        var items = _viewModel.BuildCatalogue();
        foreach (var item in items)
        {
            item.QuantityChanged += (s, e) =>
            {
                if (s is TabProductItem tpi)
                {
                    _viewModel.SetProductQuantity(tpi.Id, tpi.Name, tpi.Price, tpi.Emoji, tpi.Quantity);
                }
            };
        }
        Catalogue.ItemsSource = new List<TabProductItem>(items);
    }
}
