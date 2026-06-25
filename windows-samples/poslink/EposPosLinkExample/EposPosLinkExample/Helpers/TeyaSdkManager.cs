using EposPosLinkExample.Models;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Security.Cryptography.X509Certificates;
using System.Text.Json;
using System.Threading.Tasks;

namespace EposPosLinkExample.Helpers
{
    internal class TabEventArgs : EventArgs
    {
        public string Method { get; }
        public JsonElement Params { get; }

        public TabEventArgs(string method, JsonElement parameters)
        {
            Method = method;
            Params = parameters;
        }
    }

    internal class TeyaSdkManager
    {
        public static TeyaSdkManager Instance { get; } = new();

        public bool IsReady { get; private set; }

        public event System.EventHandler? ReadyChanged;

        public event EventHandler<TabEventArgs>? TabEventReceived;

        public void SetReady(bool ready)
        {
            IsReady = ready;
            ReadyChanged?.Invoke(this, System.EventArgs.Empty);
        }

        private Process? _process;
        private Dictionary<string, TaskCompletionSource<JsonElement>> _pendingRequests = new Dictionary<string, TaskCompletionSource<JsonElement>>();

        private readonly Dictionary<string, Models.Tabs.Tab> _fakeTabs = new();

        public void StartProcess()
        {
            var exePath = Path.Combine(AppContext.BaseDirectory, "TeyaEposIntegrationsApp", "TeyaEposIntegrationsApp.exe");
            var certificatePath = Path.Combine(AppContext.BaseDirectory, "signing_cert.cer");
            if (!MatchSignatureCertificate(exePath, certificatePath))
            {
                throw new Exception("Teya executable is not signed with the expected key!");
            }

            var processStartInfo = new ProcessStartInfo
            {
                FileName = Path.Combine(AppContext.BaseDirectory, "TeyaEposIntegrationsApp", "TeyaEposIntegrationsApp.exe"),
                RedirectStandardInput = true,
                RedirectStandardOutput = true,
                UseShellExecute = false
            };

            _process = Process.Start(processStartInfo) ?? throw new InvalidOperationException("Failed to start SDK process");

            Task.Run(ReadResponsesAsync);
        }

        public async Task<JsonElement> Initialize()
        {
            var parameters = new
            {
                requesterId = "epos-app-id", // your application id
                requesterVersion = "1.0.0", // your application version
                isProductionEnv = false, // or true for prod - make sure client id and secret match this
                clientId = "", // Replace with your Client ID
                clientSecret = "" // Replace with your Client Secret
            };
            return await SendRequestAsync("initialize", parameters);
        }

        public async Task<JsonElement> Setup()
        {
            return await SendRequestAsync("setup");
        }

        public async Task<JsonElement> ClearUserAuth()
        {
            return await SendRequestAsync("clearUserAuth");
        }

        public async Task<JsonElement> ClearDeviceLink()
        {
            return await SendRequestAsync("clearDeviceLink");
        }

        public async Task<MakePaymentStateChange> MakePayment(string id, int totalAmount, int? tipAmount)
        {
            var paymentDetails = new
            {
                transactionId = id, // business id for this transaction, or a random UUID
                amount = totalAmount, // total amount in minor units
                tip = tipAmount, // Optional tip amount in minor units (included in the total, not on top of it)
                currency = "GBP", // The ISO 4217 currency code (e.g., "GBP", "EUR").
                useTeyaDefaultUi = true, // if true, the SDK app will show an interactive "in progress" UI window
            };

            var responseJson = await SendRequestAsync("makePaymentAndSubscribe", paymentDetails);
            return JsonSerializer.Deserialize<MakePaymentStateChange>(responseJson)
                ?? throw new InvalidOperationException("Failed to deserialize payment response");
        }
         
        public async Task<JsonElement> PrintCustomTemplate(PrintTemplate template)
        {
            return await SendRequestAsync("printCustomTemplate", template);
        }

        public async Task<bool> SetPayAtTableEnabledOnStore(bool enable)
        {
            var parameters = new
            {
                enable
            };

            var result = await SendRequestAsync("setPayAtTableEnabledOnStore", parameters);
            bool success = !result.TryGetProperty("failureReason", out var reason) || reason.ValueKind == JsonValueKind.Null;
            return success;
        }

        public async Task<Models.Tabs.Tab> OpenTab(string tabId, string tabName, string currency)
        {
            var parameters = new
            {
                tabId,
                tabName,
                currency
            };

            var result = await SendRequestAsync("openTab", parameters);
            return JsonSerializer.Deserialize<Models.Tabs.Tab>(result.GetProperty("tab"))
                ?? throw new InvalidOperationException("Failed to deserialize tab response");
        }

        public async Task<IReadOnlyList<Models.Tabs.TabSummary>> ListTabs()
        {
            var parameters = new
            {
                after = (string?)null,
                before = (string?)null,
                limit = 100
            };

            var result = await SendRequestAsync("listTabs", parameters);
            return JsonSerializer.Deserialize<List<Models.Tabs.TabSummary>>(result.GetProperty("page").GetProperty("items"))
                ?? throw new InvalidOperationException("Failed to deserialize listTabs response");
        }

        public async Task<Models.Tabs.Tab> GetTab(string tabId)
        {
            var parameters = new
            {
                tabId
            };

            var result = await SendRequestAsync("getTab", parameters);
            return JsonSerializer.Deserialize<Models.Tabs.Tab>(result.GetProperty("tab"))
                ?? throw new InvalidOperationException("Failed to deserialize tab response");
        }

