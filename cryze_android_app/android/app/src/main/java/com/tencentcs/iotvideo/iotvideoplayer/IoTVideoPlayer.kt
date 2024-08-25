package com.tencentcs.iotvideo.iotvideoplayer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.view.Surface
import android.view.SurfaceHolder
import com.tencentcs.iotvideo.IoTVideoErrors
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState.Companion.fromInt
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioEncoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoEncoder
import com.tencentcs.iotvideo.iotvideoplayer.player.IIoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.utils.LogUtils
import com.tencentcs.iotvideo.utils.rxjava.IResultListener

class IoTVideoPlayer : IIoTVideoPlayer {
    // JNI throws if not atleast declared
    @Suppress("unused")
    private external fun nativeSetDisplay(surfaceHolder: SurfaceHolder)
    @Suppress("unused")
    private external fun nativeInitLastFramePath(str: String)
    @Suppress("unused")
    private external fun nativeSetSurface(surface: Surface)
    @Suppress("unused")
    private external fun nativePrepare()
    @Suppress("unused")
    private external fun nativePause()
    @Suppress("unused")
    private external fun nGetRenderAVData(): ByteArray?
    @Suppress("unused")
    private external fun nSetAccelRenderMode(i10: Int)
    @Suppress("unused")
    private external fun nSetPlayerOption(i10: Int, str: String, j10: Long)
    @Suppress("unused")
    private external fun nativeGetAVHeader(): AVHeader?
    @Suppress("unused")
    external fun getVideoDefinition(): Int
    @Suppress("unused")
    private external fun nSetPlayerOption(i10: Int, str: String, str2: String)
    @Suppress("unused")
    private external fun nativeIsMute(): Boolean
    @Suppress("unused")
    private external fun nativeIsRecording(): Boolean
    @Suppress("unused")
    private external fun nativeMute(isMuted: Boolean)
    @Suppress("unused")
    private external fun nativeSeek(j10: Long)
    @Suppress("unused")
    private external fun nativeSeekRet(z10: Boolean, j10: Long)
    @Suppress("unused")
    private external fun nativeSendUserData(data: ByteArray, z10: Boolean): Int
    @Suppress("unused")
    private external fun nativeSetAudioEncoder(iAudioEncoder: IAudioEncoder)
    @Suppress("unused")
    private external fun nativeSetRecordAudioEncoder(iAudioEncoder: IAudioEncoder)
    @Suppress("unused")
    private external fun nativeSetRecordVideoEncoder(iVideoEncoder: IVideoEncoder)
    @Suppress("unused")
    private external fun nativeSetVideoEncoder(iVideoEncoder: IVideoEncoder)
    @Suppress("unused")
    private external fun nativeSnapShot(
        j10: Long,
        str: String,
        iSnapShotListener: ISnapShotListener
    )

    @Suppress("unused")
    private external fun nativeStartRecord(str: String, iRecordListener: IRecordListener): Boolean
    @Suppress("unused")
    private external fun nativeStopRecord()
    @Suppress("unused")
    private external fun nativeWmPath(str: String)
    @Suppress("unused")
    private external fun nativeCloseCamera()
    @Suppress("unused")
    private external fun nativeGetPlaybackSpeed(): Float
    @Suppress("unused")
    private external fun nativeOpenCamera()
    @Suppress("unused")
    private external fun nativeResume()
    @Suppress("unused")
    private external fun nativeSendAudioData(aVData: AVData)
    @Suppress("unused")
    private external fun nativeSendVideoData(aVData: AVData)
    @Suppress("unused")
    private external fun nativeSetCaptureHeader(aVHeader: AVHeader)
    @Suppress("unused")
    private external fun nativeSetVideoStuckStrategy(iVideoStuckStrategy: IVideoStuckStrategy)
    @Suppress("unused")
    private external fun nativeStartTalk()
    @Suppress("unused")
    private external fun nativeStopTalk()
    @Suppress("unused")
    private external fun nativePlaybackSpeedRet(f: Float)

