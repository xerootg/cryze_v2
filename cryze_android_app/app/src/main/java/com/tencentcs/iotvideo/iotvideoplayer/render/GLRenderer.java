package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender;
import com.tencentcs.iotvideo.iotvideoplayer.OnRectChangeListener;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
/* loaded from: classes2.dex */
public class GLRenderer implements GLSurfaceView.Renderer, IVideoGestureRender {
    private static final float DEFAULT_VIDEO_ASPECT_RATIO = 1.3333334f;
    public static final int FIT_CENTER = 1;
    public static final int FIT_XY = 0;
    public static final float MAX_VIDEO_SCALE = 3.0f;
    public static final float MIN_VIDEO_SCALE = 1.0f;
    private final String TAG;
    private float[] closeUpRect;
    private int count;
    private volatile boolean isPauseRender;
    private OnRectChangeListener listener;
    private AVHeader mAVHeader;
    private float mAspectRatioHeight;
    private float mAspectRatioWidth;
    private float[] mBackgroundColors;
    private ByteBuffer mByteBufferU;
    private ByteBuffer mByteBufferV;
    private ByteBuffer mByteBufferY;
    private final float[] mConverMatrix;
    private int mFitType;
    private GLProgram mGLProgram;
    private volatile boolean mInitialized;
    private final float[] mInvertMVPMatrix;
    private final float[] mInvertMVPMatrixCalculate;
    private volatile boolean mIsFrameReady;
    private final float[] mMVPMatrix;
    private final float[] mMVPMatrixCalculate;
    private float mMaxTranslateX;
    private float mMaxTranslateY;
    private final float[] mProjectionMatrix;
    private float mRotate;
    private float mScale;
    private int mScreenHeight;
    private int mScreenWidth;
    private Rect mShowRect;
    private int mSurfaceHeight;
    private GLSurfaceView mSurfaceView;
    private int mSurfaceWidth;
    private final float[] mTempMatrix;
    private float mTranslateX;
    private float mTranslateY;
    private float mVideoAspectRatio;
    private int mVideoHeight;
    private Point mVideoPoint;
    private int mVideoWidth;
    private final float[] mViewMatrix;
    private int mViewPortHeight;
    private int mViewPortWidth;
    private int mViewPortX;
    private int mViewPortY;
    private long preGetFrameTime;
    private final RectF realVideoRectF;

    public GLRenderer(GLSurfaceView gLSurfaceView, DisplayMetrics displayMetrics) {
        this.TAG = "GLRenderer";
        this.mGLProgram = new GLProgram();
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
        this.mInitialized = false;
        this.mProjectionMatrix = new float[16];
        this.mViewMatrix = new float[16];
        this.mMVPMatrix = new float[16];
        this.mInvertMVPMatrix = new float[16];
        this.mMVPMatrixCalculate = new float[16];
        this.mInvertMVPMatrixCalculate = new float[16];
        this.mTempMatrix = new float[16];
        this.mMaxTranslateX = 0.0f;
        this.mMaxTranslateY = 0.0f;
        this.mAspectRatioWidth = 1.0f;
        this.mAspectRatioHeight = 1.0f;
        this.mTranslateX = 0.0f;
        this.mTranslateY = 0.0f;
        this.mScale = 1.0f;
        this.mRotate = 0.0f;
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        this.mViewPortX = 0;
        this.mViewPortY = 0;
        this.mFitType = 1;
        this.mVideoAspectRatio = DEFAULT_VIDEO_ASPECT_RATIO;
        this.mIsFrameReady = false;
        this.isPauseRender = true;
        this.preGetFrameTime = 0L;
        this.count = 0;
        this.realVideoRectF = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        this.mBackgroundColors = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
        this.closeUpRect = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
        this.mShowRect = new Rect(0, 0, 0, 0);
        this.mVideoPoint = new Point(0, 0);
        this.mConverMatrix = new float[]{-1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f};
        this.mSurfaceView = gLSurfaceView;
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
    }

