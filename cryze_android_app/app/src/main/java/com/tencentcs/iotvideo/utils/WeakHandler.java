package com.tencentcs.iotvideo.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.tencentcs.iotvideo.utils.IHandlerConsumer;
import java.lang.ref.WeakReference;
/* loaded from: classes2.dex */
public class WeakHandler<T extends IHandlerConsumer> extends Handler {
    private final WeakReference<T> mWeakHandler;

    public WeakHandler(T t10) {
        this.mWeakHandler = new WeakReference<>(t10);
    }

    @Override // android.os.Handler
    public void handleMessage(Message message) {
        WeakReference<T> weakReference = this.mWeakHandler;
        if (weakReference != null && weakReference.get() != null) {
            this.mWeakHandler.get().receiveHandlerMessage(message);
        }
    }

    public WeakHandler(T t10, Looper looper) {
        super(looper);
        this.mWeakHandler = new WeakReference<>(t10);
    }
}
