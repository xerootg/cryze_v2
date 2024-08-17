package com.tencentcs.iotvideo.iotvideoplayer.codec;

import java.nio.ByteBuffer;

public class AVData {
    public ByteBuffer data;
    public ByteBuffer data1;
    public ByteBuffer data2;
    public long dts;
    public ByteBuffer extraData;
    public int extraSize;
    public int height;
    public int keyFrame;
    public long pts;
    public CameraRenderRegion seiRenderRegion;
    public int size;
    public int size1;
    public int size2;
    public int width;

    public String toString() {
        StringBuilder sb2 = new StringBuilder("AVData{size=");
        sb2.append(this.size);
        sb2.append(", size1=");
        sb2.append(this.size1);
        sb2.append(", size2=");
        sb2.append(this.size2);
        sb2.append(", width=");
        sb2.append(this.width);
        sb2.append(", height=");
        sb2.append(this.height);
        sb2.append(", keyFrame=");
        sb2.append(this.keyFrame);
        sb2.append(", pts: ");
        sb2.append(this.pts);
        sb2.append('}');
        return sb2.toString();
    }
}
