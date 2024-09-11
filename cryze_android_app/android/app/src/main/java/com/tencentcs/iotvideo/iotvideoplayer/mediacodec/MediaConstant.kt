package com.tencentcs.iotvideo.iotvideoplayer.mediacodec

import android.media.MediaCodec
import androidx.media3.common.MimeTypes
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVConstants
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import java.nio.ByteBuffer

object MediaConstant {
    // used to decode the AVHeader payload values
    @Suppress("unused")
    const val AUDIO_TYPE_G711A: Int = 1
    @Suppress("unused")
    const val AUDIO_TYPE_G711U: Int = 2
    @Suppress("unused")
    const val AUDIO_TYPE_PCM: Int = 0
    @Suppress("unused")
    const val AUDIO_TYPE_PT_AAC: Int = 4
    @Suppress("unused")
    const val AUDIO_TYPE_PT_ADPCMA: Int = 6
    @Suppress("unused")
    const val AUDIO_TYPE_PT_AMR: Int = 5
    @Suppress("unused")
    const val AUDIO_TYPE_PT_G726: Int = 3
    @Suppress("unused")
    const val INPUT_BUFFER_ERROR: Int = -1

    @Suppress("unused")
    const val NAL_UNIT_TYPE_UNSPECIFIED: Int = 0
    @Suppress("unused")
    const val NAL_UNIT_TYPE_NON_IDR: Int = 1
    @Suppress("unused")
    const val NAL_UNIT_TYPE_PARTITION_A: Int = 2
    @Suppress("unused")
    const val NAL_UNIT_TYPE_PARTITION_B: Int = 3
    @Suppress("unused")
    const val NAL_UNIT_TYPE_PARTITION_C: Int = 4
    @Suppress("unused")
    const val NAL_UNIT_TYPE_IDR: Int = 5
    @Suppress("unused")
    const val NAL_UNIT_TYPE_SEI: Int = 6
    @Suppress("unused")
    const val NAL_UNIT_TYPE_SPS: Int = 7
    @Suppress("unused")
    const val NAL_UNIT_TYPE_PPS: Int = 8
    @Suppress("unused")
    const val NAL_UNIT_TYPE_AUD: Int = 9
    @Suppress("unused")
    const val NAL_UNIT_TYPE_END_OF_SEQ: Int = 10
    @Suppress("unused")
    const val NAL_UNIT_TYPE_END_OF_STREAM: Int = 11
    @Suppress("unused")
    const val NAL_UNIT_TYPE_FILLER_DATA: Int = 12
    @Suppress("unused")
    const val NAL_UNIT_TYPE_SPS_EXT: Int = 13
    @Suppress("unused")
    const val NAL_UNIT_TYPE_AUXILIARY_SLICE: Int = 19
    @Suppress("unused")
    const val NAL_UNIT_TYPE_EXTENSION: Int = 20
    @Suppress("unused")
    const val NAL_UNIT_TYPE_PREFIX: Int = 21

    @Suppress("unused")
    const val SEND_PACKET_ERROR: Int = -11
    @Suppress("unused")
    const val VIDEO_TYPE_H264: Int = 1
    @Suppress("unused")
    const val VIDEO_TYPE_H265: Int = 5
    @Suppress("unused")
    const val VIDEO_TYPE_JPEG: Int = 3
    @Suppress("unused")
    const val VIDEO_TYPE_MJPEG: Int = 4
    @Suppress("unused")
    const val VIDEO_TYPE_MPEG4: Int = 2
    @Suppress("unused")
    const val VIDEO_TYPE_NONE: Int = 0

