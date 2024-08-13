package com.tencentcs.iotvideo.messagemgr;
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
}
