package com.tencentcs.iotvideo.iotvideoplayer;

public class ConnectMode {
    public int mMode = 0;
    public int mProtocol = -1;

    public interface Mode {
        public static final int LAN = 3;
        public static final int P2P = 2;
        public static final int RELAY = 1;
        public static final int UNINITIALIZED = -1;
        public static final int UNKNOWN = 0;
    }

    public interface Protocol {
        public static final int TCP = 1;
        public static final int UDP = 0;
        public static final int UNKNOWN = -1;
    }

    private String getModeString(int mode) {
        switch (mode) {
            case Mode.LAN:
                return "LAN";
            case Mode.P2P:
                return "P2P";
            case Mode.RELAY:
                return "RELAY";
            case Mode.UNINITIALIZED:
                return "UNINITIALIZED";
            case Mode.UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    private String getProtocolString(int protocol) {
        switch (protocol) {
            case Protocol.TCP:
                return "TCP";
            case Protocol.UDP:
                return "UDP";
            case Protocol.UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("ConnectMode{mMode=");
        sb2.append(getModeString(this.mMode));
        sb2.append(", mProtocol=");
        sb2.append(getProtocolString(this.mProtocol));
        sb2.append('}');
        return sb2.toString();
    }
}
