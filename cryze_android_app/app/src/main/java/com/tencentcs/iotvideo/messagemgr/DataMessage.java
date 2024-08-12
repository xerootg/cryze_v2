package com.tencentcs.iotvideo.messagemgr;

import java.util.Arrays;
/* loaded from: classes2.dex */
public class DataMessage extends Message {
    public byte[] data;

    public DataMessage(long j10, int i10, int i11, byte[] bArr) {
        super(i10, j10, i11);
        this.data = bArr;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("DataMessage{data=");
        sb2.append(Arrays.toString(this.data));
        sb2.append(", type=");
        sb2.append(this.type);
        sb2.append(", id=");
        sb2.append(this.id);
        sb2.append(", error=");
        sb2.append(this.error);
        sb2.append('}');
        return sb2.toString();
    }
}
