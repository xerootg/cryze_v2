# The Android app
This contains a rewritten sdk, the native libraries, an rtsp remuxer, utilities to get logging out of redroid (fork_logger), and the actual cryze app that hooks the SDK up to a remuxer

## rewritten SDK
The SDK from tencent is difficult to work with and has TONS of things designed around displaying a single active camera feed within a `Surface` in an Android app, registering new cameras (that whole qr code workflow? yeah.). It's not designed for long-running operations. There's races, deadlocking, stateful singletons, its a mess. I've stripped it bare to almost exclusivly what is needed to get raw, untouched, packets directly from a camera and the messaging from the backend to know when a camera feed dies. I converted most of it (maybe all?) to kotlin because `null` is a HUGE problem in the old design. it almost does not resemble the old sdk at all because of this, but the size on disk is 90% smaller and is much easier to troubleshoot.

I also added parsers for the events I've seen the cameras send to the SDK, which have interesting information.

## native libraries
The SDK above is the jvm half of the actual SDK. Its how we interact. This stuff is how the java uses the tencent code, which is written in c/cpp. It uses JNI to give us the hooks needed to manage cameras, and viceversa, calls stuff in the java side to determine things like "network state" which would be neccesary if we were actualy using this on a phone in a dynamic network situation. It also includes decoders and encoders we have no ability to use, a sound processing library that's unused, some webrtc stuff specific to tencent's "value added" extensions (cloud recording storage) that wyze didn't even use, multiple logging utilities (primarily Xlog which is open source!!), two TLS libraries (mbed tls and openssl), and the tencent p2p library that is used by them for more than just cameras (looks like it supports p2p chat, and video/audio calling).

## RTSP remuxer
This is in two halves
1) an extensivly modified and stripped down copy of RootEncoder, with support for g711u
2) implementations of the SDK's IVideoDecoder and IAudioDecoder in the cryze app side
The cryze side uses the RootEncoder basically to do packetization, where the cryze code does the grabbing and queueing of raw packets, and the codec initialization (h264 and g711u are determined in the decoder implementations to provide to RootEncoder to do the correct SDP/RTSP stuff)

## fork_logger
This is horrifying. Android zygote cannot deal with /init having file handles it cannot serialize to java. Docker on the otherhand, attaches pipes to the /init process's 0,1, and 2'sfile descriptors to grab logging. in a real OS, init having these open is apparently a security risk, so for these reasons, /init rebinds these to /dev/null as part of it's startup.

- `main.c` - a small "bootstrapper" that forks, copying these to the child process, and then replaces the parent process with the android /init. /init then continues bringing up android, totally unaware these descriptors exist. the child process then binds those descriptors to a FIFO pipe, allowing messages from us to be forwarded to docker.

- `dockercat.rc` - an android init-compatible service that stops and starts i sync with logd, the android logger below logcat

- `log_watcher.sh` - watches for our PID, then creates a logcat instance that forwards to our fifo pipe from `main.c`. If our pid dies, it logs that and waits for a new one. It also maintains a logcat for the tag `LogcatWatchdog` which I've been putting logging into for the scripts surrounding cryze. Why? android is extremely noisy.

## the cryze app
Connects camera streams using the SDK to the rtsp remuxer, monitors for stream death (i.e. the `deadman`), etc. Uses the `cryze_api` project to get it's marching orders

## Future plans
- publish the messages from the cameras and the sdk to a web UI
- dynamically start/stop streams based off the `cryze_api`'s state, so the app doesn't need to restart to pick up new streams
- gracefully handle addition and removal of streams on the fly
- gracefully handle offline cameras. i *think* right now it will infinitely try to fetch new tokens and connect every 30-60 seconds