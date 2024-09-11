# NDK IOTVideoSDK binaries

The only way to divorce this library from a phone or otherwise native outside of an emulated phone, the following dependencies need to be handled somehow. Right now, my thought is effectivly making a chroot environment (kinda what redroid does rn, but includes many services we don't need) and using app_process to directly launch the java/kotlin code without a shell.

Seems like the current libraries were built by a NDK with GCC, so they are very old.

All of the actual communication with cameras and the p2p servers, streaming, etc occurs in some native binaries:
- libmbedtls.so - baked in TLS, seems to be used for all of the communication security in both libraries below
- libiotp2pav.so - seems to exclusivly do connection setup and maintenance
- libiotvideo.so - handles the JNI side, does most of the work for getting streams
- libc++_shared.so - contains cpp stuff not in bionic's standard library
- libgwcore.so - unclear
- libiotsoundtouch.so - unused by us in any way, but is declared in libiotvideo's ELF header as required.
- libmarsxlog.so - provides logging of the native code to both logcat and it's own file.

The only one not used in this project is iotsoundtouch, but is required by iotvideo, so it remains bundled.

They have the following binary dependencies on android, all of which come from the NDK:
These ones are provided by bionic:
- libc.so
- libdl.so
- libm.so
- libstdc++.so

these are part of the ndk and are part of android's "core" platform libraries
- libz.so - platform/external/zlib
- liblog.so - platform/system/logging

these are all part of the android framework - frameworks/libs/native_bridge_support/android_api
- libandroid.so - declared but does not seem to actually have any symbols used. this might be an opportunity to stub
- libGLESv2.so - used by the decoders and encoders embedded in libiotvideo.so, but not used by us. There's a copy of ffmpeg and faac embedded but no obvious way to use them.
- libjnigraphics.so - seems like it's used to provide viewport dimensions as an attribute in AVHeader, but i don't really understand why.

## What i've tried so far
I booted redroid without the redroid hacked up /init, using busybox's init.
This got netd up and running, dns resolution, all that, but due to the lack of Property Service, most of the ART dependencies fail to start, like app_process and therefore zygote. I wrote a standalone property service to try to get /dev/__properties__ all populated, but while I could set properties, nothing could read them. Then, i created an additional init boot flag to start just PropertyService, which would set all of the basic properties, throw and exit, meaning i could read properties but not write them. This is as bare bones as you can get and still use ART, and it's going to be too much work to get working. There's dependencies on things like com.android.Looper in the SDK, so ART is really the only true viable path.

That means without significant effort, init needs to probably remain redroid's responsibility.

## Next options
I think the most viable path is stripping init.rc down to the bare minimum tasks. No surfaceflinger, no launcher, no UI. I still need to figure out how far we can go.

## why??
Redroid running this projet with three active streams uses next to no actual cpu since it's just remuxing the streams into rtsp, allowing mediamtx to handle the messy details on native hardware. The thing is, there's tons and tons of android services, idling, using memory. My machine, I'm seeing 1.5gb of ram just wasted. I think we can get the memory footprint down to 5-600mb, maybe lower, if it's just core services, zygote, and cryze, meaning this can possibly even run on an extra raspberry pi, maybe even a pi 3, which I'd personally like because I have one that is doing nothing and is cheaper to run than a full x86 machine.
