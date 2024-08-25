package com.tencentcs.iotvideo.iotvideoplayer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import com.tencentcs.iotvideo.IoTVideoSdk
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioEncoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoEncoder
import com.tencentcs.iotvideo.iotvideoplayer.player.IIoTVideoPlayer
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.utils.LogUtils

abstract class IoTVideoPlayerNativeBase(TAG: String) : IIoTVideoPlayer {
    protected var nativeObject: Long = 0

    // Used for the background thread activities like play and stp
    protected val mTaskTHandler: Handler
    private val mTaskThread: HandlerThread
    protected val mMainHandler = Handler(Looper.getMainLooper())

    val isMainThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    init{
        if (!IoTVideoSdk.isInited()) {
            LogUtils.w(TAG, "Iot sdk is not inited for player")
            throw RuntimeException("Iot sdk is not inited for player")
        }
        nativeInit()

        LogUtils.i(TAG, "java player created, nativeObject: "
                    + java.lang.Long.toHexString(this.nativeObject))


        val handlerThread = HandlerThread("IoTVideo-Thread")
        this.mTaskThread = handlerThread
        handlerThread.start()
        this.mTaskTHandler = Handler(mTaskThread.looper)

        nativeSetPreparedListener {
            LogUtils.d(TAG,"Native onPrepared called!!")
        }

        // Used for historical playback, stubbing.
        nativeSetTimeListener(
            object : ITimeListener {
                override fun onTime(time: Long) {
                }
            }
        )

        // Totally unused for streaming, but looks to be
        // used for requesting historical playback. stubbing.
        nativeSetUserDataListener(
            object : IUserDataListener {
                override fun onReceive(data: ByteArray?) {
                }
            }
        )

        // this is used for "FluctuationStrategy" which we won't really be able to do much about
        nativeSetVideoFramesListener(object : IVideoFramesListener {
            override fun onReceiveVideoFramesPerSecond(framesSec1: Int, framesSec2: Int) {
            }
        })

        // don't care, the decoder will handle this
        nativeSetOnReceiveChangedHeaderListener(object : OnReceiveAVHeaderListener {
            override fun onReceiveChangedAVHeader(aVHeader: AVHeader?) {
            }
        })

        // be aware of redundancy when using, IStatusListener is called for identical states
        nativeSetConnectDevStateListener { }

    }

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
    @Suppress("unused")
    private external fun reviseNativeDefinition(i10: Int)


    // This class sets all this stuff, cause they need to be set, we just dont care.
    private external fun nativeSetOnReceiveChangedHeaderListener(onReceiveAVHeaderListener: OnReceiveAVHeaderListener)
    private external fun nativeSetPreparedListener(iPreparedListener: IPreparedListener)
    private external fun nativeSetTimeListener(iTimeListener: ITimeListener)
    private external fun nativeSetVideoFramesListener(iVideoFramesListener: IVideoFramesListener)
    private external fun nativeSetUserDataListener(iUserDataListener: IUserDataListener)
    private external fun nativeRelease()
    private external fun nativeInit()

    // These are actually what we use in the app
    external fun nativeGetConnectMode(): IntArray?
    external fun nativeGetPlayState(): Int
    external fun nativePlay()
    external fun nativeSetAudioDecoder(iAudioDecoder: IAudioDecoder?)
    external fun nativeSetAudioRender(iAudioRender: IAudioRender)
    external fun nativeSetConnectDevStateListener(iConnectDevStateListener: IConnectDevStateListener)
    external fun nativeSetDataResource(deviceId: String?, callType: Int, playerUserData: PlayerUserData?)
    external fun nativeSetErrorListener(iErrorListener: IErrorListener)
    external fun nativeSetStatusListener(iStatusListener: IStatusListener)
    external fun nativeSetVideoDecoder(iVideoDecoder: IVideoDecoder?)
    external fun nativeSetVideoRender(iVideoRender: IVideoRender)
    external fun nativeStop()
    external fun nativeGetAvBytesPerSec(): Int

    // Long might be the userId and String is probably token
    external fun nativeUpdateAccessIdAndToken(j10: Long, str: String?)

    abstract fun innerRelease()
    override fun release() {

        // release the stuff this class didn't make first
        innerRelease()

        // now shut it all down
        if (this.nativeObject != 0L) {
            LogUtils.i(
                this::class.simpleName, "release nativeObject:0x" + java.lang.Long.toHexString(
                    this.nativeObject
                )
            )
            nativeRelease()
            this.nativeObject = 0L
        }
        val handler = this.mTaskTHandler
        handler.removeCallbacksAndMessages(null)
        val handlerThread = this.mTaskThread
        handlerThread.quit()
    }
}