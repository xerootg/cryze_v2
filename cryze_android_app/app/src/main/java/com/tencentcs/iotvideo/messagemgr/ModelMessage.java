package com.tencentcs.iotvideo.messagemgr;

import androidx.annotation.NonNull;

/* loaded from: classes2.dex */
public class ModelMessage extends Message {
    public String data;
    public String device;
    public String path;

    public ModelMessage(String str, long j10, int i10, int i11, String str2, String str3) {
        super(i10, j10, i11);
        this.device = str;
        this.path = str2;
        this.data = str3;
    }

    @NonNull
    public String toString() {
        return "ModelMessage{device='" + this.device +
                "', path='" +
                this.path +
                "', error='" +
                this.error +
                "', data='" +
                this.data +
                "'}";
    }
}
