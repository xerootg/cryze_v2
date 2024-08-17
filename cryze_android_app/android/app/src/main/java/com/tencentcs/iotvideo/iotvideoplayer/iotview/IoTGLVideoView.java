package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.AudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class IoTGLVideoView extends GLSurfaceView implements IIoTVideoView {
    private static final String TAG = "IoTGLVideoView";
    public AudioRender mAudioRender;
    public GLRenderer mGLRenderer;

    public IoTGLVideoView(Context context) {
        this(context, null);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public IAudioRender getAudioRender() {
        return this.mAudioRender;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public IVideoGestureRender getVideoRender() {
        return this.mGLRenderer;
    }

    @Override // android.opengl.GLSurfaceView, android.view.SurfaceView, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtils.d(TAG, "onAttachedToWindow");
        setRenderMode(0);
    }

    @Override // android.opengl.GLSurfaceView, com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public void onPause() {
        super.onPause();
        LogUtils.i(TAG, "onPause");
        GLRenderer gLRenderer = this.mGLRenderer;
        if (gLRenderer != null) {
            gLRenderer.onPause();
        }
    }

    @Override // android.opengl.GLSurfaceView, com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public void onResume() {
        super.onResume();
        LogUtils.i(TAG, "onResume");
        GLRenderer gLRenderer = this.mGLRenderer;
        if (gLRenderer != null) {
            gLRenderer.onResume();
        }
    }

    public IoTGLVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setEGLContextClientVersion(2);
        this.mAudioRender = new AudioRender();
        GLRenderer gLRenderer = new GLRenderer(this, getResources().getDisplayMetrics());
        this.mGLRenderer = gLRenderer;
        setRenderer(gLRenderer);
    }
}
