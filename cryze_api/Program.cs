using System.Diagnostics;
using System.Text.Json;

var cameraIdList = Environment.GetEnvironmentVariable("CAMERA_IDS")?.Split(';').ToList() ?? throw new Exception("CAMERA_IDS environment variable is not set; it should be a semicolon-separated list of camera IDs");
var startInputPort = int.Parse(Environment.GetEnvironmentVariable("START_INPUT_PORT") ?? "5001");

// make a hashset of cameraids to ports for the rtsp server, in order to add the rtsp server to the input sockets later
var cameraIdToPort = new Dictionary<string, int>();
foreach (var cameraId in cameraIdList)
{
    cameraIdToPort[cameraId] = startInputPort + cameraIdList.IndexOf(cameraId);
}

// create a socket to recieve the raw h264 stream on for each camera ID starting at startInputPort, and a buffer to recieve the stream into
var cameraReader = new Dictionary<string, WyzeCamera>();
foreach (var camera in cameraIdToPort)
{
    Console.WriteLine($"Creating input socket for camera {camera.Key} on port {camera.Value}");
    cameraReader[camera.Key] = new WyzeCamera(camera.Value);
    cameraReader[camera.Key].Start();

    // bind the shutdown event to the dispose method
    AppDomain.CurrentDomain.ProcessExit += (sender, e) => cameraReader[camera.Key].Dispose();
}

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

// make an rtsp server from SharpRTSP
using var server = new SharpRTSPServer.RTSPServer(8554,"viewer","viewer", app.Services.GetRequiredService<ILoggerFactory>());

// add the rtsp server to the input sockets
foreach (var camera in cameraReader)
{
  server.AddVideoTrack(camera.Value.Track);
  server.SessionName = camera.Key;
}

app.UseDeveloperExceptionPage();

app.MapGet("/", () => "Hello World!");

app.MapGet("/getCameraIds", () => JsonSerializer.Serialize(cameraIdList));

app.MapGet("/getToken", (string cameraId) =>
{
    // ensure cameraId is in the list
    if (!cameraIdList.Contains(cameraId))
    {
        throw new IndexOutOfRangeException($"Camera ID {cameraId} is not in the list of allowed camera IDs");
    }

    var startInfo = new ProcessStartInfo
    {
        FileName = "python3",
        Arguments = $"/app/python/get_camera_token.py {cameraId}",
        RedirectStandardOutput = true,
        RedirectStandardError = true, // Add this line to redirect the StandardError stream
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
        throw new Exception($"Error occurred: {error}\nResult: {result}");throw new Exception($"Error occurred: {error}\nResult: {result}");
    }

    // result is a json object, we need to append a socketPort field to it so the client knows which port to connect to, preserving the original field types
    var token = JsonSerializer.Deserialize<Dictionary<string, object>>(result);
    token["socketPort"] = cameraIdToPort[cameraId];

    return JsonSerializer.Serialize(token);
});

// an endpoint to get a port for a socket to recieve the raw h264 stream on, given a camera ID, initilaize that input socket and hook up the rtsp server to it
app.MapGet("/getStreamInputPort", (string cameraId) =>
{
    // ensure cameraId is in the list
    if (!cameraIdList.Contains(cameraId))
    {
        throw new IndexOutOfRangeException($"Camera ID {cameraId} is not in the list of allowed camera IDs");
    }

    return cameraIdToPort[cameraId];
});


// register shutdown to stop the rtsp server
AppDomain.CurrentDomain.ProcessExit += (sender, e) => server.StopListen();

server.StartListen();

app.Run();
