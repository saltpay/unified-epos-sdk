using System;
using System.IO;
using System.Text.Json;

namespace EposPosLinkExample.Helpers;

public static class PreferencesHelper
{
    private const string PatEnabledKey = "pat_enabled";

    public static bool GetPatEnabled()
    {
        try
        {
            var value = Windows.Storage.ApplicationData.Current.LocalSettings.Values[PatEnabledKey];
            return value is bool b && b;
        }
        catch
        {
            return ReadFile();
        }
    }

    public static void SetPatEnabled(bool enabled)
    {
        try
        {
            Windows.Storage.ApplicationData.Current.LocalSettings.Values[PatEnabledKey] = enabled;
        }
        catch
        {
            WriteFile(enabled);
        }
    }

    private static string FilePath =>
        Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "EposPosLinkExample",
            "prefs.json");

    private static bool ReadFile()
    {
        try
        {
            if (!File.Exists(FilePath)) return false;
            var json = File.ReadAllText(FilePath);
            var dict = JsonSerializer.Deserialize<System.Collections.Generic.Dictionary<string, bool>>(json);
            return dict != null && dict.TryGetValue(PatEnabledKey, out var v) && v;
        }
        catch
        {
            return false;
        }
    }

    private static void WriteFile(bool enabled)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
            var dict = new System.Collections.Generic.Dictionary<string, bool> { [PatEnabledKey] = enabled };
            File.WriteAllText(FilePath, JsonSerializer.Serialize(dict));
        }
        catch
        {
        }
    }
}
