using Cryze.API.Services;
using Cryze.API.Store;
using Cryze.API.ViewModels;
using Microsoft.AspNetCore.Mvc;

namespace Cryze.API.Controllers
{
  [Route("Settings")]
  public class SettingsController : Controller
  {
    private readonly ICameraStore _store;
    private readonly CameraStoreActivities _activities;
    private readonly ILogger<SettingsController> _logger;

    public SettingsController(ICameraStore store, CameraStoreActivities activities, ILogger<SettingsController> logger)
    {
      _store = store;
      _activities = activities;
      _logger = logger;
    }

    public SettingsViewModel GetViewModel()
    {
      var settings = _store.GetWyzeAPICredential();
      var viewModel = new SettingsViewModel
      {
        Settings = settings
      };
      return viewModel;
    }

    [HttpGet]
    public IActionResult Index()
    {
      return View(GetViewModel());
    }

    [HttpPost]
    public IActionResult UpdateSettings([FromForm] WyzeAPICredential settings)
    {
      if (settings == null)
      {
        return NotFound();
      }
      // the refresh token is not in this form, so we need to get it from the store.
      // if it's invalid, the next invocation of the Wyze API will refresh it.
      settings.SdkToken = _store.GetWyzeAPICredential().SdkToken;
      _store.UpdateWyzeAPICredential(settings);

      if (_activities.CredsLoadedAtStartup)
      {
        _logger.LogWarning("The credentials you just entered will be reloaded on startup and may be overwritten.");
        TempData["Warning"] = "The credentials you just entered will be reloaded on startup and may be overwritten.";
      }

      return RedirectToAction(nameof(Index));
    }

    [HttpPost(nameof(UpdateSettingsFromEnvironment))]
    public IActionResult UpdateSettingsFromEnvironment()
    {
      _activities.LoadWyzeCredentials();
      return RedirectToAction(nameof(Index));
    }
  }
}