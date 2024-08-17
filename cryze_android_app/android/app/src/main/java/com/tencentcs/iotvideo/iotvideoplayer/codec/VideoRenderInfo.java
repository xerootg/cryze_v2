package com.tencentcs.iotvideo.iotvideoplayer.codec;

import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class VideoRenderInfo {
    private static final String TAG = "VideoRenderInfo";
    private final List<CameraRenderRegion> renderRegionList = new ArrayList();

    public void addRenderRegion(CameraRenderRegion cameraRenderRegion) {
        if (!this.renderRegionList.contains(cameraRenderRegion)) {
            for (CameraRenderRegion cameraRenderRegion2 : this.renderRegionList) {
                if (cameraRenderRegion2.getCameraId() == cameraRenderRegion.getCameraId()) {
                    cameraRenderRegion2.setLeft(cameraRenderRegion.getLeft());
                    cameraRenderRegion2.setTop(cameraRenderRegion.getTop());
                    cameraRenderRegion2.setWidth(cameraRenderRegion.getWidth());
                    cameraRenderRegion2.setHeight(cameraRenderRegion.getHeight());
                    return;
                }
            }
            this.renderRegionList.add(cameraRenderRegion);
        }
    }

    public VideoRenderInfo copy() {
        VideoRenderInfo videoRenderInfo = new VideoRenderInfo();
        for (CameraRenderRegion cameraRenderRegion : this.renderRegionList) {
            videoRenderInfo.addRenderRegion(cameraRenderRegion.copy());
        }
        return videoRenderInfo;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VideoRenderInfo videoRenderInfo = (VideoRenderInfo) obj;
        if (videoRenderInfo.getRegionListSize() != getRegionListSize()) {
            return false;
        }
        for (CameraRenderRegion cameraRenderRegion : this.renderRegionList) {
            if (!cameraRenderRegion.equals(videoRenderInfo.getRenderRegionById(cameraRenderRegion.getCameraId()))) {
                return false;
            }
        }
        return true;
    }

    public int getRegionListSize() {
        return this.renderRegionList.size();
    }

    public CameraRenderRegion getRenderRegionById(long j10) {
        for (CameraRenderRegion cameraRenderRegion : this.renderRegionList) {
            if (cameraRenderRegion.getCameraId() == j10) {
                return cameraRenderRegion;
            }
        }
        return null;
    }

    public CameraRenderRegion getRenderRegionByIndex(int i10) {
        if (i10 >= 0 && i10 < this.renderRegionList.size()) {
            return this.renderRegionList.get(i10);
        }
        return null;
    }

    public String toString() {
        return "VideoRenderInfo{renderRegionList=" + this.renderRegionList + '}';
    }
}
