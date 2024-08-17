package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.content.Context;
import android.graphics.RectF;
import android.view.MotionEvent;
/* loaded from: classes2.dex */
public class DefaultVideoFlingProcessor implements IVideoViewFlingProcessor {
    protected static final int MINX = 50;
    protected static final int MINY = 25;

    public int dip2px(Context context, int i10) {
        return (int) ((i10 * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.render.IVideoViewFlingProcessor
    public int onProcessorFling(Context context, MotionEvent motionEvent, MotionEvent motionEvent2, float f10, float f11, RectF rectF) {
        boolean z10;
        if (Math.abs(motionEvent2.getX() - motionEvent.getX()) > Math.abs(motionEvent2.getY() - motionEvent.getY())) {
            z10 = true;
        } else {
            z10 = false;
        }
        if (z10) {
            float x10 = motionEvent2.getX() - motionEvent.getX();
            if (Math.abs(x10) > dip2px(context, 50)) {
                if (x10 > 0.0f) {
                    return 1;
                }
                return 0;
            }
        } else {
            float y10 = motionEvent2.getY() - motionEvent.getY();
            if (Math.abs(y10) > dip2px(context, 25)) {
                if (y10 > 0.0f) {
                    return 2;
                }
                return 3;
            }
        }
        return -1;
    }
}
