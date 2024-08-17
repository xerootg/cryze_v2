package com.tencentcs.iotvideo.iotvideoplayer;

import android.content.Context;
import android.util.AttributeSet;
import com.tencentcs.iotvideo.iotvideoplayer.render.AudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer;
import com.tencentcs.iotvideo.iotvideoplayer.render.GestureGLSurfaceView;
/* loaded from: classes2.dex */
public class IoTVideoView extends GestureGLSurfaceView {
    private static final String TAG = "IotVideoView";
    public AudioRender mAudioRender;
    public GLRenderer mGLRenderer;
    private OnZoomListener mOnZoomListener;

    /* loaded from: classes2.dex */
    public interface OnZoomListener {
        void onZoom(float f10, float f11, float f12);
    }

    public IoTVideoView(Context context) {
        this(context, null);
    }

    public float getCurrentScale() {
        return this.mGLRenderer.getScale();
    }

    public GLRenderer getGLRenderer() {
        return this.mGLRenderer;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.GestureGLSurfaceView
    public IoTVideoView getIoTVideoView() {
        return this;
    }

    @Override // android.opengl.GLSurfaceView, android.view.SurfaceView, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setRenderMode(0);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.GestureGLSurfaceView
    public void onMove(float f10, float f11) {
        super.onMove(f10, f11);
        this.mGLRenderer.translateBy(-f10, f11);
    }

    @Override // android.opengl.GLSurfaceView
    public void onPause() {
        super.onPause();
        this.mGLRenderer.onPause();
    }

    @Override // android.opengl.GLSurfaceView
    public void onResume() {
        super.onResume();
        this.mGLRenderer.onResume();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.GestureGLSurfaceView
    public void onZoom(float f10, float f11, float f12) {
        super.onZoom(f10, f11, f12);
        float scaleTo = this.mGLRenderer.scaleTo(f10, f11, f12);
        OnZoomListener onZoomListener = this.mOnZoomListener;
        if (onZoomListener != null) {
            onZoomListener.onZoom(f10, f11, scaleTo);
        }
    }

    public void setFisheye(boolean z10) {
        this.mGLRenderer.setFisheye(z10);
    }

    public void setOnZoomListener(OnZoomListener onZoomListener) {
        this.mOnZoomListener = onZoomListener;
    }

    public boolean updateBGColors(float[] fArr) {
        return this.mGLRenderer.updateBGColors(fArr);
    }

    public void updateRect(float f10, float f11, float f12, float f13) {
    }

    public IoTVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setEGLContextClientVersion(2);
        this.mAudioRender = new AudioRender();
        GLRenderer gLRenderer = new GLRenderer(this, getResources().getDisplayMetrics(), new OnRectChangeListener() { // from class: com.tencentcs.iotvideo.iotvideoplayer.IoTVideoView.1
            @Override // com.tencentcs.iotvideo.iotvideoplayer.OnRectChangeListener
            public void updateRect(float f10, float f11, float f12, float f13) {
                IoTVideoView.this.updateRect(f10, f11, f12, f13);
            }
        });
        this.mGLRenderer = gLRenderer;
        setRenderer(gLRenderer);
    }
}
