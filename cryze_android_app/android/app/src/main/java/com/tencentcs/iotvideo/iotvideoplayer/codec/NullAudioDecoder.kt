package com.tencentcs.iotvideo.iotvideoplayer.codec

import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.tencentcs.iotvideo.utils.LogUtils

class NullAudioDecoder : IAudioDecoder {
    override fun init(aVHeader: AVHeader?) {
        LogUtils.i("NullAudioDecoder","Audio header mime: " + MediaConstant.getAudioMimeByAVHeader(aVHeader))
    }

    override fun receive_frame(aVData: AVData?): Int {
        return 0
    }

    override fun release() {
    }

    override fun send_packet(aVData: AVData?): Int {
        return 0
    }

}