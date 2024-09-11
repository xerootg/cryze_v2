namespace Cryze.API.Services;

// Maybe, someday, json will be replaced with a database.
public interface ICameraStore
{
  public void AddOrUpdateCameraInfo(CameraInfo cameraInfo);
  public void RemoveCameraInfo(CameraInfo cameraInfo);
  public CameraInfo? GetCameraInfo(string deviceId);
  public List<CameraInfo> GetCameraInfos();
  public List<string> GetCameraIds();
  /// <summary>
  /// Updates the list of cameras in the store.
  /// persists customized stream names, sets the default stream name if it's empty to nickname.
  /// removes cameras that are no longer on the account.
  /// adds cameras that are not saved but in the account.
  /// </summary>
  /// <param name="accountDevices">a list of cameras and the wyze nickname</param>
  public void UpdateCameraList(Dictionary<string, string> accountDevices);
  public void UpdateWyzeAPICredential(WyzeAPICredential wyzeAPICredential);
  public WyzeAPICredential GetWyzeAPICredential();
  public void SetWyzeAPICredential(WyzeAPICredential wyzeAPICredential);
}