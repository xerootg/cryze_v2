package com.tencentcs.iotvideo.iotvideoplayer;

/* loaded from: classes2.dex */
public class ConnectMode {
    public int mMode = 0;
    public int mProtocol = -1;

    /* loaded from: classes2.dex */
    public interface Mode {
        public static final int LAN = 3;
        public static final int P2P = 2;
        public static final int RELAY = 1;
        public static final int UNINITIALIZED = -1;
        public static final int UNKNOWN = 0;
    }

    /* loaded from: classes2.dex */
    public interface Protocol {
        public static final int TCP = 1;
        public static final int UDP = 0;
        public static final int UNKNOWN = -1;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("ConnectMode{mMode=");
        sb2.append(this.mMode);
        sb2.append(", mProtocol=");
        sb2.append(this.mProtocol);
        sb2.append('}');
        return sb2.toString();
    }
}
