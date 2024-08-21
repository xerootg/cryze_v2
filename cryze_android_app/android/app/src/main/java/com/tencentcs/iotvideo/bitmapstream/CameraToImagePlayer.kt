package com.tencentcs.iotvideo.bitmapstream

import com.tencentcs.iotvideo.ICameraStream
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.IoTVideoSdk.APP_LINK_KICK_OFF
import com.tencentcs.iotvideo.IoTVideoSdk.APP_LINK_ONLINE
import com.tencentcs.iotvideo.IoTVideoSdk.PREFIX_THIRD_ID
import com.tencentcs.iotvideo.IoTVideoSdkConstant.IoTSdkState.APP_LINK_DEV_REACTIVED
import com.tencentcs.iotvideo.IoTVideoSdkConstant.IoTSdkState.APP_LINK_TOKEN_EXPIRED
import com.tencentcs.iotvideo.MainActivity
import com.tencentcs.iotvideo.ThisCameraOnFrameCallback
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.LoggingConnectDevStateListener
import com.tencentcs.iotvideo.iotvideoplayer.PlayerStateEnum
import com.tencentcs.iotvideo.iotvideoplayer.codec.NullAudioStreamDecoder
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.iotvideoplayer.render.NullAudioRenderer
import com.tencentcs.iotvideo.iotvideoplayer.render.NullVideoRenderer
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.Utils.getErrorDescription
import com.tencentcs.iotvideo.utils.rxjava.IResultListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.concurrent.fixedRateTimer

class CameraToImagePlayer(override val cameraId: String, private val rtspPlaterEventHandler: IBitmapPlayerEventHandler, private val baseContext: MainActivity) : ICameraStream
{

    var cameraToRawSocketImageDecoder: ImageServerVideoStreamDecoder? = null

    var iotVideoPlayer: IoTVideoPlayer = IoTVideoPlayer()

    var cameraCredential : CameraCredential? = null

    val innerOnFrameCallback = ThisCameraOnFrameCallback()

    var lastWatchdogFrameCount = 0L

    private var watchdogEnabled = false
    private var watchdogShouldExit = false


    // basically, as soon as start is called, there should be frames within 30 seconds. if not, start the whole stream process again
    private var watchdog = fixedRateTimer("watchdog", initialDelay = 30_000, period = 10_000) {
        if (watchdogShouldExit) {
            cancel()
            return@fixedRateTimer
        }
        if (watchdogEnabled && innerOnFrameCallback.frameCount == lastWatchdogFrameCount) {
            watchdogShouldExit = true // prevent the watchdog from firing again
            // kill the activity
            LogUtils.i(CameraToImagePlayer::class.simpleName, "watchdog timeout for ${cameraId}, killing activity")

            // let the outer loop handle this
            rtspPlaterEventHandler.onWatchdogTimeout()
        } else {
            lastWatchdogFrameCount = innerOnFrameCallback.frameCount
        }
    }

    private fun refreshCameraCredentials(): Unit
    {
        cameraCredential = null;
        var requestCompleted = false;
        LogUtils.i(CameraToImagePlayer::class.simpleName, "Getting camera credentials for camera id: $cameraId")

        // get the camera credentials from the server
        val requestUrl = "${baseContext.cryzeApi}/getToken?cameraId=$cameraId"
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        val requestHandler = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                throw IOException("Request failed: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty())
                {
                    LogUtils.e(CameraToImagePlayer::class.simpleName, "Did not get a response body from the server")
                    return
                }

                // log the response body
                LogUtils.i(CameraToImagePlayer::class.simpleName, "Response body: $responseBody")

                if (!response.isSuccessful) {
                    LogUtils.e(CameraToImagePlayer::class.simpleName, "Request failed with status code: ${response.code}")
                    throw IOException("Request failed with status code: ${response.code} and message: ${response.message}")
                }

                // the response body is json serialized LoginInfoMessage, deserialize it
                cameraCredential = CameraCredential.parseFrom(responseBody)
                requestCompleted = true
            }
        }

        baseContext.client.newCall(request).enqueue(requestHandler)

