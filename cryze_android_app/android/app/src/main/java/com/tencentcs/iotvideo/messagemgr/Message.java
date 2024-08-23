package com.tencentcs.iotvideo.messagemgr;

import androidx.annotation.NonNull;

/* loaded from: classes2.dex */
public class Message {
    public int error;
    public long id;
    public int type;

    public Message(int type, long id, int error) {
        this.type = type;
        this.id = id;
        this.error = error;
    }

    @NonNull
    @Override
    public String toString() {
        return "type: " + this.type + ", id: " + this.id + ", error: " + this.error;
    }
}
