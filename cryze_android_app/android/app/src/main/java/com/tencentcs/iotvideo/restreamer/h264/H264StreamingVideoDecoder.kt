package com.tencentcs.iotvideo.restreamer.h264

import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.restreamer.ByteStreamServer
import com.tencentcs.iotvideo.restreamer.interfaces.IRendererCallback
import com.tencentcs.iotvideo.restreamer.interfaces.IRestreamingVideoDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import java.util.concurrent.atomic.AtomicBoolean


class H264StreamingVideoDecoder(
    private val socketServer: ByteStreamServer,
    private var onFrameCallback : IRendererCallback
) : IRestreamingVideoDecoder
{
    private val _initialized = AtomicBoolean(false)
    override var initialized: Boolean // latched to true
        get() = _initialized.get()
        set(value) = if(_initialized.get()) {} else _initialized.set(value)

    private var released: Boolean = false

    override fun init(avHeader: AVHeader) {
        released = false
        LogUtils.i(TAG, "init: header: $avHeader")

        initialized = true
    }

    override fun receive_frame(aVData: AVData?): Int {
        return 0
    }

    override fun send_packet(aVData: AVData?): Int {
        onFrameCallback.onFrame()

        val byteBufferContents = ByteArray(aVData?.size ?: 0)
        aVData?.data?.get(byteBufferContents)
        socketServer.sendBytes(byteBufferContents)

        return 0
    }

    override fun release() {
        // there's nothing to release. all the resources are managed externally
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("${H264StreamingVideoDecoder::class.simpleName}{");
        sb.append("\n\t\tSocket: $socketServer")
        return sb.toString()
    }

    companion object {
        private const val TAG = "H264StreamingVideoDecoder"
    }
}

