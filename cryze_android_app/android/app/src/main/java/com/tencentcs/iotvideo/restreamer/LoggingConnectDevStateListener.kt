package com.tencentcs.iotvideo.restreamer

import com.tencentcs.iotvideo.iotvideoplayer.IConnectDevStateListener
import com.tencentcs.iotvideo.iotvideoplayer.PlayerState
import com.tencentcs.iotvideo.utils.LogUtils

// This is highly redundant. It's called with the exact same int value as the
class LoggingConnectDevStateListener() : IConnectDevStateListener {
    override fun onStatus(statusCode: PlayerState) {
        LogUtils.i(LoggingConnectDevStateListener::class.simpleName, "onPlaybackStatus: state: $statusCode")
    }
}