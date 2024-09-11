package com.github.xerootg.cryze.restreamer.rtspmuxer.sources

import android.media.MediaCodec
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.MimeTypes
import com.pedro.common.AudioCodec
import com.pedro.rtsp.rtsp.RtspClient
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVConstants
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.github.xerootg.cryze.restreamer.interfaces.IRendererCallback
import com.github.xerootg.cryze.restreamer.rtspmuxer.ISourceReadyCallback
import com.github.xerootg.cryze.restreamer.interfaces.IRestreamingAudioDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import java.nio.ByteBuffer

class RemuxerAudioSource(
    private val muxer: RtspClient,
    private val readyCallback: ISourceReadyCallback,
    private val frameCb: IRendererCallback
) : IRestreamingAudioDecoder, IRtspStreamSource {
    override var initialized: Boolean = false

    private var mime = ""
    private var audioMode = 0
    private var samplerate = 0
    private var framesProcessed = 0L

    private val mTaskThread: HandlerThread = HandlerThread("AudioSenderThread")
    private val mTaskTHandler: Handler by lazy { Handler(mTaskThread.looper) }

    override fun init(aVHeader: AVHeader?) {
        if (aVHeader == null) {
            LogUtils.w(TAG, "init: aVHeader is null")
            return
        }
        if(!initialized) {

            LogUtils.i(TAG, "init: header: $aVHeader")

            // some cameras send audio with no audio mode, it's always mono
            // init: header: AVHeader{map={
            // audio-sample-rate=16000,
            // audio-type=2, AUDIO_TYPE_G711U
            // audio-bit-width=1, AUDIO_BIT_WIDTH_16
            // audio-codec-option=0, AAC_OPTION_TYPE_NONE, probably translates to G711U
            // frame-rate=15, - 15 audio frames per second, mirrors the video frame rate
            // audio-mode=0, AUDIO_SOUND_MODE_NONE - actually mono IRL
            // audio-sample-num-perframe=320, Bytes per frame, each byte is a "sample"
            // }}

            // its never none. 0 is mono, 1 is stereo, 2 is mono.
            audioMode = if (aVHeader.getInteger(
                    AVHeader.KEY_AUDIO_MODE,
                    AVConstants.AUDIO_SOUND_MODE_MONO
                ) == 1
            ) {
                AVConstants.AUDIO_SOUND_MODE_STEREO
            } else {
                AVConstants.AUDIO_SOUND_MODE_MONO
            }

            // I've only seen 16k from WYZE cameras, but it may change and we should support it
            // There's discussion (reddit AMAs) that they may change codecs in the future, the hardware certainly supports it
            samplerate =
                aVHeader.getInteger(
                    AVHeader.KEY_AUDIO_SAMPLE_RATE,
                    AVConstants.AUDIO_SAMPLE_RATE_8000
                )

            when (audioMode) {
                AVConstants.AUDIO_SOUND_MODE_MONO -> {
                    // hack, seems like vlc halves the bitrate when mono
                    muxer.setAudioInfo(samplerate, false)
                    LogUtils.i(TAG, "init: mono audio, samplerate: $samplerate")
                }

                AVConstants.AUDIO_SOUND_MODE_STEREO -> {
                    muxer.setAudioInfo(samplerate, true)
                    LogUtils.i(TAG, "init: stereo audio, samplerate: $samplerate")
                }

                else -> {
                    muxer.setOnlyVideo(true)
                    LogUtils.e(TAG, "init: Unsupported audio mode: $audioMode")
                }
            }

            mime = MediaConstant.getAudioMimeByAVHeader(aVHeader)
            if (audioMode != AVConstants.AUDIO_SOUND_MODE_NONE) {
                when (mime) {
                    MimeTypes.AUDIO_AAC -> {
                        muxer.setAudioCodec(AudioCodec.AAC)
                    }

                    MimeTypes.AUDIO_MLAW,
                    MimeTypes.AUDIO_ALAW -> {
                        when (samplerate) {
                            8000 -> muxer.setAudioCodec(AudioCodec.G711)
                            else -> {
                                // Support for G711U and G711A at samplerates outside
                                // of 8k are outside of the RTP spec. This is a custom
                                // dynamic payload type.
                                when (mime) {
                                    MimeTypes.AUDIO_MLAW -> muxer.setAudioCodec(AudioCodec.PCMU)
                                    MimeTypes.AUDIO_ALAW -> muxer.setAudioCodec(AudioCodec.PCMA)
                                }
                            }
                        }
                    }

                    else -> {
                        muxer.setOnlyVideo(true)
                        LogUtils.e(TAG, "init: Unsupported audio type: $mime")
                    }
                }
            }

            initialized = true
            // must call _after_ setting initialized
            readyCallback.sourceReady()

            LogUtils.i(
                TAG,
                "audio init: mime:$mime, channels:${audioMode + 1}, samplerate:$samplerate"
            )
        } else
        {
            LogUtils.w(TAG, "init: Already initialized")
        }
    }

    override fun receive_frame(aVData: AVData?): Int {
        // no idea what format this is in, but i'm going to assume it's not important, send_packet is
        // receive_frame: dataSize:2560, data1Size:0, data2Size:0, pts:0
        // LogUtils.i(TAG, "receive_frame: dataSize:${aVData?.size}, data1Size:${aVData?.size1}, data2Size:${aVData?.size2}, pts:${aVData?.pts}")
        return 0
    }

    override fun release() {
    }

    override fun send_packet(aVData: AVData?): Int {
        if (aVData?.data == null || aVData.pts == 0L) {
            LogUtils.w(TAG, "send_packet: Ignoring null or timeless packet")
            return 0
        }
        // for now, just log what you got and return
        // send_packet: dataSize:320, data1Size:0, data2Size:0, pts:138807921000

        if (postToMuxer) {
            val currentInfo = MediaCodec.BufferInfo()
            currentInfo.presentationTimeUs = aVData.pts
            currentInfo.size = aVData.size

            val bufferContents = ByteBuffer.allocate(aVData.size)
            bufferContents.put(aVData.data)
            bufferContents.flip() // flip the buffer so it's ready to be read

            mTaskTHandler.post {
                // send the audio to the muxer
                if (postToMuxer) {
                    muxer.sendAudio(bufferContents, currentInfo)
                }
            }
        }
        frameCb.onFrame() // should be in video probably
        return 0
    }

    companion object {
        private val TAG = RemuxerAudioSource::class.java.simpleName
    }

    override var postToMuxer = false
    override fun startStream() {
        mTaskThread.start()
        postToMuxer = true
    }

    override fun stopStream() {
        postToMuxer = false
        mTaskThread.quitSafely()
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("mime: $mime\n")
        stringBuilder.append("channels: $audioMode\n")
        stringBuilder.append("samplerate: $samplerate\n")
        stringBuilder.append("initialized: $initialized\n")
        stringBuilder.append("postToMuxer: $postToMuxer\n")
        stringBuilder.append("framesProcessed: $framesProcessed\n")
        return stringBuilder.toString()
    }

}