    public static /* synthetic */ float access$1116(GLRenderer gLRenderer, float f10) {
        float f11 = gLRenderer.mTranslateX + f10;
        gLRenderer.mTranslateX = f11;
        return f11;
    }

    public static /* synthetic */ float access$1416(GLRenderer gLRenderer, float f10) {
        float f11 = gLRenderer.mTranslateY + f10;
        gLRenderer.mTranslateY = f11;
        return f11;
    }

    private float[] fakeCloseUpData(long j10) {
        float f10;
        float max = 0;
        float f11;
        int i10 = ((int) (j10 / 100000)) % 40;
        float[] fArr = this.closeUpRect;
        float f12 = fArr[0] * 100.0f;
        float f13 = fArr[1] * 100.0f;
        float f14 = fArr[2] * 100.0f;
        float f15 = fArr[3];
        if (i10 <= 10) {
            f14 = Math.max(30.0f, f14 - 2.0f);
            float f16 = 100.0f - f14;
            f12 = Math.min(f16, f12 + 1.0f) % f16;
            f10 = Math.min(f16, f13 + 1.0f) % f16;
        } else {
            if (i10 <= 20) {
                f14 = Math.min(100.0f, f14 + 1.0f);
                max = Math.max(0.0f, f13 - 1.0f);
            } else if (i10 <= 30) {
                f14 = Math.max(20.0f, f14 - 1.0f);
                f11 = 100.0f - f14;
                f12 = Math.max(0.0f, f12 - 1.0f) % f11;
                max = Math.min(f11, f13 + 1.0f);
                f10 = max % f11;
            } else if (i10 <= 40) {
                f14 = Math.min(100.0f, f14 + 1.0f);
                max = Math.max(0.0f, f13 - 1.0f);
            } else {
                f12 = 25.0f;
                f10 = 25.0f;
            }
            f11 = 100.0f - f14;
            f10 = max % f11;
        }
        float[] fArr2 = this.closeUpRect;
        fArr2[0] = f12 / 100.0f;
        float f17 = f10 / 100.0f;
        fArr2[1] = f17;
        fArr2[2] = f14 / 100.0f;
        fArr2[3] = f17;
        return fArr2;
    }

    private void setFrameBuffer(int i10, int i11) {
        if (i10 != this.mVideoWidth || i11 != this.mVideoHeight) {
            this.mVideoWidth = i10;
            this.mVideoHeight = i11;
            setVideoAspectRatio((i10 * 1.0f) / i11);
        }
    }

