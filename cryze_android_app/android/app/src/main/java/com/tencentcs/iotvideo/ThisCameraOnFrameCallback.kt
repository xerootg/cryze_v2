package com.tencentcs.iotvideo

import com.tencentcs.iotvideo.rtsp.IOnFrameCallback

class ThisCameraOnFrameCallback : IOnFrameCallback {
    var frameCount = 0L

    override fun onFrame() {
        frameCount++
    }
}