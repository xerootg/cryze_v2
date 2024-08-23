package com.tencentcs.iotvideo.restreamer.interfaces

interface IRendererCallback {
    fun onFrame()
    fun onRenderWatchdogRequestsRestart()
    {}
}