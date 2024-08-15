package com.tencentcs.iotvideo.iotvideoplayer

import com.tencentcs.iotvideo.utils.LogUtils

class LoggingConnectDevStateListener : IConnectDevStateListener {
    override fun onStatus(i10: Int) {
        LogUtils.i(LoggingConnectDevStateListener::class.simpleName, "onStatus: $i10")
    }
}