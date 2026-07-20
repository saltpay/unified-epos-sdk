using System.Text.Json.Serialization;

namespace EposPosLinkExample.Models
{
    public class MakePaymentStateChange
    {
        [JsonPropertyName("transactionId")]
        public required string TransactionId { get; set; }

        [JsonPropertyName("amount")]
        public int Amount { get; set; }

        [JsonPropertyName("tip")]
        public int? Tip { get; set; }

        [JsonPropertyName("currency")]
        public required string Currency { get; set; }

        [JsonPropertyName("gatewayPaymentId")]
        public string? GatewayPaymentId { get; set; }

        [JsonPropertyName("state")]
        [JsonConverter(typeof(JsonStringEnumConverter))]
        public PaymentState State { get; set; }

        [JsonPropertyName("inProgressState")]
        [JsonConverter(typeof(TolerantEnumConverter<InProgressState>))]
        public InProgressState? ProgressState { get; set; }

        [JsonPropertyName("isFinal")]
        public bool IsFinal { get; set; }

        [JsonPropertyName("reason")]
        [JsonConverter(typeof(JsonStringEnumConverter))]
        public PaymentStateReason? Reason { get; set; }

        [JsonPropertyName("metadata")]
        public MetadataInfo? Metadata { get; set; }

        [JsonPropertyName("transactionTimestamp")]
        public long? TransactionTimestamp { get; set; }

        [JsonPropertyName("debugErrorMessage")]
        public string? DebugErrorMessage { get; set; }

        public enum PaymentState
        {
            PENDING,
            NEW,
            IN_PROGRESS,
            CANCELLING,
            CANCELED,
            SUCCESSFUL,
            PROCESSING_FAILED,
            COMMUNICATION_FAILED
        }

        public enum InProgressState
        {
            SENT_TO_PAYMENT_APP,
            RECEIVED,
            WAITING_FOR_CARD_ENTRY,
            TIP_SELECTION,
            CARD_PRESENTED,
            PIN_ENTRY,
            INSERT_CARD,
            DCC_SELECTION,
            REMOVE_CARD,
            APP_SELECTION,
            MOTO_FORM_SHOWN,
            UNKNOWN
        }

        public enum PaymentStateReason
        {
            PROCESSING_FAILED_DECLINED_ONLINE,
            PROCESSING_FAILED_DECLINED_OFFLINE,
            PROCESSING_FAILED_TIMEOUT,
            PROCESSING_FAILED_CONNECTION_ERROR,
            PROCESSING_FAILED_COMM_TIMEOUT,
            PROCESSING_FAILED_CARD_PROCESSING_ERROR,
            UNKNOWN,
            CANCELED_BY_EPOS,
            CANCELED_BY_USER,
            EXPIRED,
            COMMUNICATION_FAILED_NETWORK,
            COMMUNICATION_FAILED_UNEXPECTEDLY,
            COMMUNICATION_FAILED_AUTH_REQUIRED
        }

        public class MetadataInfo
        {
            [JsonPropertyName("card")]
            public Card? CardInfo { get; set; }

            [JsonPropertyName("entryMode")]
            [JsonConverter(typeof(JsonStringEnumConverter))]
            public EntryModeInfo? EntryMode { get; set; }

            [JsonPropertyName("verificationMethod")]
            [JsonConverter(typeof(JsonStringEnumConverter))]
            public VerificationMethodInfo? VerificationMethod { get; set; }

            [JsonPropertyName("applicationId")]
            public string? ApplicationId { get; set; }

            [JsonPropertyName("merchantAcquiringId")]
            public string? MerchantAcquiringId { get; set; }

            [JsonPropertyName("responseCode")]
            public string? ResponseCode { get; set; }

            [JsonPropertyName("authorisationCode")]
            public string? AuthorisationCode { get; set; }

            [JsonPropertyName("dcc")]
            public Dcc? DccInfo { get; set; }

            public class Card
            {
                [JsonPropertyName("last4")]
                public required string Last4 { get; set; }

                [JsonPropertyName("issuingCountry")]
                public string? IssuingCountry { get; set; }

                [JsonPropertyName("brand")]
                [JsonConverter(typeof(JsonStringEnumConverter))]
                public CardBrand Brand { get; set; }

                [JsonPropertyName("type")]
                [JsonConverter(typeof(JsonStringEnumConverter))]
                public CardType? Type { get; set; }
            }

            public enum EntryModeInfo
            {
                E_COM,
                EMV_PIN,
                CONTACT_ICC,
                EMV_CONTACTLESS,
                KEYED,
                MAGSTRIPE_SWIPED,
                ON_FILE,
                MAGSTRIPE_FALLBACK
            }

            public enum VerificationMethodInfo
            {
                NONE,
                ELECTRONIC_SIGNATURE,
                ON_DEVICE,
                MANUAL,
                SIGNATURE,
                OFFLINE_PIN,
                ONLINE_PIN,
                OFFLINE_PIN_PLUS_SIGNATURE,
                SECURED_ELECTRONIC_COMMERCE
            }

            public enum CardBrand
            {
                VISA,
                MAESTRO,
                MASTERCARD,
                UNION_PAY,
                JCB,
                DINERS,
                AMEX,
                OTHER
            }

            public enum CardType
            {
                DEBIT,
                CREDIT,
                PREPAID,
                CHARGE,
                PRIVATE,
                DEFERRED_DEBIT,
                UNKNOWN
            }

            public class Dcc
            {
                [JsonPropertyName("conversionFee")]
                public required string ConversionFee { get; set; }

                [JsonPropertyName("disclaimer")]
                public required string Disclaimer { get; set; }

                [JsonPropertyName("exchangeRate")]
                public required string ExchangeRate { get; set; }

                [JsonPropertyName("transactionAmount")]
                public int TransactionAmount { get; set; }

                [JsonPropertyName("transactionCurrency")]
                public required string TransactionCurrency { get; set; }
            }
        }

        public override string ToString()
        {
            var metadataString = Metadata == null ? "null" :
                $"{{ CardInfo: {(Metadata.CardInfo == null ? "null" :
                    $"{{ Last4: {Metadata.CardInfo.Last4}, IssuingCountry: {Metadata.CardInfo.IssuingCountry}, " +
                    $"Brand: {Metadata.CardInfo.Brand}, Type: {Metadata.CardInfo.Type} }}")}, " +
                $"EntryMode: {Metadata.EntryMode}, VerificationMethod: {Metadata.VerificationMethod}, " +
                $"ApplicationId: {Metadata.ApplicationId}, MerchantAcquiringId: {Metadata.MerchantAcquiringId}, " +
                $"ResponseCode: {Metadata.ResponseCode}, AuthorisationCode: {Metadata.AuthorisationCode}, " +
                $"DccInfo: {Metadata.DccInfo} }}";

            return $"MakePaymentStateChange {{ " +
                   $"TransactionId: {TransactionId}, " +
                   $"GatewayPaymentId: {GatewayPaymentId}, " +
                   $"State: {State}, " +
                   $"ProgressState: {ProgressState}, " +
                   $"Amount: {Amount}, " +
                   $"Tip: {Tip}, " +
                   $"Currency: {Currency}, " +
                   $"IsFinal: {IsFinal}, " +
                   $"Reason: {Reason}, " +
                   $"TransactionTimestamp: {TransactionTimestamp}, " +
                   $"DebugErrorMessage: {DebugErrorMessage}, " +
                   $"Metadata: {metadataString} }}";
        }
    }
}