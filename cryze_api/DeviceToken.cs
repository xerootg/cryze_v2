using System.Text.Json.Serialization;

public class DeviceConfiguration
{
  [JsonPropertyName("accessId")]
  public required string AccessId { get; set; }

  [JsonPropertyName("accessToken")]
  public required string AccessToken { get; set; }

  [JsonPropertyName("expireTime")]
  public int ExpireTime { get; set; }

  [JsonPropertyName("deviceId")]
  public required string DeviceId { get; set; }

  [JsonPropertyName("timestamp")]
  public int Timestamp { get; set; }

  [JsonPropertyName("socketPort")]
  public int SocketPort { get; set; }

  public bool IsExpired()
  {
    int currentTimestamp = (int)DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds;
    return ExpireTime < currentTimestamp;
  }

  public bool IsValid()
  {
    return !string.IsNullOrEmpty(AccessId) &&
           !string.IsNullOrEmpty(AccessToken) &&
           !string.IsNullOrEmpty(DeviceId) &&
           ExpireTime > 0 &&
           Timestamp > 0 &&
           SocketPort > 0;
  }
}