using Cryze.API.Services;

namespace Cryze.API.ViewModels
{
    public class SettingsViewModel
    {
        public WyzeAPICredential Settings { get; set; } = new WyzeAPICredential();
    }
}