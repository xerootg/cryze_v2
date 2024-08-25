package com.tencentcs.iotvideo.iotvideoplayer.codec

import com.tencentcs.iotvideo.utils.LogUtils

class AVHeader() {

    // Lets us make a copy of this object.
    constructor(aVHeader: AVHeader?) : this() {
        this.copy(aVHeader)
    }

    private var map = HashMap<String?, Any?>()

    private fun getInteger(str: String): Int? {
        return if(containsKey(str)) map[str] as Int else null
    }

    fun containsKey(str: String?): Boolean {
        return map.containsKey(str)
    }

    fun copy(aVHeader: AVHeader?) {
        if (aVHeader == null) {
            LogUtils.w(TAG, "copy AVHeader failed: aVHeader is null")
            return
        }
        for ((key, value) in aVHeader.map) {
            if (value != null) {
                if (value is Int) {
                    setInteger(key, (value as Int?)!!)
                } else if (value is String) {
                    setString(key, value as String?)
                } else if (value is VideoRenderInfo) {
                    setVideoRenderInfo(value.copy())
                }
            }
        }
    }

    fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other != null && javaClass == other.javaClass) {
            return this.map == (other as AVHeader).map
        }
        return false
    }

    private fun getString(key: String?): String? {
        return map[key] as String?
    }


    private fun setInteger(key: String?, value: Int) {
        map[key] = value
    }

    private fun setString(key: String?, value: String?) {
        map[key] = value
    }

    private fun setVideoRenderInfo(videoRenderInfo: VideoRenderInfo) {
        map[KEY_VIDEO_RENDER_INFO] = videoRenderInfo
    }

    @SuppressWarnings("unused") // JNI
    fun getVideoRenderInfo(): VideoRenderInfo? {
        return try {
            map[KEY_VIDEO_RENDER_INFO] as VideoRenderInfo?
        } catch (unused: Exception) {
            null
        }
    }

    override fun toString(): String {
        return "AVHeader{map=" + this.map + '}'
    }

    fun getInteger(key: String, defaultValue: Int): Int {
        return getInteger(key)?: defaultValue
    }

    fun getString(str: String?, str2: String): String {
        val string = getString(str)
        return string ?: str2
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    companion object {
        const val KEY_AUDIO_BIT_WIDTH: String = "audio-bit-width"
        const val KEY_AUDIO_CODEC_OPTION: String = "audio-codec-option"
        const val KEY_AUDIO_ENCODER_LIB_ID: String = "audio-encoder-lib-id"
        const val KEY_AUDIO_MODE: String = "audio-mode"
        const val KEY_AUDIO_SAMPLE_NUM_PERFRAME: String = "audio-sample-num-perframe"
        const val KEY_AUDIO_SAMPLE_RATE: String = "audio-sample-rate"
        const val KEY_AUDIO_TYPE: String = "audio-type"
        const val KEY_BIT_RATE: String = "bitrate"
        const val KEY_FRAME_RATE: String = "frame-rate"
        const val KEY_HEIGHT: String = "height"
        const val KEY_PLAYBACK_SPEED: String = "playback-speed"
        const val KEY_VIDEO_RENDER_INFO: String = "videoRenderInfo"
        const val KEY_VIDEO_TYPE: String = "video-type"
        const val KEY_WIDTH: String = "width"
        private const val TAG = "AVHeader"
    }
}
