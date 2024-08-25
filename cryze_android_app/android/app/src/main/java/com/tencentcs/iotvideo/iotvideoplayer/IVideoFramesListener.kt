package com.tencentcs.iotvideo.iotvideoplayer

interface IVideoFramesListener {
    fun onReceiveVideoFramesPerSecond(framesSec1: Int, framesSec2: Int)
}
