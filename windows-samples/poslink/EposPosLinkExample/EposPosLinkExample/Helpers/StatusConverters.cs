using System;
using EposPosLinkExample.Models.Tabs;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace EposPosLinkExample.Helpers;

internal enum TagVoice { Neutral, Info, Positive, Warning, Critical }

internal static class StatusVoice
{
    public static (string Label, TagVoice Voice) ForTab(TabStatus status) => status switch
    {
        TabStatus.Open => ("Open", TagVoice.Positive),
        TabStatus.Paying => ("Paying", TagVoice.Info),
        TabStatus.Paused => ("Paused", TagVoice.Warning),
        TabStatus.Completed => ("Completed", TagVoice.Positive),
        TabStatus.Closed => ("Closed", TagVoice.Neutral),
        _ => ("Unknown", TagVoice.Neutral),
    };

    public static (string Label, TagVoice Voice) ForPayment(PaymentState state) => state switch
    {
        PaymentState.Successful => ("Paid", TagVoice.Positive),
        PaymentState.Canceled => ("Canceled", TagVoice.Neutral),
        PaymentState.ProcessingFailed => ("Failed", TagVoice.Critical),
        PaymentState.CommunicationFailed => ("Failed", TagVoice.Critical),
        _ => (state.ToString(), TagVoice.Info),
    };

    public static (string Label, TagVoice Voice) Resolve(object value) => value switch
    {
        TabStatus t => ForTab(t),
        PaymentState p => ForPayment(p),
        _ => (value?.ToString() ?? "", TagVoice.Neutral),
    };

    public static Color Background(TagVoice voice) => voice switch
    {
        TagVoice.Positive => Color.FromArgb(0xFF, 0xF2, 0xFA, 0xE6),
        TagVoice.Info => Color.FromArgb(0xFF, 0xEA, 0xF2, 0xFF),
        TagVoice.Warning => Color.FromArgb(0xFF, 0xFF, 0xF5, 0xE6),
        TagVoice.Critical => Color.FromArgb(0xFF, 0xFE, 0xEB, 0xED),
        _ => Color.FromArgb(0xFF, 0xF1, 0xF0, 0xEE),
    };

    public static Color Foreground(TagVoice voice) => voice switch
    {
        TagVoice.Positive => Color.FromArgb(0xFF, 0x49, 0x7D, 0x00),
        TagVoice.Info => Color.FromArgb(0xFF, 0x14, 0x47, 0xE6),
        TagVoice.Warning => Color.FromArgb(0xFF, 0xBB, 0x4D, 0x00),
        TagVoice.Critical => Color.FromArgb(0xFF, 0xE4, 0x1E, 0x2B),
        _ => Color.FromArgb(0xFF, 0x1B, 0x1B, 0x19),
    };
}

public class StatusToLabelConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
        => StatusVoice.Resolve(value).Label;

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();
}

public class StatusToBackgroundConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
        => new SolidColorBrush(StatusVoice.Background(StatusVoice.Resolve(value).Voice));

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();
}

public class StatusToForegroundConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
        => new SolidColorBrush(StatusVoice.Foreground(StatusVoice.Resolve(value).Voice));

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();
}

public class NullToVisibilityConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
        => value == null ? Visibility.Collapsed : Visibility.Visible;

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();
}
