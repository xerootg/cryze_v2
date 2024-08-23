package com.tencentcs.iotvideo.restreamer.interfaces

interface ICameraStream
{
    fun start()
    fun stop()
    fun release()
    val cameraId: String
}