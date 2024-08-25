package com.tencentcs.iotvideo.iotvideoplayer

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader

// JNI expects it in this namespace
interface IAudioRender {
    fun flushRender()

    fun getWaitRenderDuration(): Long

    fun onFrameUpdate(aVData: AVData?)

    fun onInit(aVHeader: AVHeader?)

    fun onRelease()

    fun setPlayerVolume(volumeLevel: Float)
}
