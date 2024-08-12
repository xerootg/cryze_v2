package com.tencentcs.iotvideo.iotvideoplayer.render;

import com.tencentcs.iotvideo.iotvideoplayer.AVideoRender;
import com.tencentcs.iotvideo.iotvideoplayer.IVideoRender;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion;
import com.tencentcs.iotvideo.iotvideoplayer.codec.VideoRenderInfo;
import com.tencentcs.iotvideo.utils.AVDataUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/* loaded from: classes2.dex */
public class GLRenderGroup extends AVideoRender {
    private static final String TAG = "GLRenderGroup";
    private AVHeader mAvHeader;
    private InsertionRender mInsertionRender;
    private final Map<Long, IVideoRender> mRenders = new HashMap();
    private final Map<Long, AVData> mChildrenAVDataMap = new HashMap();
    private final Object mLock = new Object();
    private boolean mStartRender = false;

    public GLRenderGroup() {
        LogUtils.i(TAG, "GLRenderGroup()");
    }

    private void initReplenishRender() {
        AVHeader aVHeader = this.mAvHeader;
        if (aVHeader != null && aVHeader.getVideoRenderInfo() != null && this.mAvHeader.getVideoRenderInfo().getRegionListSize() >= 2 && this.mRenders.size() >= 2) {
            InsertionRender insertionRender = new InsertionRender();
            this.mInsertionRender = insertionRender;
            insertionRender.setRender(this.mRenders.get(1L));
            this.mInsertionRender.onInit(this.mAvHeader);
        }
    }

