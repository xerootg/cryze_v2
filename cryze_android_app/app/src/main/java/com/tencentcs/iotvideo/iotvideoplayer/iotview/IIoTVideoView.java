package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender;
/* loaded from: classes2.dex */
public interface IIoTVideoView {
    IAudioRender getAudioRender();

    IVideoGestureRender getVideoRender();

    void onPause();

    void onResume();
}
