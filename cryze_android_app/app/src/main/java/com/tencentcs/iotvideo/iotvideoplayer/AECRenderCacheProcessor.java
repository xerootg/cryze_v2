package com.tencentcs.iotvideo.iotvideoplayer;

import com.tencentcs.iotvideo.utils.LogUtils;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class AECRenderCacheProcessor {
    private static final int MAX_BUFFER_SIZE = 327680;
    private static final String TAG = "AECRenderCacheProcessor";
    private byte[] mCacheData;
    private int mOnceProcessLength;
    private byte[] renderData;
    private int validDataLength;

    public synchronized void clearCache() {
        LogUtils.i(TAG, "clearCache");
        this.validDataLength = 0;
        Arrays.fill(this.renderData, (byte) 0);
    }

    public void destroy() {
        LogUtils.i(TAG, "destroy");
        clearCache();
        this.renderData = null;
        this.mCacheData = null;
    }

    public void init(int i10) {
        if (i10 <= 0) {
            LogUtils.e(TAG, "init failure:input params is invalid, onceProcessLength:" + i10);
            return;
        }
        this.mOnceProcessLength = i10;
        this.validDataLength = 0;
        this.mCacheData = new byte[MAX_BUFFER_SIZE];
        this.renderData = new byte[i10];
        LogUtils.i(TAG, "init mAudio20MsPcmLength" + i10);
    }

    public synchronized byte[] pullCacheData() {
        if (this.mOnceProcessLength <= 0) {
            return null;
        }
        Arrays.fill(this.renderData, (byte) 0);
        int i10 = this.validDataLength;
        if (i10 > 0) {
            int min = Math.min(this.mOnceProcessLength, i10);
            System.arraycopy(this.mCacheData, 0, this.renderData, 0, min);
            int i11 = this.validDataLength - min;
            this.validDataLength = i11;
            if (i11 > 0) {
                byte[] bArr = this.mCacheData;
                System.arraycopy(bArr, min, bArr, 0, i11);
            }
        }
        return this.renderData;
    }

    public synchronized void pushCacheData(byte[] bArr) {
        if (this.mOnceProcessLength <= 0) {
            return;
        }
        int length = bArr.length + this.validDataLength;
        byte[] bArr2 = this.mCacheData;
        if (length > bArr2.length) {
            this.validDataLength = 0;
        }
        System.arraycopy(bArr, 0, bArr2, this.validDataLength, bArr.length);
        this.validDataLength += bArr.length;
    }
}
