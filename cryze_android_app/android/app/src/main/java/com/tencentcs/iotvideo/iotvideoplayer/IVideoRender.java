package com.tencentcs.iotvideo.iotvideoplayer;

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
/* loaded from: classes2.dex */
public interface IVideoRender {
    void onFrameUpdate(AVData aVData);

    void onInit(AVHeader aVHeader);

    void onPause();

    void onRelease();

    void onResume();
}
