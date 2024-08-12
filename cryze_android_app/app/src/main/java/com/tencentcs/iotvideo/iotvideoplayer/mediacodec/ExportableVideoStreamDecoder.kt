package com.tencentcs.iotvideo.iotvideoplayer.mediacodec

import android.app.Activity
import android.media.MediaCodec
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.MediaCodec.INFO_TRY_AGAIN_LATER
import android.media.MediaCodecList
import android.media.MediaFormat
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.library.util.sources.audio.NoAudioSource
import com.pedro.rtspserver.RtspServerStream
import com.pedro.rtspserver.server.ClientListener
import com.pedro.rtspserver.server.ServerClient
import com.tencentcs.iotvideo.custom.WyzeCameraVideoSource
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder
import com.tencentcs.iotvideo.iotvideoplayer.custom.ImageConverter
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant.INPUT_BUFFER_ERROR
import com.tencentcs.iotvideo.utils.LogUtils
import java.io.IOException


class ExportableVideoStreamDecoder (private val rtspPort : Int, private val baseContext : Activity) : ConnectChecker, ClientListener, IVideoDecoder {
    
    private var codec : MediaCodec? = null
    private var mediaFormat : MediaFormat? = null

    private var VideoSource : WyzeCameraVideoSource? = null
    private var RtspServerStream : RtspServerStream? = null

    private var callback: ConnectChecker? = null
    fun setCallback(connectChecker: ConnectChecker?) {
        callback = connectChecker
    }

    override fun init(aVHeader: AVHeader?) {
        val videoMime = MediaConstant.getVideoMimeByAVHeader(aVHeader)
        codec = MediaCodec.createDecoderByType(videoMime)
        val imageWidth = aVHeader!!.getInteger("width", 0)
        val imageHeight = aVHeader.getInteger("height", 0)
        val frameRate = aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 20)

