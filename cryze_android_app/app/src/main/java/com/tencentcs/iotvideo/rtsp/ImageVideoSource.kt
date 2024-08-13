package com.tencentcs.iotvideo.rtsp

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.Image
import android.view.Surface
import com.pedro.library.util.sources.video.VideoSource


class ImageVideoSource constructor(private val context: Context
) : VideoSource() {

    private var isRunning = false
    private var surfaceEncoder: Surface? = null

    private val imageConverter = ImageConverter(context)

    public fun setFrame(frame: Image)
    {
        if (isRunning)
        {
            surfaceEncoder?.let {
                // write the frame
                val canvas = surfaceEncoder?.lockCanvas(null) // repaint the whole canvas
                val bitmap = imageConverter.convert(frame, 0) // convert image to bitmap
                // image is data, data1, and data2. this is the image decomposed into
                canvas?.drawBitmap(bitmap, 0f, 0f, null) // draw the bitmap on the canvas
                surfaceEncoder?.unlockCanvasAndPost(canvas) // set the canvas on the surface
            }
        }
    }

    override fun start(surfaceTexture: SurfaceTexture)
    {
        this.surfaceTexture = surfaceTexture

        if(rotation == 90 || rotation == 270){
            surfaceTexture.setDefaultBufferSize(width, height)

        } else {
            surfaceTexture.setDefaultBufferSize(height, width)
        }

        surfaceEncoder = Surface(surfaceTexture)

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
        surfaceEncoder?.release()
        surfaceEncoder = null
    }
}