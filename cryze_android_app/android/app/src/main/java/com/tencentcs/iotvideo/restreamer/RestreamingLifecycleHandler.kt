package com.tencentcs.iotvideo.restreamer

import android.content.Context
import com.tencentcs.iotvideo.IoTVideoErrors
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.PREFIX_THIRD_ID
import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.custom.ServerType
import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState
import com.tencentcs.iotvideo.messagemgr.SubscribeError
import com.tencentcs.iotvideo.restreamer.h264.H264StreamingVideoDecoder
import com.tencentcs.iotvideo.restreamer.interfaces.IRendererCallback
import com.tencentcs.iotvideo.restreamer.interfaces.IRestreamingVideoDecoder
import com.tencentcs.iotvideo.restreamer.mjpeg.ImageServerVideoStreamDecoder
import com.tencentcs.iotvideo.restreamer.rtsp.SurfaceRtspServerStreamDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.Utils.getErrorDescription
import com.tencentcs.iotvideo.utils.rxjava.IResultListener
import java.util.concurrent.atomic.AtomicBoolean

// Context is only used by RTSP, seemingly for GL stuff, it does some very fancy stuff with sourcing video.
class RestreamingLifecycleHandler(private val cameraCredential: CameraCredential, private val context: Context): IResultListener<Boolean> {

    private val TAG = "RestreamingResultListener::${cameraCredential.serverType}::${cameraCredential.socketPort}"
    private var streamer: IRestreamingVideoDecoder? = null
    private var socketHandler: ByteStreamServer? = null
    private var socketServerThread: Thread? = null
    private var iotVideoPlayer: IoTVideoPlayer? = null
    var playbackState: PlayerState = PlayerState.STATE_UNKNOWN
        get() = iotVideoPlayer?.playState ?: PlayerState.STATE_UNKNOWN


    var framesProcessed = 0

    private var _isDecoderStalled = AtomicBoolean(false)
    var isDecoderStalled: Boolean
        get() = _isDecoderStalled.get()
        set(value) {
            if(value == _isDecoderStalled.get()) return // only log if we faulted
            LogUtils.i(TAG, "decoderStalled: $value")
            _isDecoderStalled.set(value)
        }

    private var isSubscribed = false
    private var registrationWatchdog: Thread? = null

    init {
        // ensure we are registered within some reasonable time, if not, let the outer class handle it.
        registrationWatchdog = Thread {
            try {
                Thread.sleep(30_000)
                if (!isSubscribed) {
                    isDecoderStalled = true
                }
            } catch (e: InterruptedException) {return@Thread}
        }
    }

    // a hook for the outer to bump the token with
    fun updateToken(j10: Long, str: String)
    {
        iotVideoPlayer?.updateAccessIdAndToken(j10, str);
    }

    private var onErrorCalled = false
    override fun onError(errorCode: Int, message: String?) {
        val errorType = SubscribeError.fromErrorCode(errorCode)
        val logMessage = "onError: $errorCode, $errorType, $message"
        if(!isSubscribed) {
            LogUtils.e(TAG, logMessage)
            onErrorCalled = true
        } else if(isDecoderStalled) {
            StackTraceUtils.logStackTrace(TAG, "Actually a problem: $logMessage")
        }
    }

    // I'm not entirely sure when this is called, if ever.
    override fun onStart() {
        LogUtils.i(TAG, "onStart")
    }

    private val rendererCallback = object:
        IRendererCallback {
        override fun onFrame() {
            framesProcessed++
            isDecoderStalled = false // we're processing frames, so we're not faulted.
        }
        override fun onFault() {
            isDecoderStalled = true
        }
    }

