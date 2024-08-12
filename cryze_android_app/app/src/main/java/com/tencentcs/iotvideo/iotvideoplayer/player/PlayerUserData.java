package com.tencentcs.iotvideo.iotvideoplayer.player;

/* loaded from: classes2.dex */
public class PlayerUserData {
    public int definition;
    public long fileStartTime;
    public int offset;
    public long playbackTime;
    public short sourceId;

    public PlayerUserData(int i10) {
        this.definition = i10;
        this.playbackTime = 0L;
        this.fileStartTime = 0L;
        this.sourceId = (short) 0;
    }

    public void setFileStartTime(long j10) {
        this.fileStartTime = j10;
    }

    public void setOffset(int i10) {
        this.offset = i10;
    }

    public void setSourceId(short s10) {
        this.sourceId = s10;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("PlayerUserData{definition=");
        sb2.append(this.definition);
        sb2.append(", playbackTime=");
        sb2.append(this.playbackTime);
        sb2.append(", fileStartTime=");
        sb2.append(this.fileStartTime);
        sb2.append(", sourceId=");
        sb2.append((int) this.sourceId);
        sb2.append(", offset=");
        sb2.append(this.offset);
        sb2.append('}');
        return sb2.toString();
    }

    public PlayerUserData(int i10, short s10) {
        this.definition = i10;
        this.playbackTime = 0L;
        this.fileStartTime = 0L;
        this.sourceId = s10;
    }

    public PlayerUserData(int i10, long j10, long j11) {
        this.definition = i10;
        this.playbackTime = j10;
        this.fileStartTime = j11;
        this.sourceId = (short) 0;
    }

    public PlayerUserData() {
        this.definition = 0;
        this.playbackTime = 0L;
        this.fileStartTime = 0L;
        this.sourceId = (short) 0;
    }
}
