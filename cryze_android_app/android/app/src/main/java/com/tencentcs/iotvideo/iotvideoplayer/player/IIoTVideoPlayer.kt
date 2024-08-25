package com.tencentcs.iotvideo.iotvideoplayer.player

import com.tencentcs.iotvideo.iotvideoplayer.ConnectMode
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener
import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder

// This is HEAVILY a cut down version to just what we need for our use case
interface IIoTVideoPlayer {
    @Deprecated("")
    fun changeDefinition(b10: Byte)

    val avBytesPerSec: Int

    val connectMode: ConnectMode?

    val playState: PlayerState?

    val isConnectedDevice: Boolean

    fun play()

    fun release()

    fun setAudioDecoder(iAudioDecoder: IAudioDecoder?)

    fun setAudioRender(iAudioRender: IAudioRender?)

    fun setDataResource(deviceId: String?,
                        callType: Int,
                        playerUserData: PlayerUserData?)

    fun setErrorListener(iErrorListener: IErrorListener?)

    fun setStatusListener(iStatusListener: IStatusListener?)

    fun setVideoDecoder(iVideoDecoder: IVideoDecoder?)

    fun setVideoRender(iVideoRender: IVideoRender?)

    fun stop()

    fun updateAccessIdAndToken(j10: Long, str: String?)

    companion object {
        const val INVALID_CAMERA_ID: Long = -1
    }
}
