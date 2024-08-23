package com.tencentcs.iotvideo.messagemgr;

/* loaded from: classes2.dex */
public class EventMessage extends Message {
    public String data;
    public String topic;

    public EventMessage(int type, String topic, String data) {
        super(type, 0L, 0);
        this.topic = topic;
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder sb2 = new StringBuilder("EventMessage{topic='");
        sb2.append(this.topic);
        sb2.append("', data='");
        sb2.append(this.data);
        sb2.append("'}");
        return sb2.toString();
    }
}
