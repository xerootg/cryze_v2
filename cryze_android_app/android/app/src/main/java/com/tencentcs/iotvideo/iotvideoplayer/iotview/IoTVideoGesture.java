package com.tencentcs.iotvideo.iotvideoplayer.iotview;

import android.content.Context;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.tencentcs.iotvideo.iotvideoplayer.render.DefaultVideoFlingProcessor;
import com.tencentcs.iotvideo.iotvideoplayer.render.IVideoViewFlingProcessor;
import com.tencentcs.iotvideo.utils.LogUtils;
/* loaded from: classes2.dex */
public class IoTVideoGesture implements IIoTVideoGesture {
    private static final int MARGIN = 5;
    private static final int MINX = 50;
    private static final int MINY = 25;
    private static final String TAG = "IoTVideoGesture";
    private static final boolean USING_CUSTOMS_SCALE_GESTURE_DETECTOR = true;
    public static final int USR_CMD_OPTION_PTZ_TURN_DOWN = 3;
    public static final int USR_CMD_OPTION_PTZ_TURN_LEFT = 0;
    public static final int USR_CMD_OPTION_PTZ_TURN_RIGHT = 1;
    public static final int USR_CMD_OPTION_PTZ_TURN_UP = 2;
    private Context mContext;
    private int mCurrentPointerCount;
    private IIoTVideoGesture.OnDownListener mDownListener;
    private IIoTVideoGesture.OnFlingListener mFlingListener;
    private IVideoViewFlingProcessor mFlingProcessor;
    private GestureDetector mGestureDetector;
    private float mLastPointerDistance;
    private IIoTVideoGesture.OnLongPressListener mLongPressedListener;
    private IIoTVideoGesture.OnClickUpListener mOnClickUpListener;
    private IIoTVideoGesture.OnDoubleClickListener mOnDoubleClickListener;
    private IIoTVideoGesture.OnMoveListener mOnMoveListener;
    private IIoTVideoGesture.OnZoomListener mOnZoomListener;
    private ScaleGestureDetector mScaleGestureDetector;
    private IIoTVideoGesture.OnSingleTapUp mSingleTapUpListener;
    private IIoTVideoView mVideoView;
    private float mZoomCenterX;
    private float mZoomCenterY;

