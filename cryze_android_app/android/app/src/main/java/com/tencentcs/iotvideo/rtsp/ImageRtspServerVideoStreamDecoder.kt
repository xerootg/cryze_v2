package com.tencentcs.iotvideo.rtsp

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaCodec
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.MediaCodec.INFO_TRY_AGAIN_LATER
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
import android.media.MediaCodecList
import android.media.MediaFormat
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.library.util.sources.audio.AudioSource
import com.pedro.library.util.sources.audio.NoAudioSource
import com.pedro.rtspserver.RtspServerStream
import com.pedro.rtspserver.server.ClientListener
import com.pedro.rtspserver.server.ServerClient
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant.INPUT_BUFFER_ERROR
import com.tencentcs.iotvideo.utils.LogUtils
import java.io.IOException


class ImageRtspServerVideoStreamDecoder (private val rtspPort : Int, private val onFrameCallback: IOnFrameCallback, private val baseContext : Activity) : ConnectChecker,
    ClientListener, IVideoDecoder {
    
    private var codec : MediaCodec? = null
    private var mediaFormat : MediaFormat? = null

    private var videoSource : ImageVideoSource = ImageVideoSource(baseContext)
    private var audioSource: AudioSource = NoAudioSource()
    private var rtspServerStream : RtspServerStream = RtspServerStream(baseContext, rtspPort, this, videoSource, audioSource)

    private var callback: ConnectChecker? = null
    fun setCallback(connectChecker: ConnectChecker?) {
        callback = connectChecker
    }

    @SuppressLint("WrongConstant")
    override fun init(aVHeader: AVHeader?) {
        val videoMime = MediaConstant.getVideoMimeByAVHeader(aVHeader)
        codec = MediaCodec.createDecoderByType(videoMime)
        val imageWidth = aVHeader!!.getInteger("width", 0)
        val imageHeight = aVHeader.getInteger("height", 0)
        val frameRate = aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 20)

        val videoFormat = MediaFormat.createVideoFormat(videoMime, imageWidth, imageHeight)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2 ) // default: suggested I-frame interval for webcams and h264
        videoFormat.setInteger("color-format", COLOR_FormatYUV420Flexible)

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
        
        if(!rtspServerStream.prepareVideo(imageWidth, imageHeight, videoFormat.getInteger(MediaFormat.KEY_BIT_RATE), frameRate, rotation = 90)) throw IOException("Error preparing video stream")

        if(!rtspServerStream.prepareAudio(44100, false, 256)) throw IOException("Error preparing audio stream")

        // bind so we can listen for clients
        rtspServerStream.getStreamClient().setClientListener(this)
        rtspServerStream.getStreamClient().setOnlyVideo(true)
        rtspServerStream.getStreamClient().setLogs(false) // its really noisy IRL
        rtspServerStream.setVideoCodec(VideoCodec.H264)

        // Configure the codec to decode only, we are passing the image to the VideoSource which will pass it to the surface to encode
        codec?.configure(videoFormat, null, null, 8);
        mediaFormat = codec?.outputFormat
        codec?.start()

        rtspServerStream.startStream("")
    }

    fun stopStream() {
        rtspServerStream.stopStream()
    }

    override fun receive_frame(aVData: AVData?): Int {
        var bufferInfo: MediaCodec.BufferInfo? = null
        var dequeueStatus = 0
        try {
            bufferInfo = MediaCodec.BufferInfo()
            dequeueStatus = codec!!.dequeueOutputBuffer(bufferInfo, 20000L)
        } catch (exception: Exception) {
            LogUtils.e(ImageRtspServerVideoStreamDecoder::class.simpleName, "receive_frame exception:" + exception.message)
        }
        if (dequeueStatus < 0) {
            if (dequeueStatus != INFO_TRY_AGAIN_LATER && dequeueStatus == INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = codec!!.outputFormat
            }
            return MediaConstant.SEND_PACKET_ERROR
        }

        val outputImage = codec!!.getOutputImage(dequeueStatus)

        if (outputImage == null) {
            LogUtils.e(ImageRtspServerVideoStreamDecoder::class.simpleName, "receive_frame error, image is null")
            return MediaConstant.SEND_PACKET_ERROR
        }

        // flush the image to the surface to encode
        videoSource.setFrame(outputImage)

        val rtpTimestamp = bufferInfo!!.presentationTimeUs
        aVData!!.pts = rtpTimestamp
        aVData.dts = rtpTimestamp
        aVData.keyFrame = bufferInfo.flags and 1

        codec?.releaseOutputBuffer(dequeueStatus, false)

        onFrameCallback.onFrame()

        return 0
    }

    override fun release() {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "release")
        codec?.stop()
        codec?.release()
        stopStream()
        rtspServerStream.release()
    }

    override fun send_packet(aVData: AVData?): Int {
        if(aVData == null)
            return MediaConstant.SEND_PACKET_ERROR

        if(rtspServerStream.isStreaming)
        {
//            rtspServerStream.getStreamClient().
        }

        try {
            val mediaCodec = codec
            if (mediaCodec == null) {
                LogUtils.e(ImageRtspServerVideoStreamDecoder::class.simpleName, "send_packet error:codec is null")
                return MediaConstant.SEND_PACKET_ERROR
            }
            val dequeueInputBuffer = mediaCodec.dequeueInputBuffer(20000L)
            if (dequeueInputBuffer >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(dequeueInputBuffer)
                if (inputBuffer == null) {
                    LogUtils.e(ImageRtspServerVideoStreamDecoder::class.simpleName, "send_packet error, inputBuffer is null")
                    return MediaConstant.SEND_PACKET_ERROR
                }
                inputBuffer.clear()
                inputBuffer.put(aVData.data)
                mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, aVData.size, aVData.pts, 0)

                return 0
            }
            LogUtils.e(
                ImageRtspServerVideoStreamDecoder::class.simpleName,
                "send_packet error, try again later " + aVData.pts
            )
            return INPUT_BUFFER_ERROR
        } catch (sendingException: java.lang.Exception) {
            LogUtils.e(ImageRtspServerVideoStreamDecoder::class.simpleName, "send_packet exception:" +sendingException::class.simpleName + sendingException.message + sendingException.stackTraceToString())
            return MediaConstant.SEND_PACKET_ERROR
        }
    }

    // ConnectChecker implementation
    override fun onConnectionStarted(url: String) {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onConnectionStarted")
        callback?.onConnectionStarted(url)
    }

    override fun onConnectionSuccess() {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onConnectionSuccess")
        callback?.onConnectionSuccess()
        // get a keyframe
        rtspServerStream.requestKeyframe()
    }

    override fun onNewBitrate(bitrate: Long) {
        if(bitrate == 0L){
            rtspServerStream.requestKeyframe()
            LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "requestKeyframe")
        }
        callback?.onNewBitrate(bitrate)
    }

    override fun onConnectionFailed(reason: String) {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onConnectionFailed $reason")
        callback?.onConnectionFailed(reason)
    }

    override fun onDisconnect() {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onDisconnect")
        callback?.onDisconnect()
    }

    override fun onAuthError() {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onAuthError")
        callback?.onAuthError()
    }

    override fun onAuthSuccess() {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onAuthSuccess")
        callback?.onAuthSuccess()
    }

    override fun onClientConnected(client: ServerClient) {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onClientConnected")
        // request keyframe on client connect so they might sync faster
        rtspServerStream.requestKeyframe()
    }

    override fun onClientDisconnected(client: ServerClient) {
        LogUtils.i(ImageRtspServerVideoStreamDecoder::class.simpleName, "onClientDisconnected")
    }

}