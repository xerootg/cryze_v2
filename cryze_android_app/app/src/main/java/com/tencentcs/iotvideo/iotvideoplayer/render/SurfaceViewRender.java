package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class SurfaceViewRender extends AIoTBaseRender implements IIoTGLRender {
    private static final String TAG = "SurfaceViewRender";
    private EGLProcessor mEGLProcessor;
    private final IoTSVRenderThread mRenderThread;
    private final SurfaceView mSurfaceView;
    private Surface renderSurface;

    public SurfaceViewRender(SurfaceView surfaceView, DisplayMetrics displayMetrics) {
        super(displayMetrics);
        this.mEGLProcessor = null;
        this.mSurfaceView = surfaceView;
        IoTSVRenderThread ioTSVRenderThread = new IoTSVRenderThread();
        this.mRenderThread = ioTSVRenderThread;
        ioTSVRenderThread.setIoTGLRender(this);
        surfaceView.getHolder().addCallback(ioTSVRenderThread);
    }

    private void prepareEGL(Surface surface) {
        if (this.mEGLProcessor == null) {
            this.mEGLProcessor = new EGLProcessor(surface);
        }
        this.mEGLProcessor.prepareEGL();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTSurfaceCreated(Surface surface) {
        LogUtils.i(TAG, "onIoTSurfaceCreated");
        this.renderSurface = surface;
        prepareEGL(surface);
        onSurfaceCreated(this.mSurfaceView.getContext());
        this.mRenderThread.start();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTSurfaceDestroy() {
        LogUtils.i(TAG, "onIoTSurfaceDestroy");
        EGLProcessor eGLProcessor = this.mEGLProcessor;
        if (eGLProcessor != null) {
            eGLProcessor.destroyEGL();
            this.mEGLProcessor = null;
        }
        IoTSVRenderThread ioTSVRenderThread = this.mRenderThread;
        if (ioTSVRenderThread != null) {
            ioTSVRenderThread.release();
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTSurfaceSizeChanged(int i10, int i11) {
        LogUtils.i(TAG, "onIoTSurfaceSizeChanged");
        onSurfaceChanged(i10, i11);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTUpdateFrame() {
        EGLProcessor eGLProcessor;
        if (renderFrame() && (eGLProcessor = this.mEGLProcessor) != null) {
            eGLProcessor.swapBuffers();
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender
    public void queueEvent(Runnable runnable) {
        IoTSVRenderThread ioTSVRenderThread = this.mRenderThread;
        if (ioTSVRenderThread != null) {
            ioTSVRenderThread.queueEvent(runnable);
        }
    }
}