        public async Task<JsonElement> CloseTab(string tabId)
        {
            var parameters = new
            {
                tabId
            };

            return await SendRequestAsync("closeTab", parameters);
        }

        public async Task RespondToBillRequest(string tabId, string terminalId, int totalAmountMinor, string currency, PrintTemplate printModel)
        {
            var parameters = new
            {
                tabId,
                terminalId,
                totalAmount = totalAmountMinor,
                currency,
                printModel = printModel.Rows
            };

            await SendRequestAsync("respondToBillRequest", parameters);
        }

        public async Task<JsonElement> MakeTabPayment(string tabId, string terminalId, int amount, string currency, string type, string method, int? tip = null)
        {
            var transactionId = $"tx-{Guid.NewGuid()}";
            var parameters = new
            {
                transactionId,
                amount,
                currency,
                useTeyaDefaultUi = false,
                tip,
                tabContext = new
                {
                    tabId,
                    terminalId,
                    type,
                    method
                }
            };

            return await SendRequestAsync("makePaymentAndSubscribe", parameters);
        }

        public async Task<JsonElement> SubscribeToTabEvents()
        {
            return await SendRequestAsync("subscribeTabEvents");
        }

        public async Task<JsonElement> UnsubscribeFromTabEvents()
        {
            return await SendRequestAsync("unsubscribeTabEvents");
        }

        private async Task<JsonElement> SendRequestAsync(string methodName, object? parameters = null)
        {
            if (_process == null || _process.HasExited)
            {
                throw new InvalidOperationException("SDK process is not running");
            }

            string reqId = Guid.NewGuid().ToString();

            object request;
            if (parameters == null)
            {
                request = new
                {
                    jsonrpc = "2.0",
                    id = reqId,
                    method = methodName
                };
            }
            else
            {
                request = new
                {
                    jsonrpc = "2.0",
                    id = reqId,
                    method = methodName,
                    @params = parameters
                };
            }

            string requestJson = JsonSerializer.Serialize(request);

            var tcs = new TaskCompletionSource<JsonElement>();
            lock (_pendingRequests)
            {
                _pendingRequests[reqId] = tcs;
            }

            await _process.StandardInput.WriteLineAsync(requestJson);
            await _process.StandardInput.FlushAsync();

            return await tcs.Task;
        }

        private async Task ReadResponsesAsync()
        {
            using (StreamReader reader = _process!.StandardOutput)
            {
                while (!reader.EndOfStream)
                {
                    string? line = await reader.ReadLineAsync();

                    if (string.IsNullOrEmpty(line))
                        continue;

                    ProcessResponse(line);
                }
            }
        }

        private void ProcessResponse(string responseJson)
        {
            JsonElement root = JsonDocument.Parse(responseJson).RootElement;

            if (!root.TryGetProperty("id", out JsonElement idElement))
            {
                ProcessNotification(root);
                return;
            }

            string? id = idElement.GetString();

            TaskCompletionSource<JsonElement>? tcs = null;
            lock (_pendingRequests)
            {
                if (id != null && _pendingRequests.TryGetValue(id, out tcs))
                {
                    _pendingRequests.Remove(id);
                }
            }

            if (tcs == null)
            {
                Console.WriteLine($"Received response for unknown request ID: {id}");
                return;
            }

            // Check if the response contains an error
            if (root.TryGetProperty("error", out JsonElement errorElement) && !errorElement.ValueKind.Equals(JsonValueKind.Null))
            {
                tcs.SetException(new Exception($"JSON-RPC error: {errorElement}"));
            }
            else if (root.TryGetProperty("result", out JsonElement resultElement))
            {
                tcs.SetResult(resultElement);
            }
            else
            {
                tcs.SetException(new Exception("Invalid JSON-RPC response: missing result"));
            }
        }

        private void ProcessNotification(JsonElement root)
        {
            if (!root.TryGetProperty("result", out var result))
            {
                Debug.WriteLine($"Received notification without result: {root}");
                return;
            }

            if (!result.TryGetProperty("type", out var typeElement))
            {
                Debug.WriteLine($"Received notification without type: {root}");
                return;
            }

            var type = typeElement.GetString();
            if (type == null) return;

            // Extract event name from "subscribeTabEvents->onPayRequested" format
            var arrowIndex = type.IndexOf("->");
            var eventName = arrowIndex >= 0 ? type.Substring(arrowIndex + 2) : type;

            Debug.WriteLine($"Tab event: {eventName}");
            TabEventReceived?.Invoke(this, new TabEventArgs(eventName, result));
        }

        private bool MatchSignatureCertificate(string exePath, string cerFilePath)
        {
            try
            {
                // Load both certificates as X509Certificate2
                X509Certificate2 expectedCert = new X509Certificate2(cerFilePath);
                X509Certificate2 exeCert = new X509Certificate2(X509Certificate.CreateFromSignedFile(exePath));

                Debug.WriteLine("Cert of exe =" + exeCert.Thumbprint);
                Debug.WriteLine("Cert of cert=" + expectedCert.Thumbprint);
                // Compare thumbprints
                return string.Equals(
                    exeCert.Thumbprint,
                    expectedCert.Thumbprint,
                    StringComparison.OrdinalIgnoreCase);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error verifying signature: {ex.Message}");
                return false;
            }
        }
    }
}
