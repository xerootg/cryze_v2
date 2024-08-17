package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.view.SurfaceHolder;
/* loaded from: classes2.dex */
public class IoTSVRenderThread extends GLRenderThread implements SurfaceHolder.Callback {
    @Override // android.view.SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i10, final int i11, final int i12) {
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.IoTSVRenderThread.2
            @Override // java.lang.Runnable
            public void run() {
                IIoTGLRender iIoTGLRender = IoTSVRenderThread.this.mIoTGLRender;
                if (iIoTGLRender != null) {
                    iIoTGLRender.onIoTSurfaceSizeChanged(i11, i12);
                }
            }
        });
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.IoTSVRenderThread.1
            @Override // java.lang.Runnable
            public void run() {
                IIoTGLRender iIoTGLRender = IoTSVRenderThread.this.mIoTGLRender;
                if (iIoTGLRender != null) {
                    iIoTGLRender.onIoTSurfaceCreated(surfaceHolder.getSurface());
                }
            }
        });
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.IoTSVRenderThread.3
            @Override // java.lang.Runnable
            public void run() {
                IIoTGLRender iIoTGLRender = IoTSVRenderThread.this.mIoTGLRender;
                if (iIoTGLRender != null) {
                    iIoTGLRender.onIoTSurfaceDestroy();
                }
            }
        });
    }
}
