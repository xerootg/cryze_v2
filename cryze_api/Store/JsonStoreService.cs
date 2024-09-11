
// this class will persist information for running the app to a json file
using System.Text.Json;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;

namespace Cryze.API.Services;
public class JsonCameraInfoService : ICameraStore
{
  private readonly JsonStoreConfiguration _storeConfig;
  private static SerializableCameraStore _jsonStore = new SerializableCameraStore();
  private readonly ILoggerFactory _loggerFactory;
  private ILogger<JsonCameraInfoService> _logger;

  public JsonCameraInfoService(IOptions<JsonStoreConfiguration> config, ILoggerFactory? loggerFactory)
  {
    _storeConfig = config.Value;
    _loggerFactory = loggerFactory?? NullLoggerFactory.Instance;
    _logger = _loggerFactory.CreateLogger<JsonCameraInfoService>();

    Load();
  }

  public void AddOrUpdateCameraInfo(CameraInfo cameraInfo)
  {
    _jsonStore.DeviceList.AddOrUpdateDevice(cameraInfo);
    Save();
    _logger.LogInformation($"Added/Updated Camera Info for {cameraInfo.DeviceId}");
  }

  public void RemoveCameraInfo(CameraInfo cameraInfo)
  {
    _jsonStore.DeviceList.RemoveDevice(cameraInfo);
    Save();
    _logger.LogInformation($"Removed Camera Info for {cameraInfo.DeviceId}");
  }

  public CameraInfo? GetCameraInfo(string deviceId)
  {
    _logger.LogInformation($"Getting Camera Info for {deviceId}");
    return _jsonStore.DeviceList.Cameras.Find(x => x.DeviceId == deviceId) ?? null;
  }

  // get all camera infos from the json store
  public List<CameraInfo> GetCameraInfos()
  {
    _logger.LogInformation("Getting Camera Infos");
    return _jsonStore?.DeviceList.Cameras ?? new List<CameraInfo>();
  }

  public List<string> GetCameraIds()
  {
    _logger.LogInformation("Getting Camera IDs");
    return _jsonStore?.DeviceList.Cameras.Select(x => x.DeviceId).ToList() ?? new List<string>();
  }
  /// <summary>
  /// A complete set of cameras to update the current list with.
  /// persists customized stream names.
  /// removes cameras that are no longer on the account.
  /// adds cameras that are not saved but in the account.
  /// </summary>
  /// <param name="accountDevices"></param>
  public void UpdateCameraList(Dictionary<string, string> accountDevices)
  {
    _jsonStore.DeviceList.SyncronizeDevices(accountDevices);
    Save();
    _logger.LogInformation("Updated Camera List");
  }

  // update WyzeAPICredential
  public void UpdateWyzeAPICredential(WyzeAPICredential wyzeAPICredential)
  {
    _jsonStore.WyzeAPICredential = wyzeAPICredential;
    Save();
    _logger.LogInformation("Updated Wyze API Credential");
  }

  // TODO: Make this 'leased' where only one use at a time. gc counts as the end, as does a timeout.
  public WyzeAPICredential GetWyzeAPICredential()
  {
    _logger.LogInformation("Getting Wyze API Credential");
    return _jsonStore.WyzeAPICredential;
  }

  public void SetWyzeAPICredential(WyzeAPICredential wyzeAPICredential)
  {
    _jsonStore.WyzeAPICredential = wyzeAPICredential;
    Save();
    _logger.LogInformation("Updated Wyze API Credential");
  }

  // Note: Manages concurrency for file access, probably. Locks are bugs.
  private static readonly object _lock = new object();

  private void Save()
  {
    lock (_lock)
    {
      var json = JsonSerializer.Serialize(_jsonStore);
      File.WriteAllText(_storeConfig.FilePath, json);
    }
    _logger.LogInformation($"Saved {_jsonStore.DeviceList.Cameras.Count} cameras to {_storeConfig.FilePath}");
  }

  private void Load()
  {
    if (!File.Exists(_storeConfig.FilePath))
    {
      return;
    }
    lock (_lock)
    {
      var json = File.ReadAllText(_storeConfig.FilePath);
      _jsonStore = JsonSerializer.Deserialize<SerializableCameraStore>(json) ?? new SerializableCameraStore();
    }
    _logger.LogInformation($"Loaded {_jsonStore.DeviceList.Cameras.Count} cameras from {_storeConfig.FilePath}");
  }
}