namespace Cryze.API.Services;
public class InMemoryCameraMessageStore
{
  private readonly ILogger<InMemoryCameraMessageStore> _logger;
  private readonly Dictionary<string, Dictionary<string, string>> _cameraMessages = new Dictionary<string, Dictionary<string, string>>();

  // Cameras are going to be sending messages to this store from multiple threads, so we need to lock it to prevent corruption
  private readonly object _lock = new object();

  public InMemoryCameraMessageStore(ILogger<InMemoryCameraMessageStore> logger)
  {
    _logger = logger;
  }

  public void AddMessage(string cameraId, string messageType, string message)
  {
    _logger.LogInformation($"Adding message for camera {cameraId} of type {messageType}");
    lock (_lock)
    {
      if (!_cameraMessages.ContainsKey(cameraId))
      {
        _logger.LogInformation($"Creating new message dictionary for camera {cameraId}");
        _cameraMessages.Add(cameraId, new Dictionary<string, string>());
      }

      if (_cameraMessages[cameraId].ContainsKey(messageType))
      {
        _logger.LogInformation($"Overwriting message for camera {cameraId} of type {messageType}");
        _cameraMessages[cameraId][messageType] = message;
      }
      else
      {
        _logger.LogInformation($"Adding new message for camera {cameraId} of type {messageType}");
        _cameraMessages[cameraId].Add(messageType, message);
      }
    }
  }

  public Dictionary<string, Dictionary<string, string>> GetMessages()
  {
    lock (_lock)
    {
      _logger.LogInformation("Getting all messages");
      return _cameraMessages;
    }
  }
}