    private void updateMaxTranslate() {
        float f10;
        float[] fArr = this.mTempMatrix;
        fArr[0] = -1.0f;
        fArr[1] = 1.0f;
        float f11 = 0.0f;
        fArr[2] = 0.0f;
        fArr[3] = 1.0f;
        fArr[4] = -1.0f;
        fArr[5] = -1.0f;
        fArr[6] = 0.0f;
        fArr[7] = 1.0f;
        fArr[8] = 1.0f;
        fArr[9] = -1.0f;
        fArr[10] = 0.0f;
        fArr[11] = 1.0f;
        fArr[12] = 1.0f;
        fArr[13] = 1.0f;
        fArr[14] = 0.0f;
        fArr[15] = 1.0f;
        Matrix.multiplyMM(fArr, 0, this.mMVPMatrixCalculate, 0, fArr, 0);
        float max = Math.max(Math.abs(this.mTempMatrix[0]), Math.abs(this.mTempMatrix[8]));
        float max2 = Math.max(Math.abs(this.mTempMatrix[1]), Math.abs(this.mTempMatrix[9]));
        if (max < 1.0f) {
            f10 = 0.0f;
        } else {
            f10 = max - 1.0f;
        }
        this.mMaxTranslateX = f10 * this.mAspectRatioWidth;
        if (max2 >= 1.0f) {
            f11 = max2 - 1.0f;
        }
        this.mMaxTranslateY = f11 * this.mAspectRatioHeight;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMvpMatrix() {
        Matrix.setIdentityM(this.mTempMatrix, 0);
        Matrix.translateM(this.mTempMatrix, 0, this.mTranslateX, this.mTranslateY, 0.0f);
        float[] fArr = this.mTempMatrix;
        float f10 = this.mScale;
        Matrix.scaleM(fArr, 0, f10, f10, 1.0f);
        Matrix.rotateM(this.mTempMatrix, 0, this.mRotate, 0.0f, 0.0f, 1.0f);
        float[] fArr2 = this.mTempMatrix;
        Matrix.multiplyMM(fArr2, 0, this.mViewMatrix, 0, fArr2, 0);
        Matrix.multiplyMM(this.mMVPMatrix, 0, this.mProjectionMatrix, 0, this.mTempMatrix, 0);
        Matrix.invertM(this.mInvertMVPMatrix, 0, this.mMVPMatrix, 0);
        this.mGLProgram.updateMvp(this.mMVPMatrix);
        Matrix.setIdentityM(this.mMVPMatrixCalculate, 0);
        float[] fArr3 = this.mMVPMatrixCalculate;
        float f11 = this.mScale;
        Matrix.scaleM(fArr3, 0, f11, f11, 1.0f);
        Matrix.rotateM(this.mMVPMatrixCalculate, 0, this.mRotate, 0.0f, 0.0f, 1.0f);
        float[] fArr4 = this.mMVPMatrixCalculate;
        Matrix.multiplyMM(fArr4, 0, this.mViewMatrix, 0, fArr4, 0);
        float[] fArr5 = this.mMVPMatrixCalculate;
        Matrix.multiplyMM(fArr5, 0, this.mProjectionMatrix, 0, fArr5, 0);
        Matrix.invertM(this.mInvertMVPMatrixCalculate, 0, this.mMVPMatrixCalculate, 0);
        updateShowRect();
        updateMaxTranslate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProjection() {
        float f10 = this.mViewPortWidth / this.mViewPortHeight;
        if (this.mFitType == 0) {
            this.mAspectRatioWidth = 1.0f;
            this.mAspectRatioHeight = 1.0f;
            Matrix.orthoM(this.mProjectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10.0f);
            return;
        }
        float f11 = this.mVideoAspectRatio;
        if (f10 > f11) {
            float f12 = f10 / f11;
            this.mAspectRatioWidth = f12;
            this.mAspectRatioHeight = 1.0f;
            Matrix.orthoM(this.mProjectionMatrix, 0, -f12, f12, -1.0f, 1.0f, 1.0f, 10.0f);
            return;
        }
        this.mAspectRatioWidth = 1.0f;
        float f13 = f11 / f10;
        this.mAspectRatioHeight = f13;
        Matrix.orthoM(this.mProjectionMatrix, 0, -1.0f, 1.0f, -f13, f13, 1.0f, 10.0f);
    }

    private void updateShowRect() {
        float[] fArr = this.mTempMatrix;
        fArr[0] = -1.0f;
        fArr[1] = 1.0f;
        fArr[2] = 0.0f;
        fArr[3] = 1.0f;
        fArr[4] = -1.0f;
        fArr[5] = -1.0f;
        fArr[6] = 0.0f;
        fArr[7] = 1.0f;
        fArr[8] = 1.0f;
        fArr[9] = -1.0f;
        fArr[10] = 0.0f;
        fArr[11] = 1.0f;
        fArr[12] = 1.0f;
        fArr[13] = 1.0f;
        fArr[14] = 0.0f;
        fArr[15] = 1.0f;
        Matrix.multiplyMM(fArr, 0, this.mMVPMatrix, 0, fArr, 0);
        float[] fArr2 = this.mTempMatrix;
        float min = Math.min(fArr2[0], fArr2[4]);
        float[] fArr3 = this.mTempMatrix;
        float min2 = Math.min(min, Math.min(fArr3[8], fArr3[12]));
        float f10 = ((min2 + 1.0f) * this.mViewPortWidth) / 2.0f;
        float min3 = Math.min(Math.max(min2, -1.0f), 1.0f);
        float[] fArr4 = this.mTempMatrix;
        float max = Math.max(fArr4[0], fArr4[4]);
        float[] fArr5 = this.mTempMatrix;
        float max2 = Math.max(max, Math.max(fArr5[8], fArr5[12]));
        float f11 = ((max2 - 1.0f) * this.mViewPortWidth) / 2.0f;
        float max3 = Math.max(Math.min(max2, 1.0f), -1.0f);
        float[] fArr6 = this.mTempMatrix;
        float max4 = Math.max(fArr6[1], fArr6[5]);
        float[] fArr7 = this.mTempMatrix;
        float max5 = Math.max(max4, Math.max(fArr7[9], fArr7[13]));
        float f12 = ((max5 - 1.0f) * this.mViewPortHeight) / 2.0f;
        float max6 = Math.max(Math.min(max5, 1.0f), -1.0f);
        float[] fArr8 = this.mTempMatrix;
        float min4 = Math.min(fArr8[1], fArr8[5]);
        float[] fArr9 = this.mTempMatrix;
        float min5 = Math.min(min4, Math.min(fArr9[9], fArr9[13]));
        float f13 = ((min5 + 1.0f) * this.mViewPortHeight) / 2.0f;
        float min6 = Math.min(Math.max(min5, -1.0f), 1.0f);
        int i10 = this.mViewPortWidth;
        int i11 = this.mViewPortHeight;
        int i12 = (int) ((((-min6) + 1.0f) * i11) / 2.0f);
        this.realVideoRectF.set(f10, f12, f11, f13);
        this.mShowRect.set((int) (((min3 + 1.0f) * i10) / 2.0f), (int) ((((-max6) + 1.0f) * i11) / 2.0f), (int) (((max3 + 1.0f) * i10) / 2.0f), i12);
        OnRectChangeListener onRectChangeListener = this.listener;
        if (onRectChangeListener != null) {
            onRectChangeListener.updateRect(f10, f12, f11, f13);
        }
    }

    public Point convertViewPointToVideoPoint(Point point) {
        float[] fArr = this.mConverMatrix;
        int i10 = this.mViewPortWidth;
        fArr[0] = ((point.x - (i10 / 2.0f)) * 2.0f) / i10;
        int i11 = this.mViewPortHeight;
        fArr[1] = (((i11 / 2.0f) - point.y) * 2.0f) / i11;
        fArr[3] = 0.0f;
        fArr[4] = 1.0f;
        Matrix.multiplyMM(fArr, 0, this.mInvertMVPMatrix, 0, fArr, 0);
        Point point2 = this.mVideoPoint;
        float[] fArr2 = this.mConverMatrix;
        point2.x = (int) (((fArr2[0] + 1.0f) * this.mVideoWidth) / 2.0f);
        point2.y = (int) (((1.0f - fArr2[1]) * this.mVideoHeight) / 2.0f);
        return point2;
    }

    public float getRotate() {
        return this.mRotate;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public float getScale() {
        return this.mScale;
    }

    public Rect getShowRect() {
        return this.mShowRect;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public RectF getVideoRealRectF() {
        return this.realVideoRectF;
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl10) {
        LogUtils.d(TAG, "onDrawFrame! paused: " + this.isPauseRender);
        if (this.isPauseRender) {
            return;
        }
        float[] fArr = this.mBackgroundColors;
        GLES20.glClearColor(fArr[0], fArr[1], fArr[2], fArr[3]);
        GLES20.glClear(16384);
        if (this.mIsFrameReady) {
            this.mGLProgram.drawFrame();
        } else if (this.mByteBufferY != null && this.mByteBufferU != null && this.mByteBufferV != null) {
            this.mSurfaceView.queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.1
                @Override // java.lang.Runnable
                public void run() {
                    if (GLRenderer.this.mByteBufferY != null && GLRenderer.this.mByteBufferU != null && GLRenderer.this.mByteBufferV != null) {
                        synchronized (GLRenderer.this) {
                            GLRenderer.this.mByteBufferY.position(0);
                            GLRenderer.this.mByteBufferU.position(0);
                            GLRenderer.this.mByteBufferV.position(0);
                            GLRenderer.this.mGLProgram.buildTextures(GLRenderer.this.mByteBufferY, GLRenderer.this.mByteBufferU, GLRenderer.this.mByteBufferV, GLRenderer.this.mVideoWidth, GLRenderer.this.mVideoHeight);
                            GLRenderer.this.mIsFrameReady = true;
                        }
                    }
                }
            });
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onFrameUpdate(AVData aVData) {
        if (!this.mInitialized || this.isPauseRender) {
            return;
        }
        setFrameBuffer(aVData.width, aVData.height);
        synchronized (this) {
            this.mByteBufferY = aVData.data;
            this.mByteBufferU = aVData.data1;
            this.mByteBufferV = aVData.data2;
        }
        this.mSurfaceView.queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (GLRenderer.this) {
                    if (GLRenderer.this.mByteBufferY != null && GLRenderer.this.mByteBufferU != null && GLRenderer.this.mByteBufferV != null) {
                        GLRenderer.this.mByteBufferY.position(0);
                        GLRenderer.this.mByteBufferU.position(0);
                        GLRenderer.this.mByteBufferV.position(0);
                        GLRenderer.this.mGLProgram.buildTextures(GLRenderer.this.mByteBufferY, GLRenderer.this.mByteBufferU, GLRenderer.this.mByteBufferV, GLRenderer.this.mVideoWidth, GLRenderer.this.mVideoHeight);
                        GLRenderer.this.mIsFrameReady = true;
                    }
                }
            }
        });
        this.mSurfaceView.requestRender();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onInit(AVHeader aVHeader) {
        this.mAVHeader = aVHeader;
        this.mInitialized = true;
        LogUtils.d("GLRenderer", "onInit:" + hashCode());
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onPause() {
        this.isPauseRender = true;
        LogUtils.i("GLRenderer", "onPause:" + hashCode());
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onRelease() {
        this.mInitialized = false;
        this.mIsFrameReady = false;
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        LogUtils.d("GLRenderer", "onRelease:" + hashCode());
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onResume() {
        LogUtils.i("GLRenderer", "onResume:" + hashCode());
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl10, int i10, int i11) {
        String k10 = "GLRenderer :: onSurfaceChanged width = "+ i10+ " height = "+ i11+ ", :" + hashCode();
        LogUtils.d("GLRenderer", k10);
        if (this.mSurfaceWidth == i10 && this.mSurfaceHeight == i11) {
            LogUtils.e("GLRenderer", "onSurfaceChanged same size");
            return;
        }
        this.mSurfaceWidth = i10;
        this.mSurfaceHeight = i11;
        this.mViewPortX = 0;
        this.mViewPortY = 0;
        this.mViewPortWidth = i10;
        this.mViewPortHeight = i11;
        this.mShowRect.set(0, 0, i10, i11);
        GLES20.glViewport(this.mViewPortX, this.mViewPortY, this.mViewPortWidth, this.mViewPortHeight);
        Matrix.setLookAtM(this.mViewMatrix, 0, 0.0f, 0.0f, 3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        updateProjection();
        updateMvpMatrix();
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        LogUtils.d("GLRenderer", "GLRenderer :: onSurfaceCreated:" + hashCode());
        GLSurfaceView gLSurfaceView = this.mSurfaceView;
        if (gLSurfaceView != null && gLSurfaceView.getContext() != null) {
            this.mGLProgram.buildProgram(this.mSurfaceView.getContext());
        }
        LogUtils.d("GLRenderer", "GLRenderer :: buildProgram done");
        this.mSurfaceWidth = 0;
        this.mSurfaceHeight = 0;
        this.isPauseRender = false;
        this.mIsFrameReady = false;
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
    }

    public void onVideoSizeChanged(int i10, int i11) {
        setFrameBuffer(i10, i11);
    }

    public void rotateTo(final float f10) {
        LogUtils.i("GLRenderer", "rotateTo angle:" + f10);
        this.mSurfaceView.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.7
            @Override // java.lang.Runnable
            public void run() {
                GLRenderer.this.mRotate = f10;
            }
        });
        this.mSurfaceView.requestRender();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void scaleTo(float f10) {
        LogUtils.i("GLRenderer", "scaleTo scaleThanBefore:" + f10);
        scaleTo(0.0f, 0.0f, f10);
    }

    public void setFisheye(boolean z10) {
        this.mGLProgram.setFisheye(z10);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void setFitType(final int i10) {
        LogUtils.i("GLRendererIOT", "setFitType fitType: " + i10);
        this.mSurfaceView.queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.3
            @Override // java.lang.Runnable
            public void run() {
                GLRenderer.this.mFitType = i10;
                GLRenderer.this.updateProjection();
                GLRenderer.this.updateMvpMatrix();
            }
        });
        this.mSurfaceView.requestRender();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void setOnRectChangeListener(OnRectChangeListener onRectChangeListener) {
        this.listener = onRectChangeListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void setVideoAspectRatio(final float f10) {
        LogUtils.i("GLRenderer", "setVideoAspectRatio videoAspectRatio:" + f10);
        this.mSurfaceView.queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.4
            @Override // java.lang.Runnable
            public void run() {
                GLRenderer.this.mVideoAspectRatio = f10;
                GLRenderer.this.updateProjection();
                GLRenderer.this.updateMvpMatrix();
            }
        });
        this.mSurfaceView.requestRender();
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void translateBy(final float f10, final float f11) {
        LogUtils.i("GLRenderer", "translateBy dx:" + f10 + "; dy:" + f11);
        this.mSurfaceView.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.5
            @Override // java.lang.Runnable
            public void run() {
                GLRenderer gLRenderer = GLRenderer.this;
                GLRenderer.access$1116(gLRenderer, GLRenderer.this.mAspectRatioWidth * ((f10 * 2.0f) / gLRenderer.mViewPortWidth));
                GLRenderer gLRenderer2 = GLRenderer.this;
                GLRenderer.access$1416(gLRenderer2, GLRenderer.this.mAspectRatioHeight * ((f11 * 2.0f) / gLRenderer2.mViewPortHeight));
                GLRenderer gLRenderer3 = GLRenderer.this;
                gLRenderer3.mTranslateX = Math.max(gLRenderer3.mTranslateX, -GLRenderer.this.mMaxTranslateX);
                GLRenderer gLRenderer4 = GLRenderer.this;
                gLRenderer4.mTranslateX = Math.min(gLRenderer4.mTranslateX, GLRenderer.this.mMaxTranslateX);
                GLRenderer gLRenderer5 = GLRenderer.this;
                gLRenderer5.mTranslateY = Math.max(gLRenderer5.mTranslateY, -GLRenderer.this.mMaxTranslateY);
                GLRenderer gLRenderer6 = GLRenderer.this;
                gLRenderer6.mTranslateY = Math.min(gLRenderer6.mTranslateY, GLRenderer.this.mMaxTranslateY);
                GLRenderer.this.updateMvpMatrix();
            }
        });
        this.mSurfaceView.requestRender();
    }

    public boolean updateBGColors(float[] fArr) {
        if (fArr != null) {
            float[] fArr2 = this.mBackgroundColors;
            if (fArr2.length == fArr.length) {
                System.arraycopy(fArr, 0, fArr2, 0, fArr2.length);
                LogUtils.i("GLRenderer", "updateBGColors set background color success, color is " + Arrays.toString(this.mBackgroundColors));
                return true;
            }
        }
        LogUtils.e("GLRenderer", "updateBGColors set background color failed, param is null or param.length is not mBackgroundColors.length");
        return false;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public float scaleTo(final float f10, final float f11, float f12) {
        LogUtils.i("GLRenderer", "scaleTo centerX:" + f10 + "; centerY:" + f11 + "; scaleThanBefore:" + f12);
        final float f13 = f12 * this.mScale;
        this.mSurfaceView.post(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderer.6
            @Override // java.lang.Runnable
            public void run() {
                float f14;
                float f15 = f13;
                if (f15 >= 1.0f && f15 <= 3.0f) {
                    float f16 = ((f10 - (GLRenderer.this.mViewPortWidth / 2.0f)) * 2.0f) / GLRenderer.this.mViewPortWidth;
                    float f17 = (((GLRenderer.this.mViewPortHeight / 2.0f) - f11) * 2.0f) / GLRenderer.this.mViewPortHeight;
                    GLRenderer.this.mTempMatrix[0] = f16;
                    GLRenderer.this.mTempMatrix[1] = f17;
                    float f18 = 0.0f;
                    GLRenderer.this.mTempMatrix[2] = 0.0f;
                    GLRenderer.this.mTempMatrix[3] = 1.0f;
                    Matrix.multiplyMM(GLRenderer.this.mTempMatrix, 0, GLRenderer.this.mInvertMVPMatrix, 0, GLRenderer.this.mTempMatrix, 0);
                    GLRenderer.this.mTempMatrix[2] = 0.0f;
                    GLRenderer.this.mTempMatrix[3] = 1.0f;
                    float[] fArr = new float[16];
                    Matrix.setIdentityM(fArr, 0);
                    float f19 = f13;
                    Matrix.scaleM(fArr, 0, f19, f19, 1.0f);
                    Matrix.rotateM(fArr, 0, GLRenderer.this.mRotate, 0.0f, 0.0f, 1.0f);
                    Matrix.multiplyMM(fArr, 0, GLRenderer.this.mViewMatrix, 0, fArr, 0);
                    Matrix.multiplyMM(fArr, 0, GLRenderer.this.mProjectionMatrix, 0, fArr, 0);
                    Matrix.multiplyMM(GLRenderer.this.mTempMatrix, 0, fArr, 0, GLRenderer.this.mTempMatrix, 0);
                    GLRenderer gLRenderer = GLRenderer.this;
                    gLRenderer.mTranslateX = GLRenderer.this.mAspectRatioWidth * (f16 - gLRenderer.mTempMatrix[0]);
                    GLRenderer gLRenderer2 = GLRenderer.this;
                    gLRenderer2.mTranslateY = GLRenderer.this.mAspectRatioHeight * (f17 - gLRenderer2.mTempMatrix[1]);
                    GLRenderer.this.mTempMatrix[0] = -1.0f;
                    GLRenderer.this.mTempMatrix[1] = 1.0f;
                    GLRenderer.this.mTempMatrix[2] = 0.0f;
                    GLRenderer.this.mTempMatrix[3] = 1.0f;
                    GLRenderer.this.mTempMatrix[4] = -1.0f;
                    GLRenderer.this.mTempMatrix[5] = -1.0f;
                    GLRenderer.this.mTempMatrix[6] = 0.0f;
                    GLRenderer.this.mTempMatrix[7] = 1.0f;
                    GLRenderer.this.mTempMatrix[8] = 1.0f;
                    GLRenderer.this.mTempMatrix[9] = -1.0f;
                    GLRenderer.this.mTempMatrix[10] = 0.0f;
                    GLRenderer.this.mTempMatrix[11] = 1.0f;
                    GLRenderer.this.mTempMatrix[12] = 1.0f;
                    GLRenderer.this.mTempMatrix[13] = 1.0f;
                    GLRenderer.this.mTempMatrix[14] = 0.0f;
                    GLRenderer.this.mTempMatrix[15] = 1.0f;
                    Matrix.multiplyMM(GLRenderer.this.mTempMatrix, 0, fArr, 0, GLRenderer.this.mTempMatrix, 0);
                    float max = Math.max(Math.abs(GLRenderer.this.mTempMatrix[0]), Math.abs(GLRenderer.this.mTempMatrix[8]));
                    float max2 = Math.max(Math.abs(GLRenderer.this.mTempMatrix[1]), Math.abs(GLRenderer.this.mTempMatrix[9]));
                    GLRenderer gLRenderer3 = GLRenderer.this;
                    if (max < 1.0f) {
                        f14 = 0.0f;
                    } else {
                        f14 = max - 1.0f;
                    }
                    gLRenderer3.mMaxTranslateX = gLRenderer3.mAspectRatioWidth * f14;
                    GLRenderer gLRenderer4 = GLRenderer.this;
                    if (max2 >= 1.0f) {
                        f18 = max2 - 1.0f;
                    }
                    gLRenderer4.mMaxTranslateY = gLRenderer4.mAspectRatioHeight * f18;
                    GLRenderer gLRenderer5 = GLRenderer.this;
                    gLRenderer5.mTranslateX = Math.max(gLRenderer5.mTranslateX, -GLRenderer.this.mMaxTranslateX);
                    GLRenderer gLRenderer6 = GLRenderer.this;
                    gLRenderer6.mTranslateX = Math.min(gLRenderer6.mTranslateX, GLRenderer.this.mMaxTranslateX);
                    GLRenderer gLRenderer7 = GLRenderer.this;
                    gLRenderer7.mTranslateY = Math.max(gLRenderer7.mTranslateY, -GLRenderer.this.mMaxTranslateY);
                    GLRenderer gLRenderer8 = GLRenderer.this;
                    gLRenderer8.mTranslateY = Math.min(gLRenderer8.mTranslateY, GLRenderer.this.mMaxTranslateY);
                    GLRenderer.this.mScale = f13;
                    GLRenderer.this.updateMvpMatrix();
                }
            }
        });
        this.mSurfaceView.requestRender();
        return f13;
    }

    public GLRenderer(GLSurfaceView gLSurfaceView, DisplayMetrics displayMetrics, OnRectChangeListener onRectChangeListener) {
        this.TAG = "GLRenderer";
        this.mGLProgram = new GLProgram();
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
        this.mInitialized = false;
        this.mProjectionMatrix = new float[16];
        this.mViewMatrix = new float[16];
        this.mMVPMatrix = new float[16];
        this.mInvertMVPMatrix = new float[16];
        this.mMVPMatrixCalculate = new float[16];
        this.mInvertMVPMatrixCalculate = new float[16];
        this.mTempMatrix = new float[16];
        this.mMaxTranslateX = 0.0f;
        this.mMaxTranslateY = 0.0f;
        this.mAspectRatioWidth = 1.0f;
        this.mAspectRatioHeight = 1.0f;
        this.mTranslateX = 0.0f;
        this.mTranslateY = 0.0f;
        this.mScale = 1.0f;
        this.mRotate = 0.0f;
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        this.mViewPortX = 0;
        this.mViewPortY = 0;
        this.mFitType = 1;
        this.mVideoAspectRatio = DEFAULT_VIDEO_ASPECT_RATIO;
        this.mIsFrameReady = false;
        this.isPauseRender = true;
        this.preGetFrameTime = 0L;
        this.count = 0;
        this.realVideoRectF = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        this.mBackgroundColors = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
        this.closeUpRect = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
        this.mShowRect = new Rect(0, 0, 0, 0);
        this.mVideoPoint = new Point(0, 0);
        this.mConverMatrix = new float[]{-1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f};
        this.mSurfaceView = gLSurfaceView;
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
        this.listener = onRectChangeListener;
    }
}
