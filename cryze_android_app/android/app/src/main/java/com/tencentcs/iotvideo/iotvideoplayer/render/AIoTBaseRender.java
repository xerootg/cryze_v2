package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender;
import com.tencentcs.iotvideo.iotvideoplayer.OnRectChangeListener;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.nio.ByteBuffer;
/* loaded from: classes2.dex */
public abstract class AIoTBaseRender implements IVideoGestureRender {
    private static final float DEFAULT_VIDEO_ASPECT_RATIO = 1.3333334f;
    public static final int FIT_CENTER = 1;
    public static final int FIT_XY = 0;
    public static final float MAX_VIDEO_SCALE = 3.0f;
    public static final float MIN_VIDEO_SCALE = 1.0f;
    private static final String TAG = "AIoTBaseRender";
    private static float[] mProjectionMatrix = new float[16];
    protected volatile boolean isPauseRender;
    private AVHeader mAVHeader;
    private float mAspectRatioHeight;
    private float mAspectRatioWidth;
    private ByteBuffer mByteBufferU;
    private ByteBuffer mByteBufferV;
    private ByteBuffer mByteBufferY;
    private final float[] mConverMatrix;
    private int mFitType;
    protected GLProgram mGLProgram;
    private volatile boolean mInitialized;
    private final float[] mInvertMVPMatrix;
    private final float[] mInvertMVPMatrixCalculate;
    protected volatile boolean mIsFrameReady;
    private final float[] mMVPMatrix;
    private final float[] mMVPMatrixCalculate;
    private float mMaxTranslateX;
    private float mMaxTranslateY;
    private OnRectChangeListener mRectChangeListener;
    private float mRotate;
    private float mScale;
    private int mScreenHeight;
    private int mScreenWidth;
    private final Rect mShowRect;
    private int mSurfaceHeight;
    private int mSurfaceWidth;
    private final float[] mTempMatrix;
    private float mTranslateX;
    private float mTranslateY;
    private float mVideoAspectRatio;
    private int mVideoHeight;
    private final Point mVideoPoint;
    private int mVideoWidth;
    private final float[] mViewMatrix;
    private int mViewPortHeight;
    private int mViewPortWidth;
    private int mViewPortX;
    private int mViewPortY;
    private final RectF realVideoRectF;

    public AIoTBaseRender(DisplayMetrics displayMetrics) {
        this.mGLProgram = new GLProgram();
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
        this.mInitialized = false;
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
        this.realVideoRectF = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        this.isPauseRender = true;
        this.mShowRect = new Rect(0, 0, 0, 0);
        this.mVideoPoint = new Point(0, 0);
        this.mConverMatrix = new float[]{-1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f};
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
    }

    public static /* synthetic */ float access$1216(AIoTBaseRender aIoTBaseRender, float f10) {
        float f11 = aIoTBaseRender.mTranslateY + f10;
        aIoTBaseRender.mTranslateY = f11;
        return f11;
    }

