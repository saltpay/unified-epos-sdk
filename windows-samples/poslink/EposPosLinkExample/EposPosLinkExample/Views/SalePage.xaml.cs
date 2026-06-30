using EposPosLinkExample.Helpers;
using EposPosLinkExample.ViewModels;
using Microsoft.UI.Xaml.Controls;

namespace EposPosLinkExample.Views;

public sealed partial class SalePage : Page
{
    public SaleViewModel ViewModel { get; } = new();

    public SalePage()
    {
        InitializeComponent();
    }

    private void TipInput_BeforeTextChanging(TextBox sender, TextBoxBeforeTextChangingEventArgs args)
    {
        args.Cancel = !PriceUtils.IsValidTipInput(args.NewText);
    }
}
