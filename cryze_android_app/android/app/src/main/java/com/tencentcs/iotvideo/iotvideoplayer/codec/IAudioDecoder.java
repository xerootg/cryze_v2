package com.tencentcs.iotvideo.iotvideoplayer.codec;
public interface IAudioDecoder {
    void init(AVHeader aVHeader);

    int receive_frame(AVData aVData);

    void release();

    int send_packet(AVData aVData);
}