    public static /* synthetic */ float access$916(AIoTBaseRender aIoTBaseRender, float f10) {
        float f11 = aIoTBaseRender.mTranslateX + f10;
        aIoTBaseRender.mTranslateX = f11;
        return f11;
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
        Matrix.multiplyMM(this.mMVPMatrix, 0, mProjectionMatrix, 0, this.mTempMatrix, 0);
        Matrix.invertM(this.mInvertMVPMatrix, 0, this.mMVPMatrix, 0);
        GLProgram gLProgram = this.mGLProgram;
        if (gLProgram != null) {
            gLProgram.updateMvp(this.mMVPMatrix);
        }
        Matrix.setIdentityM(this.mMVPMatrixCalculate, 0);
        float[] fArr3 = this.mMVPMatrixCalculate;
        float f11 = this.mScale;
        Matrix.scaleM(fArr3, 0, f11, f11, 1.0f);
        Matrix.rotateM(this.mMVPMatrixCalculate, 0, this.mRotate, 0.0f, 0.0f, 1.0f);
        float[] fArr4 = this.mMVPMatrixCalculate;
        Matrix.multiplyMM(fArr4, 0, this.mViewMatrix, 0, fArr4, 0);
        float[] fArr5 = this.mMVPMatrixCalculate;
        Matrix.multiplyMM(fArr5, 0, mProjectionMatrix, 0, fArr5, 0);
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
            Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10.0f);
            return;
        }
        float f11 = this.mVideoAspectRatio;
        if (f10 > f11) {
            float f12 = f10 / f11;
            this.mAspectRatioWidth = f12;
            this.mAspectRatioHeight = 1.0f;
            Matrix.orthoM(mProjectionMatrix, 0, -f12, f12, -1.0f, 1.0f, 1.0f, 10.0f);
            return;
        }
        this.mAspectRatioWidth = 1.0f;
        float f13 = f11 / f10;
        this.mAspectRatioHeight = f13;
        Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -f13, f13, 1.0f, 10.0f);
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
        this.realVideoRectF.set(f10, f12, f11, f13);
        Rect rect = this.mShowRect;
        int i10 = this.mViewPortWidth;
        int i11 = this.mViewPortHeight;
        rect.set((int) (((min3 + 1.0f) * i10) / 2.0f), (int) ((((-max6) + 1.0f) * i11) / 2.0f), (int) (((max3 + 1.0f) * i10) / 2.0f), (int) ((((-min6) + 1.0f) * i11) / 2.0f));
        OnRectChangeListener onRectChangeListener = this.mRectChangeListener;
        if (onRectChangeListener != null) {
            onRectChangeListener.updateRect(f10, f12, f11, f13);
        }
    }

    public void buildTexturesStatus(boolean z10) {
        LogUtils.i(TAG, "buildTexturesStatus buildStatus :" + z10);
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
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AIoTBaseRender.this) {
                    if (AIoTBaseRender.this.mByteBufferY != null && AIoTBaseRender.this.mByteBufferU != null && AIoTBaseRender.this.mByteBufferV != null) {
                        boolean z10 = false;
                        AIoTBaseRender.this.mByteBufferY.position(0);
                        AIoTBaseRender.this.mByteBufferU.position(0);
                        AIoTBaseRender.this.mByteBufferV.position(0);
                        AIoTBaseRender aIoTBaseRender = AIoTBaseRender.this;
                        int buildTextures = aIoTBaseRender.mGLProgram.buildTextures(aIoTBaseRender.mByteBufferY, AIoTBaseRender.this.mByteBufferU, AIoTBaseRender.this.mByteBufferV, AIoTBaseRender.this.mVideoWidth, AIoTBaseRender.this.mVideoHeight);
                        if (!AIoTBaseRender.this.mIsFrameReady) {
                            AIoTBaseRender aIoTBaseRender2 = AIoTBaseRender.this;
                            if (buildTextures == 0) {
                                z10 = true;
                            }
                            aIoTBaseRender2.buildTexturesStatus(z10);
                        }
                        AIoTBaseRender.this.mIsFrameReady = true;
                    }
                }
            }
        });
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onInit(AVHeader aVHeader) {
        this.mAVHeader = aVHeader;
        this.mInitialized = true;
        LogUtils.d(TAG, "onInit");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onPause() {
        LogUtils.i(TAG, "onPause");
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
        LogUtils.d(TAG, "onRelease");
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onResume() {
        LogUtils.i(TAG, "onResume");
    }

    public void onSurfaceChanged(int i10, int i11) {
        String k10 = "onSurfaceChanged width = "+ i10+ " height = "+ i11+ "; threadId:" + Thread.currentThread().getId();
        LogUtils.d(TAG, k10);
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

    public void onSurfaceCreated(Context context) {
        LogUtils.d(TAG, "onSurfaceCreated:" + Thread.currentThread().getId());
        this.mGLProgram.buildProgram(context);
        LogUtils.d(TAG, "onSurfaceCreated uildProgram done");
        this.isPauseRender = false;
        this.mIsFrameReady = false;
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
    }

    public void onSurfaceDestroy() {
        this.isPauseRender = true;
        GLProgram gLProgram = this.mGLProgram;
        if (gLProgram != null) {
            gLProgram.release();
        }
    }

    public void onVideoSizeChanged(int i10, int i11) {
        setFrameBuffer(i10, i11);
    }

    public abstract void queueEvent(Runnable runnable);

    public boolean renderFrame() {
        boolean z10;
        if (this.isPauseRender) {
            return false;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(16384);
        if (this.mIsFrameReady) {
            GLProgram gLProgram = this.mGLProgram;
            if (gLProgram == null) {
                return false;
            }
            gLProgram.drawFrame();
            return true;
        }
        if (this.mByteBufferY != null && this.mByteBufferU != null && this.mByteBufferV != null) {
            synchronized (this) {
                this.mByteBufferY.position(0);
                this.mByteBufferU.position(0);
                this.mByteBufferV.position(0);
                int buildTextures = this.mGLProgram.buildTextures(this.mByteBufferY, this.mByteBufferU, this.mByteBufferV, this.mVideoWidth, this.mVideoHeight);
                if (!this.mIsFrameReady) {
                    if (buildTextures == 0) {
                        z10 = true;
                    } else {
                        z10 = false;
                    }
                    buildTexturesStatus(z10);
                }
                this.mIsFrameReady = true;
            }
        }
        return false;
    }

    public void rotateTo(float f10) {
        this.mRotate = f10;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void scaleTo(float f10) {
        scaleTo(0.0f, 0.0f, f10);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void setFitType(final int i10) {
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender.2
            @Override // java.lang.Runnable
            public void run() {
                AIoTBaseRender.this.mFitType = i10;
                AIoTBaseRender.this.updateProjection();
                AIoTBaseRender.this.updateMvpMatrix();
            }
        });
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void setOnRectChangeListener(OnRectChangeListener onRectChangeListener) {
        this.mRectChangeListener = onRectChangeListener;
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void setVideoAspectRatio(final float f10) {
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender.3
            @Override // java.lang.Runnable
            public void run() {
                AIoTBaseRender.this.mVideoAspectRatio = f10;
                AIoTBaseRender.this.updateProjection();
                AIoTBaseRender.this.updateMvpMatrix();
            }
        });
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public void translateBy(final float f10, final float f11) {
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender.4
            @Override // java.lang.Runnable
            public void run() {
                AIoTBaseRender aIoTBaseRender = AIoTBaseRender.this;
                AIoTBaseRender.access$916(aIoTBaseRender, AIoTBaseRender.this.mAspectRatioWidth * ((f10 * 2.0f) / aIoTBaseRender.mViewPortWidth));
                AIoTBaseRender aIoTBaseRender2 = AIoTBaseRender.this;
                AIoTBaseRender.access$1216(aIoTBaseRender2, AIoTBaseRender.this.mAspectRatioHeight * ((f11 * 2.0f) / aIoTBaseRender2.mViewPortHeight));
                AIoTBaseRender aIoTBaseRender3 = AIoTBaseRender.this;
                aIoTBaseRender3.mTranslateX = Math.max(aIoTBaseRender3.mTranslateX, -AIoTBaseRender.this.mMaxTranslateX);
                AIoTBaseRender aIoTBaseRender4 = AIoTBaseRender.this;
                aIoTBaseRender4.mTranslateX = Math.min(aIoTBaseRender4.mTranslateX, AIoTBaseRender.this.mMaxTranslateX);
                AIoTBaseRender aIoTBaseRender5 = AIoTBaseRender.this;
                aIoTBaseRender5.mTranslateY = Math.max(aIoTBaseRender5.mTranslateY, -AIoTBaseRender.this.mMaxTranslateY);
                AIoTBaseRender aIoTBaseRender6 = AIoTBaseRender.this;
                aIoTBaseRender6.mTranslateY = Math.min(aIoTBaseRender6.mTranslateY, AIoTBaseRender.this.mMaxTranslateY);
                AIoTBaseRender.this.updateMvpMatrix();
            }
        });
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.IVideoGestureRender
    public float scaleTo(final float f10, final float f11, float f12) {
        LogUtils.d(TAG, "scaleTo scaleThanBefore = " + f12 + " mScale = " + this.mScale);
        final float f13 = f12 * this.mScale;
        queueEvent(new Runnable() { // from class: com.tencentcs.iotvideo.iotvideoplayer.render.AIoTBaseRender.5
            @Override // java.lang.Runnable
            public void run() {
                float f14;
                float f15 = f13;
                if (f15 >= 1.0f && f15 <= 3.0f) {
                    float f16 = ((f10 - (AIoTBaseRender.this.mViewPortWidth / 2.0f)) * 2.0f) / AIoTBaseRender.this.mViewPortWidth;
                    float f17 = (((AIoTBaseRender.this.mViewPortHeight / 2.0f) - f11) * 2.0f) / AIoTBaseRender.this.mViewPortHeight;
                    AIoTBaseRender.this.mTempMatrix[0] = f16;
                    AIoTBaseRender.this.mTempMatrix[1] = f17;
                    float f18 = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[2] = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[3] = 1.0f;
                    Matrix.multiplyMM(AIoTBaseRender.this.mTempMatrix, 0, AIoTBaseRender.this.mInvertMVPMatrix, 0, AIoTBaseRender.this.mTempMatrix, 0);
                    AIoTBaseRender.this.mTempMatrix[2] = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[3] = 1.0f;
                    float[] fArr = new float[16];
                    Matrix.setIdentityM(fArr, 0);
                    float f19 = f13;
                    Matrix.scaleM(fArr, 0, f19, f19, 1.0f);
                    Matrix.rotateM(fArr, 0, AIoTBaseRender.this.mRotate, 0.0f, 0.0f, 1.0f);
                    Matrix.multiplyMM(fArr, 0, AIoTBaseRender.this.mViewMatrix, 0, fArr, 0);
                    Matrix.multiplyMM(fArr, 0, AIoTBaseRender.mProjectionMatrix, 0, fArr, 0);
                    Matrix.multiplyMM(AIoTBaseRender.this.mTempMatrix, 0, fArr, 0, AIoTBaseRender.this.mTempMatrix, 0);
                    AIoTBaseRender aIoTBaseRender = AIoTBaseRender.this;
                    aIoTBaseRender.mTranslateX = AIoTBaseRender.this.mAspectRatioWidth * (f16 - aIoTBaseRender.mTempMatrix[0]);
                    AIoTBaseRender aIoTBaseRender2 = AIoTBaseRender.this;
                    aIoTBaseRender2.mTranslateY = AIoTBaseRender.this.mAspectRatioHeight * (f17 - aIoTBaseRender2.mTempMatrix[1]);
                    AIoTBaseRender.this.mTempMatrix[0] = -1.0f;
                    AIoTBaseRender.this.mTempMatrix[1] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[2] = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[3] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[4] = -1.0f;
                    AIoTBaseRender.this.mTempMatrix[5] = -1.0f;
                    AIoTBaseRender.this.mTempMatrix[6] = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[7] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[8] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[9] = -1.0f;
                    AIoTBaseRender.this.mTempMatrix[10] = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[11] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[12] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[13] = 1.0f;
                    AIoTBaseRender.this.mTempMatrix[14] = 0.0f;
                    AIoTBaseRender.this.mTempMatrix[15] = 1.0f;
                    Matrix.multiplyMM(AIoTBaseRender.this.mTempMatrix, 0, fArr, 0, AIoTBaseRender.this.mTempMatrix, 0);
                    float max = Math.max(Math.abs(AIoTBaseRender.this.mTempMatrix[0]), Math.abs(AIoTBaseRender.this.mTempMatrix[8]));
                    float max2 = Math.max(Math.abs(AIoTBaseRender.this.mTempMatrix[1]), Math.abs(AIoTBaseRender.this.mTempMatrix[9]));
                    AIoTBaseRender aIoTBaseRender3 = AIoTBaseRender.this;
                    if (max < 1.0f) {
                        f14 = 0.0f;
                    } else {
                        f14 = max - 1.0f;
                    }
                    aIoTBaseRender3.mMaxTranslateX = aIoTBaseRender3.mAspectRatioWidth * f14;
                    AIoTBaseRender aIoTBaseRender4 = AIoTBaseRender.this;
                    if (max2 >= 1.0f) {
                        f18 = max2 - 1.0f;
                    }
                    aIoTBaseRender4.mMaxTranslateY = aIoTBaseRender4.mAspectRatioHeight * f18;
                    AIoTBaseRender aIoTBaseRender5 = AIoTBaseRender.this;
                    aIoTBaseRender5.mTranslateX = Math.max(aIoTBaseRender5.mTranslateX, -AIoTBaseRender.this.mMaxTranslateX);
                    AIoTBaseRender aIoTBaseRender6 = AIoTBaseRender.this;
                    aIoTBaseRender6.mTranslateX = Math.min(aIoTBaseRender6.mTranslateX, AIoTBaseRender.this.mMaxTranslateX);
                    AIoTBaseRender aIoTBaseRender7 = AIoTBaseRender.this;
                    aIoTBaseRender7.mTranslateY = Math.max(aIoTBaseRender7.mTranslateY, -AIoTBaseRender.this.mMaxTranslateY);
                    AIoTBaseRender aIoTBaseRender8 = AIoTBaseRender.this;
                    aIoTBaseRender8.mTranslateY = Math.min(aIoTBaseRender8.mTranslateY, AIoTBaseRender.this.mMaxTranslateY);
                    AIoTBaseRender.this.mScale = f13;
                    AIoTBaseRender.this.updateMvpMatrix();
                }
            }
        });
        return f13;
    }

    public AIoTBaseRender() {
        this.mGLProgram = new GLProgram();
        this.mByteBufferY = null;
        this.mByteBufferU = null;
        this.mByteBufferV = null;
        this.mInitialized = false;
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
        this.realVideoRectF = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        this.isPauseRender = true;
        this.mShowRect = new Rect(0, 0, 0, 0);
        this.mVideoPoint = new Point(0, 0);
        this.mConverMatrix = new float[]{-1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f};
    }
}
