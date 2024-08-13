package com.tencentcs.iotvideo

import android.app.Activity
import com.tencentcs.iotvideo.custom.CameraCredential
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
import com.tencentcs.iotvideo.iotvideoplayer.IConnectDevStateListener
import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import com.tencentcs.iotvideo.rtsp.AudioStreamDecoder
import com.tencentcs.iotvideo.rtsp.RtspServerVideoStreamDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.Utils.getErrorDescription
import com.tencentcs.iotvideo.utils.rxjava.IResultListener

class CameraPlayer(private val cameraCredential: CameraCredential, private val baseContext: Activity) {

    var iotVideoPlayer: IoTVideoPlayer = IoTVideoPlayer()
    var playerThread: Thread? = null
    private var frameCount = 0L

    val port = cameraCredential.socketPort

    private var onFrameUpdateCallbacks = mutableListOf<() -> Unit>()

    private fun onFrameUpdate() {
        for (callback in onFrameUpdateCallbacks) {
            // Run on UI thread to avoid crash
            callback()
        }
        frameCount++
    }

    fun addOnFrameUpdateCallback(callback: () -> Unit) {
        onFrameUpdateCallbacks.add(callback)
    }

    fun start() {
       // registerIotVideoSdk(loginInfo)
        IoTVideoSdk.register(cameraCredential.accessId, cameraCredential.accessToken, 3)
        IoTVideoSdk.getMessageMgr().removeModelListeners()
        IoTVideoSdk.getMessageMgr().addModelListener {
            LogUtils.d("CLModelListener", "new model message! ${it.device}, path: ${it.messageType}, data: ${it.payload}")
//            deviceIdList.add(it.device)
        }
        // addAppLinkListener
        if(MessageMgr.getSdkStatus() != 1)
        {
            IoTVideoSdk.getMessageMgr().addAppLinkListener {
                LogUtils.i(CameraPlayer::class.simpleName, "appLinkListener state = $it")
                var shouldExit = false
                if (it == 1) {
                    LogUtils.i(CameraPlayer::class.simpleName, "Reg success, app online, start live")
                    addSubscribeDevice(cameraCredential)
                    shouldExit = true
                }
                if (!shouldExit)
                {
                    var z = true
                    if (it != 6 && it != 13 && (12 > it || it >= 18))
                    {
                        z = false
                    }
                    if (z)
                    {
                        IoTVideoSdk.unRegister()
                        IoTVideoSdk.getMessageMgr().removeAppLinkListeners()
                        IoTVideoSdk.getMessageMgr().removeModelListeners()
                    }
                }

            }
        } else {
            LogUtils.i(CameraPlayer::class.simpleName, "appLinkListener() iot is register")
            addSubscribeDevice(cameraCredential)
        }

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
                val ioTVideoPlayer: IoTVideoPlayer = iotVideoPlayer

                ioTVideoPlayer.mute(true)
                ioTVideoPlayer.setDataResource("_@." + loginInfo.deviceId, 1, PlayerUserData(2))
                ioTVideoPlayer.setConnectDevStateListener(object : IConnectDevStateListener
                {
                    override fun onStatus(i10: Int) {
                        LogUtils.i(CameraPlayer::class.simpleName, "onStatus for iotvideo player: $i10")
                    }

                })
                ioTVideoPlayer.setAudioRender(object : IAudioRender {
                    override fun flushRender() {
                    }

                    override fun getWaitRenderDuration(): Long {
                        return 0L
                    }

                    override fun onFrameUpdate(aVData: AVData?) {
                    }

                    override fun onInit(aVHeader: AVHeader?) {
                    }

                    override fun onRelease() {
                    }

                    override fun setPlayerVolume(volume: Float) {
                    }

                })

                // Setting the video render will only get a frame if you use a decoder in android like MediaCodecVideoDecoder,
                // if you use a custom IVideoDecoder then nothing will get sent to onFrameUpdate
                ioTVideoPlayer.setVideoRender(object : IVideoRender {
                    override fun onFrameUpdate(aVData: AVData?) {
                        LogUtils.d(CameraPlayer::class.simpleName, "CustomVideoRender onFrameUpdate for iotvideo player, size: ${aVData.toString()}")
                    }

                    override fun onInit(aVHeader: AVHeader?) {
                        LogUtils.d(CameraPlayer::class.simpleName, "IVideoRender override fun onInit(aVHeader: AVHeader?) {\n for iotvideo player, size: ${aVHeader.toString()}")
                    }

                    override fun onPause() {
//                LogUtils.d(TAG, "IVideoRender onPause for iotvideo player")
                    }

                    override fun onRelease() {
//                LogUtils.d(TAG, "IVideoRender onRelease for iotvideo player")
                    }

                    override fun onResume() {
//                LogUtils.d(TAG, "IVideoRender onResume for iotvideo player")
                    }

                })

                ioTVideoPlayer.setAudioDecoder(AudioStreamDecoder())
                val decoder = RtspServerVideoStreamDecoder(loginInfo.socketPort, baseContext)
                ioTVideoPlayer.setVideoDecoder(decoder)
                decoder.addOnFrameCallback { onFrameUpdate() }

                ioTVideoPlayer.setErrorListener { errorNumber ->
                    LogUtils.i(
                        CameraPlayer::class.simpleName,
                        "errorListener onError for iotvideo player: $errorNumber with message: ${getErrorDescription(errorNumber)}"
                    )
                }
                ioTVideoPlayer.setStatusListener { statusCode ->
                    LogUtils.i(
                        CameraPlayer::class.simpleName,
                        "IStatusListener onStatus for iotvideo player: $statusCode"
                    )
                }

                iotVideoPlayer.play() // start receiving packets
                LogUtils.i(CameraPlayer::class.simpleName, "Player state: ${ioTVideoPlayer.playState}");
            }
        })
    }

    fun equalsCameraId(deviceId: String): Boolean {
        return cameraCredential.deviceId == deviceId
    }

    override fun hashCode(): Int{
        return cameraCredential.deviceId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraPlayer

        return cameraCredential.deviceId == other.cameraCredential.deviceId
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("CameraPlayer{")
        stringBuilder.append("cameraId='").append(cameraCredential.deviceId)
        stringBuilder.append("cameraPort=").append(cameraCredential.socketPort)
        stringBuilder.append("framesHandled=").append(frameCount)
        stringBuilder.append("}")
        return stringBuilder.toString()
    }

}