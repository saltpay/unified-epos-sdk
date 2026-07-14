using System.Text.Json.Serialization;

namespace EposPosLinkExample.Models
{
    public class RefundResult
    {
        [JsonPropertyName("gatewayRefundId")]
        public string? GatewayRefundId { get; set; }

        [JsonPropertyName("result")]
        [JsonConverter(typeof(JsonStringEnumConverter))]
        public RefundResultCode Result { get; set; }

        [JsonPropertyName("debugErrorMessage")]
        public string? DebugErrorMessage { get; set; }

        public bool IsSuccess => Result == RefundResultCode.SUCCESS && GatewayRefundId != null;

        public enum RefundResultCode
        {
            SUCCESS,
            INVALID_AMOUNT_OR_PAYMENT_ID,
            AMOUNT_EXCEEDS_ALLOWED,
            DECLINED,
            NETWORK_ERROR,
            CANCELLED,
            FAILED
        }

        public override string ToString() =>
            $"RefundResult {{ GatewayRefundId: {GatewayRefundId}, Result: {Result}, DebugErrorMessage: {DebugErrorMessage} }}";
    }
}
