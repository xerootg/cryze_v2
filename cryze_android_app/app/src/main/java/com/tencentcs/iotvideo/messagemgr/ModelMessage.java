package com.tencentcs.iotvideo.messagemgr;

import androidx.annotation.NonNull;

/* loaded from: classes2.dex */
public class ModelMessage extends Message {
    public String payload;
    public String device;
    public String messageType;

    public ModelMessage(String str, long j10, int i10, int i11, String str2, String str3) {
        super(i10, j10, i11);
        this.device = str;
        this.messageType = str2;
        this.payload = str3;
    }

    @NonNull
    public String toString() {
        return "ModelMessage{device='" + this.device +
                "', path='" +
                this.messageType +
                "', error='" +
                this.error +
                "', data='" +
                this.payload +
                "'}";
    }
}
