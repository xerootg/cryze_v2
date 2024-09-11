// describes all of the information that is important to describe a camera

using System.Text.Json.Serialization;

namespace Cryze.API.Services;

public class CameraInfo : IEquatable<CameraInfo>
{
  [JsonPropertyName("cameraId")]
  public string DeviceId { get; set; } = string.Empty;
  [JsonPropertyName("streamName")]
  public string StreamName { get; set; } = string.Empty;

  // the default constructor is required for deserialization and the web form
  public CameraInfo()
  {
  }

  public CameraInfo(string deviceId, string streamName)
  {
    DeviceId = deviceId;
    // Here's the thing about streamnames. by default, they are actually the camera's nickname. however, if the camera's nickname is not set, the stream name is "live/{deviceId}"
    StreamName = string.IsNullOrEmpty(streamName) ? streamName : $"live/{deviceId}";
  }

  // The default stream name is "live/{deviceId}", providing a default constructor
  public CameraInfo(string deviceId)
  {
    DeviceId = deviceId;
    StreamName = $"live/{deviceId}";
  }

  public bool Equals(CameraInfo? other)
  {
    if (other == null)
    {
      return false;
    }
    // the DeviceId is the unique identifier for a camera, the stream name is not important
    return DeviceId == other.DeviceId;
  }

  public override int GetHashCode()
  {
    return DeviceId.GetHashCode();
  }
}
