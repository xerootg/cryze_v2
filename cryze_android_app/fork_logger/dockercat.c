#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>

int main() {
    struct stat st;
    const char *stdout_descriptor = "/dockerlogs";

    // Check if our custom descriptor exists on /init
    if (stat(stdout_descriptor, &st) == 0) {
        execl("/system/bin/logcat", "logcat", "-f", stdout_descriptor, (char *)NULL);
    } else {
        printf("no docker log descriptor found at %s\n", stdout_descriptor);
        return 0;
    }

    // If execl fails
    perror("execl");
    return EXIT_FAILURE;
}
