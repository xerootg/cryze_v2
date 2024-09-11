namespace Cryze.API.Services;
public class WyzeSdkServiceException : Exception
{
  public WyzeSdkServiceException(string message) : base(message)
  {
  }

  public WyzeSdkServiceException(string message, Exception innerException) : base(message, innerException)
  {
  }
}