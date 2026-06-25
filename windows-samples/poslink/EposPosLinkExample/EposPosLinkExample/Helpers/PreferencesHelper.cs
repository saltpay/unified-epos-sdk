namespace EposPosLinkExample.Helpers;

public static class PreferencesHelper
{
    private const string PatEnabledKey = "pat_enabled";

    private static Windows.Storage.ApplicationDataContainer Settings =>
        Windows.Storage.ApplicationData.Current.LocalSettings;

    public static bool GetPatEnabled()
    {
        return Settings.Values.TryGetValue(PatEnabledKey, out var value) && value is true;
    }

    public static void SetPatEnabled(bool enabled)
    {
        Settings.Values[PatEnabledKey] = enabled;
    }
}
