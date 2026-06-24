using EposPosLinkExample.ViewModels;
using EposPosLinkExample.Views;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace EposPosLinkExample;

public sealed partial class MainWindow : Window
{
    public ShellViewModel ViewModel { get; } = new();

    public MainWindow()
    {
        InitializeComponent();
    }

    private void Nav_Loaded(object sender, RoutedEventArgs e)
    {
        Nav.SelectedItem = Nav.MenuItems[0];
        ContentFrame.Navigate(typeof(SalePage));
    }

    private void Nav_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.SelectedItem is NavigationViewItem item)
        {
            switch (item.Tag as string)
            {
                case "sale":
                    ContentFrame.Navigate(typeof(SalePage));
                    break;
                case "pat":
                    ContentFrame.Navigate(typeof(PayAtTablePage));
                    break;
            }
        }
    }
}
