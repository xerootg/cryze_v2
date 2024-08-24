package com.tencentcs.iotvideo.restreamer.mjpeg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.MediaCodec.INFO_TRY_AGAIN_LATER
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
import android.media.MediaCodecList
import android.media.MediaFormat
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant.INPUT_BUFFER_ERROR
import com.tencentcs.iotvideo.restreamer.ByteStreamServer
import com.tencentcs.iotvideo.restreamer.interfaces.IRendererCallback
import com.tencentcs.iotvideo.restreamer.interfaces.IRestreamingVideoDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class ImageServerVideoStreamDecoder (private val socketHandler: ByteStreamServer, private val onFrameCallback: IRendererCallback, baseContext : Context) :
    IRestreamingVideoDecoder {
        
    private val TAG = ImageServerVideoStreamDecoder::class.simpleName
    private val _initialized = AtomicBoolean(false)
    override var initialized: Boolean // latched to true
        get() = _initialized.get()
        set(value) = if(_initialized.get()) {} else _initialized.set(value)
    
    private var codec : MediaCodec? = null
    private var mediaFormat : MediaFormat? = null

    private var converter : ImageConverter = ImageConverter(baseContext)

    init {
        // set the MJPEG header
        socketHandler.setHeader(Header)
        // track FPS for toString
        converter.logFps = true
    }


    private var watchdog : Thread? = Thread {
        while(!Thread.currentThread().isInterrupted)
        {
            try {
                Thread.sleep(60_000)
            } catch (e: InterruptedException) {
                return@Thread
            }
            if(frames == previousFrames)
            {
                onFrameCallback.onFault()
            } else {
                previousFrames = frames
            }

        }

    }

    @SuppressLint("WrongConstant")
    override fun init(aVHeader: AVHeader?) {
        val videoMime = MediaConstant.getVideoMimeByAVHeader(aVHeader)
        codec = MediaCodec.createDecoderByType(videoMime)
        val imageWidth = aVHeader!!.getInteger("width", 0)
        val imageHeight = aVHeader.getInteger("height", 0)
        val frameRate = aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 20)

        val incomingFormat = MediaFormat.createVideoFormat(videoMime, imageWidth, imageHeight)
        incomingFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        incomingFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2 ) // default: suggested I-frame interval for webcams and h264
        incomingFormat.setInteger("color-format", COLOR_FormatYUV420Flexible)

        // clamp the bitrate https://stackoverflow.com/questions/26110337/what-are-valid-bit-rates-to-set-for-mediacodec
        var bitRate: Int = aVHeader.getInteger(AVHeader.KEY_BIT_RATE, (4.8*imageHeight*imageWidth).toInt())
        val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in mcl.codecInfos) {
            if (!info.isEncoder) {
                continue
            }
            try {
                val caps = info.getCapabilitiesForType(videoMime)
                if (caps != null && caps.isFormatSupported(incomingFormat)) {
                    bitRate = caps.videoCapabilities.bitrateRange.clamp(bitRate)
                    incomingFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                    break
                }
            } catch (e: IllegalArgumentException) {
                // type is not supported
            }
        }

        // Configure the codec to decode only, we are passing the image to the VideoSource which will pass it to the surface to encode
        codec?.configure(incomingFormat, null, null, 8)
        mediaFormat = codec?.outputFormat
        codec?.start()

        watchdog?.start()
    }

    var previousFrames = 0L
    var frames = 0L

    override fun receive_frame(aVData: AVData?): Int {
        var bufferInfo: MediaCodec.BufferInfo? = null
        var dequeueStatus = 0
        try {
            bufferInfo = MediaCodec.BufferInfo()
            dequeueStatus = codec!!.dequeueOutputBuffer(bufferInfo, 20000L)
        }
        catch (_ : IOException)
        {
            close()
        }
        catch (exception: Exception)
        {
            LogUtils.e(TAG, "receive_frame exception:" + exception.message)
        }

        if (dequeueStatus < 0) {
            if (dequeueStatus != INFO_TRY_AGAIN_LATER && dequeueStatus == INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = codec!!.outputFormat
            }
            return MediaConstant.SEND_PACKET_ERROR
        }

        val outputImage = codec!!.getOutputImage(dequeueStatus)

        if (outputImage == null) {
            LogUtils.e(TAG, "receive_frame error, image is null")
            return MediaConstant.SEND_PACKET_ERROR
        }

        try {
            // flush the image to the surface to encode
            val bitmap = converter.convert(outputImage)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // mjpeg "frame"
            socketHandler.sendBytes(IntraPictureSeperator)
            socketHandler.sendBytes(byteArray)
            socketHandler.sendBytes("\r\n".toByteArray())

        }
        catch (e: Exception) {
            LogUtils.e(
                TAG,
                "receive_frame exception:" + e.message
            )
        }

        val rtpTimestamp = bufferInfo!!.presentationTimeUs
        aVData!!.pts = rtpTimestamp
        aVData.dts = rtpTimestamp
        aVData.keyFrame = bufferInfo.flags and 1

        codec?.releaseOutputBuffer(dequeueStatus, false)

        onFrameCallback.onFrame()
        frames++

        return 0
    }

    fun close() {

        LogUtils.i(TAG, "Server closed")
    }

    override fun release() {
        // if we are initialized then this is a real release
        release(initialized)
    }

    fun release(force: Boolean)
    {
        if(!initialized && !force)
            return

        close()
        watchdog?.interrupt()
        watchdog?.join()
        watchdog = null

        LogUtils.i(TAG, "release")
        codec?.stop()
        codec?.release()
    }

    fun finalize() {
        release()
    }

    override fun send_packet(aVData: AVData?): Int {
        if(aVData == null)
            return MediaConstant.SEND_PACKET_ERROR

        try {
            val mediaCodec = codec
            if (mediaCodec == null) {
                LogUtils.e(TAG, "send_packet error:codec is null")
                return MediaConstant.SEND_PACKET_ERROR
            }
            val dequeueInputBuffer = mediaCodec.dequeueInputBuffer(20000L)
            if (dequeueInputBuffer >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(dequeueInputBuffer)
                if (inputBuffer == null) {
                    LogUtils.e(TAG, "send_packet error, inputBuffer is null")
                    return MediaConstant.SEND_PACKET_ERROR
                }
                inputBuffer.clear()
                inputBuffer.put(aVData.data)
                mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, aVData.size, aVData.pts, 0)

                return 0
            }
            LogUtils.e(
                TAG,
                "send_packet error, try again later " + aVData.pts
            )
            return INPUT_BUFFER_ERROR
        } catch (sendingException: java.lang.Exception) {
            LogUtils.e(TAG, "send_packet exception:" +sendingException::class.simpleName + sendingException.message + sendingException.stackTraceToString())
            return MediaConstant.SEND_PACKET_ERROR
        }
    }

    override fun toString(): String {
        return "ImageServerVideoStreamDecoder{FPS: ${converter.lastFps} Socket: ${socketHandler}}"
    }

    companion object {
        const val MJPEGBoundary = "--frameboundary--"
        val IntraPictureSeperator = "${Companion.MJPEGBoundary}\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray()
        val Header =  "HTTP/1.1 200 OK\r\nContent-Type: multipart/x-mixed-replace; boundary=${Companion.MJPEGBoundary}\r\n\r\n".toByteArray()
    }

}