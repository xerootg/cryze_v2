package com.tencentcs.iotvideo.messagemgr;

/* loaded from: classes2.dex */
public class EventMessage extends Message {
    public String data;
    public String topic;

    public EventMessage(int i10, String str, String str2) {
        super(i10, 0L, 0);
        this.topic = str;
        this.data = str2;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("EventMessage{topic='");
        sb2.append(this.topic);
        sb2.append("', data='");
        sb2.append(this.data);
        sb2.append("'}");
        return sb2.toString();
    }
}
