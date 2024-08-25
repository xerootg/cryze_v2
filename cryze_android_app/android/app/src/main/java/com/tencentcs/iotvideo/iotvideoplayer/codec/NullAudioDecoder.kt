package com.tencentcs.iotvideo.iotvideoplayer.codec

class NullAudioDecoder : IAudioDecoder {
    override fun init(aVHeader: AVHeader?) {
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