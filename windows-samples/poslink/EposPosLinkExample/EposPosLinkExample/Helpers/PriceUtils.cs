using System.Globalization;

namespace EposPosLinkExample.Helpers;

public static class PriceUtils
{
    private static readonly CultureInfo UkCulture = CultureInfo.CreateSpecificCulture("en-GB");

    public static string FormatPrice(decimal amount)
    {
        return amount.ToString("C", UkCulture);
    }

    public static string FormatMinor(int amountMinor)
    {
        return FormatPrice(amountMinor / 100m);
    }

    public static int ToMinorUnits(decimal amountMajor)
    {
        return (int)System.Math.Round(amountMajor * 100m);
    }

    public static bool IsValidTipInput(string input)
    {
        if (string.IsNullOrEmpty(input)) return true;

        var dotIndex = input.IndexOf('.');
        if (dotIndex != input.LastIndexOf('.')) return false;

        foreach (var c in input)
        {
            if (c != '.' && !char.IsDigit(c)) return false;
        }

        return !(dotIndex >= 0 && input.Length - dotIndex - 1 > 2);
    }
}
