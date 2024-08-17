package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.AudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.SurfaceViewRender;
/* loaded from: classes2.dex */
public class IoTSurfaceVideoView extends SurfaceView implements IIoTVideoView {
    public AudioRender mAudioRender;
    public IVideoGestureRender mGLRenderer;

    public IoTSurfaceVideoView(Context context) {
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

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public void onPause() {
        IVideoGestureRender iVideoGestureRender = this.mGLRenderer;
        if (iVideoGestureRender != null) {
            iVideoGestureRender.onPause();
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public void onResume() {
        IVideoGestureRender iVideoGestureRender = this.mGLRenderer;
        if (iVideoGestureRender != null) {
            iVideoGestureRender.onResume();
        }
    }

    public IoTSurfaceVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAudioRender = new AudioRender();
        this.mGLRenderer = new SurfaceViewRender(this, getResources().getDisplayMetrics());
    }
}
