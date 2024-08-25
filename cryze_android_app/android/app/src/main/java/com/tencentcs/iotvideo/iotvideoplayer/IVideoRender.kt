package com.tencentcs.iotvideo.iotvideoplayer

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader

// Must be in com.tencentcs.iotvideo.iotvideoplayer for JNI reasons
interface IVideoRender {
    fun onFrameUpdate(aVData: AVData?)

    fun onInit(aVHeader: AVHeader?)

    fun onPause()

    fun onRelease()

    fun onResume()
}
