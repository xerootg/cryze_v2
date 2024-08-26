#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>

int main() {
    struct stat st;
    const char *stdout_descriptor = "/dockerlogs";

    // Check if our custom descriptor exists on /init
    if (stat(stdout_descriptor, &st) == 0) {
        execl("/system/bin/logcat", "logcat", "-f", stdout_descriptor, "-b", "main",
          "hwcomposer:s", "TcpSocketTracker:s", "cmd:s", "Netd:s", "TcpSocketMonitor:s", "chatty:s",
          "KeyguardClockSwitch:s", "system_server:s", "dex2oat32:s", "installd:s", "BackgroundDexOptService:s",
          "UserPackageInfos:s", "PermissionControllerServiceImpl:s", "LancherPackagesLiveData:s", "PackageDexOptimizer:s",
          "LatinIME:s", "AvrcpMediaPlayerList:s", "AvrcpBrowsablePlayerConnector:s", "UserSensitiveFlagsUtils:s",
          "PhoneInterfaceManager:s", "CarrierSvcBindHelper:s", (char *)NULL);
        //execl("/system/bin/logcat", "logcat", "-f", stdout_descriptor, "-e", "com.tencentcs.iotvideo", (char *)NULL);
    } else {
        printf("no docker log descriptor found at %s\n", stdout_descriptor);
        return 0;
    }

    // If execl fails
    perror("execl");
    return EXIT_FAILURE;
}
