package com.tencentcs.iotvideo.iotvideoplayer

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader

// JNI expects this to remain in this namespace
interface OnReceiveAVHeaderListener {
    fun onReceiveChangedAVHeader(aVHeader: AVHeader?)
}
