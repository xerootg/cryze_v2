namespace Cryze.API.Services;

// if there are any options we want to use in the JsonCameraStore, we can add them here
public class StoreConfiguration
{
  /// <summary>
  /// If true, the Wyze credentials will be loaded from the .env file on startup
  /// </summary>
  public bool LoadWyzeCredentialsOnStartup { get; set; } = true;
  
  /// <summary>
  /// If true, the supported cameras will be loaded from Wyze on startup
  /// </summary>
  public bool LoadWyzeSupportedCamerasOnStartup { get; set; } = true;
}

