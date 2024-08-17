package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.graphics.SurfaceTexture;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.TextureView;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class TextureViewRender extends AIoTBaseRender implements IIoTGLRender, TextureView.SurfaceTextureListener {
    private static final String TAG = "TextureViewRender";
    private EGLProcessor mEGLProcessor;
    private IoTTVRenderThread mRenderThread;
    private final TextureView mTextureView;
    private Surface renderSurface;

    public TextureViewRender(TextureView textureView, DisplayMetrics displayMetrics) {
        super(displayMetrics);
        this.mEGLProcessor = null;
        this.mTextureView = textureView;
        textureView.setSurfaceTextureListener(this);
        IoTTVRenderThread ioTTVRenderThread = new IoTTVRenderThread();
        this.mRenderThread = ioTTVRenderThread;
        ioTTVRenderThread.setIoTGLRender(this);
        refreshSurfaceSize();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareEGL(Surface surface) {
        if (this.mEGLProcessor == null) {
            this.mEGLProcessor = new EGLProcessor(surface);
        }
        this.mEGLProcessor.prepareEGL();
    }

    private void refreshSurfaceSize() {
        TextureView textureView = this.mTextureView;
        if (textureView != null) {
            textureView.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.TextureViewRender.1
                @Override // java.lang.Runnable
                public void run() {
                    LogUtils.i(TextureViewRender.TAG, "TextureViewRender textureView width:" + TextureViewRender.this.mTextureView.getWidth() + "; height:" + TextureViewRender.this.mTextureView.getHeight());
                    TextureViewRender.this.queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.TextureViewRender.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            TextureViewRender textureViewRender = TextureViewRender.this;
                            textureViewRender.onSurfaceChanged(textureViewRender.mTextureView.getWidth(), TextureViewRender.this.mTextureView.getHeight());
                        }
                    });
                }
            });
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTSurfaceCreated(Surface surface) {
        LogUtils.i(TAG, "onIoTSurfaceCreated");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTSurfaceDestroy() {
        LogUtils.i(TAG, "onIoTSurfaceDestroy");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTSurfaceSizeChanged(int i10, int i11) {
        String k10 = "onIoTSurfaceSizeChanged width:"+ i10+ "; height:"+ i11+ "; viewSize:"+this.mTextureView.getWidth() + "; viewHeight:" + this.mTextureView.getHeight();
        LogUtils.i(TAG, k10);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IIoTGLRender
    public void onIoTUpdateFrame() {
        EGLProcessor eGLProcessor;
        if (renderFrame() && (eGLProcessor = this.mEGLProcessor) != null) {
            eGLProcessor.swapBuffers();
        }
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, int i10, int i11) {
        LogUtils.i(TAG, "onSurfaceTextureAvailable width:" + i10 + " height:" + i11);
        if (this.mRenderThread == null) {
            IoTTVRenderThread ioTTVRenderThread = new IoTTVRenderThread();
            this.mRenderThread = ioTTVRenderThread;
            ioTTVRenderThread.setIoTGLRender(this);
            refreshSurfaceSize();
        }
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.TextureViewRender.2
            @Override // java.lang.Runnable
            public void run() {
                TextureViewRender.this.renderSurface = new Surface(surfaceTexture);
                TextureViewRender textureViewRender = TextureViewRender.this;
                textureViewRender.prepareEGL(textureViewRender.renderSurface);
                TextureViewRender textureViewRender2 = TextureViewRender.this;
                textureViewRender2.onSurfaceCreated(textureViewRender2.mTextureView.getContext());
                TextureViewRender.this.mRenderThread.start();
            }
        });
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        LogUtils.i(TAG, "onSurfaceTextureDestroyed");
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.TextureViewRender.4
            @Override // java.lang.Runnable
            public void run() {
                if (TextureViewRender.this.mEGLProcessor != null) {
                    TextureViewRender.this.mEGLProcessor.destroyEGL();
                    TextureViewRender.this.mEGLProcessor = null;
                }
                if (TextureViewRender.this.mRenderThread != null) {
                    TextureViewRender.this.mRenderThread.release();
                    TextureViewRender.this.mRenderThread.setIoTGLRender(null);
                    TextureViewRender.this.mRenderThread = null;
                }
            }
        });
        return false;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, final int i10, final int i11) {
        LogUtils.i(TAG, "onSurfaceTextureSizeChanged");
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.TextureViewRender.3
            @Override // java.lang.Runnable
            public void run() {
                TextureViewRender.this.onSurfaceChanged(i10, i11);
            }
        });
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender
    public void queueEvent(Runnable runnable) {
        IoTTVRenderThread ioTTVRenderThread = this.mRenderThread;
        if (ioTTVRenderThread != null) {
            ioTTVRenderThread.queueEvent(runnable);
        }
    }
}