    private void updateHeaderRegion(AVData aVData) {
        VideoRenderInfo videoRenderInfo;
        CameraRenderRegion renderRegionByIndex;
        CameraRenderRegion cameraRenderRegion = aVData.seiRenderRegion;
        if (cameraRenderRegion == null || (videoRenderInfo = this.mAvHeader.getVideoRenderInfo()) == null || (renderRegionByIndex = videoRenderInfo.getRenderRegionByIndex(1)) == null) {
            return;
        }
        if (this.mInsertionRender == null) {
            initReplenishRender();
        }
        if (renderRegionByIndex.getLeft() != cameraRenderRegion.getLeft()) {
            renderRegionByIndex.setLeft(cameraRenderRegion.getLeft());
        }
        if (renderRegionByIndex.getTop() != cameraRenderRegion.getTop()) {
            renderRegionByIndex.setTop(cameraRenderRegion.getTop());
        }
        if (renderRegionByIndex.getHeight() != cameraRenderRegion.getHeight()) {
            renderRegionByIndex.setHeight(cameraRenderRegion.getHeight());
        }
        if (renderRegionByIndex.getWidth() != cameraRenderRegion.getWidth()) {
            renderRegionByIndex.setWidth(cameraRenderRegion.getWidth());
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onFrameUpdate(AVData aVData) {
        super.onFrameUpdate(aVData);
        synchronized (this.mLock) {
            if (this.mAvHeader == null) {
                LogUtils.e(TAG, "onFrameUpdate(..), failure:the AvHeader is null");
            } else if (this.mRenders.size() <= 0) {
                LogUtils.e(TAG, "onFrameUpdate(..), failure:the size of renders is 0");
            } else if (this.mChildrenAVDataMap.size() <= 0) {
                LogUtils.e(TAG, "onFrameUpdate(..), mChildrenAVDataMap.size() <= 0");
            } else {
                VideoRenderInfo videoRenderInfo = this.mAvHeader.getVideoRenderInfo();
                if (videoRenderInfo != null && videoRenderInfo.getRegionListSize() != 0) {
                    if (!this.mStartRender) {
                        LogUtils.i(TAG, "onFrameUpdate start render");
                        this.mStartRender = true;
                    }
                    updateHeaderRegion(aVData);
                    ArrayList arrayList = new ArrayList();
                    ArrayList arrayList2 = new ArrayList();
                    for (Map.Entry<Long, AVData> entry : this.mChildrenAVDataMap.entrySet()) {
                        CameraRenderRegion renderRegionById = videoRenderInfo.getRenderRegionById(entry.getKey().longValue());
                        if (renderRegionById != null) {
                            arrayList.add(entry.getValue());
                            arrayList2.add(renderRegionById);
                        }
                    }
                    AVDataUtils.setChildrenAVData(this.mAvHeader.getInteger(AVHeader.KEY_VIDEO_TYPE, 0), aVData, arrayList, arrayList2);
                    for (Map.Entry<Long, IVideoRender> entry2 : this.mRenders.entrySet()) {
                        CameraRenderRegion renderRegionById2 = videoRenderInfo.getRenderRegionById(entry2.getKey().longValue());
                        AVData aVData2 = this.mChildrenAVDataMap.get(entry2.getKey());
                        if (renderRegionById2 != null && aVData2 != null) {
                            CameraRenderRegion cameraRenderRegion = aVData.seiRenderRegion;
                            if (cameraRenderRegion != null && cameraRenderRegion.getCameraId() == entry2.getKey().longValue()) {
                                this.mInsertionRender.onFrameUpdate(aVData);
                            } else {
                                entry2.getValue().onFrameUpdate(aVData2);
                            }
                        }
                    }
                    return;
                }
                LogUtils.e(TAG, "onFrameUpdate(..), no videoRenderInfo");
            }
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onInit(AVHeader aVHeader) {
        super.onInit(aVHeader);
        LogUtils.i(TAG, "onInit(..), header = " + aVHeader);
        update(aVHeader, this.mRenders);
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onPause() {
        LogUtils.i(TAG, "onPause");
        synchronized (this.mLock) {
            for (IVideoRender iVideoRender : this.mRenders.values()) {
                iVideoRender.onPause();
            }
            InsertionRender insertionRender = this.mInsertionRender;
            if (insertionRender != null) {
                insertionRender.onPause();
            }
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onRelease() {
        LogUtils.i(TAG, "onRelease");
        synchronized (this.mLock) {
            for (IVideoRender iVideoRender : this.mRenders.values()) {
                iVideoRender.onRelease();
            }
            InsertionRender insertionRender = this.mInsertionRender;
            if (insertionRender != null) {
                insertionRender.onRelease();
                this.mInsertionRender = null;
            }
            this.mRenders.clear();
            this.mChildrenAVDataMap.clear();
        }
    }

    @Override // com.tencentcs.iotvideo.iotvideoplayer.AVideoRender, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender
    public void onResume() {
        LogUtils.i(TAG, "onResume");
        synchronized (this.mLock) {
            for (IVideoRender iVideoRender : this.mRenders.values()) {
                iVideoRender.onResume();
            }
            InsertionRender insertionRender = this.mInsertionRender;
            if (insertionRender != null) {
                insertionRender.onResume();
            }
        }
    }

    public void setChildren(Map<Long, IVideoRender> map) {
        LogUtils.i(TAG, "setChildren(..), childrenMap = " + map);
        synchronized (this.mLock) {
            Map<Long, IVideoRender> map2 = this.mRenders;
            if (map2 != map) {
                map2.clear();
                this.mRenders.putAll(map);
            }
            this.mChildrenAVDataMap.clear();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x009f A[Catch: all -> 0x00f3, TryCatch #0 {, blocks: (B:7:0x0024, B:9:0x002c, B:12:0x0033, B:13:0x003d, B:15:0x0043, B:17:0x0059, B:18:0x0063, B:20:0x0071, B:25:0x009b, B:27:0x009f, B:28:0x00a7, B:29:0x00b1, B:31:0x00b7, B:32:0x00c3, B:34:0x00c7, B:35:0x00d0, B:37:0x00ec, B:38:0x00f1, B:21:0x007b, B:22:0x0085, B:24:0x008b), top: B:44:0x0024 }] */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00b7 A[Catch: all -> 0x00f3, LOOP:1: B:29:0x00b1->B:31:0x00b7, LOOP_END, TryCatch #0 {, blocks: (B:7:0x0024, B:9:0x002c, B:12:0x0033, B:13:0x003d, B:15:0x0043, B:17:0x0059, B:18:0x0063, B:20:0x0071, B:25:0x009b, B:27:0x009f, B:28:0x00a7, B:29:0x00b1, B:31:0x00b7, B:32:0x00c3, B:34:0x00c7, B:35:0x00d0, B:37:0x00ec, B:38:0x00f1, B:21:0x007b, B:22:0x0085, B:24:0x008b), top: B:44:0x0024 }] */
    /* JADX WARN: Removed duplicated region for block: B:34:0x00c7 A[Catch: all -> 0x00f3, TryCatch #0 {, blocks: (B:7:0x0024, B:9:0x002c, B:12:0x0033, B:13:0x003d, B:15:0x0043, B:17:0x0059, B:18:0x0063, B:20:0x0071, B:25:0x009b, B:27:0x009f, B:28:0x00a7, B:29:0x00b1, B:31:0x00b7, B:32:0x00c3, B:34:0x00c7, B:35:0x00d0, B:37:0x00ec, B:38:0x00f1, B:21:0x007b, B:22:0x0085, B:24:0x008b), top: B:44:0x0024 }] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00ec A[Catch: all -> 0x00f3, TryCatch #0 {, blocks: (B:7:0x0024, B:9:0x002c, B:12:0x0033, B:13:0x003d, B:15:0x0043, B:17:0x0059, B:18:0x0063, B:20:0x0071, B:25:0x009b, B:27:0x009f, B:28:0x00a7, B:29:0x00b1, B:31:0x00b7, B:32:0x00c3, B:34:0x00c7, B:35:0x00d0, B:37:0x00ec, B:38:0x00f1, B:21:0x007b, B:22:0x0085, B:24:0x008b), top: B:44:0x0024 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void update(com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader r6, java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r7) {
        /*
            r5 = this;
            if (r6 == 0) goto Lf6
            if (r7 != 0) goto L6
            goto Lf6
        L6:
            java.lang.String r0 = "GLRenderGroup"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            java.lang.String r2 = "update(..), newAvHeader = "
            r1.<init>(r2)
            r1.append(r6)
            java.lang.String r2 = ", childrenMap = "
            r1.append(r2)
            r1.append(r7)
            java.lang.String r1 = r1.toString()
            com.tencentcs.iotvideo.utils.LogUtils.i(r0, r1)
            java.lang.Object r0 = r5.mLock
            monitor-enter(r0)
            r5.mAvHeader = r6     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.codec.VideoRenderInfo r6 = r6.getVideoRenderInfo()     // Catch: java.lang.Throwable -> Lf3
            if (r6 == 0) goto L7b
            int r1 = r6.getRegionListSize()     // Catch: java.lang.Throwable -> Lf3
            if (r1 > 0) goto L33
            goto L7b
        L33:
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r1 = r5.mRenders     // Catch: java.lang.Throwable -> Lf3
            java.util.Set r1 = r1.entrySet()     // Catch: java.lang.Throwable -> Lf3
            java.util.Iterator r1 = r1.iterator()     // Catch: java.lang.Throwable -> Lf3
        L3d:
            boolean r2 = r1.hasNext()     // Catch: java.lang.Throwable -> Lf3
            if (r2 == 0) goto L9b
            java.lang.Object r2 = r1.next()     // Catch: java.lang.Throwable -> Lf3
            java.util.Map$Entry r2 = (java.util.Map.Entry) r2     // Catch: java.lang.Throwable -> Lf3
            java.lang.Object r3 = r2.getKey()     // Catch: java.lang.Throwable -> Lf3
            java.lang.Long r3 = (java.lang.Long) r3     // Catch: java.lang.Throwable -> Lf3
            long r3 = r3.longValue()     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion r3 = r6.getRenderRegionById(r3)     // Catch: java.lang.Throwable -> Lf3
            if (r3 != 0) goto L63
            java.lang.Object r2 = r2.getValue()     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.IVideoRender r2 = (com.tencentcs.iotvideo.iotvideoplayer.IVideoRender) r2     // Catch: java.lang.Throwable -> Lf3
            r2.onRelease()     // Catch: java.lang.Throwable -> Lf3
            goto L3d
        L63:
            java.lang.Object r3 = r2.getValue()     // Catch: java.lang.Throwable -> Lf3
            java.lang.Object r4 = r2.getKey()     // Catch: java.lang.Throwable -> Lf3
            java.lang.Object r4 = r7.get(r4)     // Catch: java.lang.Throwable -> Lf3
            if (r3 == r4) goto L3d
            java.lang.Object r2 = r2.getValue()     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.IVideoRender r2 = (com.tencentcs.iotvideo.iotvideoplayer.IVideoRender) r2     // Catch: java.lang.Throwable -> Lf3
            r2.onRelease()     // Catch: java.lang.Throwable -> Lf3
            goto L3d
        L7b:
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r6 = r5.mRenders     // Catch: java.lang.Throwable -> Lf3
            java.util.Set r6 = r6.entrySet()     // Catch: java.lang.Throwable -> Lf3
            java.util.Iterator r6 = r6.iterator()     // Catch: java.lang.Throwable -> Lf3
        L85:
            boolean r1 = r6.hasNext()     // Catch: java.lang.Throwable -> Lf3
            if (r1 == 0) goto L9b
            java.lang.Object r1 = r6.next()     // Catch: java.lang.Throwable -> Lf3
            java.util.Map$Entry r1 = (java.util.Map.Entry) r1     // Catch: java.lang.Throwable -> Lf3
            java.lang.Object r1 = r1.getValue()     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.IVideoRender r1 = (com.tencentcs.iotvideo.iotvideoplayer.IVideoRender) r1     // Catch: java.lang.Throwable -> Lf3
            r1.onRelease()     // Catch: java.lang.Throwable -> Lf3
            goto L85
        L9b:
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r6 = r5.mRenders     // Catch: java.lang.Throwable -> Lf3
            if (r6 == r7) goto La7
            r6.clear()     // Catch: java.lang.Throwable -> Lf3
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r6 = r5.mRenders     // Catch: java.lang.Throwable -> Lf3
            r6.putAll(r7)     // Catch: java.lang.Throwable -> Lf3
        La7:
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r6 = r5.mRenders     // Catch: java.lang.Throwable -> Lf3
            java.util.Collection r6 = r6.values()     // Catch: java.lang.Throwable -> Lf3
            java.util.Iterator r6 = r6.iterator()     // Catch: java.lang.Throwable -> Lf3
        Lb1:
            boolean r7 = r6.hasNext()     // Catch: java.lang.Throwable -> Lf3
            if (r7 == 0) goto Lc3
            java.lang.Object r7 = r6.next()     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.IVideoRender r7 = (com.tencentcs.iotvideo.iotvideoplayer.IVideoRender) r7     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader r1 = r5.mAvHeader     // Catch: java.lang.Throwable -> Lf3
            r7.onInit(r1)     // Catch: java.lang.Throwable -> Lf3
            goto Lb1
        Lc3:
            com.tencentcs.iotvideo.iotvideoplayer.render.InsertionRender r6 = r5.mInsertionRender     // Catch: java.lang.Throwable -> Lf3
            if (r6 == 0) goto Ld0
            r6.onRelease()     // Catch: java.lang.Throwable -> Lf3
            r6 = 0
            r5.mInsertionRender = r6     // Catch: java.lang.Throwable -> Lf3
            r5.initReplenishRender()     // Catch: java.lang.Throwable -> Lf3
        Ld0:
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.codec.AVData> r6 = r5.mChildrenAVDataMap     // Catch: java.lang.Throwable -> Lf3
            r6.clear()     // Catch: java.lang.Throwable -> Lf3
            com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader r6 = r5.mAvHeader     // Catch: java.lang.Throwable -> Lf3
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.IVideoRender> r7 = r5.mRenders     // Catch: java.lang.Throwable -> Lf3
            java.util.Set r7 = r7.keySet()     // Catch: java.lang.Throwable -> Lf3
            r1 = 0
            java.lang.Long[] r1 = new java.lang.Long[r1]     // Catch: java.lang.Throwable -> Lf3
            java.lang.Object[] r7 = r7.toArray(r1)     // Catch: java.lang.Throwable -> Lf3
            java.lang.Long[] r7 = (java.lang.Long[]) r7     // Catch: java.lang.Throwable -> Lf3
            java.util.Map r6 = com.tencentcs.iotvideo.utils.AVDataUtils.createRegionAVDataByAVHeader(r6, r7)     // Catch: java.lang.Throwable -> Lf3
            if (r6 == 0) goto Lf1
            java.util.Map<java.lang.Long, com.tencentcs.iotvideo.iotvideoplayer.codec.AVData> r7 = r5.mChildrenAVDataMap     // Catch: java.lang.Throwable -> Lf3
            r7.putAll(r6)     // Catch: java.lang.Throwable -> Lf3
        Lf1:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> Lf3
            return
        Lf3:
            r6 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> Lf3
            throw r6
        Lf6:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.tencentcs.iotvideo.iotvideoplayer.render.GLRenderGroup.update(com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader, java.util.Map):void");
    }
}
