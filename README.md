Hello!

This is a rough RTSP server for WYZE cameras of the GWELL variety

To use this, docker compose is easiest.
copy sample.env to .env
update your details

build the thing:
```
docker compose build
docker compose up -d
```

you can view the android container over adb with something like scrcpy: `scrcpy -s localhost:5555`

The RTSP server has issues. Rendering of keyframes is highly delayed when connecting, so using something like go2rtc to pre-buffer is critical.

AGAIN: DO NOT DIRECTLY OPEN THESE RTSP CONNECTIONS

my connection string in go2rtc looks like `rtsp://192.168.1.3:5001#timeout=120#media=video`