    override fun onSuccess(success: Boolean?) {
        LogUtils.e(TAG, "Subscribe: onSuccess: $success")

        streamer = when(cameraCredential.serverType ?: ServerType.RAW) {
                ServerType.RAW -> {

                    ensureSocketServerInitialized()
                    H264StreamingVideoDecoder(
                    socketHandler!!,
                    rendererCallback
                )
            }

            ServerType.RTSP -> SurfaceRtspServerStreamDecoder(
                cameraCredential.socketPort,
                rendererCallback,
                context
            )

            ServerType.MJPEG -> {
                ensureSocketServerInitialized()
                ImageServerVideoStreamDecoder(
                    socketHandler!!,
                    rendererCallback,
                    context
                )
            }
        }

        val errorListener = object: IErrorListener {
            override fun onError(errorType: SubscribeError) {
                // This happens sometimes after the stream has started. I don't really have a good way
                // to signal up to the watchdog that something interesting is going on, so flag as stalled
                // and let the onFrame callback unset the flag.
                if(errorType == SubscribeError.AV_ER_CLIENT_NO_AVLOGIN || errorType == SubscribeError.AV_ER_SERV_NO_RESPONSE) {
                    LogUtils.e(TAG, "errorListener onError for iotvideo player: $errorType")
                    isDecoderStalled = true
                } else {
                    LogUtils.w(
                        TAG,
                        "errorListener onError for iotvideo player: $errorType with message: ${getErrorDescription(errorType.ordinal)}"
                    )
                }
            }
        }

        val stateListener = object : IStatusListener {
            override fun onStatus(state: PlayerState) {
                // this is highly redundant in setting up or tearing down the player
                // and flat out wrong when playing has stopped or not started. The preparing
                // state also has been flakey, as sometimes by the time play happens, its flipped
                // so the only reliable state to log this in is when it's actually playing.
                if(state == PlayerState.STATE_PLAY) {
                    LogUtils.i(TAG, "Player connect mode: ${iotVideoPlayer?.connectMode}")
                }
            }
        }

        iotVideoPlayer = IoTVideoPlayer(
            cameraCredential.deviceId,
            streamer!!,
            stateListener,
            errorListener)

        LogUtils.i(TAG, "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet")

        iotVideoPlayer?.play() // start receiving packets

        LogUtils.i(TAG, "Player state: ${iotVideoPlayer?.playState}")

        isSubscribed = true
    }

    private fun ensureSocketServerInitialized(){
        if(socketHandler == null) { // construct a socket handler if we don't have one
            socketHandler = ByteStreamServer(cameraCredential.socketPort)
            socketServerThread = Thread(socketHandler)
            socketServerThread?.start()
        }
    }

    // I've yet to see this be true for any devices, at least inside of the docker container
    private fun isLanDevConnectable() : Boolean
    {
        return IoTVideoSdk.lanDevConnectable(PREFIX_THIRD_ID + cameraCredential.deviceId) == 1
    }

    fun release()
    {
        if(streamer?.initialized == true)
        {
            isDecoderStalled = true
            StackTraceUtils.logStackTrace(TAG, "release")
        } else {
            StackTraceUtils.logStackTrace(TAG, "skipped release")
        }

        socketHandler?.shutdown()
        socketServerThread?.join()
        socketServerThread = null
        socketHandler = null
        streamer = null
        iotVideoPlayer?.stop()
        var maxShutdownMs = 5000L
        while(iotVideoPlayer?.playState == PlayerState.STATE_PLAY && maxShutdownMs > 0)  {maxShutdownMs -= 100; Thread.sleep(100)}
        iotVideoPlayer?.release()
        iotVideoPlayer = null
    }

    protected fun finalize()
    {
        StackTraceUtils.logStackTrace(TAG, "finalize")
        if(registrationWatchdog != null)
        {
            registrationWatchdog?.interrupt()
            registrationWatchdog?.join()
            registrationWatchdog = null
        }
        streamer?.release()
        streamer = null
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("${cameraCredential.serverType}Camera{").append("\n\t\t\t")
        sb.append("isFaulted=").append(isDecoderStalled).append("\n\t\t\t")
        sb.append("isSubscribed=").append(isSubscribed).append("\n\t\t\t")
        sb.append("framesProcessed=").append(framesProcessed)
        if(iotVideoPlayer!= null) {
            sb.append("\n\t\t\t")
            .append("fromCamera: ${(iotVideoPlayer?.avBytesPerSec ?: 0) / 1024}kb/s")
        }
        if(streamer != null){
            sb.append("\n\t\t\t")
            .append("streamer=")
            .append(streamer)
        }
        sb.append('}')
        return sb.toString()
    }

}