using System.Net;
using Cryze.API.Python;
using Microsoft.Extensions.Options;
using Python.Runtime; // Used for PyObject primarily, it's not PythonEngine.
using WyzeSdkCodecs;

namespace Cryze.API.Services;

public class WyzeSdkService
{

  private const string ClientToken = "_token";
  private const string ClientRefreshToken = "_refresh_token";
  private const string AccessToken = "accessToken";
  private const string AccessId = "accessId";

  private readonly ICameraStore _cameraStore;
  private readonly WyzeSdkServiceConfiguration _config;
  private readonly ILogger<WyzeSdkService> _logger;
  private readonly PythonServices _pythonServices;
  private readonly PythonRuntimeTask _pythonRuntimeTask;

  public WyzeSdkService(ICameraStore cameraStore, IOptions<WyzeSdkServiceConfiguration> config, PythonServices pythonServices, PythonRuntimeTask pythonRuntimeTask, ILogger<WyzeSdkService> logger)
  {
    _cameraStore = cameraStore;
    _config = config.Value;
    _pythonServices = pythonServices;
    _logger = logger;
    _pythonRuntimeTask = pythonRuntimeTask;
  }

  /// <summary>
  /// Attempt to login to the Wyze API. If the token is invalid, it will attempt to refresh it. If the token is missing, it will attempt to login.
  /// </summary>
  /// <param name="wyze_sdk_library"></param>
  /// <returns>wyze_sdk.Client</returns>
  /// <exception cref="HttpRequestException">Something about a resty thing that wyze_sdk did threw</exception>
  /// <exception cref="Exception"></exception>
  private PyObject DoLogin(PyModule scope, dynamic wyze_sdk_library)
  {
    // needs a value to start with, but will be replaced with a PyObject
    dynamic client = new object();
    var creds = _cameraStore.GetWyzeAPICredential();
    PyObject pyCreds = creds.ToPython();
    dynamic pySdkToken = creds.SdkToken.ToPython();
    scope.Set("creds", pyCreds);

    // Attempt to use the stored token, if there is none, invoking the python script will make one
    var token = creds.SdkToken;

    // SDK WyzeResponse object. Nullable because casting can and does fail.
    WyzeResponse? login = null;

    try
    {
      if (token?.AccessToken != null && token?.RefreshToken != null)
      {
        try
        {
          client = wyze_sdk_library.Client(token: pySdkToken.AccessToken, refresh_token: pySdkToken.RefreshToken);
          var resp = client.refresh_token() as PyObject;
          login = resp?.MarshalToWyzeResponse();
          if (login == null)
          {
            _logger.LogWarning("Failed to login to Wyze API");
          }
          if (login?.StatusCode == 429)
          {
            throw new WyzeSdkServiceRestException(HttpStatusCode.TooManyRequests, "Wyze API rate limited");
          }
          else if (login?.StatusCode != 200)
          {
            _logger.LogWarning($"Failed to login to Wyze API: {login?.StatusCode}");
            login = null; // we didn't have a valid token, so we need to get a new one
          }

          if (login != null && login.StatusCode == 200)
          {
            _logger.LogInformation("Logged in to Wyze API using cached token");
            return client;
          }
        }
        catch (PythonException ex)
        {
          if (ex.Type.GetPythonType().ToString() == "wyze_sdk.errors.WyzeClientError")
          {
            _logger.LogWarning("Failed to login to Wyze API using cached token, resetting");
            creds.SdkToken = null;
            _cameraStore.SetWyzeAPICredential(creds);
            // refresh creds
            creds = _cameraStore.GetWyzeAPICredential();
            // we didn't have a valid token, so we need to get a new one
          }
          else
          {
            // We don't know what happened, so we'll just rethrow. Its probably our bug on the c# side anyway
            throw;
          }
        }
      }

      // a distinct failure to chooch has occurred (None == null)
      // This is intentionally not an else, because we want to try to login again if we failed to login with the token
      // its also intentionally not a try/catch, because any failure at this point is a failure to login
      if (login == null)
      {
        if (client is PyObject)
        {
          client.Dispose();
        }
        client = wyze_sdk_library.Client();
        // Use the wyze_sdk module
        var resp = client.login(email: creds.Email, password: creds.Password, key_id: creds.KeyId, api_key: creds.ApiKey) as PyObject;
        login = resp?.MarshalToWyzeResponse();
        if (login == null)
        {
          throw new HttpRequestException(HttpRequestError.ConnectionError, message: "Failed to login to Wyze API", statusCode: System.Net.HttpStatusCode.InternalServerError);
        }
        if (login.StatusCode != 200)
        {
          throw new WyzeSdkServiceRestException(login.StatusCode, "Failed to login to Wyze API");
        }

        _logger.LogInformation("Logged in to Wyze API");
        return client;
      }
      throw new WyzeSdkServiceException("Failed to login to Wyze API, client is null");
    }
    finally
    {
      if (login == null || login.StatusCode != 200 || client is not PyObject)
      {
        _logger.LogWarning("Failed to login to Wyze API, not saving token");
      }
      else
      {
        TryRecoverRefreshToken(client);
        _logger.LogInformation("Token Recovery Complete");
      }
    }
  }

