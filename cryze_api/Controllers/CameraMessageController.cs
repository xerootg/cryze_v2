using System.Text;
using Cryze.API.Services;
using Cryze.API.ViewModels;
using Microsoft.AspNetCore.Mvc;

namespace Cryze.API.Controllers;

/*
* Here's what I know about messages from the cameras I have:
* There's three generic types:
* - ProConst
* - ProReadonly
* - ProWritable
* 
* ProReadonly has a couple subtypes:
* - generic (no subtype)
* - ProReadonly Power
* - ProReadonly Tfinfo
* - ProReadonly Netinfo
*
* ProWritable has a couple subtypes, but they are actually the same "MessagePath" but the payload is different:
* - MessagePath is "Action", payload is always StVal
* - ProWritable _otaVersion
* - ProWritable _otaUpdate
* - ProWritable turnOff
* - ProWritable fillLight
* - ProWritable executeCmd
* - ProWritable siren
* 
* The goal here is to collect one of each for each camera as received from the android app.
* The root object is a Dictionary<string, Dictionary<string, object>> where the key is the camera's ID, value is the messages recieved for it.
*
* Long-term persistence of these messages is not necessary, but it would be nice to have a way to store them for a short period of time.
*/

[Route("/CameraMessage")]
public class CameraMessageController : Controller
{
  private readonly ILogger<CameraMessageController> _logger;
  private readonly InMemoryCameraMessageStore _cameraMessageStore;

  public CameraMessageController(ILogger<CameraMessageController> logger, InMemoryCameraMessageStore cameraMessageStore)
  {
    _logger = logger;
    _cameraMessageStore = cameraMessageStore;
  }

  [HttpGet]
  public IActionResult Index()
  {
    // return the view
    return View(new CameraMessageViewModel(_cameraMessageStore.GetMessages()));
  }

  [HttpPost]
  public async Task<IActionResult> PostCameraMessage([FromQuery] string cameraId, [FromQuery] string messageType, [FromQuery] string? path)
  {
    if (string.IsNullOrWhiteSpace(cameraId) || string.IsNullOrWhiteSpace(messageType))
    {
      return BadRequest("Invalid data");
    }

    // This is the actual message data
    string data = string.Empty;
    using (StreamReader reader = new StreamReader(Request.Body, Encoding.UTF8))
    {
      data = await reader.ReadToEndAsync();
    }
    if (string.IsNullOrWhiteSpace(data))
    {
      return BadRequest("Invalid data");
    }

    // proconst, just store it
    if (messageType.Equals("MSG_TYPE_PRO_CONST"))
    {
      _logger.LogInformation($"Received ProConst message for camera {cameraId}");
      _logger.LogInformation(data);
      _cameraMessageStore.AddMessage(cameraId, messageType, data);
      return Ok();
    }

    // proreadonly, key is the message type and path
    if (messageType.Equals("MSG_TYPE_PRO_READONLY"))
    {
      if (string.IsNullOrWhiteSpace(path))
      {
        return BadRequest("Invalid data");
      }

      _logger.LogInformation($"Received ProReadonly message for camera {cameraId}");
      _logger.LogInformation(data);
      _cameraMessageStore.AddMessage(cameraId, $"{messageType}::{path}", data);
      return Ok();
    }

    // prowriteable, key is the message type, the path is always "Action", but the payload will be different. we are going to need to parse the payload to get the actual message type
    if (messageType.Equals("MSG_TYPE_PRO_WRITABLE"))
    {
      if (string.IsNullOrWhiteSpace(path))
      {
        return BadRequest("Invalid data");
      }

      _logger.LogInformation($"Received ProWritable message for camera {cameraId}");
      _logger.LogInformation(data);

      // parse the payload to get the actual message type
      var jsonContent = JsonContent.Create(data);
      var payload = await jsonContent.ReadFromJsonAsync<Dictionary<string, object>>();
      // the only thing that matters is the keys. we don't care about the values. value will be passed as string to the store

      foreach (var payloadSubMessage in payload?.Keys ?? Enumerable.Empty<string>())
      {
        _logger.LogInformation($"Adding ProWritable message for camera {cameraId} of type {payloadSubMessage}");
        _cameraMessageStore.AddMessage(cameraId, $"{messageType}::{path}::{payloadSubMessage}", payload?[payloadSubMessage] as string ?? string.Empty);
      }

      return Ok();
    }
    if(messageType.Equals("MSG_TYPE_EVENT"))
    {
      _logger.LogInformation($"Received Event message for camera {cameraId}");
      _logger.LogInformation(data);
      _cameraMessageStore.AddMessage(cameraId, messageType, data);
      return Ok();
    }
    if(messageType.Equals("MSG_TYPE_ACTION"))
    {
      _logger.LogInformation($"Received Action message for camera {cameraId}");
      _logger.LogInformation(data);
      _cameraMessageStore.AddMessage(cameraId, messageType, data);
      return Ok();
    }

    return BadRequest("No matching message type found");
  }
}