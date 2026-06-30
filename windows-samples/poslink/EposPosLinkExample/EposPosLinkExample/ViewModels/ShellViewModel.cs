using System.Diagnostics;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EposPosLinkExample.Helpers;

namespace EposPosLinkExample.ViewModels;

public partial class ShellViewModel : ObservableObject
{
    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsSdkSettingUp))]
    public partial bool IsSdkReady { get; private set; }

    [ObservableProperty]
    public partial string SdkStatusMessage { get; private set; } = "";

    public bool IsSdkSettingUp => !IsSdkReady;

    private readonly TeyaSdkManager _teyaSdkManager = TeyaSdkManager.Instance;

    public ShellViewModel()
    {
        _ = StartAndSetupTeyaSdk();
    }

    private async Task StartAndSetupTeyaSdk()
    {
        SdkStatusMessage = "Starting SDK process...";
        _teyaSdkManager.StartProcess();

        SdkStatusMessage = "Initializing SDK...";
        var initResponse = await _teyaSdkManager.Initialize();
        Debug.WriteLine($"{initResponse}");

        SdkStatusMessage = "Setting up SDK...";
        var setupResponse = await _teyaSdkManager.Setup();
        Debug.WriteLine($"{setupResponse}");

        IsSdkReady = _teyaSdkManager.IsReady;
        SdkStatusMessage = IsSdkReady ? "SDK ready" : "SDK setup failed";
    }

    [RelayCommand]
    private async Task ClearUserAuth()
    {
        await _teyaSdkManager.ClearUserAuth();
        await _teyaSdkManager.Setup();
    }

    [RelayCommand]
    private async Task ClearDeviceLink()
    {
        await _teyaSdkManager.ClearDeviceLink();
        await _teyaSdkManager.Setup();
    }
}
