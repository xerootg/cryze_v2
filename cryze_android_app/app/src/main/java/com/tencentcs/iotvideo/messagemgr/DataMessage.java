package com.tencentcs.iotvideo.messagemgr;

import java.util.Arrays;
public class DataMessage extends Message {
    public byte[] data;

    public DataMessage(long id, int type, int error, byte[] data) {
        super(type, id, error);
        this.data = data;
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
