package com.tencentcs.iotvideo.rtsp

import android.media.MediaCodec
import android.media.MediaFormat
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVConstants
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.tencentcs.iotvideo.utils.LogUtils

class AudioStreamDecoder(): IAudioDecoder {
    var audioCodec: MediaCodec? = null;

    override fun init(aVHeader: AVHeader?) {
        LogUtils.i( AudioStreamDecoder::class.simpleName, "init, header: ${aVHeader.toString()}")

        // determine the correct decoder based off the header
        val audioMime = MediaConstant.getAudioMineByAVHeader(aVHeader);
        audioCodec = MediaCodec.createDecoderByType(audioMime);

        val sampleRate = aVHeader!!.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 0)
        val audioChanelType = aVHeader!!.getInteger(AVHeader.KEY_AUDIO_MODE, 0)
        // 0 = mono, 1 = stereo, 2 = none
        var channelCount = 0; // AUDIO_SOUND_MODE_NONE
        if (audioChanelType == AVConstants.AUDIO_SOUND_MODE_MONO) {
            channelCount = 1
        } else if (audioChanelType == AVConstants.AUDIO_SOUND_MODE_STEREO) {
            channelCount = 2
        }

        val mediaFormat = MediaFormat.createAudioFormat(audioMime, sampleRate, channelCount);

        audioCodec!!.configure(mediaFormat, null, null, 0);
        audioCodec!!.start();

    }

    override fun receive_frame(aVData: AVData?): Int {
        // eventually, this should take the decoded packed and pass it to the RTSP AudioSource

        return 0
    }

    override fun release() {
        LogUtils.i( ImageRtspServerVideoStreamDecoder::class.simpleName, "release")
        audioCodec?.stop()
        audioCodec?.release()
    }

    override fun send_packet(aVData: AVData?): Int {
        // eventually, this should enqueue the packet for decoding

        return 0
    }
}