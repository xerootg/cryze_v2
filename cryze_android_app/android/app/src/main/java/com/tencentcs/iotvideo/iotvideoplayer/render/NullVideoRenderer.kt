package com.tencentcs.iotvideo.iotvideoplayer.render

import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader

class NullVideoRenderer :
    IVideoRender {
    override fun onFrameUpdate(aVData: AVData?) {
    }

    override fun onInit(aVHeader: AVHeader?) {
    }

    override fun onPause() {
    }

    override fun onRelease() {
    }

    override fun onResume() {
    }
}