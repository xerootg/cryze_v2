using System.Text.Json.Serialization;
using Microsoft.Extensions.Logging.Abstractions;

namespace Cryze.API.Services;
public class WyzeAPICredential
{
  [JsonIgnore]
  public bool IsInitialized => !string.IsNullOrEmpty(Email) && !string.IsNullOrEmpty(Password) && !string.IsNullOrEmpty(KeyId) && !string.IsNullOrEmpty(ApiKey);

  // Required for the basic wyze_sdk Client login
  [JsonPropertyName("email")]
  public string Email { get; set; }
  [JsonPropertyName("password")]
  public string Password { get; set; }
  [JsonPropertyName("key_id")]
  public string KeyId { get; set; }
  [JsonPropertyName("api_key")]
  public string ApiKey { get; set; }

  // Only available after a successful login
  [JsonPropertyName("sdkToken")]
  public WyzeSdkRefreshToken? SdkToken;

  private readonly ILogger<WyzeAPICredential> _logger = NullLogger<WyzeAPICredential>.Instance;

  public WyzeAPICredential()
  {
    Email = string.Empty;
    Password = string.Empty;
    KeyId = string.Empty;
    ApiKey = string.Empty;
    SdkToken = null;
  }

  public static WyzeAPICredential ReadSdkCredentialsFromConfig(IConfiguration configuration)
  {
    return new WyzeAPICredential
    {
      Email = configuration["WYZE_EMAIL"] ?? string.Empty,
      Password = configuration["WYZE_PASSWORD"] ?? string.Empty,
      KeyId = configuration["API_ID"] ?? string.Empty,
      ApiKey = configuration["API_KEY"] ?? string.Empty
    };
  }
}