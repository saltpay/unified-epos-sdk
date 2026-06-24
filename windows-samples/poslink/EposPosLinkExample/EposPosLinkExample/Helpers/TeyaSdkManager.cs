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
    internal class TeyaSdkManager
    {
        public static TeyaSdkManager Instance { get; } = new();

        public bool IsReady { get; private set; }

        public event System.EventHandler? ReadyChanged;

        public void SetReady(bool ready)
        {
            IsReady = ready;
            ReadyChanged?.Invoke(this, System.EventArgs.Empty);
        }

        private Process? _process;
        private Dictionary<string, TaskCompletionSource<JsonElement>> _pendingRequests = new Dictionary<string, TaskCompletionSource<JsonElement>>();

        private readonly Dictionary<string, Models.Tabs.Tab> _fakeTabs = new();
        private bool _fakePatEnabled;

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

        public Task<bool> SetPayAtTableEnabledOnStore(bool enable)
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "setPayAtTableEnabledOnStore"
            _fakePatEnabled = enable;
            return Task.FromResult(true);
        }

        public Task<Models.Tabs.Tab> OpenTab(string tabId, string tabName, string currency)
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "openTab"
            var tab = new Models.Tabs.Tab(
                TabId: tabId,
                Name: tabName,
                Status: Models.Tabs.TabStatus.Open,
                TotalAmount: 0,
                TotalPaid: null,
                Remaining: null,
                Currency: currency,
                ShowingBillTerminalId: null,
                PaymentRequests: new List<Models.Tabs.PaymentRequestSummary>());
            _fakeTabs[tabId] = tab;
            return Task.FromResult(tab);
        }

        public Task<IReadOnlyList<Models.Tabs.TabSummary>> ListTabs()
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "listTabs"
            IReadOnlyList<Models.Tabs.TabSummary> result =
                _fakeTabs.Values
                    .Where(t => t.Status != Models.Tabs.TabStatus.Closed)
                    .Select(t => t.ToSummary())
                    .ToList();
            return Task.FromResult(result);
        }

        public Task<Models.Tabs.Tab> GetTab(string tabId)
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "getTab"
            if (!_fakeTabs.TryGetValue(tabId, out var tab))
            {
                throw new InvalidOperationException($"Tab not found: {tabId}");
            }
            return Task.FromResult(tab);
        }

        public Task CloseTab(string tabId)
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "closeTab"
            _fakeTabs.Remove(tabId);
            return Task.CompletedTask;
        }

        public Task RespondToBillRequest(string tabId, string terminalId, int totalAmountMinor, string currency)
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "respondToBillRequest"
            Debug.WriteLine($"[PAT placeholder] respondToBillRequest(tab={tabId}, terminal={terminalId}, total={totalAmountMinor} {currency})");
            return Task.CompletedTask;
        }

        public Task MakeTabPayment(string tabId, int amount, string currency)
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC call to "makePaymentAndSubscribe" with a tab context
            Debug.WriteLine($"[PAT placeholder] makeTabPayment(tab={tabId}, amount={amount} {currency})");
            return Task.CompletedTask;
        }

        public void SubscribeToTabEvents()
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC subscription to tab events
            Debug.WriteLine("[PAT placeholder] subscribeToTabEvents");
        }

        public void UnsubscribeFromTabEvents()
        {
            // TODO: replace this in-memory placeholder with a real JSON-RPC unsubscription from tab events
            Debug.WriteLine("[PAT placeholder] unsubscribeFromTabEvents");
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
                // This might be a notification, not a response to a request
                Debug.WriteLine($"Received notification: {responseJson}");
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
