using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using Microsoft.UI.Xaml;

namespace EposPosLinkExample.ViewModels;

public partial class ProductItem : ObservableObject
{
    public int Id { get; }
    public string Name { get; }
    public decimal Price { get; }
    public string FormattedPrice { get; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(QuantityText))]
    [NotifyPropertyChangedFor(nameof(QuantityVisibility))]
    public partial int Quantity { get; set; }

    public string QuantityText => $"x{Quantity}";
    public Visibility QuantityVisibility => Quantity > 0 ? Visibility.Visible : Visibility.Collapsed;

    public ProductItem(int id, string name, decimal price)
    {
        Id = id;
        Name = name;
        Price = price;
        FormattedPrice = PriceUtils.FormatPrice(price);
    }

    [RelayCommand]
    private void Add() => Quantity++;

    [RelayCommand]
    private void Remove()
    {
        if (Quantity > 0) Quantity--;
    }
}
