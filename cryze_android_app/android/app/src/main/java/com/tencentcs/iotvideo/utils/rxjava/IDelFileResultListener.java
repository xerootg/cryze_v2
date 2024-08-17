package com.tencentcs.iotvideo.utils.rxjava;

import com.tencentcs.iotvideo.messagemgr.DelPlaybackData;
/* loaded from: classes2.dex */
public interface IDelFileResultListener {
    void onComplete();

    void onError(int i10, String str);

    void onProgress(DelPlaybackData delPlaybackData);

    void onStart();
}
