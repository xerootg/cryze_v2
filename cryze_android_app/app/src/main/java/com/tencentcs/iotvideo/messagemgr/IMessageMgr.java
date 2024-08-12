package com.tencentcs.iotvideo.messagemgr;

import com.tencentcs.iotvideo.utils.rxjava.IResultListener;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes2.dex */
public interface IMessageMgr {
    void addAppLinkListener(IAppLinkListener iAppLinkListener);

    void addEventListener(IEventListener iEventListener);

    void addModelListener(IModelListener iModelListener);

    void notifyDevModelToServer(String str, String str2);

    void operateProUser(String str, String str2, String str3, String str4, int i10, IResultListener<ModelMessage> iResultListener);

    void operateProUser(String str, String str2, String str3, String str4, IResultListener<ModelMessage> iResultListener);

    void readProperty(String str, String str2, int i10, IResultListener<ModelMessage> iResultListener);

    void readProperty(String str, String str2, IResultListener<ModelMessage> iResultListener);

    void removeAppLinkListener(IAppLinkListener iAppLinkListener);

    void removeAppLinkListeners();

    void removeEventListener(IEventListener iEventListener);

    void removeEventListeners();

    void removeModelListener(IModelListener iModelListener);

    void removeModelListeners();

    void sendDataToDevice(String str, byte b10, byte[] bArr, boolean z10, int i10, IResultListener<DataMessage> iResultListener);

    void sendDataToDevice(String str, byte[] bArr, boolean z10, int i10, IResultListener<DataMessage> iResultListener);

    void sendDataToDevice(String str, byte[] bArr, boolean z10, int i10, boolean z11, IResultListener<DataMessage> iResultListener);

    void sendDataToDeviceWithResponse(String str, byte b10, byte[] bArr, IResultListener<DataMessage> iResultListener);

    void sendDataToDeviceWithResponse(String str, byte[] bArr, IResultListener<DataMessage> iResultListener);

    void sendDataToDeviceWithoutResponse(String str, byte b10, byte[] bArr, IResultListener<DataMessage> iResultListener);

    void sendDataToDeviceWithoutResponse(String str, byte[] bArr, IResultListener<DataMessage> iResultListener);

    void sendDataToServer(String str, byte[] bArr, int i10, IResultListener<DataMessage> iResultListener);

    void sendDataToServer(String str, byte[] bArr, IResultListener<DataMessage> iResultListener);

    void setPassThroughListener(IPassThroughListener iPassThroughListener);

    void subscribeDevice(ConcurrentHashMap<Integer, IResultListener<Boolean>> concurrentHashMap);

    void writeProperty(String str, String str2, String str3, int i10, IResultListener<ModelMessage> iResultListener);

    void writeProperty(String str, String str2, String str3, IResultListener<ModelMessage> iResultListener);
}
