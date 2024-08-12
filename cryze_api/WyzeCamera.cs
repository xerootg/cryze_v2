// each camera needs
// a port number to listen on for the raw h264 stream
// a socket to recieve the raw h264 stream 
// a channel for the h246 stream to be read into
// a timer to track the RTP timestamps
// a worker to read the h264 stream into the channel
// a worker to read the channel and send the frames to the rtsp server

using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Threading.Channels;
using SharpRTSPServer;

public class WyzeCamera : IDisposable
{

  private Channel<H264Packet> buffer;
  private Socket inputSocket;

  private Stopwatch stopwatch;

  private CancellationTokenSource cts;

  private Task recieveTask;
  private Task writeTask;
  private bool disposedValue;

  public int InputPort { get; init; }
  public H264Track Track { get; init; }

  public WyzeCamera(int port, int bufferSize = 1024)
  {
    InputPort = port;

    inputSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
    buffer = Channel.CreateBounded<H264Packet>(bufferSize);
    stopwatch = new Stopwatch();
    cts = new CancellationTokenSource();

    Track = new H264Track();
  }

  public void Start()
  {
    // create a socket to listen on the port
    inputSocket.Bind(new IPEndPoint(IPAddress.Any, InputPort));
    inputSocket.Listen(10);

    Recieve(); // start the recieve task
  }

  public async Task StopAsync()
  {
    
    try
    {
      cts.Cancel();
      await recieveTask;
      await writeTask;
    }catch{}

    if(inputSocket.Connected)
    {
        inputSocket.Shutdown(SocketShutdown.Both);
        inputSocket.Close();
    }

    buffer.Writer.Complete();

    stopwatch.Stop();

    Console.WriteLine($"Port {InputPort} shutdown complete");
  }

  private void Recieve(){
    #pragma warning disable CS4014 // Because this call is not awaited, execution of the current method continues before the call is completed
    recieveTask = Task.Run(async () =>
    {
        while (true && !cts.Token.IsCancellationRequested)
        {
            var client = await inputSocket.AcceptAsync();
            stopwatch.Start(); // start the stopwatch when the first client connects
            Console.WriteLine($"Accepted connection from {client.RemoteEndPoint}");

            var stream = new NetworkStream(client);
            while (stream.CanRead && !cts.Token.IsCancellationRequested)
            {
                // try to get 1mb at a time
                var data = new byte[1024 * 1024];
                var bytesRead = await stream.ReadAsync(data, 0, data.Length);

                //queue the packet
                await buffer.Writer.WriteAsync(new H264Packet
                {
                    Timestamp = (uint)stopwatch.ElapsedMilliseconds,
                    Data = data.Take(bytesRead).ToArray()
                });
            }
        }
    }, cts.Token);
    #pragma warning restore CS4014 // Because this call is not awaited, execution of the current method continues before the call is completed
  }

  private void WriteToTrack()
  {
    Task.Run(async () =>
    {
      while (await buffer.Reader.WaitToReadAsync(cts.Token) && !cts.Token.IsCancellationRequested)
      {
        if (buffer.Reader.TryRead(out var packet))
        {
          Track.FeedInRawSamples(packet.Timestamp, new(){packet.Data});
        }
      }
    }, cts.Token);
  }

  protected virtual void Dispose(bool disposing)
  {
    if (!disposedValue)
    {
      if (disposing)
      {
        StopAsync().GetAwaiter().GetResult();
        inputSocket.Dispose();
      }
      // TODO: free unmanaged resources (unmanaged objects) and override finalizer
      // TODO: set large fields to null
      disposedValue = true;
    }
  }

  public void Dispose()
  {
    // Do not change this code. Put cleanup code in 'Dispose(bool disposing)' method
    Dispose(disposing: true);
    GC.SuppressFinalize(this);
  }
}