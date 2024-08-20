#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>~
#include <sys/stat.h>
#include <sys/types.h>
#include <stdbool.h>


int daemonize() {
    pid_t pid;
    int stdout_fd, fifo_fd;

    // Save the parent's stdout file descriptor
    stdout_fd = dup(STDOUT_FILENO);
    if (stdout_fd < 0) {
        perror("dup");
        exit(EXIT_FAILURE);
    }

    pid = fork();

    if (pid < 0) {
        // Fork failed
        perror("fork");
        exit(EXIT_FAILURE);
    }

    if (pid > 0) {
        // The parent process needs to not have any additional descriptors or init panics

        // Parent process exits
        return stdout_fd;
    }

    // Close all open file descriptors
    for (int x = sysconf(_SC_OPEN_MAX); x >= 0; x--) {
        if (x != stdout_fd) { // Keep the duplicated stdout_fd open
            close(x);
        }
    }

    // Redirect standard file descriptors to /dev/null
    open("/dev/null", O_RDWR); // stdin
    dup(0); // stdout
    dup(0); // stderr

    // Restore the parent's stdout
    dup2(stdout_fd, STDOUT_FILENO);
    close(stdout_fd);

    dprintf(STDOUT_FILENO, "Binding the logging fifo to the docker stdout pipe\n");
    fflush(stdout);

    // Open the FIFO for reading
    fifo_fd = open("/dockerlogs", O_RDONLY);
    if (fifo_fd < 0)
    {
        perror("open");
        exit(EXIT_FAILURE);
    }

    dprintf(STDOUT_FILENO, "Logging daemon is running...\n");
    fflush(stdout);


    // a buffer to hold the contents we will be flushing to docker
    char buffer[1024];
    ssize_t bytes_read;

    while (1) {
        while ((bytes_read = read(fifo_fd, buffer, sizeof(buffer))) > 0) {
            write(STDOUT_FILENO, buffer, bytes_read);
        }
    }
}


int main(int argc, char *argv[]) {

    if (access("/dockerlogs", F_OK) == -1) {
        // Create the FIFO
        if (mkfifo("/dockerlogs", 0666 | S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH) == -1) {
            perror("mkfifo");
            return 1;
        }
    }

    int extra_fd = daemonize();

    dprintf(STDOUT_FILENO, "created fifo at /dockerlogs\n");
    fflush(stdout);

    // Debug print before execv
    dprintf(STDOUT_FILENO, "Launching android /init\n");
    fflush(stdout);

    // Execute the /init process
    close(extra_fd);
    execv("/init", argv);

    // If execv returns, there was an error
    perror("execv");
    return 1;
}
