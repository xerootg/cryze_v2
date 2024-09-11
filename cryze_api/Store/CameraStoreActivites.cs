using Cryze.API.Services;
using Microsoft.Extensions.Options;

namespace Cryze.API.Store;

public class CameraStoreActivities
{
  private readonly StoreConfiguration _storeConfig;
  private readonly IConfiguration _configuration;
  private readonly ICameraStore _cameraStore;
  private readonly WyzeSdkService _wyzeSdkService;
  private readonly ILogger<CameraStoreActivities> _logger;

  // This class is used to run startup activities that are not directly related to the API
  public CameraStoreActivities(
    IOptions<StoreConfiguration> storeConfig, // Configuration for the store, such as whether to load Wyze credentials on startup
    IConfiguration configuration, // Configuration for reading credentials from .env/etc
    ICameraStore cameraStore, // The camera store to update
    WyzeSdkService wyzeSdkService, // The Wyze SDK service to use
    ILogger<CameraStoreActivities> logger // The logger to use
  )
  {
    _storeConfig = storeConfig.Value;
    _configuration = configuration;
    _cameraStore = cameraStore;
    _wyzeSdkService = wyzeSdkService;
    _logger = logger;
  }

  public bool CredsLoadedAtStartup => _storeConfig.LoadWyzeCredentialsOnStartup;

  /// <summary>
  /// Run the automatic startup activities for the camera store
  /// This includes loading Wyze credentials and supported cameras
  /// </summary>
  /// <returns></returns>
  public async Task RunStartupActivitiesAsync()
  {
    if (_storeConfig.LoadWyzeCredentialsOnStartup || !_cameraStore.GetWyzeAPICredential().IsInitialized)
    {
      LoadWyzeCredentials();
    }

    if (_storeConfig.LoadWyzeSupportedCamerasOnStartup)
    {
      await LoadWyzeSupportedCamerasAsync();
    }
  }

/// <summary>
/// Load Wyze API credentials from .env file or environment variables
/// and update the camera store with the new credentials if they changed
/// 
/// note: its important to only update the store if the credentials changed,
/// changes to this invalidate the refresh token
/// </summary>
  public void LoadWyzeCredentials()
  {
    // Load Wyze API credentials from .env file or environment variables
    var newConfig = WyzeAPICredential.ReadSdkCredentialsFromConfig(_configuration);

    // Update the camera store with the new credentials ONLY if they changed
    var oldConfig = _cameraStore.GetWyzeAPICredential();

    // if it's not initialized, the new config is different
    var changed = !oldConfig.IsInitialized;
    if(DoSaveCredentialsIfTrue(newConfig, changed))return;
    // if it is initialized, check if the new config is different
    changed = changed || oldConfig.Email != newConfig.Email;
    if(DoSaveCredentialsIfTrue(newConfig, changed))return;
    changed = changed || oldConfig.Password != newConfig.Password;
    if(DoSaveCredentialsIfTrue(newConfig, changed))return;
    changed = changed || oldConfig.KeyId != newConfig.KeyId;
    if(DoSaveCredentialsIfTrue(newConfig, changed))return;
    changed = changed || oldConfig.ApiKey != newConfig.ApiKey;
    if(DoSaveCredentialsIfTrue(newConfig, changed))return;
  }
  private bool DoSaveCredentialsIfTrue(WyzeAPICredential newConfig, bool changed)
  {
    if (changed)
    {
      _logger.LogInformation("Wyze API credentials changed, updating store");
      _cameraStore.SetWyzeAPICredential(newConfig);
      return true;
    }
    return false;
  }

  private async Task LoadWyzeSupportedCamerasAsync()
  {
    // Load supported cameras from Wyze API and update the camera list
    await _wyzeSdkService.UpdateCameraList();
  }
}