using System.Net;

namespace Cryze.API.Services;

public class WyzeSdkServiceRestException : WyzeSdkServiceException
{
  public readonly HttpStatusCode StatusCode;
  public WyzeSdkServiceRestException(HttpStatusCode statusCode, string message) : base($"Status Code: {statusCode} Reason:{message}")
  {
    StatusCode = statusCode;
  }

  private static HttpStatusCode fromInt(int statusCode)
  {
    try
    {
      return (HttpStatusCode)statusCode;
    }
    catch
    {
      return HttpStatusCode.InternalServerError;
    }
  }

  public WyzeSdkServiceRestException(int statusCode, string message) : this(fromInt(statusCode), message)
  {
  }
}