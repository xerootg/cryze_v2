package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.tencentcs.iotvideo.iotvideoplayer.OnRectChangeListener;
import com.tencentcs.iotvideo.iotvideoplayer.render.IVideoViewFlingProcessor;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class IoTVideoRenderView extends FrameLayout implements IIoTVideoGesture.OnZoomListener, IIoTVideoGesture.OnMoveListener {
    public static final int RENDER_VIEW_GL_SURFACE_VIEW = 1;
    public static final int RENDER_VIEW_SURFACE_VIEW = 0;
    public static final int RENDER_VIEW_TEXTURE_VIEW = 2;
    private static final String TAG = "IoTVideoRenderView";
    private IIoTVideoGesture ioTVideoGesture;
    private int mRenderType;
    IIoTVideoView renderView;

    public IoTVideoRenderView(Context context) {
        this(context, null);
    }

    private void init() {
        IoTVideoGesture ioTVideoGesture = new IoTVideoGesture(getContext());
        this.ioTVideoGesture = ioTVideoGesture;
        ioTVideoGesture.setOnZoomListener(this);
        this.ioTVideoGesture.setOnMoveListener(this);
        setBackgroundColor(0);
    }

    public IIoTVideoView getRenderView() {
        boolean z10;
        StringBuilder sb2 = new StringBuilder("getRenderView render view  is null:");
        if (this.renderView == null) {
            z10 = true;
        } else {
            z10 = false;
        }
        sb2.append(z10);
        LogUtils.d(TAG, sb2.toString());
        if (this.renderView == null) {
            setRenderView(1);
        }
        return this.renderView;
    }

    public float getScale() {
        IIoTVideoView iIoTVideoView = this.renderView;
        if (iIoTVideoView != null && iIoTVideoView.getVideoRender() != null) {
            return this.renderView.getVideoRender().getScale();
        }
        return 1.0f;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture.OnMoveListener
    public void onMove(float f10, float f11) {
        IIoTVideoView iIoTVideoView = this.renderView;
        if (iIoTVideoView != null && iIoTVideoView.getVideoRender() != null) {
            this.renderView.getVideoRender().translateBy(-f10, f11);
        }
    }

    public void onPause() {
        LogUtils.i(TAG, "onPause");
        IIoTVideoView iIoTVideoView = this.renderView;
        if (iIoTVideoView != null) {
            iIoTVideoView.onPause();
        }
    }

    public void onResume() {
        LogUtils.i(TAG, "onResume");
        IIoTVideoView iIoTVideoView = this.renderView;
        if (iIoTVideoView != null) {
            iIoTVideoView.onResume();
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.ioTVideoGesture.onTouchEvent(motionEvent);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture.OnZoomListener
    public void onZoom(float f10, float f11, float f12) {
        IIoTVideoView iIoTVideoView = this.renderView;
        if (iIoTVideoView != null && iIoTVideoView.getVideoRender() != null) {
            this.renderView.getVideoRender().scaleTo(f10, f11, f12);
        }
    }

    public void scaleVideo(float f10, float f11, float f12) {
        onZoom(f10, f11, f12);
    }

    public void setFlingProcessor(IVideoViewFlingProcessor iVideoViewFlingProcessor) {
        this.ioTVideoGesture.setFlingProcessor(iVideoViewFlingProcessor);
    }

    public void setLongPressedListener(IIoTVideoGesture.OnLongPressListener onLongPressListener) {
        this.ioTVideoGesture.setLongPressedListener(onLongPressListener);
    }

    public void setOnClickUpListener(IIoTVideoGesture.OnClickUpListener onClickUpListener) {
        this.ioTVideoGesture.setOnClickUpListener(onClickUpListener);
    }

    public void setOnDoubleClickListener(IIoTVideoGesture.OnDoubleClickListener onDoubleClickListener) {
        this.ioTVideoGesture.setOnDoubleClickListener(onDoubleClickListener);
    }

    public void setOnDownListener(IIoTVideoGesture.OnDownListener onDownListener) {
        this.ioTVideoGesture.setOnDownListener(onDownListener);
    }

    public void setOnFlingListener(IIoTVideoGesture.OnFlingListener onFlingListener) {
        this.ioTVideoGesture.setOnFlingListener(onFlingListener);
    }

    public void setOnMoveListener(IIoTVideoGesture.OnMoveListener onMoveListener) {
        this.ioTVideoGesture.setOnMoveListener(onMoveListener);
    }

    public void setRenderView(int i10) {
        LogUtils.i(TAG, "setRenderView renderType " + i10 + "; mRenderType: " + this.mRenderType);
        if (this.mRenderType == i10 && this.renderView != null && getChildCount() > 0) {
            LogUtils.e(TAG, "repeat set same render view");
            return;
        }
        this.mRenderType = i10;
        if (i10 == 0) {
            this.renderView = new IoTSurfaceVideoView(getContext());
        } else if (2 == i10) {
            this.renderView = new IoTTextureVideoView(getContext());
        } else {
            this.renderView = new IoTGLVideoView(getContext());
        }
        LogUtils.i(TAG, "setRenderView before add, child count:" + getChildCount());
        if (getChildCount() > 0) {
            removeAllViews();
        }
        addView((View) this.renderView, new FrameLayout.LayoutParams(-1, -1));
        ((View) this.renderView).post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.iotview.IoTVideoRenderView.1
            @Override // java.lang.Runnable
            public void run() {
                LogUtils.i(IoTVideoRenderView.TAG, "setRenderView video view width:" + IoTVideoRenderView.this.getWidth() + "; height:" + IoTVideoRenderView.this.getHeight());
            }
        });
        IIoTVideoGesture iIoTVideoGesture = this.ioTVideoGesture;
        if (iIoTVideoGesture instanceof IoTVideoGesture) {
            ((IoTVideoGesture) iIoTVideoGesture).setRenderView(this.renderView);
        }
        this.renderView.getVideoRender().setOnRectChangeListener(new OnRectChangeListener() { // from class: com.tencentcs.iotvideo.iotvideoplayer.iotview.IoTVideoRenderView.2
            @Override // com.tencentcs.iotvideo.iotvideoplayer.OnRectChangeListener
            public void updateRect(float f10, float f11, float f12, float f13) {
                IoTVideoRenderView.this.updateRect(f10, f11, f12, f13);
            }
        });
    }

    public void setSingleTapUpListener(IIoTVideoGesture.OnSingleTapUp onSingleTapUp) {
        this.ioTVideoGesture.setSingleTapUpListener(onSingleTapUp);
    }

    public void updateRect(float f10, float f11, float f12, float f13) {
    }

    public IoTVideoRenderView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.renderView = null;
        this.mRenderType = -1;
        init();
    }
}
