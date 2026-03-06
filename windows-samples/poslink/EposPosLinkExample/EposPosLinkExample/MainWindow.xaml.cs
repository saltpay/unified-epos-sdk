using EposPosLinkExample.Helpers;
using EposPosLinkExample.ViewModels;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace EposPosLinkExample;

public sealed partial class MainWindow : Window
{
    public MainViewModel ViewModel { get; } = new();

    public MainWindow()
    {
        InitializeComponent();
    }

    private void TipInput_BeforeTextChanging(TextBox sender, TextBoxBeforeTextChangingEventArgs args)
    {
        args.Cancel = !PriceUtils.IsValidTipInput(args.NewText);
    }
}
