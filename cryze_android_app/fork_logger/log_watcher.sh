#!/system/bin/sh

# Logger functions, main logger flushes these to the pipe
standard_logcat_arguments="-v brief -v time -v color"
function logwrapper() {
    local message="$@"
    /system/bin/busybox logger -t LogcatWatchdog -p 1 "$message"
}

# cryze app functions
function setCryzePid() {
    cryze_pid=$(/system/bin/pidof com.github.xerootg.cryze)
}

# cryze logger functions
cryze_logcat_pid=0 # pid of the logcat process for the cryze app
function ensureCryzeLoggerRunning() {
    if [ ! -d "/proc/$cryze_logcat_pid" ]; then
        logwrapper "cryze logcat died, restarting"
        startCryzeLogger
    fi
}

function startCryzeLogger() {
    if [ -d "/proc/$cryze_logcat_pid" ]; then
        /system/bin/kill -9 $cryze_logcat_pid
    fi
    /system/bin/logcat --pid $cryze_pid $standard_logcat_arguments >> /dockerlogs &
    cryze_logcat_pid=$!
    logwrapper "logcat started for process $cryze_pid with pid $cryze_logcat_pid"
}

function stopCryzeLogger() {
    /system/bin/kill -9 $cryze_logcat_pid
}

# logger insurance for while cryze is running
function waitForCryze() {
    while [ -d "/proc/$cryze_pid" ]; do
        ensureMainLoggerRunning
        ensureCryzeLoggerRunning
        sleep 1
    done
}

# Main logger functions
main_logcat_pid=0

function ensureMainLoggerRunning() {
    if [ ! -d "/proc/$main_logcat_pid" ]; then
        logwrapper "main logcat died, restarting"
        startMainLogcat
    fi
}

function startMainLogcat() {
    if [ -d "/proc/$main_logcat_pid" ]; then
        /system/bin/kill -9 $main_logcat_pid
    fi
    # -s for silent, verbose LogcatWatchdog tag, -T1 for don't print backwards
    /system/bin/logcat -s LogcatWatchdog:v -T1 >> /dockerlogs &
    main_logcat_pid=$!
    logwrapper "Main logger started with pid $main_logcat_pid"
}

function stopMainLogcat() {
    if [ -d "/proc/$main_logcat_pid" ]; then
        /system/bin/kill -9 $main_logcat_pid
    fi
}

# Function to run when the script is killed
function cleanup() {
    logwrapper "Script killed, performing cleanup..."
    stopCryzeLogger
    stopMainLogcat
    logwrapper "Cleanup complete"
}

# Set up trap to call the cleanup function when the script is killed
trap cleanup EXIT

# Main loop
function run() {
    # ensure the log file exists
    if [ ! -p /dockerlogs ]; then
        logwrapper echo "ERROR: Log pipe not found, there will be no logging in docker"
    fi
    
    # start a logcat for this script and others that may be running
    startMainLogcat
    
    logwrapper "Main logger started with pid $main_logcat_pid"
    
    while true; do
        # Check if the process is running
        setCryzePid

        if [ -z "$cryze_pid" ]; then
            logwrapper "cryze process not found"
            sleep 5
            continue
        fi
        
        if [ -d "/proc/$cryze_pid" ]; then
            # Start logcat for the process
            startCryzeLogger
            
            # Wait for the process to exit using busybox
            waitForCryze
            
            logwrapper "cryze process exited"
            
            stopCryzeLogger
            
            logwrapper "logcat stopped"
        else
            logwrapper "cryze process not running"
            sleep 10
        fi
    done
}

run

