using Cryze.API.Services;

namespace Cryze.API.ViewModels;

public class CameraViewModel
{
    public List<CameraInfo> Cameras { get; set; } = new List<CameraInfo>();
    public CameraInfo NewCamera { get; set; } = new CameraInfo();
}