package com.tencentcs.iotvideo.iotvideoplayer.render

import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader

class NullAudioRenderer : IAudioRender {
    override fun flushRender() {
    }

    override fun getWaitRenderDuration(): Long {
        return 0
    }

    override fun onFrameUpdate(aVData: AVData?) {
    }

    override fun onInit(aVHeader: AVHeader?) {
    }

    override fun onRelease() {
    }

    override fun setPlayerVolume(f10: Float) {
    }
}