        val videoFormat = MediaFormat.createVideoFormat(videoMime, imageWidth, imageHeight)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20 )
        videoFormat.setInteger("color-format", 19)

        // clamp the bitrate https://stackoverflow.com/questions/26110337/what-are-valid-bit-rates-to-set-for-mediacodec
        var bitRate: Int = aVHeader.getInteger(AVHeader.KEY_BIT_RATE, (4.8*imageHeight*imageWidth).toInt())
        val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in mcl.codecInfos) {
            if (!info.isEncoder) {
                continue
            }
            try {
                val caps = info.getCapabilitiesForType(videoMime)
                if (caps != null && caps.isFormatSupported(videoFormat)) {
                    bitRate = caps.videoCapabilities.bitrateRange.clamp(bitRate)
                    videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                    break
                }
            } catch (e: IllegalArgumentException) {
                // type is not supported
            }
        }

        VideoSource = WyzeCameraVideoSource()
        val audioSource = NoAudioSource()

        RtspServerStream = RtspServerStream(baseContext, rtspPort, this, VideoSource!!, audioSource)
        if(!RtspServerStream!!.prepareVideo(imageWidth, imageHeight, videoFormat.getInteger(MediaFormat.KEY_BIT_RATE), frameRate)) throw IOException("Error preparing video stream")

        if(!RtspServerStream!!.prepareAudio(44100, false, 256)) throw IOException("Error preparing audio stream")

        // bind so we can listen for clients
        RtspServerStream!!.getStreamClient().setClientListener(this)
        RtspServerStream!!.getStreamClient().setOnlyVideo(true)
        RtspServerStream!!.setVideoCodec(VideoCodec.H264)
        RtspServerStream!!.getStreamClient();

        // Finally, start the server
        RtspServerStream!!.startStream()

        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "RTSP listening on ${RtspServerStream!!.getStreamClient().getEndPointConnection()}")

        codec?.configure(videoFormat, null, null, 0);
        mediaFormat = codec?.outputFormat
        codec?.start()

        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "init codec is $videoFormat")

    }

    override fun receive_frame(aVData: AVData?): Int {
        var bufferInfo: MediaCodec.BufferInfo? = null
        var dequeueStatus = 0
        try {
            bufferInfo = MediaCodec.BufferInfo()
            dequeueStatus = codec!!.dequeueOutputBuffer(bufferInfo, 20000L)
        } catch (exception: Exception) {
            LogUtils.e(ExportableVideoStreamDecoder::class.simpleName, "receive_frame exception:" + exception.message)
        }
        if (dequeueStatus < 0) {
            if (dequeueStatus != INFO_TRY_AGAIN_LATER && dequeueStatus == INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = codec!!.outputFormat
            }
            return MediaConstant.SEND_PACKET_ERROR
        }

        val outputImage = codec!!.getOutputImage(dequeueStatus)

        if (outputImage == null) {
            LogUtils.e(ExportableVideoStreamDecoder::class.simpleName, "receive_frame error, image is null")
            return MediaConstant.SEND_PACKET_ERROR
        }

        // write the frame
        val canvas = VideoSource?.surfaceEncoder?.lockCanvas(null)
        val bitmap = ImageConverter(baseContext).convert(outputImage, 0)
        // image is data, data1, and data2. this is the image decomposed into
        canvas?.drawBitmap(bitmap, 0f, 0f, null)
        VideoSource?.surfaceEncoder?.unlockCanvasAndPost(canvas)

        val rtpTimestamp = bufferInfo!!.presentationTimeUs
        aVData!!.pts = rtpTimestamp
        aVData.dts = rtpTimestamp
        aVData.keyFrame = bufferInfo.flags and 1

        codec?.releaseOutputBuffer(dequeueStatus, false)

//        val params = Bundle()
//        params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, aVData!!.keyFrame)
//        codec?.setParameters(params)
        return 0
    }

    override fun release() {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "release")
        codec?.stop()
        codec?.release()
        RtspServerStream?.release()
    }

    override fun send_packet(aVData: AVData?): Int {
        if(aVData == null)
            return MediaConstant.SEND_PACKET_ERROR

        try {
            val mediaCodec = codec
            if (mediaCodec == null) {
                LogUtils.e(ExportableVideoStreamDecoder::class.simpleName, "send_packet error:codec is null")
                return MediaConstant.SEND_PACKET_ERROR
            }
            val dequeueInputBuffer = mediaCodec.dequeueInputBuffer(20000L)
            if (dequeueInputBuffer >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(dequeueInputBuffer)
                if (inputBuffer == null) {
                    LogUtils.e(ExportableVideoStreamDecoder::class.simpleName, "send_packet error, inputBuffer is null")
                    return MediaConstant.SEND_PACKET_ERROR
                }
                inputBuffer.clear()
                inputBuffer.put(aVData.data)
                mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, aVData.size, aVData.pts, 0)

                return 0
            }
            LogUtils.e(
                ExportableVideoStreamDecoder::class.simpleName,
                "send_packet error, try again later " + aVData.pts
            )
            return INPUT_BUFFER_ERROR
        } catch (sendingException: java.lang.Exception) {
            LogUtils.e(ExportableVideoStreamDecoder::class.simpleName, "send_packet exception:" +sendingException::class.simpleName + sendingException.message + sendingException.stackTraceToString())
            return MediaConstant.SEND_PACKET_ERROR
        }
    }

    // ConnectChecker implementation
    override fun onConnectionStarted(url: String) {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onConnectionStarted")
        callback?.onConnectionStarted(url)
        RtspServerStream?.requestKeyframe()
    }

    override fun onConnectionSuccess() {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onConnectionSuccess")
        callback?.onConnectionSuccess()
    }

    override fun onNewBitrate(bitrate: Long) {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onNewBitrate $bitrate")
        callback?.onNewBitrate(bitrate)
    }

    override fun onConnectionFailed(reason: String) {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onConnectionFailed $reason")
        callback?.onConnectionFailed(reason)
    }

    override fun onDisconnect() {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onDisconnect")
        callback?.onDisconnect()
    }

    override fun onAuthError() {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onAuthError")
        callback?.onAuthError()
    }

    override fun onAuthSuccess() {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onAuthSuccess")
        callback?.onAuthSuccess()
    }

    override fun onClientConnected(client: ServerClient) {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onClientConnected")
    }

    override fun onClientDisconnected(client: ServerClient) {
        LogUtils.i(ExportableVideoStreamDecoder::class.simpleName, "onClientDisconnected")
    }

}