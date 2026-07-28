using EposPosLinkExample.ViewModels;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;

namespace EposPosLinkExample.Views;

public sealed partial class TransactionHistoryPage : Page
{
    public TransactionHistoryViewModel ViewModel { get; } = new();

    public TransactionHistoryPage()
    {
        InitializeComponent();
        NavigationCacheMode = NavigationCacheMode.Required;
    }
}
