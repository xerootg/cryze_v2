namespace Cryze.API
{
  public partial class Program
  {
    public static Task Main(string[] args)
    {
      return CreateHostBuilder(args).Build().RunAsync();
    }


    public static IHostBuilder CreateHostBuilder(string[] args) =>
      Host.CreateDefaultBuilder(args)
        .ConfigureLogging(logging =>
        {
          logging.ClearProviders();
          logging.AddConsole();
        })
        .ConfigureWebHostDefaults(webBuilder =>
        {
          webBuilder.UseStartup<Startup>();
        }).ConfigureAppConfiguration(appCfg =>
        {
          appCfg.AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);
          appCfg.AddEnvironmentVariables();
          // Load the .env file if it exists, and do it last so it can override other settings
          var envFile = DotNetEnv.Env.Load();
          if (envFile != null)
          {
            Console.WriteLine("Loaded .env file");
            appCfg.AddInMemoryCollection(envFile);
          }
        }).ConfigureHostOptions(hostOptions =>
        {
          hostOptions.ShutdownTimeout = TimeSpan.FromSeconds(15);
        }).UseConsoleLifetime(); // Use the console lifetime so we can run as a console app
  }
}