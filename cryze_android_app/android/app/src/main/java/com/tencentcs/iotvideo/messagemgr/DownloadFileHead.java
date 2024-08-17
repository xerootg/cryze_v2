package com.tencentcs.iotvideo.messagemgr;

public class DownloadFileHead {
    private String fileName;
    private long fileStartTime;
    private long fileTotalSize;
    private long sendDataSize;

    public DownloadFileHead(long j10, long j11, long j12, String str) {
        this.fileStartTime = j10;
        this.fileTotalSize = j11;
        this.sendDataSize = j12;
        this.fileName = str;
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getFileStartTime() {
        return this.fileStartTime;
    }

    public long getFileTotalSize() {
        return this.fileTotalSize;
    }

    public long getSendDataSize() {
        return this.sendDataSize;
    }

    public void setFileName(String str) {
        this.fileName = str;
    }

    public void setFileStartTime(long j10) {
        this.fileStartTime = j10;
    }

    public void setFileTotalSize(long j10) {
        this.fileTotalSize = j10;
    }

    public void setSendDataSize(long j10) {
        this.sendDataSize = j10;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder("DownloadFileHead{fileStartTime=");
        sb2.append(this.fileStartTime);
        sb2.append(", fileTotalSize=");
        sb2.append(this.fileTotalSize);
        sb2.append(", sendDataSize=");
        sb2.append(this.sendDataSize);
        sb2.append(", fileName='");
        sb2.append(this.fileName);
        sb2.append("'}");
        return sb2.toString();
    }
}
