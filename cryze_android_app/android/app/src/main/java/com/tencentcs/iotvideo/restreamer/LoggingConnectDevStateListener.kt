package com.tencentcs.iotvideo.restreamer

import com.tencentcs.iotvideo.iotvideoplayer.IConnectDevStateListener
import com.tencentcs.iotvideo.utils.LogUtils

class LoggingConnectDevStateListener : IConnectDevStateListener {
    override fun onStatus(statusCode: Int) {
        LogUtils.i(LoggingConnectDevStateListener::class.simpleName, "onStatus: $statusCode")
    }
}