  private void TryRecoverRefreshToken(PyObject client)
  {
    // Annoyingly, the cast to string can fail, so we have to catch it
    var respToken = string.Empty;
    var respRefreshToken = string.Empty;
    try
    {
      try { respToken = client.GetAttr(ClientToken).As<string>().ToString(); } catch { }
      try { respRefreshToken = client.GetAttr(ClientRefreshToken).As<string>(); } catch { }
      if (!string.IsNullOrEmpty(respToken) && !string.IsNullOrEmpty(respRefreshToken))
      {
        _logger.LogInformation("Recovered token from client");

        var updatedCreds = _cameraStore.GetWyzeAPICredential();

        updatedCreds.SdkToken = new WyzeSdkRefreshToken()
        {
          AccessToken = respToken!,
          RefreshToken = respRefreshToken!
        };

        _cameraStore.SetWyzeAPICredential(updatedCreds);
        _logger.LogInformation("Updated token in store");
      }
      else
      {
        // there should be a much more elegant way to do this, but I'm not sure what it is
        var valsThatAreNull = new List<string>();

        if (string.IsNullOrEmpty(respToken)) valsThatAreNull.Add(ClientToken);
        if (string.IsNullOrEmpty(respRefreshToken)) valsThatAreNull.Add(ClientRefreshToken);

        // PyObject.HasAttr could have given us a better error message, but yolo. Hobbies, amirite?
        _logger.LogWarning("Failed to recover token from client, [{0}] were null or not defined", string.Join(", ", valsThatAreNull));
      }
    }
    catch (Exception ex)
    {
      _logger.LogWarning($"Failed to recover token from client: {ex.Message}");
    }
  }

  public async Task<Dictionary<string, string>> GetSupportedCameras()
  {
    // mac -> nickname
    var promise = new TaskCompletionSource<Dictionary<string, string>>();

    _pythonRuntimeTask.EnqueuePythonTask(() =>
    {
      try{
      _logger.LogInformation("Begin work on the main thread");

      var dictionaryOfDevices = new Dictionary<string, string>();
      // using (var gil = Py.GIL())
      // {
        _logger.LogInformation("Got GIL");
        using (PyModule scope = Py.CreateScope())
        {
          _logger.LogInformation("Created scope");
          _pythonServices.RegisterLogger();
          _logger.LogInformation("Registered logger");

          // Recast to dynamic so we can use the wyze_sdk module
          // PyObject needs to be understood as IDisposable, so wrap it in a using
          using (dynamic wyze_sdk_library = PyModule.Import("wyze_sdk"))
          using (dynamic client = DoLogin(scope, wyze_sdk_library))
          {
            _logger.LogInformation("Got client");

            // standard CameraClient filters out non-camera devices
            using (var devices = client.devices_list())
            {

              foreach (var device in devices)
              {
                if (device.type == "Camera")
                {
                  using (device)
                  {
                    _logger.LogInformation($"Found device: {device.type} {device.mac} {device.nickname} {device.get_non_null_attributes()}");
                    var deviceId = device.mac.As<string>();
                    var nickname = device.nickname.As<string>();
                    _logger.LogInformation($"Adding device: {deviceId} {nickname}");
                    dictionaryOfDevices.Add(deviceId, nickname);
                  }
                }
              }
            }
            _logger.LogInformation("Disposed device object");
            _pythonServices.UnregisterLogger();
          }
          _logger.LogInformation("Disposed Wyze API client");
        }
        using dynamic gc = PyModule.Import("gc");
        gc.collect();
      // post the result back to the main thread
      promise.SetResult(dictionaryOfDevices);
      }
      // let the promise waiter handle the exception
      catch (Exception ex)
      {
        _logger.LogWarning(ex, "Failed to get supported cameras");
        promise.SetException(ex);
      }
    });

    var dictionaryOfDevices = await promise.Task;
    // now filter out the devices that are not supported
    var supported = dictionaryOfDevices.Where(device => _config.ValidMarsDevicePrefix.Any(prefix => device.Key.StartsWith(prefix)))
                       .ToDictionary(device => device.Key, device => device.Value);

    _logger.LogInformation("Supported devices discovered: {0}", string.Join(", ", supported.Keys));
    return supported;
  }

