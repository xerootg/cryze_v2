using System.Text.Json.Serialization;

namespace Cryze.API.Services;
public class DeviceList
{
  [JsonPropertyName("deviceIds")]
  public List<CameraInfo> Cameras { get; set; } = new List<CameraInfo>();

  public void AddOrUpdateDevice(CameraInfo deviceId)
  {
    if (Cameras.Contains(deviceId))
    {
      Cameras.Remove(deviceId);
    }
    Cameras.Add(deviceId);
  }

  public void RemoveDevice(CameraInfo deviceId)
  {
    if (Cameras.Contains(deviceId))
    {
      Cameras.Remove(deviceId);
    }
  }

  internal void SyncronizeDevices(Dictionary<string, string> accountDevices)
  {
    // copy the current state so we can modify it
    var currentDevices = new List<CameraInfo>(Cameras);

    // add cameras that are not in the current state
    foreach (var cam in accountDevices)
    {
      if (currentDevices.Any(c => c.DeviceId == cam.Key))
      {
        continue;
      }

      // if it's not, add it
      AddOrUpdateDevice(new CameraInfo
      {
        DeviceId = cam.Key,
        // replace spaces with underscores and make it lowercase, and default to empty string if it's null or whitespace
        StreamName = string.IsNullOrWhiteSpace(cam.Value) ? string.Empty : cam.Value.Replace(" ", "_").ToLowerInvariant()
      });
    }

    // remove cameras that are not supported
    foreach (var cam in currentDevices)
    {
      // if the camera in currentCameras (our state) is not in the accountCameras (the state from Wyze), remove it
      if (!accountDevices.Keys.Contains(cam.DeviceId))
      {
        RemoveDevice(cam);
      }
    }
  }
}