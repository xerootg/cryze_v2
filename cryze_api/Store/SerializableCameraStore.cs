using System.Text.Json.Serialization;

namespace Cryze.API.Services;
public class SerializableCameraStore{

  [JsonPropertyName("devices")]
  public DeviceList DeviceList { get; set; } = new DeviceList();

  [JsonPropertyName("accessCredential")]
  public WyzeAPICredential WyzeAPICredential { get; set; } = new WyzeAPICredential();

  public SerializableCameraStore()
  {
    DeviceList = new DeviceList();
    WyzeAPICredential = new WyzeAPICredential();
  }
}