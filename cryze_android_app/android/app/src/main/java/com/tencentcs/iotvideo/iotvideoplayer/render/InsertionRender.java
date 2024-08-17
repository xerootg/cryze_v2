package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.os.HandlerThread;
import android.os.Message;
import com.tencentcs.iotvideo.iotvideoplayer.AVideoRender;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion;
import com.tencentcs.iotvideo.utils.AVDataUtils;
import com.tencentcs.iotvideo.utils.IHandlerConsumer;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.WeakHandler;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class InsertionRender extends AVideoRender implements IHandlerConsumer {
    private static final int MAX_CACHE_REGION_SIZE = 5;
    private static final long MAX_PTS_DIFF = 1000;
    private static final String TAG = "InsertionRender";
    private static final int WHAT_RENDER = 1;
    private AVHeader mAvHeader;
    private AVData mCurrentRenderData;
    private CameraRenderRegion mCurrentRenderInfo;
    private int mFrameInterval;
    private CameraRenderRegion mPreRenderInfo;
    protected WeakHandler mRenderHandler;
    private CameraRenderRegion mRenderInfo;
    private HandlerThread mRenderThread;
    private int mRenderTimes;
    private IVideoRender mVideoRender;
    private final List<AVData> mAvDataList = new ArrayList();
    private final List<CameraRenderRegion> mCacheRenderRegions = new ArrayList();
    private final Object mLock = new Object();

    private void addCacheRegion(CameraRenderRegion cameraRenderRegion) {
        if (this.mCacheRenderRegions.size() == 5) {
            this.mCacheRenderRegions.remove(0);
        }
        this.mCacheRenderRegions.add(cameraRenderRegion.copy());
    }

    private void calculateCoordinate() {
        if (this.mCacheRenderRegions.isEmpty()) {
            return;
        }
        int i10 = 0;
        int i11 = 0;
        for (int i12 = 0; i12 < this.mCacheRenderRegions.size(); i12++) {
            CameraRenderRegion cameraRenderRegion = this.mCacheRenderRegions.get(i12);
            i10 += cameraRenderRegion.getLeft();
            i11 += cameraRenderRegion.getTop();
        }
        this.mCurrentRenderInfo.setLeft(i10 / this.mCacheRenderRegions.size());
        this.mCurrentRenderInfo.setTop(i11 / this.mCacheRenderRegions.size());
    }

    private void createAVDataList(CameraRenderRegion cameraRenderRegion) {
        this.mAvDataList.clear();
        if (cameraRenderRegion != null) {
            AVData aVData = new AVData();
            int height = cameraRenderRegion.getHeight() * cameraRenderRegion.getWidth();
            aVData.size = height;
            aVData.data = ByteBuffer.allocateDirect(height);
            int i10 = aVData.size / 4;
            aVData.size1 = i10;
            aVData.data1 = ByteBuffer.allocateDirect(i10);
            int i11 = aVData.size / 4;
            aVData.size2 = i11;
            aVData.data2 = ByteBuffer.allocateDirect(i11);
            this.mAvDataList.add(aVData);
        }
    }

    private void renderNextFrame(int i10) {
        if (i10 >= this.mRenderTimes) {
            return;
        }
        synchronized (this.mLock) {
            int top = this.mCurrentRenderInfo.getTop() - this.mPreRenderInfo.getTop();
            int left = this.mCurrentRenderInfo.getLeft() - this.mPreRenderInfo.getLeft();
            int integer = this.mAvHeader.getInteger(AVHeader.KEY_VIDEO_TYPE, 0);
            ArrayList arrayList = new ArrayList();
            if (left == 0 && top == 0) {
                createAVDataList(this.mCurrentRenderInfo);
                arrayList.add(this.mCurrentRenderInfo);
                AVDataUtils.setChildrenAVData(integer, this.mCurrentRenderData, this.mAvDataList, arrayList);
                this.mVideoRender.onFrameUpdate(this.mAvDataList.get(0));
                return;
            }
            this.mRenderInfo.setTop(((top / this.mRenderTimes) * i10) + this.mPreRenderInfo.getTop());
            this.mRenderInfo.setLeft(((left / this.mRenderTimes) * i10) + this.mPreRenderInfo.getLeft());
            createAVDataList(this.mRenderInfo);
            arrayList.add(this.mRenderInfo);
            AVDataUtils.setChildrenAVData(integer, this.mCurrentRenderData, this.mAvDataList, arrayList);
            this.mVideoRender.onFrameUpdate(this.mAvDataList.get(0));
            Message obtainMessage = this.mRenderHandler.obtainMessage();
            obtainMessage.what = 1;
            obtainMessage.obj = Integer.valueOf(i10 + 1);
            this.mRenderHandler.sendMessageDelayed(obtainMessage, this.mFrameInterval);
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onFrameUpdate(AVData aVData) {
        CameraRenderRegion cameraRenderRegion;
        this.mRenderHandler.removeMessages(1);
        if (aVData != null && (cameraRenderRegion = aVData.seiRenderRegion) != null) {
            this.mCurrentRenderData = aVData;
            if (this.mRenderInfo == null) {
                this.mRenderInfo = cameraRenderRegion.copy();
            }
            if (this.mCurrentRenderInfo == null) {
                this.mCurrentRenderInfo = aVData.seiRenderRegion.copy();
            }
            this.mPreRenderInfo = this.mCurrentRenderInfo.copy();
            addCacheRegion(aVData.seiRenderRegion);
            calculateCoordinate();
            renderNextFrame(0);
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onInit(AVHeader aVHeader) {
        super.onInit(aVHeader);
        LogUtils.i(TAG, "onInit:" + aVHeader);
        if (aVHeader != null && aVHeader.getVideoRenderInfo() != null) {
            this.mAvHeader = aVHeader;
            int integer = 1000 / aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 15);
            int i10 = integer / 20;
            this.mRenderTimes = i10;
            this.mFrameInterval = integer / i10;
            if (this.mRenderThread == null) {
                HandlerThread handlerThread = new HandlerThread(TAG);
                this.mRenderThread = handlerThread;
                handlerThread.start();
            }
            if (this.mRenderHandler == null) {
                this.mRenderHandler = new WeakHandler(this, this.mRenderThread.getLooper());
            }
            StringBuilder sb2 = new StringBuilder("onInit mFrameInterval:");
            sb2.append(this.mFrameInterval);
            sb2.append(" ,renderTimes:");
            LogUtils.i(TAG, sb2.toString() + this.mRenderTimes);
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onRelease() {
        super.onRelease();
        this.mAvDataList.clear();
        this.mCurrentRenderData = null;
        this.mPreRenderInfo = null;
        this.mCurrentRenderInfo = null;
        this.mRenderInfo = null;
        this.mVideoRender = null;
        this.mCacheRenderRegions.clear();
        WeakHandler weakHandler = this.mRenderHandler;
        if (weakHandler != null) {
            weakHandler.removeCallbacksAndMessages(this);
            this.mRenderHandler = null;
        }
        HandlerThread handlerThread = this.mRenderThread;
        if (handlerThread != null) {
            handlerThread.quit();
            this.mRenderThread = null;
        }
    }

    @Override // com.tencentcs.iotvideo.utils.IHandlerConsumer
    public void receiveHandlerMessage(Message message) {
        if (message.what == 1) {
            renderNextFrame(((Integer) message.obj).intValue());
        }
    }

    public void setRender(IVideoRender iVideoRender) {
        this.mVideoRender = iVideoRender;
    }
}
