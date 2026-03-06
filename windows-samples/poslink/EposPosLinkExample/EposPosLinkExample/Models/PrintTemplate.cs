using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace EposPosLinkExample.Models;

public class PrintTemplate
{
    [JsonPropertyName("rows")]
    public required List<ReceiptRow> Rows { get; set; }
}

[JsonPolymorphic(TypeDiscriminatorPropertyName = "rowType")]
[JsonDerivedType(typeof(ReceiptRowItems), "items")]
[JsonDerivedType(typeof(ReceiptRowItem), "item")]
[JsonDerivedType(typeof(ReceiptRowSpacer), "spacer")]
[JsonDerivedType(typeof(ReceiptRowDivider), "divider")]
public abstract class ReceiptRow { }

public class ReceiptRowItems : ReceiptRow
{
    [JsonPropertyName("items")]
    public required List<RowElement> Items { get; set; }
}

public class ReceiptRowItem : ReceiptRow
{
    [JsonPropertyName("item")]
    public required RowElement Item { get; set; }
}

public class ReceiptRowSpacer : ReceiptRow { }

public class ReceiptRowDivider : ReceiptRow { }

[JsonPolymorphic(TypeDiscriminatorPropertyName = "elementType")]
[JsonDerivedType(typeof(RowElementText), "text")]
[JsonDerivedType(typeof(RowElementQrCode), "qrCode")]
public abstract class RowElement { }

public class RowElementText : RowElement
{
    [JsonPropertyName("text")]
    public required string Text { get; set; }

    [JsonPropertyName("align")]
    [JsonConverter(typeof(JsonStringEnumConverter))]
    public Align Align { get; set; }

    [JsonPropertyName("bold")]
    public bool Bold { get; set; }
}

public class RowElementQrCode : RowElement
{
    [JsonPropertyName("url")]
    public required string Url { get; set; }

    [JsonPropertyName("align")]
    [JsonConverter(typeof(JsonStringEnumConverter))]
    public Align Align { get; set; }
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum Align
{
    LEFT,
    RIGHT,
    CENTER
}
