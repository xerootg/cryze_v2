package com.tencentcs.iotvideo.iotvideoplayer.player

import com.tencentcs.iotvideo.iotvideoplayer.ConnectMode
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState

// This is HEAVILY a cut down version to just what we need for our use case
interface IIoTVideoPlayer {
    @Deprecated("")
    fun changeDefinition(b10: Byte){} // we don't need this for restreaming

    val avBytesPerSec: Int

    val connectMode: ConnectMode?

    val playState: PlayerState?

    val isConnectedDevice: Boolean

    fun play()

    fun release()

    fun stop()

    fun updateAccessIdAndToken(accessId: Long, token: String?)

    //TODO: determine where this would apply
    companion object {
        const val INVALID_CAMERA_ID: Long = -1
    }
}