    @Suppress("unused")
    // Get a ByteBuffer containing the AAC Codec Specific Data
    // TODO: Implement this in RemuxerAudioSource for AAC
    fun getAacCsd0(aVHeader: AVHeader): ByteBuffer {
        val sample2MediaCodecIndex = (((if (aVHeader.getInteger(
                AVHeader.KEY_AUDIO_MODE,
                AVConstants.AUDIO_SOUND_MODE_MONO
            ) == AVConstants.AUDIO_SOUND_MODE_MONO
        ) AVConstants.AUDIO_SOUND_MODE_STEREO else AVConstants.AUDIO_SOUND_MODE_NONE) shl 3) or
                ((sample2MediaCodecIndex(
                    aVHeader.getInteger(
                        AVHeader.KEY_AUDIO_SAMPLE_RATE,
                        AVConstants.AUDIO_SAMPLE_RATE_8000
                    )
                ) shl 7) or 4096).toShort()
                    .toInt()).toShort()
        val put = ByteBuffer.allocate(2).put(
            byteArrayOf(
                ((sample2MediaCodecIndex.toInt() shr 8) and 255).toByte(),
                (sample2MediaCodecIndex.toInt() and 255).toByte()
            )
        )
        put.position(0)
        return put
    }

    @Throws(IllegalArgumentException::class)
    fun getAudioMimeByAVHeader(aVHeader: AVHeader?): String {
        if (aVHeader == null) {
            throw IllegalArgumentException("AVHeader is null")
        }
        val integer = aVHeader.getInteger(AVHeader.KEY_AUDIO_TYPE, -1)
        if (integer == AUDIO_TYPE_PT_AAC) {
            return MimeTypes.AUDIO_AAC
        }
        if (integer == AUDIO_TYPE_PT_AMR) {
            return MimeTypes.AUDIO_AMR_NB
        }
        if (integer == AUDIO_TYPE_G711U) return MimeTypes.AUDIO_MLAW
        if (integer == AUDIO_TYPE_G711A) return MimeTypes.AUDIO_ALAW
        throw IllegalArgumentException("not support this media type")
    }

    @Throws(IllegalArgumentException::class)
    fun getVideoMimeByAVHeader(aVHeader: AVHeader?): String {
        if (aVHeader == null) {
            throw IllegalArgumentException("AVHeader is null")
        }
        val integer = aVHeader.getInteger(AVHeader.KEY_VIDEO_TYPE, -1)
        if (integer == VIDEO_TYPE_H264) {
            return MimeTypes.VIDEO_H264
        }
        if (integer == VIDEO_TYPE_H265) {
            return MimeTypes.VIDEO_H265
        }
        throw IllegalArgumentException("not support this media type")
    }




    fun sample2MediaCodecIndex(sampleRate: Int): Int {
        return when (sampleRate) {
            AVConstants.AUDIO_SAMPLE_RATE_8000 -> 11
            AVConstants.AUDIO_SAMPLE_RATE_11025 -> 10
            AVConstants.AUDIO_SAMPLE_RATE_12000 -> 9
            AVConstants.AUDIO_SAMPLE_RATE_16000 -> 8
            AVConstants.AUDIO_SAMPLE_RATE_22050 -> 7
            AVConstants.AUDIO_SAMPLE_RATE_24000 -> 6
            AVConstants.AUDIO_SAMPLE_RATE_32000 -> 5
            AVConstants.AUDIO_SAMPLE_RATE_44100 -> 4
            AVConstants.AUDIO_SAMPLE_RATE_48000 -> 3
            AVConstants.AUDIO_SAMPLE_RATE_64000 -> 2
            AVConstants.AUDIO_SAMPLE_RATE_88200 -> 1
            AVConstants.AUDIO_SAMPLE_RATE_96000 -> 0
            else -> 11
        }
    }

    enum class DecodeState {
        Init,
        Ready,
        Release
    }

    class MediaCodecInputBuffer(
        var inputBuffer: ByteBuffer,
        var size: Int,
        var presentationTimeUs: Long
    )

    class MediaCodecOutputBuffer(
        var outputBuffer: ByteBuffer,
        var bufferId: Int,
        var bufferInfo: MediaCodec.BufferInfo
    )

    enum class MediaType {
        Video,
        Audio,
        Subtitles
    }

    interface OnMediaCodecStateChangedListener {
        fun onInit(mediaType: MediaType?)

        fun onReady(mediaType: MediaType?)

        fun onRelease(mediaType: MediaType?)
    }
}