    public IoTVideoGesture(Context context) {
        this.mContext = context;
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
        this.mGestureDetector = new GestureDetector(this.mContext, new GestureDetector.SimpleOnGestureListener() { // from class: com.tencentcs.iotvideo.iotvideoplayer.iotview.IoTVideoGesture.2
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onDoubleTap(MotionEvent motionEvent) {
                if (IoTVideoGesture.this.mOnDoubleClickListener != null) {
                    IoTVideoGesture.this.mOnDoubleClickListener.onDoubleClick(motionEvent);
                    return IoTVideoGesture.USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
                }
                return false;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onDown(MotionEvent motionEvent) {
                if (IoTVideoGesture.this.mCurrentPointerCount == 1 && IoTVideoGesture.this.mDownListener != null) {
                    IoTVideoGesture.this.mDownListener.onDown(motionEvent);
                }
                return IoTVideoGesture.USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f10, float f11) {
                RectF rectF;
                if (IoTVideoGesture.this.mFlingProcessor == null) {
                    IoTVideoGesture.this.mFlingProcessor = new DefaultVideoFlingProcessor();
                }
                if (IoTVideoGesture.this.mVideoView != null) {
                    rectF = IoTVideoGesture.this.mVideoView.getVideoRender().getVideoRealRectF();
                } else {
                    rectF = null;
                }
                int onProcessorFling = IoTVideoGesture.this.mFlingProcessor.onProcessorFling(IoTVideoGesture.this.mContext, motionEvent, motionEvent2, f10, f11, rectF);
                LogUtils.i(IoTVideoGesture.TAG, "onFling id: "+ onProcessorFling);
                if (onProcessorFling != -1 && IoTVideoGesture.this.mFlingListener != null && IoTVideoGesture.this.mCurrentPointerCount != 2) {
                    IoTVideoGesture.this.mFlingListener.onFling(onProcessorFling);
                    return IoTVideoGesture.USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
                }
                return IoTVideoGesture.USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public void onLongPress(MotionEvent motionEvent) {
                if (IoTVideoGesture.this.mCurrentPointerCount == 1 && IoTVideoGesture.this.mLongPressedListener != null) {
                    IoTVideoGesture.this.mLongPressedListener.onLongPress(motionEvent);
                }
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f10, float f11) {
                if (IoTVideoGesture.this.mCurrentPointerCount == 1) {
                    LogUtils.i(IoTVideoGesture.TAG, "onScroll ishorizontal");
                    if (IoTVideoGesture.this.mOnMoveListener != null) {
                        IoTVideoGesture.this.mOnMoveListener.onMove(f10, f11);
                    }
                    return IoTVideoGesture.USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
                }
                return false;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                if (IoTVideoGesture.this.mSingleTapUpListener != null) {
                    IoTVideoGesture.this.mSingleTapUpListener.onSingleTapUp(motionEvent);
                    return IoTVideoGesture.USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
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

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public boolean onTouchEvent(MotionEvent motionEvent) {
        IIoTVideoGesture.OnClickUpListener onClickUpListener;
        if (motionEvent.getAction() == 5 || motionEvent.getAction() == 261 || motionEvent.getAction() == 517 || motionEvent.getAction() == 0) {
            int pointerCount = motionEvent.getPointerCount();
            this.mCurrentPointerCount = pointerCount;
            if (pointerCount == 2) {
                this.mLastPointerDistance = getDistance(motionEvent);
            }
        }
        if (1 == motionEvent.getAction() && (onClickUpListener = this.mOnClickUpListener) != null) {
            onClickUpListener.onClickUp(motionEvent);
        }
        if (motionEvent.getPointerCount() == 1) {
            this.mGestureDetector.onTouchEvent(motionEvent);
            return USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
        } else if (motionEvent.getPointerCount() != 2) {
            return false;
        } else {
            this.mZoomCenterX = (motionEvent.getX(1) + motionEvent.getX(0)) / 2.0f;
            this.mZoomCenterY = (motionEvent.getY(1) + motionEvent.getY(0)) / 2.0f;
            float distance = getDistance(motionEvent);
            if (Math.abs(distance - this.mLastPointerDistance) > 5.0f) {
                float f10 = distance / this.mLastPointerDistance;
                this.mLastPointerDistance = distance;
                IIoTVideoGesture.OnZoomListener onZoomListener = this.mOnZoomListener;
                if (onZoomListener != null) {
                    onZoomListener.onZoom(this.mZoomCenterX, this.mZoomCenterY, f10);
                }
            }
            return USING_CUSTOMS_SCALE_GESTURE_DETECTOR;
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setFlingProcessor(IVideoViewFlingProcessor iVideoViewFlingProcessor) {
        LogUtils.i(TAG, "setFlingProcessor");
        this.mFlingProcessor = iVideoViewFlingProcessor;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setLongPressedListener(IIoTVideoGesture.OnLongPressListener onLongPressListener) {
        this.mLongPressedListener = onLongPressListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setOnClickUpListener(IIoTVideoGesture.OnClickUpListener onClickUpListener) {
        this.mOnClickUpListener = onClickUpListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setOnDoubleClickListener(IIoTVideoGesture.OnDoubleClickListener onDoubleClickListener) {
        LogUtils.i(TAG, "setOnDoubleClickListener");
        this.mOnDoubleClickListener = onDoubleClickListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setOnDownListener(IIoTVideoGesture.OnDownListener onDownListener) {
        this.mDownListener = onDownListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setOnFlingListener(IIoTVideoGesture.OnFlingListener onFlingListener) {
        this.mFlingListener = onFlingListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setOnMoveListener(IIoTVideoGesture.OnMoveListener onMoveListener) {
        this.mOnMoveListener = onMoveListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setOnZoomListener(IIoTVideoGesture.OnZoomListener onZoomListener) {
        this.mOnZoomListener = onZoomListener;
    }

    public void setRenderView(IIoTVideoView iIoTVideoView) {
        this.mVideoView = iIoTVideoView;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.iotview.IIoTVideoGesture
    public void setSingleTapUpListener(IIoTVideoGesture.OnSingleTapUp onSingleTapUp) {
        this.mSingleTapUpListener = onSingleTapUp;
    }
}
