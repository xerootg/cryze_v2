#!/system/bin/sh

function logwrapper() {
    local message="$@"
    /system/bin/busybox logger -t LogcatWatchdog -p 1 "$message"
}

logwrapper "Starting ensure_running"

if [ "`getprop breakloop`" -eq 1 ]; then
    logwrapper 'breakloop property set, ensure_running exiting pre-install';
    exit 0;
fi

if [ -f "/app/app.apk" ]; then
    logwrapper "Installing cryze with full permissions"
    pm install --abi arm64-v8a -g --full /app/app.apk >> /dockerlogs # log the install to docker logs
    logwrapper "App installed, starting main loop"
else
    logwrapper "No app.apk found, skipping install"
fi


while true; do
    if [ "`getprop breakloop`" -eq 1 ]; then
        logwrapper 'breakloop property set, ensure_running exiting loop';
        break;
    fi

    logwrapper "Starting cryze"
    am start -n com.github.xerootg.cryze/.MainActivity;

    # wait for cryze to start
    sleep 5
    cryze_pid=$(pidof com.github.xerootg.cryze)
    if [ -z "$cryze_pid" ]; then
        logwrapper "cryze not started, retrying"
        sleep 10
        continue
    fi
    while [ -d "/proc/$cryze_pid" ]; do
        sleep 1
    done

    sleep 30 # let the wyze servers chill. they don't seem to like requests too close together
done;


