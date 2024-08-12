package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.os.HandlerThread;
import android.os.Message;
import com.tencentcs.iotvideo.utils.IHandlerConsumer;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.WeakHandler;
/* loaded from: classes2.dex */
public class GLRenderThread implements IHandlerConsumer {
    public static final int MSG_ID_SURFACE_CREATED = 1;
    public static final int MSG_ID_SURFACE_DESTROY = 4;
    public static final int MSG_ID_SURFACE_SIZE_CHANGE = 2;
    public static final int MSG_ID_SURFACE_UPDATE_FRAME = 3;
    private static final String TAG = "GLRenderThread";
    private static final int UPDATE_TIME_INTERVAL = 16;
    protected IIoTGLRender mIoTGLRender;
    protected WeakHandler mRenderHandler;
    private HandlerThread renderThread;

    public GLRenderThread() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.renderThread = handlerThread;
        handlerThread.start();
        this.mRenderHandler = new WeakHandler(this, this.renderThread.getLooper());
    }

    public void pause() {
        LogUtils.i(TAG, "pause");
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null && weakHandler.hasMessages(3)) {
            this.mRenderHandler.removeMessages(3);
        }
    }

    public void queueEvent(Runnable runnable) {
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null) {
            weakHandler.post(runnable);
        } else {
            LogUtils.i(TAG, "queueEvent mRenderHandler is null");
        }
    }

    @Override // com.tencentcs.iotvideo.utils.IHandlerConsumer
    public void receiveHandlerMessage(Message message) {
        if (3 == message.what) {
            IIoTGLRender iIoTGLRender = this.mIoTGLRender;
            if (iIoTGLRender != null) {
                iIoTGLRender.onIoTUpdateFrame();
            }
            WeakHandler weakHandler = this.mRenderHandler;
            if (weakHandler == null) {
                LogUtils.i(TAG, "receiveHandlerMessage is null");
            } else {
                weakHandler.sendEmptyMessageDelayed(3, 16L);
            }
        }
    }

    public void release() {
        LogUtils.i(TAG, "release");
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null) {
            weakHandler.removeCallbacksAndMessages(null);
            this.mRenderHandler = null;
        }
        HandlerThread handlerThread = this.renderThread;
        if (handlerThread != null) {
            handlerThread.quit();
            this.renderThread = null;
        }
    }

    public void resume() {
        LogUtils.i(TAG, "resume");
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null && weakHandler.hasMessages(3)) {
            this.mRenderHandler.removeMessages(3);
        }
        WeakHandler weakHandler2 = this.mRenderHandler;
        if (weakHandler2 == null) {
            LogUtils.i(TAG, "mRenderHandler is null");
        } else {
            weakHandler2.sendEmptyMessage(3);
        }
    }

    public void sendMsgToRenderThread(Message message) {
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null && message != null) {
            weakHandler.sendMessage(message);
        }
    }

    public void setIoTGLRender(IIoTGLRender iIoTGLRender) {
        this.mIoTGLRender = iIoTGLRender;
    }

    public void start() {
        LogUtils.i(TAG, "start");
        if (this.renderThread == null) {
            HandlerThread handlerThread = new HandlerThread(TAG);
            this.renderThread = handlerThread;
            handlerThread.start();
        }
        if (this.mRenderHandler == null) {
            this.mRenderHandler = new WeakHandler(this, this.renderThread.getLooper());
        }
        if (this.mRenderHandler.hasMessages(3)) {
            this.mRenderHandler.removeMessages(3);
        }
        this.mRenderHandler.sendEmptyMessage(3);
    }

    public void queueEvent(Runnable runnable, long j10) {
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null) {
            weakHandler.postDelayed(runnable, j10);
        }
    }
}