    private external fun nativeGetConnectMode(): IntArray?
    private external fun nativeGetPlayState(): Int
    private external fun nativeInit()
    private external fun nativePlay()
    private external fun nativeRelease()
    private external fun nativeSetAudioDecoder(iAudioDecoder: IAudioDecoder?)
    private external fun nativeSetAudioRender(iAudioRender: IAudioRender)
    private external fun nativeSetConnectDevStateListener(iConnectDevStateListener: IConnectDevStateListener)
    private external fun nativeSetDataResource(
        str: String?,
        i10: Int,
        playerUserData: PlayerUserData?
    )

    private external fun nativeSetErrorListener(iErrorListener: IErrorListener)
    private external fun nativeSetOnReceiveChangedHeaderListener(onReceiveAVHeaderListener: OnReceiveAVHeaderListener)
    private external fun nativeSetPreparedListener(iPreparedListener: IPreparedListener)
    private external fun nativeSetStatusListener(iStatusListener: IStatusListener)
    private external fun nativeSetTimeListener(iTimeListener: ITimeListener)
    private external fun nativeSetUserDataListener(iUserDataListener: IUserDataListener)
    private external fun nativeSetVideoDecoder(iVideoDecoder: IVideoDecoder?)
    private external fun nativeSetVideoFramesListener(iVideoFramesListener: IVideoFramesListener)
    private external fun nativeSetVideoRender(iVideoRender: IVideoRender)
    private external fun nativeStop()
    private external fun reviseNativeDefinition(i10: Int)
    private external fun nativeGetAvBytesPerSec(): Int
    private external fun nativeUpdateAccessIdAndToken(j10: Long, str: String?)


    private var nativeObject: Long = 0
    private val mTaskTHandler: Handler
    private val mTaskThread: HandlerThread
    private val mMainHandler = Handler(Looper.getMainLooper())
    private var mStatusListener: IStatusListener? = null
    private var mErrorListener: IErrorListener? = null
    private var mConnectDevStateListener: IConnectDevStateListener? = null
    private var mVideoRender: IVideoRender? = null
    private var mReceiveAvHeader: AVHeader? = null
    private var mAudioRender: IAudioRender? = null
    private val mOnReceiveAVHeaderListener: OnReceiveAVHeaderListener? = null

    private var waitSendDefinition: Byte = -1
    private var currentDefinition: Byte = -1


