using System.Diagnostics;
using System.Text.RegularExpressions;
using Cryze.API.PythonExtensions;
using Python.Runtime;

namespace Cryze.API.Services;
public partial class PythonServices
{
  private readonly ILogger<PythonServices> _logger;

  // Used to make a logger for the pythonlogger class
  private readonly ILoggerFactory _loggerFactory;
  public PythonServices(ILogger<PythonServices> logger, ILoggerFactory loggerFactory)
  {
    _logger = logger;
    _loggerFactory = loggerFactory;
  }

  // General pattern theory: **\python3[1 or 2 additional numbers].dll. python3.dll is version agnostic and not useful for pythonnet.
  // Tested on windows 11
  [GeneratedRegex(@".*\\python3[0-9]{1,2}\.dll$", RegexOptions.Compiled)]
  private static partial Regex WindowsPythonDll();

  // General pattern theory: <arbitrary path>/libpython3.*.so<end of line>
  // Tested on debian.
  [GeneratedRegex(@"/libpython3\.[0-9]{1,2}\.so$", RegexOptions.Compiled)]
  private static partial Regex LinuxPythonLib();

  /// <summary>
  /// Sets a global log in python, and binds it to the ILogger<PythonLogger> instance.
  /// Run inside of a GIL scope.
  /// </summary>
  public void RegisterLogger()
  {

    // dynamic logModule = Py.CreateScope().Execute();
    using dynamic logging_libbrary = PyModule.FromString("logging_library", loggingLibrary);

    // Create a new instance of the PythonLogger class
    var logger = new PythonLogger(_loggerFactory.CreateLogger<PythonLogger>());

    // Create a delegate to the Log method
    Action<string> logAction = logger.Log;

    // Set the logger handler in the logging_setup module
    logging_libbrary.set_ilogger_handler(logAction);
  }

  public void UnregisterLogger()
  {
    using dynamic logging_libbrary = PyModule.FromString("logging_library", loggingLibrary);
    logging_libbrary.destroy_ilogger_handler();
  }


