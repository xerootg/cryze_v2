package com.tencentcs.iotvideo.messagemgr;

public interface IFileDownloadInnerDataListener {
    void onDownloadError(long j, int i);

    void onReceiveAVFileData(byte[] bArr);

    void onReceiveAvFileHead(DownloadFileHead downloadFileHead);
}