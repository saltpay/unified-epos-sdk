using System;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;
using Microsoft.UI.Xaml;

namespace EposPosLinkExample.ViewModels;

public partial class TabProductItem : ObservableObject
{
    public int Id { get; }
    public string Name { get; }
    public decimal Price { get; }
    public string Emoji { get; }

    public string EmojiName => $"{Emoji} {Name}";

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(LineTotalFormatted))]
    [NotifyPropertyChangedFor(nameof(QuantityDisplay))]
    [NotifyPropertyChangedFor(nameof(HasQuantity))]
    public partial int Quantity { get; set; }

    public string LineTotalFormatted => PriceUtils.FormatPrice(Price * Quantity);
    public string QuantityDisplay => $"x{Quantity}";
    public Visibility HasQuantity => Quantity > 0 ? Visibility.Visible : Visibility.Collapsed;

    public event EventHandler? QuantityChanged;

    public TabProductItem(int id, string name, decimal price, string emoji)
    {
        Id = id;
        Name = name;
        Price = price;
        Emoji = emoji;
    }

    partial void OnQuantityChanged(int value) => QuantityChanged?.Invoke(this, EventArgs.Empty);

    [RelayCommand]
    private void Add() => Quantity++;

    [RelayCommand]
    private void Remove()
    {
        if (Quantity > 0) Quantity--;
    }
}
