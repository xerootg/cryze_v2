package com.tencentcs.iotvideo.utils.rxjava;
/* loaded from: classes2.dex */
public interface IIoTMultiResultListener<T> {
    void onComplete();

    void onError(int i10, String str);

    boolean onNext(T t10);

    void onStart();
}
