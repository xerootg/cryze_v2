package com.tencentcs.iotvideo.rtsp

import android.graphics.SurfaceTexture
import android.view.Surface
import com.pedro.library.util.sources.video.VideoSource


class SurfaceVideoSource : VideoSource() {

    private var isRunning = false

    var surface: Surface? = null

    override fun start(surfaceTexture: SurfaceTexture)
    {
        this.surfaceTexture = surfaceTexture

        if(rotation == 90 || rotation == 270){
            surfaceTexture.setDefaultBufferSize(width, height)

        } else {
            surfaceTexture.setDefaultBufferSize(height, width)
        }

        surface = Surface(surfaceTexture)

        isRunning = true
    }

    override fun stop()
    {
        isRunning = false
        surfaceTexture = null
    }

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
        return true
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun release() {
        surface?.release()
        surface = null
    }
}