    init {
        if (!IoTVideoSdk.isInited()) {
            LogUtils.w(TAG, "Iot sdk is not inited for player")
            throw RuntimeException("Iot sdk is not inited for player")
        }
        nativeInit()
        LogUtils.i(
            TAG,
            "java player created, javaObj: " + hashCode() + " nativeObject: " + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
        val handlerThread = HandlerThread("IoTVideo-Thread")
        this.mTaskThread = handlerThread
        handlerThread.start()
        this.mTaskTHandler = Handler(mTaskThread.looper)

        nativeSetPreparedListener { LogUtils.i(TAG, "onPrepared native listener fired!") }

        nativeSetStatusListener { stat: Int ->
            onPlayerStateChange(stat)
            val statusMessage = StringBuilder("nativeSetStatusListener status :")
            statusMessage.append(stat)
            statusMessage.append("; listener:")
            if (mStatusListener == null) {
                statusMessage.append("listener is null")
            } else {
                statusMessage.append(mStatusListener.hashCode())
            }

            LogUtils.i(TAG, statusMessage.toString())
            if (mStatusListener == null) {
                return@nativeSetStatusListener
            }
            mMainHandler.post {
                if (mStatusListener != null) {
                    mStatusListener!!.onStatus(stat)
                }
            }
            LogUtils.i(TAG, "onStatus native listener fired!")
        }

        nativeSetTimeListener(
            object : ITimeListener {
                override fun onTime(time: Long) {
//                    LogUtils.i(TAG, "onTime native listener fired!")
                }
            }
        )

        nativeSetErrorListener(
            object : IErrorListener {
                override fun onError(errorCode: Int) {
                    if (mErrorListener == null) {
                        return
                    }
                    if (isMainThread) {
                        mErrorListener!!.onError(errorCode)
                    } else {
                        mMainHandler.post {
                            if (mErrorListener != null) {
                                mErrorListener!!.onError(errorCode)
                            }
                        }
                    }
                    LogUtils.i(TAG, "onError native listener fired!")
                }
            }
        )

        // I dont think this is used
        // TODO: try to remove this
        nativeSetUserDataListener(
            object : IUserDataListener {
                override fun onReceive(data: ByteArray?) {
                    LogUtils.i(TAG, "onUserData: data = $data")
                }
            }
        )

        // This passes the defined video render to the native layer
        nativeSetVideoRender(object : IVideoRender {
            override fun onFrameUpdate(aVData: AVData?) {
                if (aVData != null && mVideoRender != null) {
                    mVideoRender!!.onFrameUpdate(aVData)
                }
            }

            override fun onInit(aVHeader: AVHeader?) {
                LogUtils.i(
                    TAG,
                    "nativeSetVideoRender jni: IVideoRender.onInit(..), header = $aVHeader"
                )
                if (mReceiveAvHeader != null && mVideoRender != null) {
                    mVideoRender!!.onInit(mReceiveAvHeader)
                }
            }

            override fun onPause() {
            }

            override fun onRelease() {
                LogUtils.i(TAG, "nativeSetVideoRender jni: AVideoRender.onRelease(..)")
                if (mVideoRender != null) {
                    mVideoRender!!.onRelease()
                }
            }

            override fun onResume() {
            }
        })

        nativeSetAudioRender(object : IAudioRender {
            override fun flushRender() {
                if (mAudioRender == null) {
                    return
                }
                mAudioRender!!.flushRender()
            }

            override fun getWaitRenderDuration(): Long {
                if (mAudioRender != null) {
                    return mAudioRender!!.getWaitRenderDuration()
                }
                return 0L
            }

            override fun onFrameUpdate(aVData: AVData?) {
                if (mAudioRender == null) {
                    return
                }
                mAudioRender!!.onFrameUpdate(aVData)
            }

            override fun onInit(aVHeader: AVHeader?) {
                LogUtils.i(TAG, "jni: IAudioRender.onInit(..), header = $aVHeader")
                if (mReceiveAvHeader != null && mAudioRender != null) {
                    mAudioRender!!.onInit(aVHeader)
                }
            }

            override fun onRelease() {
                LogUtils.i(TAG, "jni: IAudioRender.onRelease()")
                if (mAudioRender != null) {
                    mAudioRender!!.onRelease()
                }
            }

            override fun setPlayerVolume(volumeLevel: Float) {
            }
        })

        nativeSetConnectDevStateListener { statusCode: Int ->
            if (mConnectDevStateListener == null) {
                return@nativeSetConnectDevStateListener
            }
            if (isMainThread) {
                mConnectDevStateListener!!.onStatus(statusCode)
            } else {
                mMainHandler.post {
                    if (mConnectDevStateListener != null) {
                        mConnectDevStateListener!!.onStatus(statusCode)
                    }
                }
            }
            LogUtils.i(TAG, "IConnectDevStateListener onStatus native listener fired!")
        }

        nativeSetVideoFramesListener(object : IVideoFramesListener {
            override fun onReceiveVideoFramesPerSecond(framesSec1: Int, framesSec2: Int) {
                //TODO("Not yet implemented")
            }
        })

        nativeSetOnReceiveChangedHeaderListener(object : OnReceiveAVHeaderListener {
            override fun onReceiveChangedAVHeader(aVHeader: AVHeader?) {
                LogUtils.d(TAG, "onReceiveChangedAVHeader(..), AVHeader = $aVHeader")
                if (aVHeader == null) {
                    return
                }
                if (mReceiveAvHeader == null) {
                    mReceiveAvHeader = AVHeader()
                }
                mReceiveAvHeader!!.copy(aVHeader)
                if (isMainThread) {
                    localOnReceiveChangedAVHeader(mReceiveAvHeader)
                    return
                }
                mMainHandler.post { localOnReceiveChangedAVHeader(mReceiveAvHeader) }            }
        })

    }

    fun localOnReceiveChangedAVHeader(aVHeader: AVHeader?) {
        val onReceiveAVHeaderListener = this.mOnReceiveAVHeaderListener
        onReceiveAVHeaderListener?.onReceiveChangedAVHeader(aVHeader)
    }

    fun setConnectDevStateListener(iConnectDevStateListener: IConnectDevStateListener?) {
        this.mConnectDevStateListener = iConnectDevStateListener
    }

    private fun onPlayerStateChange(state: Int) {
        LogUtils.i(TAG, "onPlayerStateChange playerState:$state")

        if (isConnectedDevice && waitSendDefinition >= 0)
        {
            changeDefinition(state.toByte(), null)
        }
    }

    private fun changeDefinition(definition: Byte, iResultListener: IResultListener<Boolean?>?) {
        changeDefinition(definition, true, iResultListener)
    }

    private fun changeDefinition(
        definition: Byte,
        haveNotifyFromDev: Boolean,
        iResultListener: IResultListener<Boolean?>?
    ) {
        LogUtils.i(
            TAG, "changeDefinition nativeObject:0x%s; definition:%d", java.lang.Long.toHexString(
                this.nativeObject
            ), definition
        )
        this.currentDefinition = definition
        if (!isConnectedDevice) {
            LogUtils.i(
                TAG,
                "changeDefinition ret:disconnect with device, waiting connecting dev, player status:$playState"
            )
            iResultListener?.onSuccess(java.lang.Boolean.TRUE)
            if (playState == PlayerState.STATE_PREPARING) {
                this.waitSendDefinition = definition
                LogUtils.i(TAG, "changeDefinition is connecting device")
            }
            reviseNativeDefinition(definition.toInt())
            return
        }
        this.waitSendDefinition = ((-1).toByte())
        sendInnerUserData(
            SET_USER_DATA_CHANGE_DEFINITION_CMD,
            byteArrayOf(definition),
            object : IResultListener<ByteArray?> {
                override fun onError(errorCode: Int, message: String) {
                    if (iResultListener != null && haveNotifyFromDev) {
                        iResultListener.onError(errorCode, message)
                    }
                    LogUtils.i(
                        TAG,
                        "changeDefinition onError definition: $definition; errorcode: $errorCode; errormsg: $message"
                    )
                }

                override fun onStart() {
                    if (iResultListener != null && haveNotifyFromDev) {
                        iResultListener.onStart()
                    }
                }

                override fun onSuccess(success: ByteArray?) {
                    if (!haveNotifyFromDev) {
                        LogUtils.i(TAG, "changeDefinition onSuccess, haveNotifyFromDev: false")
                    } else if ((success != null && success.isNotEmpty()) && 255 != (success[0].toInt() and 255)) {
                        iResultListener?.onSuccess(java.lang.Boolean.TRUE)
                        reviseNativeDefinition(definition.toInt())
                        LogUtils.i(TAG, "changeDefinition onSuccess definition:$definition")
                    } else {
                        iResultListener?.onError(IoTVideoErrors.ERROR_RESULT, null)
                        LogUtils.i(TAG, "changeDefinition error:" + success.contentToString())
                    }
                }
            })

        if (haveNotifyFromDev) {
            return
        }
        iResultListener?.onSuccess(true)
        reviseNativeDefinition(currentDefinition.toInt())
    }

    // Seems possibly related to downloading playback, see InnerUserDataHandler in msgmanager
    private fun sendInnerUserData(
        command: Byte,
        data: ByteArray,
        iResultListener: IResultListener<ByteArray?>
    ) {
        sendInnerUserData(command, data, false, iResultListener)
    }

    private fun sendInnerUserData(
        command: Byte,
        data: ByteArray,
        z: Boolean,
        iResultListener: IResultListener<ByteArray?>
    ) {
        sendInnerUserData(command, data, 10, z, iResultListener)
    }

    private fun sendInnerUserData(
        command: Byte,
        data: ByteArray,
        i: Int,
        z: Boolean,
        iResultListener: IResultListener<ByteArray?>
    ) {
        LogUtils.i(TAG, "sendInnerUserData: command:$command data:$data i:$i z:$z iResultListener:$iResultListener")
    }

    val isMainThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    @Deprecated("because it just is, okay?")
    override fun changeDefinition(b10: Byte) {
        changeDefinition(b10, null)
    }

    override val avBytesPerSec: Int
        //KEEP
        get() = nativeGetAvBytesPerSec()

    override val connectMode: ConnectMode
        get() {
            val connectionMode = nativeGetConnectMode()
            val connectMode = ConnectMode()
            if (connectionMode != null && connectionMode.size == 3) {
                val i = connectionMode[0]
                if (i == 0) {
                    connectMode.mMode = connectionMode[1]
                    connectMode.mProtocol = connectionMode[2]
                } else {
                    connectMode.mMode = i
                    connectMode.mProtocol = -1
                }
            } else {
                connectMode.mMode = 0
                connectMode.mProtocol = -1
            }
            LogUtils.i(TAG, "getConnectMode(): connectMode = $connectMode")
            return connectMode
        }

    override val playState: PlayerState
        get() = fromInt(nativeGetPlayState())

    override val isConnectedDevice: Boolean
        get() {
            val playState = playState
            return PlayerState.STATE_READY == playState || PlayerState.STATE_LOADING == playState || PlayerState.STATE_PLAY == playState || PlayerState.STATE_PAUSE == playState || PlayerState.STATE_SEEKING == playState
        }

    override fun play() {
        LogUtils.i(
            TAG, "play nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        if (this.nativeObject == 0L) {
            LogUtils.i(TAG, "play failure:nativeObject is invalid, maybe player is released")
        } else if (playState == PlayerState.STATE_PREPARING || playState == PlayerState.STATE_READY || playState == PlayerState.STATE_LOADING || playState == PlayerState.STATE_PLAY) {
            LogUtils.i(TAG, "play failure:preparing or playing, player state:$playState")
        } else {
            mTaskTHandler.removeCallbacksAndMessages(null)
            mTaskTHandler.post {
                LogUtils.i(TAG, "nativePlay running")
                nativePlay()
            }
        }
        LogUtils.i(TAG, "play complete state: $playState")
    }

    override fun release() {
        LogUtils.i(
            TAG, "release nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        if (this.nativeObject != 0L) {
            nativeRelease()
            this.nativeObject = 0L
        }
        val handler = this.mTaskTHandler
        handler.removeCallbacksAndMessages(null)
        val handlerThread = this.mTaskThread
        handlerThread.quit()
        this.mReceiveAvHeader = null
    }

    override fun setAudioDecoder(iAudioDecoder: IAudioDecoder?) {
        LogUtils.i(
            TAG, "setAudioDecoder nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        nativeSetAudioDecoder(iAudioDecoder)
    }

    override fun setAudioRender(iAudioRender: IAudioRender?) {
        LogUtils.i(
            TAG,
            "setAudioRender(render = " + iAudioRender + "), original mAudioRender = " + this.mAudioRender
        )
        val iAudioRender2 = this.mAudioRender
        if (iAudioRender2 === iAudioRender) {
            return
        }
        iAudioRender2?.onRelease()
        this.mAudioRender = iAudioRender
        if (iAudioRender == null) {
            return
        }
        val aVHeader = this.mReceiveAvHeader
        if (aVHeader != null) {
            iAudioRender.onInit(aVHeader)
        }
    }

    // https://github.com/GWTimes/IoTVideo-PC/blob/master/%E5%A4%9A%E5%AA%92%E4%BD%93.pdf
    // CONN_TYPE_VIDEO_CALL = 0
    // CONN_TYPE_MONITOR = 1
    // CONN_TYPE_PLAY_REC_FILE = 2
    override fun setDataResource(
        deviceId: String?,
        callType: Int,
        playerUserData: PlayerUserData?
    ) {
        LogUtils.i(
            TAG, "setDataResource nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
        LogUtils.i(
            TAG,
            "setDataResource deviceId:$deviceId; callType:$callType; userData:$playerUserData"
        )
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        if (TextUtils.isEmpty(deviceId)) {
            LogUtils.i(TAG, "setDataResource failure:the id of dev is null")
        } else {
            nativeSetDataResource(deviceId, callType, playerUserData)
        }
    }

    override fun setErrorListener(iErrorListener: IErrorListener?) {
        this.mErrorListener = iErrorListener
    }

    override fun setStatusListener(iStatusListener: IStatusListener?) {
        this.mStatusListener = iStatusListener
    }

    override fun setVideoDecoder(iVideoDecoder: IVideoDecoder?) {
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        nativeSetVideoDecoder(iVideoDecoder)
        LogUtils.i(
            TAG, "setVideoDecoder nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
    }

    override fun setVideoRender(iVideoRender: IVideoRender?) {
        LogUtils.i(
            TAG,
            "setVideoRender(render = " + iVideoRender + "), original mVideoRender = " + this.mVideoRender
        )
        if (this.mVideoRender === iVideoRender) {
            return
        }
        mVideoRender?.onRelease()
        this.mVideoRender = iVideoRender
        LogUtils.i(TAG, "setVideoRender success")

        if (iVideoRender != null && mReceiveAvHeader != null) {
            iVideoRender.onInit(mReceiveAvHeader)
        }
    }

    override fun stop() {
        LogUtils.i(
            TAG, "stop nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        if (this.nativeObject == 0L) {
            LogUtils.i(TAG, "stop failure:nativeObject is invalid")
            return
        }

        mTaskTHandler.removeCallbacksAndMessages(null)
        if (0L != this.nativeObject) {
            nativeStop()
        }
    }

    //TODO: This would be far far better to use than shutting down the whole SDK
    override fun updateAccessIdAndToken(j10: Long, str: String?) {
        LogUtils.i(TAG, "updateAccessIdAndToken nativeObject:" + this.nativeObject)
        if (!IoTVideoSdk.isSupportedCurrentAbi()) {
            return
        }
        nativeUpdateAccessIdAndToken(j10, str)
    }

    companion object {
        private const val DEFAULT_AUDIO_RENDER_ID: Long = -1
        private const val LOOP_GET_AV_BYTES_TIME_INTERVAL: Long = 1000
        private const val SET_USER_DATA_CHANGE_DEFINITION_CMD: Byte = 5
        private const val SET_USER_DATA_PLAYBACK_PAUSE_CMD: Byte = 1
        private const val SET_USER_DATA_PLAYBACK_RESUME_CMD: Byte = 2
        private const val SET_USER_DATA_PLAYBACK_SEEK_CMD: Byte = 3
        private const val SET_USER_DATA_TO_INNER = 0
        private const val SET_USER_DATA_TO_OUTSIDE = 1
        private const val TAG = "IoTVideoPlayer"

        //TODO: confirm JNI doesn't throw before removing. its related to setinneruserdata
        @Suppress("unused")
        private fun addHeader(
            domain: Byte,
            cmd: Byte,
            cameraId: Byte,
            timestamp: Long,
            data: ByteArray?
        ): ByteArray {
            val realData = if (data == null) ByteArray(8) else ByteArray(data.size + 8)
            realData[0] = domain
            realData[1] = cmd
            realData[2] = (cameraId.toInt() and 255).toByte() // in the 1.3 SDK, this is not set
            realData[4] = (timestamp and 255L).toByte()
            realData[5] = ((timestamp shr 8) and 255L).toByte()
            realData[6] = ((timestamp shr 16) and 255L).toByte()
            realData[7] = (255L and (timestamp shr 24)).toByte()
            if (data != null && data.isNotEmpty()) {
                System.arraycopy(data, 0, realData, 8, data.size)
            }
            LogUtils.i(TAG, "header:" + realData.contentToString())
            return realData
        }
    }
}

