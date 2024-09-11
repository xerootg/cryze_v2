using System.Text.Json.Serialization;

namespace Cryze.API.Services;
// These are issued by Wyze per-camera and are used to
// authenticate with the IoTVideoSDK. These should not
// be persisted for any reason. they are one-time use
// tokens that expire immediately after first use
public class AccessCredential
{
    [JsonPropertyName("accessId")]
    public long AccessId { get; set; }
    [JsonPropertyName("accessToken")]
    public string AccessToken { get; set; }

    public AccessCredential()
    {
        AccessId = 0;
        AccessToken = string.Empty;
    }
}