package com.tencentcs.iotvideo.iotvideoplayer;

import android.graphics.RectF;
/* loaded from: classes2.dex */
public interface IVideoGestureRender extends IVideoRender {
    float getScale();

    RectF getVideoRealRectF();

    float scaleTo(float f10, float f11, float f12);

    void scaleTo(float f10);

    void setFitType(int i10);

    void setOnRectChangeListener(OnRectChangeListener onRectChangeListener);

    void setVideoAspectRatio(float f10);

    void translateBy(float f10, float f11);
}
