package com.tencentcs.iotvideo.iotvideoplayer.codec;

/* loaded from: classes2.dex */
public class CameraRenderRegion {
    private static final String TAG = "CameraRenderRegion";
    private final long cameraId;
    private int height;
    private int left;
    private int top;
    private int width;

    public CameraRenderRegion(long j10, int i10, int i11, int i12, int i13) {
        this.cameraId = j10;
        this.left = i10;
        this.top = i11;
        this.width = i12;
        this.height = i13;
    }

    public CameraRenderRegion copy() {
        return new CameraRenderRegion(this.cameraId, this.left, this.top, this.width, this.height);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CameraRenderRegion cameraRenderRegion = (CameraRenderRegion) obj;
        if (this.cameraId == cameraRenderRegion.cameraId && this.left == cameraRenderRegion.left && this.top == cameraRenderRegion.top && this.width == cameraRenderRegion.width && this.height == cameraRenderRegion.height) {
            return true;
        }
        return false;
    }

    public long getCameraId() {
        return this.cameraId;
    }

    public int getHeight() {
        return this.height;
    }

    public int getLeft() {
        return this.left;
    }

    public int getTop() {
        return this.top;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int i10) {
        this.height = i10;
    }

    public void setLeft(int i10) {
        this.left = i10;
    }

    public void setTop(int i10) {
        this.top = i10;
    }

    public void setWidth(int i10) {
        this.width = i10;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("CameraRenderRegion{regionId=");
        sb2.append(this.cameraId);
        sb2.append(", left=");
        sb2.append(this.left);
        sb2.append(", top=");
        sb2.append(this.top);
        sb2.append(", width=");
        sb2.append(this.width);
        sb2.append(", height=");
        sb2.append(this.height);
        sb2.append('}');
        return sb2.toString();
    }
}