        // wait for the request to complete
        while (!requestCompleted) {
            Thread.sleep(100)
        }
    }

    @Synchronized
    override fun start() {
        //TODO: determine if the watchdog is needed for Image based streaming
        watchdogEnabled = false
        if (cameraCredential == null) {
            refreshCameraCredentials()
        }
        val thisCameraCredential = cameraCredential!!
        IoTVideoSdk.register(thisCameraCredential.accessId, thisCameraCredential.accessToken, 3)
        IoTVideoSdk.getMessageMgr().removeModelListeners()
        IoTVideoSdk.getMessageMgr().addModelListener {
            LogUtils.d(
                "CLModelListener",
                "new model message! ${it.device}, path: ${it.messageType}, data: ${it.payload}"
            )
        }

        if (MessageMgr.getSdkStatus() != 1) {
            val messageMgr = IoTVideoSdk.getMessageMgr()
            messageMgr.removeAppLinkListeners()
            messageMgr.addAppLinkListener {
                LogUtils.i(CameraToImagePlayer::class.simpleName, "appLinkListener state = $it")
                var exitSetupLoop = false
                if (it == APP_LINK_ONLINE) {
                    LogUtils.i(
                        CameraToImagePlayer::class.simpleName,
                        "Reg success, app online, start live"
                    )
                    addSubscribeDevice(thisCameraCredential)
                    exitSetupLoop = true
                }
                if (!exitSetupLoop) {
                    var shouldRestart = true
                    // if its not starting, and the link token is not expired and
                    if (it != APP_LINK_KICK_OFF && it != APP_LINK_TOKEN_EXPIRED && (APP_LINK_DEV_REACTIVED > it || it >= 18)) {
                        shouldRestart = false
                    }
                    if (shouldRestart) {
                        unregisterSdk()
                        start()
                    }
                }

            }
        } else {
            LogUtils.i(
                CameraToImagePlayer::class.simpleName,
                "appLinkListener() iot is register"
            )
            addSubscribeDevice(thisCameraCredential)
        }
    }

    private fun unregisterSdk()
    {
        IoTVideoSdk.unRegister()
        val messageMgr = IoTVideoSdk.getMessageMgr()
        messageMgr.removeAppLinkListeners()
        messageMgr.removeModelListeners()
    }

    private fun addSubscribeDevice(loginInfo: CameraCredential) {
        IoTVideoSdk.getNetConfig().subscribeDevice(loginInfo.accessToken, loginInfo.deviceId, object :
            IResultListener<Boolean> {

            override fun onError(errorCode: Int, message: String?) {
                LogUtils.e("DeviceResultIOT", "on Error: $errorCode with messsage: $message")
            }

            override fun onStart() {
                LogUtils.e("DeviceResultIOT", "onStart")
            }

            override fun onSuccess(success: Boolean?) {
                LogUtils.e("DeviceResultIOT", "onSuccess: $success")
                iotVideoPlayer = IoTVideoPlayer()

                iotVideoPlayer.setDataResource(PREFIX_THIRD_ID + loginInfo.deviceId, 1, PlayerUserData(2))
                iotVideoPlayer.setConnectDevStateListener(LoggingConnectDevStateListener())
                iotVideoPlayer.setAudioRender(NullAudioRenderer())
                iotVideoPlayer.setVideoRender(NullVideoRenderer())

                iotVideoPlayer.setAudioDecoder(NullAudioStreamDecoder())

                // set the stream decoder
                cameraToRawSocketImageDecoder = ImageServerVideoStreamDecoder(loginInfo.socketPort, innerOnFrameCallback, baseContext)
                iotVideoPlayer.setVideoDecoder(cameraToRawSocketImageDecoder)

                iotVideoPlayer.setErrorListener { errorNumber ->
                    LogUtils.i(
                        CameraToImagePlayer::class.simpleName,
                        "errorListener onError for iotvideo player: $errorNumber with message: ${getErrorDescription(errorNumber)}"
                    )
                }
                iotVideoPlayer.setStatusListener { statusCode ->
                    LogUtils.i(
                        CameraToImagePlayer::class.simpleName,
                        "IStatusListener onStatus for IotVideoPlayer: ${PlayerStateEnum.valueOf(statusCode)}($statusCode)"
                    )
                    LogUtils.i(CameraToImagePlayer::class.simpleName, "Player connect mode: ${iotVideoPlayer.getConnectMode()}");
                    LogUtils.i(CameraToImagePlayer::class.simpleName, "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet")
                }

                LogUtils.i(CameraToImagePlayer::class.simpleName, "Lan dev is ${if (isLanDevConnectable()) "" else "not "}connectable, streaming from internet")

                iotVideoPlayer.play() // start receiving packets

                LogUtils.i(CameraToImagePlayer::class.simpleName, "Player state: ${iotVideoPlayer.playState}");
            }
        })
    }

    private fun isLanDevConnectable() : Boolean
    {
        return IoTVideoSdk.lanDevConnectable(PREFIX_THIRD_ID + cameraId) == 1
    }

    override fun stop() {
        try {
            iotVideoPlayer.stop()

            // Give the player a chance to stop
            var maxWaitTime = 10_000
            while (iotVideoPlayer.playState != PlayerStateEnum.STATE_STOP || iotVideoPlayer.playState == -1) {
                maxWaitTime -= 100
                if (maxWaitTime <= 0) {
                    break
                }
                LogUtils.i(CameraToImagePlayer::class.simpleName, "Waiting for player to stop: ${iotVideoPlayer.playState} ($maxWaitTime ms remaining)")
                Thread.sleep(100)
            }

            iotVideoPlayer.release()

            cameraToRawSocketImageDecoder?.close()
            cameraToRawSocketImageDecoder?.release()
            cameraToRawSocketImageDecoder = null
            unregisterSdk()
        } catch (e: Exception) {
            LogUtils.e(CameraToImagePlayer::class.simpleName, "Error stopping player: ${e.message}")
        }
    }

    override fun release()
    {
        stop()
        watchdog.cancel()
    }

    override fun hashCode(): Int{
        return cameraId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraToImagePlayer

        return cameraId == other.cameraId
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("CameraPlayer{")
        stringBuilder.append("\n\tcameraId='").append(cameraCredential?.deviceId)
        stringBuilder.append("\n\tcameraPort=").append(cameraCredential?.socketPort)
        stringBuilder.append("\n\tframesHandled=").append(innerOnFrameCallback.frameCount)
        stringBuilder.append("\n\tstreamerState=").append(cameraToRawSocketImageDecoder?.toString())
        stringBuilder.append("\n}")
        return stringBuilder.toString()
    }

}