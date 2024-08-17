package com.tencentcs.iotvideo.utils.rxjava;

public interface IResultListener<T> {
    void onError(int errorCode, String message);

    void onStart();

    void onSuccess(T success);
}
