package com.github.xerootg.cryze.restreamer.interfaces

import com.github.xerootg.cryze.httpclient.responses.CameraInfo

interface ICameraStream
{
    fun start()
    fun stop()
    fun release()
    val cameraInfo: CameraInfo
}