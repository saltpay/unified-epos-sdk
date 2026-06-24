using Microsoft.UI.Xaml.Controls;

namespace EposPosLinkExample.Views;

public sealed partial class AddTableDialog : ContentDialog
{
    public string TableName => NameBox.Text;

    public AddTableDialog()
    {
        InitializeComponent();
    }

    private void NameBox_TextChanged(object sender, TextChangedEventArgs e)
    {
        IsPrimaryButtonEnabled = !string.IsNullOrWhiteSpace(NameBox.Text);
    }
}
