package com.tencentcs.iotvideo.iotvideoplayer

import com.tencentcs.iotvideo.IoTVideoSdk.PREFIX_THIRD_ID
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState.Companion.fromInt
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.NullAudioDecoder
import com.tencentcs.iotvideo.iotvideoplayer.player.PlayerUserData
import com.tencentcs.iotvideo.iotvideoplayer.render.NullAudioRenderer
import com.tencentcs.iotvideo.iotvideoplayer.render.NullVideoRenderer
import com.tencentcs.iotvideo.messagemgr.SubscribeError
import com.tencentcs.iotvideo.utils.LogUtils

class IoTVideoPlayer(
    deviceId: String,
    videoDecoder: IVideoDecoder,
    statusListener: IStatusListener,
    errorListener: IErrorListener
) : IoTVideoPlayerNativeBase(TAG) {

    // the stubbed implementations are in the base init, the ones doing actual work are here.
    init{
        // IErrorListener has a default implementation for Int,
        // we just need to call the SubscribeError implementation
        // we want to ensure its called from the main thread so
        // thats why the redundant object
        nativeSetErrorListener(object : IErrorListener {
            override fun onError(errorType: SubscribeError) {
                if (isMainThread) {
                    errorListener.onError(errorType)
                } else {
                    mMainHandler.post {
                        errorListener.onError(errorType)
                    }
                }
            }
        })

        // same as IErrorListener, call on the main thread
        nativeSetStatusListener(object : IStatusListener {
            override fun onStatus(state: PlayerState) {
                if (isMainThread) {
                    statusListener.onStatus(state)
                } else {
                    mMainHandler.post {
                        statusListener.onStatus(state)
                    }
                }
            }
        })

        // The renderers are stubbed because the playback is ignored.
        // the raw AV stream is passed through the decoder instead.
        nativeSetVideoRender(NullVideoRenderer())
        nativeSetAudioRender(NullAudioRenderer())

        // If you decide to add audio, pass in the audio decoder here.
        // you'll need an outer muxer to handle recombining the audio
        // and video streams and I don't use audio and it is difficult
        // to get right
        nativeSetAudioDecoder(NullAudioDecoder())

        // This is where we can grab raw video frames and pass them to our own socket
        nativeSetVideoDecoder(videoDecoder)

        val playerUserData = PlayerUserData(2)

        nativeSetDataResource(PREFIX_THIRD_ID + deviceId, CONN_TYPE_MONITOR, playerUserData)
    }

    private var lastUpdateTime = 0L
    private var lastSpeed = 0
    override val avBytesPerSec: Int
        get() {
            if (System.currentTimeMillis() - lastUpdateTime > 5000) {
                lastUpdateTime = System.currentTimeMillis()
                lastSpeed = nativeGetAvBytesPerSec()
            }
            return lastSpeed
        }

    // This has the protocol and connection type information which is more fun than useful
    override val connectMode: ConnectMode
        get() = ConnectMode(nativeGetConnectMode())

    // tells you what the current playback state is
    override val playState: PlayerState
        get() = fromInt(nativeGetPlayState())

    // any state where the player is in communication with the camera
    override val isConnectedDevice: Boolean
        get() {
            val playState = playState
            return PlayerState.STATE_READY == playState ||
                    PlayerState.STATE_LOADING == playState ||
                    PlayerState.STATE_PLAY == playState ||
                    PlayerState.STATE_PAUSE == playState ||
                    PlayerState.STATE_SEEKING == playState
        }

    override fun play() {
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
    }

    override fun stop() {
        LogUtils.i(
            TAG, "stop nativeObject:0x" + java.lang.Long.toHexString(
                this.nativeObject
            )
        )

        if (this.nativeObject == 0L) {
            LogUtils.i(TAG, "stop failure:nativeObject is invalid")
            return
        }

        mTaskTHandler.removeCallbacksAndMessages(null)
        if (0L != this.nativeObject) {
            nativeStop()
        }
    }

    // if you add something needing releasing, this is called by the base class
    // before releasing the native object
    override fun innerRelease() {
    }

    override fun updateAccessIdAndToken(j10: Long, str: String?) {
        nativeUpdateAccessIdAndToken(j10, str)
        LogUtils.i(TAG, "Updated: AccessIdAndToken")
    }

    companion object {
        private const val TAG = "IoTVideoPlayer"

        // https://github.com/GWTimes/IoTVideo-PC/blob/master/%E5%A4%9A%E5%AA%92%E4%BD%93.pdf
        private const val CONN_TYPE_MONITOR = 1

    }
}

