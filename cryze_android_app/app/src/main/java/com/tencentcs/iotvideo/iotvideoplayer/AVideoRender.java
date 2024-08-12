package com.tencentcs.iotvideo.iotvideoplayer;

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
/* loaded from: classes2.dex */
public abstract class AVideoRender implements IVideoRender {
    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onFrameUpdate(AVData aVData) {
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onInit(AVHeader aVHeader) {
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onPause() {
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onRelease() {
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onResume() {
    }
}
