package com.tencentcs.iotvideo.netconfig;

/* loaded from: classes2.dex */
public class NetConfigResult {
    public static final int STATUS_FAILED = 0;
    public static final int STATUS_WAITING = 1;
    private String devId;
    private int status;
    private String token;

    public String getDevId() {
        return this.devId;
    }

    public int getStatus() {
        return this.status;
    }

    public String getToken() {
        return this.token;
    }

    public void setDevId(String str) {
        this.devId = str;
    }

    public void setStatus(int i10) {
        this.status = i10;
    }

    public void setToken(String str) {
        this.token = str;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("NetConfigResult{token='");
        sb2.append(this.token);
        sb2.append("', devId='");
        sb2.append(this.devId);
        sb2.append("', status=");
        sb2.append(this.status);
        sb2.append('}');
        return sb2.toString();
    }
}
