namespace Cryze.API.PythonExtensions;

public class PythonLogger
{
  private readonly ILogger<PythonLogger> _logger;

  public PythonLogger(ILogger<PythonLogger> logger)
  {
    _logger = logger;
  }

  public void Log(string message)
  {
    _logger.LogInformation(message);
  }
}
