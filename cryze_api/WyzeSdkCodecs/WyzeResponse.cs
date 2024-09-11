using Python.Runtime;

namespace WyzeSdkCodecs
{
  public class WyzeResponse
  {
    public string? HttpVerb { get; set; }
    public string? ApiArgs { get; set; }
    public Dictionary<string, string>? ReqArgs { get; set; }
    public object? Data { get; set; }// annoyingly, dict or bytes on the python side

    /// <summary>
    /// if data_type is dict (python...), this will be the data as a dictionary, otherwise it'll probably throw an exception. check the type.
    /// </summary>
    public Dictionary<string, object>? DataAsDict => Data as Dictionary<string, object>;
    /// <summary>
    /// if data_type is byte (python...), this will be the data as byte[], otherwise it'll probably throw an exception. check the type.
    /// </summary>
    public byte[]? DataAsBytes => Data as byte[];

    /// <summary>
    /// The type of the data object, which could be a dictionary or a byte array, cause thanks for inconsistency wyze
    /// </summary>
    public Type? DataType => Data?.GetType();
    public Dictionary<string, object>? Headers { get; set; }
    public int StatusCode { get; set; }

    /// <summary>
    /// The original PyObject that this object was created from. This is useful for debugging and getting attributes that aren't deserialized (yay dynamic...).
    /// </summary>
    public required PyObject PyObject { get; set; }
  }
  public static class WyzeResponseHelper
  {

    /// <summary>
    /// Try to get an attribute from a PyObject, and cast it to a type. If the attribute doesn't exist, return null.
    /// This exists because dynamic is the underlying type of PyObject, and dynamic can't be used with generics.
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <param name="pyObject">the response object to cast to WyzeResponse</param>
    /// <param name="attr">the name of the attribute in the bag</param>
    /// <returns></returns>
    private static T? TryGetAs<T>(this PyObject pyObject, string attr)
    {
      if (!pyObject.HasAttr(attr))
      {
        return default;
      }
      try{
        return pyObject.GetAttr(attr).As<T>();
      }
      catch (Exception e)
      {
        Console.WriteLine($"Failed to cast {attr} to {typeof(T).Name}: {e.Message}");
        return default; // probably a bad cast
      }
    }

    public static WyzeResponse? MarshalToWyzeResponse(this PyObject pyObject)
    {
      try
      {
        return new WyzeResponse
        {
          HttpVerb = pyObject.TryGetAs<string>("http_verb"),
          ApiArgs = pyObject.TryGetAs<string>("api_args"),
          ReqArgs = pyObject.TryGetAs<Dictionary<string, string>>("req_args"),
          Data = pyObject.TryGetAs<object>("data"),
          Headers = pyObject.TryGetAs<Dictionary<string, object>>("headers"),
          StatusCode = pyObject.TryGetAs<int>("status_code"),
          PyObject = pyObject // keep the original object for all of the pythonic edge cases
        };
      }
      catch (Exception)
      {
        return null;
      }
    }
  }
}
