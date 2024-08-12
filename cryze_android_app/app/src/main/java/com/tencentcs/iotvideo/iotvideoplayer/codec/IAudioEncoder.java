package com.tencentcs.iotvideo.iotvideoplayer.codec;
/* loaded from: classes2.dex */
public interface IAudioEncoder {
    void init(AVHeader aVHeader);

    int receive_packet(AVData aVData);

    void release();

    int send_frame(AVData aVData);
}
