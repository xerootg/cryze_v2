package com.tencentcs.iotvideo.utils.rxjava;
/* loaded from: classes2.dex */
public interface IResultListener<T> {
    void onError(int i10, String str);

    void onStart();

    void onSuccess(T t10);
}
