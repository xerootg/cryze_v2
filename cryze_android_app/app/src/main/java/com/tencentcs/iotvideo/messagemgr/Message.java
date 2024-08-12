package com.tencentcs.iotvideo.messagemgr;
/* loaded from: classes2.dex */
public class Message {
    public int error;

    /* renamed from: id  reason: collision with root package name */
    public long id;
    public int type;

    public Message(int i10, long j10, int i11) {
        this.type = i10;
        this.id = j10;
        this.error = i11;
    }
}
