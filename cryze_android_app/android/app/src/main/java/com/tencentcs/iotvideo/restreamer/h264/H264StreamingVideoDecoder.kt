package com.tencentcs.iotvideo.restreamer.h264

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.restreamer.ByteStreamServer
import com.tencentcs.iotvideo.restreamer.interfaces.IRendererCallback
import com.tencentcs.iotvideo.restreamer.interfaces.IRestreamingVideoDecoder
import com.tencentcs.iotvideo.utils.LogUtils


class H264StreamingVideoDecoder(
    private val socketServer: ByteStreamServer,
    private var onFrameCallback : IRendererCallback
) : IRestreamingVideoDecoder
{
    override var initialized = false

    // The header is unimportant, ffmpeg will determine what to do with it
    private val mTaskThread: HandlerThread = HandlerThread("PacketSender")
    private val mTaskTHandler: Handler by lazy { Handler(mTaskThread.looper) }

    override fun init(avHeader: AVHeader) {
        mTaskThread.start()
        initialized = true
    }

    // This has raw color data on data,data1,data2, but we don't need that.
    override fun receive_frame(aVData: AVData?): Int {
        return 0
    }

    // this
    override fun send_packet(aVData: AVData?): Int {
        onFrameCallback.onFrame()

        val byteBufferContents = ByteArray(aVData?.size ?: 0)
        aVData?.data?.get(byteBufferContents)

        // Do not block send_packet, native will release on you.
        mTaskTHandler.post {
            socketServer.sendBytes(byteBufferContents)
        }

        return 0
    }

    override fun release() {
        // there's nothing to release. all the resources are managed externally
        if(initialized) {
            try {
                mTaskTHandler.removeCallbacksAndMessages(null)
                mTaskThread.quit()
            } catch (e: Exception)
            {
                LogUtils.e(TAG, "Failed to shutdown the packet sending thread")
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("${H264StreamingVideoDecoder::class.simpleName}{")
        sb.append("\n\t\tSocket: $socketServer")
        return sb.toString()
    }

    companion object {
        private const val TAG = "H264StreamingVideoDecoder"
    }
}

