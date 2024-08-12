package com.tencentcs.iotvideo.messagemgr;

import android.annotation.SuppressLint;
import com.tencentcs.iotvideo.IoTVideoError;
import com.tencentcs.iotvideo.messagemgr.DelPlaybackData;
import com.tencentcs.iotvideo.utils.ByteUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class DelPlaybackDataMessage {
    private static final String TAG = "DelPlaybackDataMessage";
    private boolean isLastPage = false;
    private DelPlaybackData mDelPlaybackData;

    public DelPlaybackData getDelPlaybackData() {
        return this.mDelPlaybackData;
    }

    public boolean isLastPage() {
        return this.isLastPage;
    }

    @SuppressLint({"DefaultLocale"})
    public void parseData(byte[] bArr) {
        String str = TAG;
        LogUtils.i(str, "parseData: data = " + Arrays.toString(bArr));
        int bytesToInt = ByteUtils.bytesToInt(bArr, 4);
        int bytesToInt2 = ByteUtils.bytesToInt(bArr, 8);
        int bytesToInt3 = ByteUtils.bytesToInt(bArr, 12);
        int bytesToInt4 = ByteUtils.bytesToInt(bArr, 16);
        int bytesToInt5 = ByteUtils.bytesToInt(bArr, 20);
        long bytesTolong = ByteUtils.bytesTolong(bArr, 24);
        if (255 == (bytesToInt4 & 255)) {
            this.isLastPage = true;
        }
        LogUtils.i(str, String.format("parseData errCode = %d delCount = %d failCount = %d pkgIndex = %d fileCount = %d baseTime = %d", Integer.valueOf(bytesToInt), Integer.valueOf(bytesToInt2), Integer.valueOf(bytesToInt3), Integer.valueOf(bytesToInt4), Integer.valueOf(bytesToInt5), Long.valueOf(bytesTolong)));
        ArrayList arrayList = new ArrayList();
        for (int i10 = 0; i10 < bytesToInt5; i10++) {
            int i11 = (i10 * 8) + 32;
            int bytesToInt6 = ByteUtils.bytesToInt(bArr, i11);
            int bytesToInt7 = ByteUtils.bytesToInt(bArr, i11 + 4);
            LogUtils.i(TAG, "parseData: statusCode = " + bytesToInt7);
            if (bytesToInt7 != 22077) {
                arrayList.add(new DelPlaybackData.FailDeleteInfo(bytesToInt6 + bytesTolong, bytesToInt7 + IoTVideoError.DEL_FILE_CODE_NONE));
            }
        }
        this.mDelPlaybackData = new DelPlaybackData(bytesToInt2, bytesToInt3, arrayList);
    }
}
