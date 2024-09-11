using System.Text.Json.Serialization;
using Microsoft.Extensions.Logging.Abstractions;

namespace Cryze.API.Services;
public class WyzeSdkRefreshToken
{
  // Only available after a successful login
  [JsonPropertyName("accessToken")]
  public string AccessToken { get; set; }
  // Only available after a successful login
  [JsonPropertyName("refreshToken")]
  public string RefreshToken { get; set; }

  private readonly ILogger<WyzeAPICredential> _logger = NullLogger<WyzeAPICredential>.Instance;

  // basic constructor for serialization
  public WyzeSdkRefreshToken()
  {
    AccessToken = string.Empty;
    RefreshToken = string.Empty;
    _logger = NullLogger<WyzeAPICredential>.Instance;
  }

  // constructor for actual use
  public WyzeSdkRefreshToken(string aToken, string rToken, ILogger<WyzeAPICredential> logger)
  {
    AccessToken = aToken;
    RefreshToken = rToken;
    _logger = logger;
  }

  public WyzeSdkRefreshToken(WyzeSdkRefreshToken token, ILogger<WyzeAPICredential> logger)
  : this(token.RefreshToken, token.AccessToken, logger)
  {
  }

  /// <summary>
  /// Updates the access and refresh tokens from the wyze_sdk produced file.
  /// If it exists, it will read the file and delete it.
  /// If it doesn't exist, it will do nothing.
  /// </summary>
  /// <param name="fullFilePath">path to the file produced by the wyze_sdk script</param>
  public void AccessAndRefreshTokenFromFile(string fullFilePath)
  {
    if (!File.Exists(fullFilePath))
    {
      return;
    }
    try
    {
      var tokenFile = File.ReadAllLines(fullFilePath);
      if (tokenFile.Length < 2)
      {
        return;
      }
      AccessToken = tokenFile[0];
      RefreshToken = tokenFile[1];

      // delete the file after reading it
      File.Delete(fullFilePath);
    }
    catch (Exception e)
    {
      _logger.LogError(e, $"Error reading token file: {e.Message}");
    }
  }

  // to be called before get_camera_token.py runs
  public void WriteAccessAndRefreshTokenToFile(string fullFilePath)
  {
    File.WriteAllText(fullFilePath, $"{AccessToken}\n{RefreshToken}");
  }
}