package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.view.Surface;
/* loaded from: classes2.dex */
public interface IIoTGLRender {
    void onIoTSurfaceCreated(Surface surface);

    void onIoTSurfaceDestroy();

    void onIoTSurfaceSizeChanged(int i10, int i11);

    void onIoTUpdateFrame();
}
