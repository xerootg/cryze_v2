package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class IoTTVRenderThread extends GLRenderThread implements TextureView.SurfaceTextureListener {
    private static final int DELAY_NOTIFY_SIZE_CHANGE = 20;
    private static final String TAG = "IoTTVRenderThread";
    private int lastWidth;

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, int i10, int i11) {
        LogUtils.i(TAG, "onSurfaceTextureAvailable");
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.IoTTVRenderThread.1
            @Override // java.lang.Runnable
            public void run() {
                IIoTGLRender iIoTGLRender = IoTTVRenderThread.this.mIoTGLRender;
                if (iIoTGLRender != null) {
                    iIoTGLRender.onIoTSurfaceCreated(new Surface(surfaceTexture));
                }
            }
        });
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        LogUtils.i(TAG, "onSurfaceTextureDestroyed");
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.IoTTVRenderThread.3
            @Override // java.lang.Runnable
            public void run() {
                IIoTGLRender iIoTGLRender = IoTTVRenderThread.this.mIoTGLRender;
                if (iIoTGLRender != null) {
                    iIoTGLRender.onIoTSurfaceDestroy();
                }
            }
        });
        return false;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, final int i10, final int i11) {
        int i12;
        LogUtils.d(TAG, "onSurfaceTextureSizeChanged");
        Runnable runnable = new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.IoTTVRenderThread.2
            @Override // java.lang.Runnable
            public void run() {
                IIoTGLRender iIoTGLRender = IoTTVRenderThread.this.mIoTGLRender;
                if (iIoTGLRender != null) {
                    iIoTGLRender.onIoTSurfaceSizeChanged(i10, i11);
                }
            }
        };
        int i13 = this.lastWidth;
        if (i13 != 0 && i10 <= i13) {
            i12 = 20;
        } else {
            i12 = 0;
        }
        queueEvent(runnable, i12);
        this.lastWidth = i10;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}
