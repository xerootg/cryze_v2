namespace Cryze.API.Services
{
    public class WyzeSdkServiceConfiguration
    {
        public string MarsUrl { get; set; } = "https://wyze-mars-service.wyzecam.com";
        public string MarsRegisterGwUserRoute { get; set; } = "/plugin/mars/v2/regist_gw_user/";

        // BE1 is the doorbell, GC1 is the OG, GC2 is the OG telephoto. It's entirely possible there's other IoTVideoSDK devices, but these are the ones I know about.
        public string[] ValidMarsDevicePrefix { get; set; } = ["GW_BE1_", "GW_GC1_", "GW_GC2_"];
    }
}