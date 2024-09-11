using System.Collections.Concurrent;
using System.Reflection;
using System.Runtime.InteropServices;
using Cryze.API.Services;
using Python.Runtime;

namespace Cryze.API.Python;

public class PythonRuntimeTask : IDisposable
{
  // All of this fanfare is to run all the python code in a single thread and handle it's oddities. like stealing the signal handler from the clr
  private Thread? _pythonThread;
  private readonly BlockingCollection<Action> _taskQueue = new BlockingCollection<Action>();
  private readonly CancellationTokenSource _cts = new CancellationTokenSource();
  private readonly ILogger<PythonRuntimeTask> _logger;
  private readonly PythonServices _pythonServices;
  public PythonRuntimeTask(PythonServices pythonServices, IHostApplicationLifetime hostApplicationLifetime, ILogger<PythonRuntimeTask> logger)
  {
    _pythonServices = pythonServices;
    _logger = logger;
    hostApplicationLifetime.ApplicationStopping.Register(ShutdownHandler);
    Start();
  }

  private void Start()
  {
    _pythonThread = new Thread(() => RunPythonCode(_cts.Token));
    _pythonThread?.Start();
  }

  /// <summary>
  /// Run the Python code all here so managing the GIL is easier and we can handle the signal handler rebind
  /// </summary>
  /// <param name="token"></param>
  private void RunPythonCode(CancellationToken token)
  {
    // Configure the python engine
    _pythonServices.ConfigurePythonEngine();

    PythonEngine.Initialize();
    var ptr = PythonEngine.BeginAllowThreads();

    // post initialization setup
    using (Py.GIL())
    {
      dynamic v = Py.Import("sys");
      _logger.LogInformation($"system version: {v.version}");
      _logger.LogInformation($"system python_path: {v.path}");
    }

    try
    {
      while (!token.IsCancellationRequested)
      {
        if (_taskQueue.TryTake(out var task, Timeout.Infinite, token))
        {
          try
          {
            using (Py.GIL())
            {
              // Execute the Python task
              task();
            }
          }
          finally
          {
            _logger.LogInformation("Python task completed, rebinding PosixSignalHandler");
            if (OperatingSystem.IsWindows())
            {
              _logger.LogInformation("Rebinding PosixSignalHandler for Windows");
              WindowsRebindHandlerRoutine();
            }
            else if (OperatingSystem.IsLinux())
            {
              _logger.LogInformation("Rebinding PosixSignalHandler for Linux");
              LinuxRebindHandlerRoutine();
            }
            else
            {
              _logger.LogWarning("No PosixSignalHandler to rebind");
            }
            _logger.LogInformation("PosixSignalHandler rebound");
          }
        }
      }
    }
    catch (OperationCanceledException)
    {
      _logger.LogCritical("Python thread cancelled");
      PythonEngine.Shutdown();
      _logger.LogCritical("Python engine shutdown");
    }
  }

  private IntPtr HandlerRoutine = IntPtr.Zero;
  private MethodInfo? ConsoleCtrlHandlerMethodInfo;
  private void WindowsRebindHandlerRoutine()
  {
    // If we already found the HandlerRoutine, we don't need to do it again. Reflection is slow.
    if (HandlerRoutine == IntPtr.Zero)
    {
      _logger.LogInformation("Locating PosixSignalRegistration HandlerRoutine");
      // HandlerRoutine's signature is: private static Interop.BOOL HandlerRoutine(int dwCtrlType)
      // It's in  System.Runtime.InteropServices.PosixSignalRegistration
      // we need to bind to it using reflection so we can SetConsoleCtrlHandler(&HandlerRoutine, Add: false)
      var type = typeof(System.Runtime.InteropServices.PosixSignalRegistration);
      var method = type.GetMethod("HandlerRoutine", BindingFlags.NonPublic | BindingFlags.Static);
      // make sure the method is callable
      if (method == null)
      {
        throw new InvalidOperationException("HandlerRoutine method not found");
      }
      // Finally, get a pointer to the method
      HandlerRoutine = method.MethodHandle.GetFunctionPointer();
      _logger.LogInformation("Found HandlerRoutine");
    }
    else
    {
      _logger.LogInformation("HandlerRoutine already found");
    }

    // If we already found the ConsoleCtrlHandlerMethodInfo, we don't need to do it again. Reflection is (still) slow.
    if (ConsoleCtrlHandlerMethodInfo is null)
    {
      _logger.LogInformation("Locating SetConsoleCtrlHandler method");
      // To bind the handler, we need to call:
      //System.Runtime.InteropServices.Interop.Kernel32.SetConsoleCtrlHandler(&method, bool);
      var interopType = typeof(Program).GetType().Assembly.GetType("Interop");

      // Get the Kernel32 nested type
      var kernel32Type = interopType?.GetNestedType("Kernel32", BindingFlags.NonPublic);

      // Get the SetConsoleCtrlHandler method
      ConsoleCtrlHandlerMethodInfo = kernel32Type?.GetMethod("SetConsoleCtrlHandler", BindingFlags.Static | BindingFlags.NonPublic);

      if (ConsoleCtrlHandlerMethodInfo == null)
      {
        throw new InvalidOperationException("SetConsoleCtrlHandler method not found");
      }
      _logger.LogInformation("Found SetConsoleCtrlHandler method");
    }
    else
    {
      _logger.LogInformation("SetConsoleCtrlHandler method already found");
    }
    _logger.LogInformation("Rebinding ConsoleCtrlHandler");
    // call the SetConsoleCtrlHandler method with the HandlerRoutine pointer. true means we're adding the handler, false would remove it. python removes it. :(
    ConsoleCtrlHandlerMethodInfo!.Invoke(null, [HandlerRoutine, true]);
    _logger.LogInformation("Rebound ConsoleCtrlHandler");
  }

