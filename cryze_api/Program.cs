using System.Diagnostics;
using System.Text.Json;

var cameras = new Dictionary<string, CameraConfig>();

foreach (var cameraIdPair in
  // get the list of camera IDs from the environment variable, or throw an exception if it's not set
  Environment.GetEnvironmentVariable("CAMERA_IDS")?.Split(',').ToList() ??
  throw new Exception("CAMERA_IDS environment variable is not set; it should be a semicolon-separated list of camera IDs")
  )
{
  // three positions, camera ID, port, and server type
  var pair = cameraIdPair.Split(':');
  var port = 0;
  if (pair.Length == 2 || pair.Length == 3 && int.TryParse(pair[1], out port))
  {
    if(pair.Length == 2)
    {
      Console.WriteLine("WARNING: No server type specified, defaulting to RTSP");
    }

    // ports need to be in the unassigned range
    if(port < 1025 || port > 65535)
    {
      throw new Exception("Port is not a valid integer");
    }

    var cameraConfig = new CameraConfig
    {
      CameraId = pair[0],
      Port = port,
      ServerType = pair.Length == 3 ? pair[2] : "RTSP"
    };
    cameras.Add(pair[0], cameraConfig);
  }
}

var pythonScriptPath =  Environment.GetEnvironmentVariable("APP_PYTHON_SCRIPT_PATH") ?? "/app/python/"; // path to the python scripts in the container

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.UseDeveloperExceptionPage();

app.MapGet("/", () => "Hello World!");

app.MapGet("/getCameraIds", () => {
  // needs to return a list of camera IDs and their types so we can construct the camera list on the client
  return JsonSerializer.Serialize(cameras.Select(x => new { x.Key, x.Value.ServerType }));
});

// this can't be cached, as the token is one time use
app.MapGet("/getToken", (string cameraId) =>
{
  // ensure cameraId is in the list
  if (!cameras.Keys.Contains(cameraId))
  {
    throw new IndexOutOfRangeException($"Camera ID {cameraId} is not in the list of allowed camera IDs");
  }

  var startInfo = new ProcessStartInfo
  {
    FileName = "python3",
    Arguments = $"{pythonScriptPath}{Path.DirectorySeparatorChar}get_camera_token.py {cameraId}",
    RedirectStandardOutput = true,
    RedirectStandardError = true,
    UseShellExecute = false,
    CreateNoWindow = true
  };

  using var process = Process.Start(startInfo);

  if (process == null)
  {
    throw new Exception("Failed to start process");
  }

  using var reader = process.StandardOutput;
  using var errorReader = process.StandardError;
  string result = reader.ReadToEnd();
  string error = errorReader.ReadToEnd();
  process.WaitForExit();

  if (process.ExitCode != 0)
  {
    throw new Exception($"Error occurred: {error}\nResult: {result}");
  }

  Console.WriteLine($"Token result: {result}");

  // result is a json object, we need to append a socketPort field to it so the client knows which port to connect to, preserving the original field types
  var token = JsonSerializer.Deserialize<DeviceConfiguration>(result) ?? throw new Exception("Failed to deserialize token");
  
  token.SocketPort = cameras[cameraId].Port;
  token.ServerType = cameras[cameraId].ServerType;

  return JsonSerializer.Serialize(token);
});

app.MapGet("/getCameraConfig", (string cameraId) =>
{
    // ensure cameraId is in the list
    if (!cameras.Keys.Contains(cameraId))
    {
        throw new IndexOutOfRangeException($"Camera ID {cameraId} is not in the list of allowed camera IDs");
    }

    return JsonSerializer.Serialize(cameras[cameraId]);
});

// an endpoint to get a port for a socket to recieve the raw h264 stream on, given a camera ID, initilaize that input socket and hook up the rtsp server to it
app.MapGet("/getStreamInputPort", (string cameraId) =>
{
    // ensure cameraId is in the list
    if (!cameras.Keys.Contains(cameraId))
    {
        throw new IndexOutOfRangeException($"Camera ID {cameraId} is not in the list of allowed camera IDs");
    }

    return cameras[cameraId].Port;
});

app.Run();
