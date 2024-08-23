package com.tencentcs.iotvideo.restreamer

import android.content.Context
import com.tencentcs.iotvideo.IoTVideoErrors
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.PREFIX_THIRD_ID
import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.custom.ServerType
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.LoggingConnectDevStateListener
import com.tencentcs.iotvideo.iotvideoplayer.PlayerStateEnum
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
class RestreamingResultListener(private val cameraCredential: CameraCredential, private val context: Context): IResultListener<Boolean> {

    private val TAG = "RestreamingResultListener::${cameraCredential.serverType}::${cameraCredential.socketPort}"
    var streamer: IRestreamingVideoDecoder? = null
    var framesProcessed = 0

    private var _isFaulted = AtomicBoolean(false)
    var isFaulted: Boolean
        get() = _isFaulted.get()
        set(value) {
            LogUtils.i(TAG, "set isFaulted: $value")
            _isFaulted.set(value)
        }

    private var isSubscribed = false
    private var registrationWatchdog: Thread? = null

    init {
        // ensure we are registered within some reasonable time, if not, let the outer class handle it.
        registrationWatchdog = Thread {
            try {
                Thread.sleep(30_000)
                if (!isSubscribed) {
                    isFaulted = true
                }
            } catch (e: InterruptedException) {return@Thread}
        }
    }

    override fun onError(errorCode: Int, message: String?) {
        val errorType = SubscribeError.fromErrorCode(errorCode)
        val logMessage = "onError: $errorCode, $errorType, $message"
        if(!isSubscribed) {
            LogUtils.e(TAG, logMessage)
            isFaulted = true
        } else {
            StackTraceUtils.logStackTrace(TAG, "possibly a zombie error: $logMessage")
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
        }
        override fun onRenderWatchdogRequestsRestart() {
            isFaulted = true
        }
    }

    override fun onSuccess(success: Boolean?) {
        LogUtils.e(TAG, "Subscribe: onSuccess: $success")
        val iotVideoPlayer = IoTVideoPlayer()

        iotVideoPlayer.setDataResource(PREFIX_THIRD_ID + cameraCredential.deviceId, 1, PlayerUserData(2))
        iotVideoPlayer.setConnectDevStateListener(LoggingConnectDevStateListener())
        iotVideoPlayer.setAudioRender(NullAudioRenderer())

        iotVideoPlayer.setAudioDecoder(NullAudioStreamDecoder())
        iotVideoPlayer.setVideoRender(NullVideoRenderer())

        // set the stream decoder
        streamer = when(cameraCredential.serverType) {
            ServerType.RAW -> H264StreamingVideoDecoder(
                cameraCredential.socketPort,
                rendererCallback
            )

            ServerType.RTSP -> SurfaceRtspServerStreamDecoder(
                cameraCredential.socketPort,
                rendererCallback,
                context
            )

            ServerType.MJPEG -> ImageServerVideoStreamDecoder(
                cameraCredential.socketPort,
                rendererCallback,
                context
            )

            else -> ImageServerVideoStreamDecoder(
                cameraCredential.socketPort,
                rendererCallback,
                context
            )
        }

        iotVideoPlayer.setVideoDecoder(streamer)

        iotVideoPlayer.setErrorListener { errorNumber ->
            LogUtils.i(
                TAG,
                "errorListener onError for iotvideo player: $errorNumber with message: ${getErrorDescription(errorNumber)}"
            )

            if(errorNumber == IoTVideoErrors.Term_msg_calling_timeout_disconnect)
            {
                isFaulted = true
            }
        }
        iotVideoPlayer.setStatusListener { statusCode ->
            LogUtils.i(
                TAG,
                "IStatusListener onStatus for IotVideoPlayer: ${PlayerStateEnum.valueOf(statusCode)}($statusCode)"
            )
            LogUtils.i(TAG, "Player connect mode: ${iotVideoPlayer.getConnectMode()}");
            LogUtils.i(TAG, "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet")
        }

        LogUtils.i(TAG, "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet")

        iotVideoPlayer.play() // start receiving packets

        LogUtils.i(TAG, "Player state: ${iotVideoPlayer.playState}")

        isSubscribed = true
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
            isFaulted = true
            StackTraceUtils.logStackTrace(TAG, "release")
        } else {
            StackTraceUtils.logStackTrace(TAG, "skipped release")
        }
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
        sb.append("isFaulted=").append(isFaulted).append("\n\t\t\t")
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