  private IntPtr NixPosixSignalHandler = IntPtr.Zero;
  private MethodInfo? SetPosixSignalHandlerMethodInfo;
  private bool disposedValue;

  private void LinuxRebindHandlerRoutine()
  {
    // https://github.com/dotnet/runtime/blob/2fb362909e34d5d28a6e2f6f3de0c62b5b0dd4d8/src/libraries/System.Private.CoreLib/src/System/Runtime/InteropServices/PosixSignalRegistration.Unix.cs
    if (NixPosixSignalHandler == IntPtr.Zero)
    {
      _logger.LogInformation("Locating OnPosixSignal method");
      // Namespace: System.Runtime.InteropServices
      // Class/Method: PosixSignalRegistration.OnPosixSignal(int signo, PosixSignal signal)
      var type = typeof(System.Runtime.InteropServices.PosixSignalRegistration);
      var method = type.GetMethod("OnPosixSignal", BindingFlags.NonPublic | BindingFlags.Static);
      if (method == null)
      {
        throw new InvalidOperationException("OnPosixSignal method not found");
      }
      // Finally, get a pointer to the method
      NixPosixSignalHandler = method.MethodHandle.GetFunctionPointer();
      _logger.LogInformation("Found OnPosixSignal method");
    }
    else
    {
      _logger.LogInformation("OnPosixSignal already found");
    }
    // namespace: System.Runtime.InteropServices
    // Class/method: Interop.Sys.SetPosixSignalHandler(&OnPosixSignal);
    // https://github.com/dotnet/runtime/blob/18eedbe65fe5b1946f8c6df0a70357b7ed01a884/src/libraries/Common/src/Interop/Unix/System.Native/Interop.PosixSignal.cs#L10
    if (SetPosixSignalHandlerMethodInfo is null)
    {
      _logger.LogInformation("Locating SetPosixSignalHandler method");
      // I know this is ugly, but it'll help diagnose what part of the reflection is failing if it does. Signatures change, ya know?
      // var interopType = typeof(Program).GetType().Assembly.GetType("Interop") ?? throw new InvalidOperationException("Interop not found");
      //System.Runtime.InteropServices.Interop
      var interopType = typeof(PosixSignalRegistration).Assembly.GetType("Interop") ?? throw new InvalidOperationException("Interop not found");

      // Get the Sys nested type, its private so we need to use BindingFlags.NonPublic
      var sysType = interopType.GetNestedType("Sys", BindingFlags.NonPublic) ?? throw new InvalidOperationException("Sys not found");
      SetPosixSignalHandlerMethodInfo = sysType.GetMethod("SetPosixSignalHandler", BindingFlags.NonPublic | BindingFlags.Static);
      if (SetPosixSignalHandlerMethodInfo == null)
      {
        throw new InvalidOperationException("SetPosixSignalHandler method not found");
      }
      _logger.LogInformation("Found SetPosixSignalHandler method");
    }
    else
    {
      _logger.LogInformation("SetPosixSignalHandler already found");
    }

    _logger.LogInformation("Rebinding PosixSignalHandler");
    SetPosixSignalHandlerMethodInfo.Invoke(null, [HandlerRoutine]);
    _logger.LogInformation("Rebound PosixSignalHandler");
  }

  public void EnqueuePythonTask(Action task)
  {
    if (_cts.IsCancellationRequested)
    {
      throw new InvalidOperationException("Python thread stopped and cannot be restarted");
    }
    _taskQueue.Add(task);
  }

  public void ShutdownHandler()
  {
    _logger.LogCritical("Shutting down python worker thread");
    _cts.Cancel();
    _taskQueue.CompleteAdding();
    _logger.LogCritical("Waiting for python worker thread to shutdown");
    _pythonThread?.Join();
    _logger.LogCritical("Python worker thread shutdown complete");
  }

  protected virtual void Dispose(bool disposing)
  {
    if (!disposedValue)
    {
      if (disposing)
      {
        try
        {
          ShutdownHandler();
        }
        catch { } // We're shutting down, so we don't care if this fails
        _pythonThread = null;
        _taskQueue.Dispose();
        _cts.Dispose();
      }
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