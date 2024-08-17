package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.content.Context;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoView;
import com.tencentcs.iotvideo.utils.LogUtils;

/* loaded from: classes2.dex */
public abstract class GestureGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "GestureGLSurfaceView";
    public static final int USR_CMD_OPTION_PTZ_TURN_DOWN = 3;
    public static final int USR_CMD_OPTION_PTZ_TURN_LEFT = 0;
    public static final int USR_CMD_OPTION_PTZ_TURN_RIGHT = 1;
    public static final int USR_CMD_OPTION_PTZ_TURN_UP = 2;
    private int mCurrentPointerCount;
    private OnDownListener mDownListener;
    private boolean mEnableScaleGesture;
    private OnFlingListener mFlingListener;
    private IVideoViewFlingProcessor mFlingProcessor;
    private GestureDetector mGestureDetector;
    private float mLastPointerDistance;
    private OnLongPressListener mLongPressedListener;
    private OnClickUpListener mOnClickUpListener;
    private OnMoveListener mOnMoveListener;
    private ScaleGestureDetector mScaleGestureDetector;
    private OnSingleTapUp mSingleTapUpListener;
    private float mZoomCenterX;
    private float mZoomCenterY;
    private OnDoubleClickListener onDoubleClickListener;

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

    public GestureGLSurfaceView(Context context) {
        super(context);
        this.mEnableScaleGesture = true;
        initView();
    }

    private int dip2px(Context context, int i10) {
        return (int) ((i10 * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private float getDistance(MotionEvent motionEvent) {
        float f10;
        float f11 = 0.0f;
        try {
            f10 = motionEvent.getX(0) - motionEvent.getX(1);
            try {
                f11 = motionEvent.getY(0) - motionEvent.getY(1);
            } catch (IllegalArgumentException e10) {
                e10.printStackTrace();
                return (float) Math.sqrt((f11 * f11) + (f10 * f10));
            }
        } catch (IllegalArgumentException e11) {
            e11.printStackTrace();
            f10 = 0.0f;
        }
        return (float) Math.sqrt((f11 * f11) + (f10 * f10));
    }

    private void initGestureDetector() {
        this.mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GestureGLSurfaceView.1
            @Override // android.view.ScaleGestureDetector.SimpleOnScaleGestureListener, android.view.ScaleGestureDetector.OnScaleGestureListener
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                GestureGLSurfaceView gestureGLSurfaceView = GestureGLSurfaceView.this;
                gestureGLSurfaceView.onZoom(gestureGLSurfaceView.mZoomCenterX, GestureGLSurfaceView.this.mZoomCenterY, scaleGestureDetector.getScaleFactor());
                return true;
            }
        });
        this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GestureGLSurfaceView.2
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onDoubleTap(MotionEvent motionEvent) {
                if (GestureGLSurfaceView.this.onDoubleClickListener != null) {
                    GestureGLSurfaceView.this.onDoubleClickListener.onDoubleClick(motionEvent);
                    return true;
                }
                return false;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onDown(MotionEvent motionEvent) {
                if (GestureGLSurfaceView.this.mCurrentPointerCount == 1 && GestureGLSurfaceView.this.mDownListener != null) {
                    GestureGLSurfaceView.this.mDownListener.onDown(motionEvent);
                }
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f10, float f11) {
                RectF rectF;
                if (GestureGLSurfaceView.this.mFlingProcessor == null) {
                    GestureGLSurfaceView.this.mFlingProcessor = new DefaultVideoFlingProcessor();
                }
                if (GestureGLSurfaceView.this.getIoTVideoView() != null) {
                    rectF = GestureGLSurfaceView.this.getIoTVideoView().getGLRenderer().getVideoRealRectF();
                } else {
                    rectF = null;
                }
                int onProcessorFling = GestureGLSurfaceView.this.mFlingProcessor.onProcessorFling(GestureGLSurfaceView.this.getContext(), motionEvent, motionEvent2, f10, f11, rectF);
                LogUtils.i(GestureGLSurfaceView.TAG, "onFling id:" + onProcessorFling);
                if (onProcessorFling != -1 && GestureGLSurfaceView.this.mFlingListener != null && GestureGLSurfaceView.this.mCurrentPointerCount != 2) {
                    GestureGLSurfaceView.this.mFlingListener.onFling(onProcessorFling);
                    return true;
                }
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public void onLongPress(MotionEvent motionEvent) {
                if (GestureGLSurfaceView.this.mCurrentPointerCount == 1 && GestureGLSurfaceView.this.mLongPressedListener != null) {
                    GestureGLSurfaceView.this.mLongPressedListener.onLongPress(motionEvent);
                }
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f10, float f11) {
                if (GestureGLSurfaceView.this.mCurrentPointerCount == 1) {
                    if (GestureGLSurfaceView.this.mOnMoveListener != null) {
                        GestureGLSurfaceView.this.mOnMoveListener.onMove(f10, f11);
                    }
                    GestureGLSurfaceView.this.onMove(f10, f11);
                    return true;
                }
                return false;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                if (GestureGLSurfaceView.this.mSingleTapUpListener != null) {
                    GestureGLSurfaceView.this.mSingleTapUpListener.onSingleTapUp(motionEvent);
                    return true;
                }
                return false;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return super.onSingleTapUp(motionEvent);
            }
        });
    }

    private void initView() {
        initGestureDetector();
    }

    public void enableScaleGesture(boolean z10) {
        this.mEnableScaleGesture = z10;
    }

    public boolean getEnableScaleGesture() {
        return this.mEnableScaleGesture;
    }

    public IoTVideoView getIoTVideoView() {
        return null;
    }

    public void onMove(float f10, float f11) {
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        OnClickUpListener onClickUpListener;
        if (motionEvent.getAction() == 5 || motionEvent.getAction() == 261 || motionEvent.getAction() == 517 || motionEvent.getAction() == 0) {
            int pointerCount = motionEvent.getPointerCount();
            this.mCurrentPointerCount = pointerCount;
            if (this.mEnableScaleGesture && pointerCount == 2) {
                this.mLastPointerDistance = getDistance(motionEvent);
            }
        }
        if (1 == motionEvent.getAction() && (onClickUpListener = this.mOnClickUpListener) != null) {
            onClickUpListener.onClickUp(motionEvent);
        }
        if (motionEvent.getPointerCount() == 1) {
            this.mGestureDetector.onTouchEvent(motionEvent);
            return true;
        } else if (motionEvent.getPointerCount() == 2) {
            this.mZoomCenterX = (motionEvent.getX(1) + motionEvent.getX(0)) / 2.0f;
            this.mZoomCenterY = (motionEvent.getY(1) + motionEvent.getY(0)) / 2.0f;
            if (!this.mEnableScaleGesture) {
                this.mScaleGestureDetector.onTouchEvent(motionEvent);
            } else {
                float distance = getDistance(motionEvent);
                if (Math.abs(distance - this.mLastPointerDistance) > 5.0f) {
                    float f10 = distance / this.mLastPointerDistance;
                    this.mLastPointerDistance = distance;
                    onZoom(this.mZoomCenterX, this.mZoomCenterY, f10);
                }
            }
            return true;
        } else {
            return super.onTouchEvent(motionEvent);
        }
    }

    public void onZoom(float f10, float f11, float f12) {
    }

    public void scaleVideo(float f10, float f11, float f12) {
        onZoom(f10, f11, f12);
    }

    public void setFlingProcessor(IVideoViewFlingProcessor iVideoViewFlingProcessor) {
        this.mFlingProcessor = iVideoViewFlingProcessor;
    }

    public void setLongPressedListener(OnLongPressListener onLongPressListener) {
        this.mLongPressedListener = onLongPressListener;
    }

    public void setOnClickUpListener(OnClickUpListener onClickUpListener) {
        this.mOnClickUpListener = onClickUpListener;
    }

    public void setOnDoubleClickListener(OnDoubleClickListener onDoubleClickListener) {
        this.onDoubleClickListener = onDoubleClickListener;
    }

    public void setOnDownListener(OnDownListener onDownListener) {
        this.mDownListener = onDownListener;
    }

    public void setOnFlingListener(OnFlingListener onFlingListener) {
        this.mFlingListener = onFlingListener;
    }

    public void setOnMoveListener(OnMoveListener onMoveListener) {
        this.mOnMoveListener = onMoveListener;
    }

    public void setSingleTapUpListener(OnSingleTapUp onSingleTapUp) {
        this.mSingleTapUpListener = onSingleTapUp;
    }

    public GestureGLSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnableScaleGesture = true;
        initView();
    }
}
