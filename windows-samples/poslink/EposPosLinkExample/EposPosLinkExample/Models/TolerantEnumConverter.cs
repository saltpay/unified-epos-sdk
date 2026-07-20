using System;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace EposPosLinkExample.Models
{
    /// <summary>
    /// Reads a string-valued enum tolerantly: a value this sample doesn't recognise (for example a
    /// new in-progress state introduced by a newer SDK) falls back to the enum's UNKNOWN member
    /// instead of throwing. Writing keeps the enum name as-is.
    /// </summary>
    public class TolerantEnumConverter<T> : JsonConverter<T> where T : struct, Enum
    {
        public override T Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            if (reader.TokenType == JsonTokenType.String)
            {
                var value = reader.GetString();
                if (value != null && Enum.TryParse<T>(value, ignoreCase: true, out var parsed))
                {
                    return parsed;
                }
            }

            return Enum.TryParse<T>("UNKNOWN", out var unknown) ? unknown : default;
        }

        public override void Write(Utf8JsonWriter writer, T value, JsonSerializerOptions options)
        {
            writer.WriteStringValue(value.ToString());
        }
    }
}
