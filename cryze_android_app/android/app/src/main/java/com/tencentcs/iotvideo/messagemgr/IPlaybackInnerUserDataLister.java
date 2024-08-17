package com.tencentcs.iotvideo.messagemgr;
/* loaded from: classes2.dex */
public interface IPlaybackInnerUserDataLister extends IInnerUserDataLister {
    void onPlayFileFinished(long j10);

    void onPlayFileStart(long j10);

    void onSeekRet(boolean z10, long j10);
}