  /// <summary>
  /// Configure the python engine. This is a one-time setup. It will find the python library and set it for the python engine.
  /// </summary>
  /// <exception cref="Exception">IDK, I didn't feel like being specific.</exception>
  /// <exception cref="NotImplementedException">I didn't write a loader for your OS, probably cause I don't have anything running that</exception>
  public void ConfigurePythonEngine()
  {
    RuntimeData.FormatterType = typeof(NoopFormatter);

    if (OperatingSystem.IsWindows())
    {
      var pathFolders = Environment.GetEnvironmentVariable("PATH")?.Split(';');
      if (pathFolders == null)
      {
        throw new Exception("PATH is not set. How did you even get here?");
      }
      var pythonDll = string.Empty;
      foreach (var folder in pathFolders)
      {
        foreach (var file in Directory.EnumerateFiles(folder))
        {
          if (WindowsPythonDll().IsMatch(file))
          {
            pythonDll = file;
            break;
          }
        }
        if (!string.IsNullOrEmpty(pythonDll))
        {
          break;
        }
      }

      if (!string.IsNullOrEmpty(pythonDll))
      {
        _logger.LogInformation($"Found python3.dll at {pythonDll}");
        Runtime.PythonDLL = pythonDll;
        // Initialize the python engine. Do it once
      }
      else
      {
        throw new Exception("Could not find python3.dll in PATH");
      }
    }
    else if (OperatingSystem.IsLinux())
    {
      var pythonLib = string.Empty;
      // to be more universal, use the path. /usr/lib is a common location, but not guaranteed
      // to be the only location. other locations could be /usr/local/lib, /usr/lib64, etc.
      // to get those locations, if ldconfig is a thing, try `ldconfig -p | grep libpython3`

      // # ldconfig -p
      // 236 libs found in cache `/etc/ld.so.cache' <-- this won't be splittable, so filter it out
      // libpython3.11.so.1.0 (libc6,x86-64) => /lib/x86_64-linux-gnu/libpython3.11.so.1.0
      // libpython3.11.so (libc6,x86-64) => /lib/x86_64-linux-gnu/libpython3.11.so
      // we've found each line, the library is the first thing in the line, then the path to it.

      try
      {
        var processInfo = new ProcessStartInfo
        {
          FileName = "ldconfig",
          Arguments = "-p",
          RedirectStandardOutput = true,
          RedirectStandardError = true,
          UseShellExecute = false,
          CreateNoWindow = true
        };
        var ldconfigOutput = new List<string>();
        using (var process = Process.Start(processInfo))
        {
          if (process == null)
          {
            throw new Exception("Failed to start ldconfig");
          }
          process.WaitForExit();
          if (process.ExitCode != 0)
          {
            throw new Exception("ldconfig failed");
          }
          var linesOfOutput = new List<string>();
          using (var reader = process.StandardOutput)
          {
            while (!reader.EndOfStream)
            {
              ldconfigOutput.Add(reader.ReadLine() ?? Environment.NewLine);
            }
          }
        }

        foreach (var line in ldconfigOutput)
        {
          if (line.Contains("=>"))
          {
            var parts = line.Split("=>");
            if (parts.Length != 2) // not a library line
            {
              continue;
            }
            var path = parts[1];
            if (LinuxPythonLib().IsMatch(path))
            {
              var match = path.Trim(); // after => theres a space. remove it. this should be the path to the library
              if (File.Exists(match)) // make sure the file exists
              {
                pythonLib = match;
              }
              break;
            }
          }
        }
      }
      catch (Exception e)
      {
        _logger.LogCritical(e, $"Failed to run ldconfig: {e.GetType()}: {e.Message}");
      }

      try
      {
        if (string.IsNullOrEmpty(pythonLib))
        {
          // be dumb and just look in /usr/lib
          var genericLibraryPath = "/usr/lib";
          pythonLib = Directory.EnumerateFiles(genericLibraryPath, "libpython3.*.so").FirstOrDefault(string.Empty);
          if (string.IsNullOrEmpty(pythonLib))
          {
            throw new IndexOutOfRangeException("Could not find libpython3.*.so in /usr/lib");
          }
        }
        _logger.LogInformation($"Found libpython3.*.so at {pythonLib}");
      }
      catch (Exception e)
      {
        _logger.LogCritical(e, $"Failed to find libpython3.*.so: {e.GetType()}: {e.Message}");
        throw;
      }
      if (!string.IsNullOrEmpty(pythonLib))
      {
        Runtime.PythonDLL = pythonLib;
      }
      else
      {
        throw new Exception("Could not find libpython3.*.so");
      }
    }
    else
    {
      throw new NotImplementedException($"{Environment.OSVersion.ToString} is not implemented yet");
    }
  }

  // Im not a good python programmer. This is a hack to get logging working in pythonnet in wyze_sdk. Maybe someone can make it better?
  private static string loggingLibrary = @"
import logging
from clr import AddReference
AddReference(""System"")
from System import Action

class ILoggerHandler(logging.Handler):
    def __init__(self, log_action):
        super().__init__()
        self.log_action = log_action

    def emit(self, record):
        log_entry = self.format(record)
        self.log_action(log_entry)

def set_ilogger_handler(log_action, level=logging.INFO, format_string=None):
    global log # wyze_sdk sets this global variable to the logger in it's logging setup, see it's __init__.py

    if not format_string:
        format_string = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'

    handler = ILoggerHandler(log_action)
    handler.setLevel(level)
    formatter = logging.Formatter(format_string)
    handler.setFormatter(formatter)

    logger = logging.getLogger()
    logger.addHandler(handler)
    logger.setLevel(level)
    log = logger

def destroy_ilogger_handler():
    global log
    for handler in log.handlers:
        if isinstance(handler, ILoggerHandler):
            log.removeHandler(handler)
            handler.close()
";
}

