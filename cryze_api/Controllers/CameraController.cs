using Cryze.API.Services;
using Cryze.API.ViewModels;
using Microsoft.AspNetCore.Mvc;

namespace Cryze.API.Controllers;
[Route("Camera")]
public class CameraController : Controller
{
  private readonly ICameraStore _store;
  private readonly WyzeSdkService _wyzeSdkService;
  private readonly ILogger<CameraController> _logger;

  public CameraController(ICameraStore store, WyzeSdkService wyzeSdkService, ILogger<CameraController> logger)
  {
    _store = store;
    _wyzeSdkService = wyzeSdkService;
    _logger = logger;
  }

  public CameraViewModel GetViewModel()
  {
    var cameras = _store.GetCameraInfos();
    var viewModel = new CameraViewModel
    {
      Cameras = cameras
    };
    return viewModel;
  }

  [HttpGet]
  public IActionResult Index()
  {
    return View(GetViewModel());
  }

  [HttpPost("AddOrUpdate")]
  public IActionResult AddOrUpdateCamera([FromForm] CameraInfo camera)
  {
    if (camera == null)
    {
      return NotFound();
    }
    // If the stream name is empty, we will set it to live/{deviceId}
    if (string.IsNullOrEmpty(camera.StreamName))
    {
      camera.StreamName = $"live/{camera.DeviceId}";
    }
    _store.AddOrUpdateCameraInfo(camera);
    return RedirectToAction(nameof(Index));
  }

  [HttpPost("GetAllSupportedCameras")]
  public async Task<IActionResult> GetAllSupportedCameras()
  {
    // this is python and takes a while to run.
    await _wyzeSdkService.UpdateCameraList();
    
    return RedirectToAction(nameof(Index));
  }

  [HttpPost("Delete")]
  public IActionResult DeleteCamera([FromForm] CameraInfo camera)
  {
    if (camera == null)
    {
      return NotFound();
    }

    _store.RemoveCameraInfo(camera);
    return RedirectToAction(nameof(Index));
  }

  [HttpGet("CameraToken")]
  [Produces("application/json")]
  // These are literally one time use tokens, so we don't want to cache them ever. The IoTVideoSdk side is very picky about this.
  [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
  public async Task<IActionResult> GetCameraToken(string deviceId)
  {
    if (_store.GetCameraInfo(deviceId) == null)
    {
      return NotFound();
    }

    // The token is returned as a JSON object cause kotlin is on the other side and it's easier to parse
    return Json(await _wyzeSdkService.GetCameraTokenAsync(deviceId));
  }

  [HttpGet("CameraList")]
  [Produces("application/json")]
  public IActionResult GetCameraList()
  {
    // just a json array of device ids
    return Json(_store.GetCameraInfos().Select(x => x.DeviceId).ToArray());
  }

  [HttpGet("DeviceInfo")]
  [Produces("application/json")]
  public IActionResult GetDeviceInfo(string deviceId)
  {
    var camera = _store.GetCameraInfo(deviceId);
    if (camera == null)
    {
      return NotFound();
    }

    return Json(camera);
  }
}