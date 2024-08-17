package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import android.view.MotionEvent;
import com.tencentcs.iotvideo.iotvideoplayer.render.IVideoViewFlingProcessor;
/* loaded from: classes2.dex */
public interface IIoTVideoGesture {

    /* loaded from: classes2.dex */
    public interface OnClickUpListener {
        void onClickUp(MotionEvent motionEvent);
    }

    /* loaded from: classes2.dex */
    public interface OnDoubleClickListener {
        void onDoubleClick(MotionEvent motionEvent);
    }

    /* loaded from: classes2.dex */
    public interface OnDownListener {
        void onDown(MotionEvent motionEvent);
    }

    /* loaded from: classes2.dex */
    public interface OnFlingListener {
        void onFling(int i10);
    }

    /* loaded from: classes2.dex */
    public interface OnLongPressListener {
        void onLongPress(MotionEvent motionEvent);
    }

    /* loaded from: classes2.dex */
    public interface OnMoveListener {
        void onMove(float f10, float f11);
    }

    /* loaded from: classes2.dex */
    public interface OnSingleTapUp {
        void onSingleTapUp(MotionEvent motionEvent);
    }

    /* loaded from: classes2.dex */
    public interface OnZoomListener {
        void onZoom(float f10, float f11, float f12);
    }

    boolean onTouchEvent(MotionEvent motionEvent);

    void setFlingProcessor(IVideoViewFlingProcessor iVideoViewFlingProcessor);

    void setLongPressedListener(OnLongPressListener onLongPressListener);

    void setOnClickUpListener(OnClickUpListener onClickUpListener);

    void setOnDoubleClickListener(OnDoubleClickListener onDoubleClickListener);

    void setOnDownListener(OnDownListener onDownListener);

    void setOnFlingListener(OnFlingListener onFlingListener);

    void setOnMoveListener(OnMoveListener onMoveListener);

    void setOnZoomListener(OnZoomListener onZoomListener);

    void setSingleTapUpListener(OnSingleTapUp onSingleTapUp);
}
