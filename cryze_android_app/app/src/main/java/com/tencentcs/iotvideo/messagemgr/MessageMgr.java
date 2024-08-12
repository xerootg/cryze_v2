package com.tencentcs.iotvideo.messagemgr;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tencentcs.iotvideo.IoTVideoSdk;
import com.tencentcs.iotvideo.iotvideoplayer.ConnectMode;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.FileIOUtils;
import com.tencentcs.iotvideo.utils.rxjava.IResultListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageMgr implements IMessageMgr {

    public static final String TAG = "CLMessageMgr";
    private static Application mContext = null;
    private static int mLastAppReceiveSdkStatus = -1;
    private static int mSdkStatus = 2;
    private CopyOnWriteArrayList<IAppLinkListener> mAppLinkListeners = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<Integer, IResultListener<Boolean>> mSubscribeDeviceMap = new ConcurrentHashMap<>();
    private List<IModelListener> mModelListeners = new ArrayList();
    private List<IEventListener> mEventListeners = new ArrayList();
    private Handler mMainHandler = new Handler(Looper.getMainLooper());




    public static class MessageMgrHolder {
        private static final MessageMgr INSTANCE = new MessageMgr();

        private MessageMgrHolder() {
        }
    }

    public static MessageMgr getInstance() {
        return MessageMgrHolder.INSTANCE;
    }

    public static int getSdkStatus() {
        return mSdkStatus;
    }

    public void register(Application application) {
        LogUtils.i(TAG, "MessageMgr register");
        mContext = application;
        mLastAppReceiveSdkStatus = -1;
        nativeRegister();
    }

    public native long nativeGetData(String str, String str2);

    private native void nativeNotifyDevModelToServer(String str, String str2);

    private native void nativeRegister();

    public native long nativeSendDataToDevice(String str, byte[] bArr, int i10, int i11);

    public native long nativeSendDataToServer(String str, byte[] bArr);

    public native long nativeSetData(String str, String str2, String str3);

    public native long nativeSetUserParam(String str, String str2, String str3, String str4);

    private native void nativeUnregister();

    private static void onModelMessage(String str, long j10, int i10, int i11, String str2, String str3) {
        LogUtils.i(TAG, "onModelMessage deviceId:" + str + ", id:" + j10 + ", type:" + i10 + ", error:" + i11 + ", path:" + str2 + ", data:" + str3);
        getInstance().dispatchModelMessage(new ModelMessage(str, j10, i10, i11, str2, str3));
    }

    private static void onEventMessage(final String str, final String str2) {
        LogUtils.i(TAG, "onEventMessage topic:" + str + ", data:" + str2);
    }

    private static void onDataMessage(String str, long j10, int i10, int i11, byte[] bArr) {
        LogUtils.i(TAG, "onDataMessage deviceId = " + str + ", id " + j10 + ", type:" + i10 + ", error:" + i11 + "; data:" + Arrays.toString(bArr));
    }

    private static void onSubscribeDevice(final int i10, final int i11) {
        LogUtils.i(TAG, "onSubscribeDevice ==========start==========");
        if (getInstance().mSubscribeDeviceMap != null && getInstance().mSubscribeDeviceMap.size() > 0)
        {
            IResultListener<Boolean> currentResListener = getInstance().mSubscribeDeviceMap.get(i10);
            if (currentResListener == null)
            {
                LogUtils.w(TAG, "onSubscribeDevice listener is null");
                return;
            }
            LogUtils.i(TAG, "onSubscribeDevice: msgId: " + i10 + " error: " + i11);
            if (i11 != 0)
            {
                if (isMainThread())
                {
                    currentResListener.onError(i11, "subscribe device error" + i11);
                    return;
                }
                getInstance().mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.i(TAG, "onSubscribeDevice app status at subThread");
                        currentResListener.onError(i11, "subscribe device error" + i11);
                    }
                });
                return;
            }
            if (isMainThread())
            {
                LogUtils.i(TAG, "onSubscribeDevice app status at uiThread");
                currentResListener.onSuccess(Boolean.TRUE);
                getInstance().mSubscribeDeviceMap.remove(i10);
            } else {
                getInstance().mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.i(TAG, "onSubscribeDevice app status at uiThread");
                        currentResListener.onSuccess(Boolean.TRUE);
                        getInstance().mSubscribeDeviceMap.remove(i10);
                    }
                });
            }
        }
        LogUtils.i(TAG, "onSubscribeDevice ==========end==========");
    }

    private static int onNetworkDetect(int i10, int i11, int i12, int[] iArr, int i13) {
        String ret = "onNetworkDetech fromIp: " + i10 + ", totalSend: " + i11 + ", totalRecv: " + i12 + ", pingDelay: " + Arrays.toString(iArr) + ", resv: " + i13;
        LogUtils.i(TAG, ret);
        return 0;
    }

    private static int ivCommonGetCb(int i10, byte[] bArr, int i11) {
        LogUtils.i(TAG, String.format(Locale.getDefault(), "ivCommonGetCb type:%d,  len:%d, minimum:%d", i10, i11, i11));
        byte[] readFile2BytesByStream = FileIOUtils.readFile2BytesByStream(getP2PSavePath(i10));
        System.arraycopy(readFile2BytesByStream, 0, bArr, 0, Math.min(i11, readFile2BytesByStream.length));
        return Math.min(i11, readFile2BytesByStream.length);
    }

    private static int ivCommonSetCb(int i10, byte[] bArr, int i11) {
        LogUtils.d(TAG, String.format(Locale.getDefault(), "ivCommonSetCb type:%d, data:%s, len:%d", i10, Arrays.toString(bArr), i11));
        if (FileIOUtils.writeFileFromBytesByStream(getP2PSavePath(i10), bArr)) {
            return 0;
        }
        return -1;
    }

    private static String getP2PSavePath(int i10) {
        Locale locale = Locale.getDefault();
        String absolutePath = mContext.getExternalFilesDir(null).getAbsolutePath();
        String str = File.separator;
        return String.format(locale, "%s%s%s%s%d%s", absolutePath, str, "p2pSave", str, i10, ".p2p");
    }

    static Boolean isSubbed = false;


    // This gets called from inside the Native Library from best I can tell
    private static void onAppLinkStateChanged(final int status) {
        LogUtils.d(TAG, "onAppLinkStateChanged "+ status);
        switch (status) {
            case 1:
                LogUtils.i(TAG, "App link online");
                break;
            case 2:
                LogUtils.i(TAG, "App link offline");
                break;
            case 3:
                LogUtils.i(TAG, "App link access token error");
                break;
            case 4:
                LogUtils.i(TAG, "App link TID init error");
                break;
            case 5:
                LogUtils.i(TAG, "App link invalid TID");
                break;
            case 6:
                LogUtils.i(TAG, "App link kick off");
                break;
            case 7:
                LogUtils.i(TAG, "App link dev disable");
                break;
        }
        setSdkStatus(status);
        if (mLastAppReceiveSdkStatus == status)
        {
            LogUtils.i(TAG, "same status as last notify, don't send app, sdk state: " + getSdkStatus());
        }
        else if (getInstance().mAppLinkListeners.isEmpty())
        {
            LogUtils.i(TAG, "no app link listeners to send to! status: " + getSdkStatus());
        } else {
            if (isMainThread())
            {
                LogUtils.i(TAG, "onAppLinkStateChanged notify app status at uiThread");
                for (IAppLinkListener mAppLinkListener : getInstance().mAppLinkListeners) {
                    mAppLinkListener.onAppLinkStateChanged(status);
                }
            } else {
                getInstance().mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (IAppLinkListener mAppLinkListener : MessageMgr.getInstance().mAppLinkListeners) {
                            LogUtils.i(TAG, "onAppLinkStateChanged notify app status at subThread");
                            mAppLinkListener.onAppLinkStateChanged(status);
                        }
                    }
                });
            }
            mLastAppReceiveSdkStatus = status;
        }
    }

    private void dispatchModelMessage(final ModelMessage modelMessage) {
        if (getInstance().mModelListeners.isEmpty())
        {
            LogUtils.i(TAG, "no model listeners to send to! status: " + getSdkStatus());
        } else {
            if (isMainThread())
            {
                for (IModelListener mModelListener : getInstance().mModelListeners)
                {
                    mModelListener.onNotify(modelMessage);
                }
            } else {
                getInstance().mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (IModelListener mModelListener : getInstance().mModelListeners)
                        {
                            mModelListener.onNotify(modelMessage);
                        }
                    }
                });
            }
        }
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public void unregister() {
        unregister(true);
    }
    public void unregister(boolean z10) {
        LogUtils.i(TAG, "unregister start， clearCacheData：" + z10);
        if (z10) {
            mContext = null;
            mModelListeners.clear();
        }
        mLastAppReceiveSdkStatus = -1;
        nativeUnregister();
        setSdkStatus(2);
        LogUtils.i(TAG, "unregister end");
    }

    public void addAppLinkListener(IAppLinkListener iAppLinkListener) {
        boolean alreadyExists = false;
        for (IAppLinkListener mAppLinkListener : getInstance().mAppLinkListeners) {
            if (mAppLinkListener == iAppLinkListener) {
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) {
            this.mAppLinkListeners.add(iAppLinkListener);
        }
    }

    @Override
    public void addEventListener(IEventListener iEventListener) {

    }

    @Override
    public void addModelListener(IModelListener iModelListener) {
        boolean alreadyExists = false;
        Iterator<IModelListener> it = getInstance().mModelListeners.iterator();
        for (IModelListener mModelListener : getInstance().mModelListeners) {
            if (mModelListener == iModelListener) {
                alreadyExists = true;
            break;
            }
        }
        if (!alreadyExists) {
            this.mModelListeners.add(iModelListener);
        }
    }

    @Override
    public void notifyDevModelToServer(String str, String str2) {

    }

    @Override
    public void operateProUser(String str, String str2, String str3, String str4, int i10, IResultListener<ModelMessage> iResultListener) {

    }

    @Override
    public void operateProUser(String str, String str2, String str3, String str4, IResultListener<ModelMessage> iResultListener) {

    }

    @Override
    public void readProperty(String str, String str2, int i10, IResultListener<ModelMessage> iResultListener) {

    }

    @Override
    public void readProperty(String str, String str2, IResultListener<ModelMessage> iResultListener) {

    }

    @Override
    public void removeAppLinkListener(IAppLinkListener iAppLinkListener) {

    }

    @Override
    public void removeAppLinkListeners() {

    }

    @Override
    public void removeEventListener(IEventListener iEventListener) {

    }

    @Override
    public void removeEventListeners() {

    }

    @Override
    public void removeModelListener(IModelListener iModelListener) {
        if (iModelListener != null) {
            this.mModelListeners.remove(iModelListener);
        }
    }

    @Override
    public void removeModelListeners() {
        this.mModelListeners.clear();
    }

    @Override
    public void sendDataToDevice(String str, byte b10, byte[] bArr, boolean z10, int i10, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToDevice(String str, byte[] bArr, boolean z10, int i10, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToDevice(String str, byte[] bArr, boolean z10, int i10, boolean z11, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToDeviceWithResponse(String str, byte b10, byte[] bArr, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToDeviceWithResponse(String str, byte[] bArr, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToDeviceWithoutResponse(String str, byte b10, byte[] bArr, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToDeviceWithoutResponse(String str, byte[] bArr, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToServer(String str, byte[] bArr, int i10, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void sendDataToServer(String str, byte[] bArr, IResultListener<DataMessage> iResultListener) {

    }

    @Override
    public void setPassThroughListener(IPassThroughListener iPassThroughListener) {

    }

    public void subscribeDevice(ConcurrentHashMap<Integer, IResultListener<Boolean>> concurrentHashMap) {
        this.mSubscribeDeviceMap = concurrentHashMap;
    }

    @Override
    public void writeProperty(String str, String str2, String str3, int i10, IResultListener<ModelMessage> iResultListener) {

    }

    @Override
    public void writeProperty(String str, String str2, String str3, IResultListener<ModelMessage> iResultListener) {

    }

    private static void setSdkStatus(int status) {
        mSdkStatus = status;
    }


    private static void getLocalNetIpFromP2p(byte[] bArr, byte[] bArr2) {
        try {
            String[] split = IoTVideoSdk.getLocalIPAddress().split("\\.");
            if (4 == split.length) {
                int min = Math.min(bArr.length, split.length);
                for (int i10 = 0; i10 < min; i10++) {
                    int intValue = Integer.valueOf(split[i10]).intValue();
                    if (intValue > 127) {
                        intValue -= 256;
                    }
                    bArr[i10] = (byte) intValue;
                }
                Arrays.fill(bArr2, (byte) 0);
                return;
            }
            LogUtils.e(TAG, "net list is invalid");
            Arrays.fill(bArr, (byte) 0);
            Arrays.fill(bArr2, (byte) 0);
        } catch (Exception e10) {
            e10.printStackTrace();
            Arrays.fill(bArr, (byte) 0);
            Arrays.fill(bArr2, (byte) 0);
        }
    }

}