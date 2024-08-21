package com.tencentcs.iotvideo

import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoPlayer

interface ICameraStream
{
    fun start()
    fun stop()
    fun release()
    val cameraId: String
}