package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import com.tencentcs.iotvideo.iotvideoplayer.IAudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.AudioRender;
import com.tencentcs.iotvideo.iotvideoplayer.render.TextureViewRender;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class IoTTextureVideoView extends TextureView implements IIoTVideoView {
    private static final String TAG = "IoTTextureVideoView";
    public AudioRender mAudioRender;
    public IVideoGestureRender mVideoRenderer;

    public IoTTextureVideoView(Context context) {
        this(context, null);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public IAudioRender getAudioRender() {
        return this.mAudioRender;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public IVideoGestureRender getVideoRender() {
        return this.mVideoRenderer;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public void onPause() {
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoView
    public void onResume() {
    }

    public IoTTextureVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LogUtils.i(TAG, "execute constructor, hashCode:" + hashCode());
        this.mAudioRender = new AudioRender();
        this.mVideoRenderer = new TextureViewRender(this, getResources().getDisplayMetrics());
    }
}
