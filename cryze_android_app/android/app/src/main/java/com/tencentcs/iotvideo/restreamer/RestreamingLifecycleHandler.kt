package com.tencentcs.iotvideo.restreamer

import android.content.Context
import com.tencentcs.iotvideo.IoTVideoErrors
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.PREFIX_THIRD_ID
import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.custom.ServerType
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState
import com.tencentcs.iotvideo.iotvideoplayer.codec.NullAudioStreamDecoder
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.iotvideoplayer.render.NullAudioRenderer
import com.tencentcs.iotvideo.iotvideoplayer.render.NullVideoRenderer
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
    private var sockerServerThread: Thread? = null
    private var iotVideoPlayer: IoTVideoPlayer? = null

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

    private var onErrorCalled = false;
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

    val rendererCallback = object:
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
        iotVideoPlayer = IoTVideoPlayer()

        // callType 1 = monitor
        iotVideoPlayer?.setDataResource(PREFIX_THIRD_ID + cameraCredential.deviceId, 1, PlayerUserData(2))
        iotVideoPlayer?.setConnectDevStateListener(LoggingConnectDevStateListener())
        iotVideoPlayer?.setAudioRender(NullAudioRenderer())

        iotVideoPlayer?.setAudioDecoder(NullAudioStreamDecoder())
        iotVideoPlayer?.setVideoRender(NullVideoRenderer())

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

        iotVideoPlayer?.setVideoDecoder(streamer)

        iotVideoPlayer?.setErrorListener { errorNumber ->
            LogUtils.i(
                TAG,
                "errorListener onError for iotvideo player: $errorNumber with message: ${getErrorDescription(errorNumber)}"
            )

            if(errorNumber == IoTVideoErrors.Term_msg_calling_timeout_disconnect)
            {
                isDecoderStalled = true
            }
        }
        iotVideoPlayer?.setStatusListener(object : IStatusListener {
            override fun onStatus(code: Int) {
                LogUtils.i(
                    TAG,
                    "IStatusListener onStatus for IotVideoPlayer: ${PlayerState.fromInt(code)}($code)"
                )
                LogUtils.i(TAG, "Player connect mode: ${iotVideoPlayer?.connectMode}");
                LogUtils.i(
                    TAG,
                    "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet"
                )
            }
        })

        LogUtils.i(TAG, "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet")

        iotVideoPlayer?.play() // start receiving packets

        LogUtils.i(TAG, "HLSPort=${IoTVideoSdk.getHLSHttpPort()}}")

        LogUtils.i(TAG, "Player state: ${iotVideoPlayer?.playState}")

        isSubscribed = true
    }

    private fun ensureSocketServerInitialized(){
        if(socketHandler == null) { // construct a socket handler if we don't have one
            socketHandler = ByteStreamServer(cameraCredential.socketPort)
            sockerServerThread = Thread(socketHandler)
            sockerServerThread?.start()
        }
    }

    // I've yet to see this be true for any devices, atleast inside of the docker container
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
        sockerServerThread?.join()
        sockerServerThread = null
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
        if(streamer != null){
            sb.append("\n\t\t\t")
            .append("streamer=")
            .append(streamer)
        }
        sb.append('}')
        return sb.toString()
    }

}