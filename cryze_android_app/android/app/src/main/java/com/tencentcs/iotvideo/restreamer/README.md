## General structure
The SDK guided me to a couple layers.
1) a VideoRenderer, I call them `Decoder` - an object that receives raw h264 frames. The SDK suggests h265 is possible, but it appears unimplemented.
2) a MediaPlayer, `RestreamingResultListener` - Interfaces with the events of the `IoTVideoSDK` device registration and uses `IoTVideoPlayer` to connect the restreaming plugins to the SDK.
3) a "Camera", `RestreamingVideoPlayer` - sets the SDK up to allow subscription to a camera in the traditional sense. Manages tokens and event subscribing.

The SDK, as setup, is not architected to make multiple parallel camera streams easy to manage, so each layer informs the layer above it of failure or state that would result in a stream not happening. 
