package com.tencentcs.iotvideo.custom

import android.graphics.SurfaceTexture
import android.util.Log.VERBOSE
import android.view.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pedro.library.util.sources.video.VideoSource


class WyzeCameraVideoSource constructor(
) : VideoSource() {

    private var isRunning = false
    internal var surfaceEncoder: Surface? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    override fun start(surfaceTexture: SurfaceTexture)
    {
        this.surfaceTexture = surfaceTexture
        surfaceTexture.setDefaultBufferSize(imageWidth, imageHeight)

        surfaceEncoder = Surface(surfaceTexture)

        isRunning = true
    }

    override fun stop()
    {
        isRunning = false
        surfaceTexture = null
    }

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
        imageWidth = width
        imageHeight = height
        return true
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun release() {
        surfaceEncoder?.release()
        surfaceEncoder = null
    }
}