  public async Task UpdateCameraList()
  {
    var supported = await GetSupportedCameras();
    _cameraStore.UpdateCameraList(supported);
  }

  public async Task<AccessCredential> GetCameraTokenAsync(string deviceId)
  {

    var promise = new TaskCompletionSource<AccessCredential>();

    _pythonRuntimeTask.EnqueuePythonTask(() =>{
    var accessId = 0L;
    var accessToken = string.Empty;
      // TODO: Correctly handle dispose and use of the scope throughout the method
      using PyModule scope = Py.CreateScope();
      try
      {
        _pythonServices.RegisterLogger();

        // SDK BaseClient object
        using dynamic wyze_sdk_library = PyModule.Import("wyze_sdk");
        using dynamic client = DoLogin(scope, wyze_sdk_library);
        // from wyze_sdk.service.base import WpkNetServiceClient (base is a reserved word in C#)
        using dynamic baseLib = PyModule.Import("wyze_sdk.service.base");
        dynamic WpkNetServiceClient = baseLib.WpkNetServiceClient;

        var mars_base_url = _config.MarsUrl;
        using var wpk = WpkNetServiceClient(token: client._token, base_url: mars_base_url);

        using var nonce = wpk.request_verifier.clock.nonce();

        using var json_dict = new PyDict();
        json_dict["ttl_minutes"] = 10080.ToPython();
        json_dict["nonce"] = nonce;
        json_dict["unique_id"] = wpk.phone_id;

        using var header_dict = new PyDict();
        header_dict["appid"] = wpk.app_id;

        var mars_path_url = $"{_config.MarsRegisterGwUserRoute}{deviceId}";

        dynamic nonceString = nonce.ToString();

        using var wkpResp = wpk.api_call(api_method: mars_path_url, json: json_dict, headers: header_dict, nonce: nonceString) as PyObject;

        var respAsResponse = wkpResp?.MarshalToWyzeResponse();

        if (respAsResponse == null)
        {
          throw new WyzeSdkServiceRestException(HttpStatusCode.InternalServerError, "Failed to register gateway user");
        }
        if (respAsResponse.StatusCode != 200)
        {
          throw new WyzeSdkServiceRestException(respAsResponse.StatusCode, "Failed to get camera token");
        }

        _logger.LogInformation($"Got camera token{wkpResp}");

        if (wkpResp == null)
        {
          throw new WyzeSdkServiceRestException(HttpStatusCode.InternalServerError, "Token response was null");
        }

        // Parse the response.
        // This is a dict[dict], so we can't use the helper.


        try { accessId = long.Parse((string)((dynamic)wkpResp)["data"][AccessId]); }
        catch (Exception e) { _logger.LogWarning($"Failed to parse {AccessId}: {e.Message}"); }

        try { accessToken = ((dynamic)wkpResp)["data"][AccessToken]; }
        catch (Exception e) { _logger.LogWarning($"Failed to parse {AccessToken}: {e.Message}"); }

        if (accessId == 0L || string.IsNullOrEmpty(accessToken))
        {
          _logger.LogWarning("Failed to get camera token");
          throw new WyzeSdkServiceRestException(HttpStatusCode.InternalServerError, "Failed to get camera token");
        }

        promise.SetResult(new AccessCredential()
        {
          AccessId = accessId,
          AccessToken = accessToken
        });
      }
      // Let the promise await the exception
      catch (Exception ex)
      {
        _logger.LogWarning(ex, "Failed to get camera token");
        promise.SetException(ex);
      }
      finally // Before we loose the GIL
      {
        _logger.LogInformation("Unregistering logger");
        _pythonServices.UnregisterLogger();
      }
    });
    
    return await promise.Task;
  }
}
