package com.tencentcs.iotvideo.messagemgr;

import com.tencentcs.iotvideo.utils.ByteUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class PlaybackMessage extends DataMessage {
    private static final String TAG = "PlaybackMessage";
    public int currentPage;
    private boolean isLastPage;
    public int pageCount;
    public ArrayList<PlaybackNode> playbackList;
    private byte version;
    private int type;
    private long id;
    private int error;

    /* loaded from: classes2.dex */
    public static class PlaybackNode {
        public long endTime;
        public String recordType;
        public long startTime;

        public PlaybackNode(long j10, long j11, String str) {
            this.startTime = j10;
            this.endTime = j11;
            this.recordType = str;
        }

        public String getEndTimeDisplay() {
            return Utils.timeFormat(this.endTime);
        }

        public String getStartTimeDisplay() {
            return Utils.timeFormat(this.startTime);
        }

        public String toString() {
            return "statTime:" + this.startTime + ", endTime:" + this.endTime + ", recordType:" + this.recordType;
        }
    }

    public PlaybackMessage(byte b10, long j10, int i10, int i11) {
        super(j10, i10, i11, null);
        this.playbackList = new ArrayList<>();
        this.isLastPage = false;
        this.version = b10;
    }

    private void parseData(byte[] bArr) {
        StringBuilder sb2 = new StringBuilder("parseData,version:");
        sb2.append((int) this.version);
        sb2.append("; dataLength:");
        LogUtils.i(TAG, sb2.toString() + bArr.length);
        byte b10 = this.version;
        if (1 == b10) {
            LogUtils.i(TAG, "parseData isLastPage is true");
            this.isLastPage = true;
            parseV1Data(bArr);
        } else if (2 == b10 || 3 == b10) {
            parseV2OrV3Data(bArr);
        }
    }

    private void parseV1Data(byte[] bArr) {
        int i10 = 12;
        if (bArr.length < 12) {
            return;
        }
        this.currentPage = ByteUtils.bytesToInt(bArr, 0);
        this.pageCount = ByteUtils.bytesToInt(bArr, 4);
        int bytesToInt = ByteUtils.bytesToInt(bArr, 8);
        if (bArr.length - 12 != bytesToInt * 33) {
            LogUtils.e(TAG, "parse data err!");
            this.currentPage = 0;
            this.pageCount = 0;
            return;
        }
        for (int i11 = 0; i11 < bytesToInt; i11++) {
            this.playbackList.add(new PlaybackNode(ByteUtils.bytesTolong(bArr, i10), ByteUtils.bytesTolong(bArr, i10 + 8), byteToStr(bArr, i10 + 16, 17)));
            i10 += 33;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void parseV2OrV3Data(byte[] bArr) {
        int i10 = 0;
        if (bArr.length < 17) {
            LogUtils.i(TAG, "parseV2OrV3Data failure: the length of data < 17");
            return;
        }
        byte b10 = bArr[0];
        if (2 != b10 && 3 != b10) {
            this.version = b10;
            parseData(bArr);
            return;
        }
        int bytesToInt = ByteUtils.bytesToInt(bArr, 1);

        LogUtils.i(TAG, "pkgIndex: " + bytesToInt);
        if (255 == (bytesToInt & 255)) {
            LogUtils.i(TAG, "parseV2OrV3Data isLastPage is true");
            this.isLastPage = true;
        }
        this.currentPage = ByteUtils.bytesToInt(bArr, 5);
        this.pageCount = ByteUtils.bytesToInt(bArr, 9);
        LogUtils.i(TAG, "pageCount: " + this.pageCount);
        LogUtils.i(TAG, "playbackList size: " + this.playbackList.size());
        int bytesToInt2 = ByteUtils.bytesToInt(bArr, 13);
        if (bytesToInt2 <= 0) {
            LogUtils.i(TAG, "play list is null");
            return;
        }
        long bytesTolong = ByteUtils.bytesTolong(bArr, 17);
        int i11 = bArr[25];
        int i12 = (i11 * 17) + 26;
        if (bArr.length < i12) {
            LogUtils.e(TAG, "data invalid: parse type string failure");
            return;
        }
        byte[][] bArr2 = (byte[][]) Array.newInstance(Byte.TYPE, i11, 17);
        for (int i13 = 0; i13 < i11; i13++) {
            System.arraycopy(bArr, (i13 * 17) + 26, bArr2[i13], 0, 17);
        }
        if (bArr.length < (bytesToInt2 * 9) + i12) {
            LogUtils.e(TAG, "data invalid: parse item data failure");
            return;
        }
        LogUtils.i(TAG, "baseTime:" + bytesTolong);
        if (2 == b10) {
            int i14 = 0;
            while (i14 < bytesToInt2) {
                long bytesToInt3 = (ByteUtils.bytesToInt(bArr, i10) & 4294967295L) + bytesTolong;
                long bytesToInt4 = bytesToInt3 + (ByteUtils.bytesToInt(bArr, i10 + 4) & 4294967295L);
                byte[] bArr3 = bArr2[bArr[(i14 * 9) + i12 + 8]];
                this.playbackList.add(new PlaybackNode(bytesToInt3, bytesToInt4, byteToStr(bArr3, 0, bArr3.length)));
                i14++;
                i12 = i12;
            }
            return;
        }
        for (int i15 = 0; i15 < bytesToInt2; i15++) {
            int i16 = (i15 * 13) + i12;
            long bytesTolong2 = ByteUtils.bytesTolong(bArr, i16) + bytesTolong;
            byte[] bArr4 = bArr2[bArr[i16 + 12]];
            this.playbackList.add(new PlaybackNode(bytesTolong2, bytesTolong2 + (ByteUtils.bytesToInt(bArr, i16 + 8) & 4294967295L), byteToStr(bArr4, 0, bArr4.length)));
        }
    }

    public void addOnePageData(byte b10, long j10, int i10, int i11, byte[] bArr) {
        this.version = b10;
        this.id = j10;
        this.type = i10;
        this.error = i11;
        parseData(bArr);
    }

    public String byteToStr(byte[] bArr, int i10, int i11) {
        int i12 = 0;
        int i13 = 0;
        while (true) {
            if (i13 >= i11) {
                break;
            }
            try {
                if (bArr[i13 + i10] == 0) {
                    i12 = i13;
                    break;
                }
                i13++;
            } catch (Exception unused) {
                return "";
            }
        }
        try {
            return new String(bArr, i10, i12, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isLastPage() {
        return this.isLastPage;
    }

    @Override // com.tencentcs.iotvideo.messagemgr.DataMessage
    public String toString() {
        return "PlaybackMessage{currentPage=" + this.currentPage + ", pageCount=" + this.pageCount + ", playbackList=" + this.playbackList + '}';
    }

    public PlaybackMessage(byte b10, long j10, int i10, int i11, byte[] bArr) {
        super(j10, i10, i11, bArr);
        this.playbackList = new ArrayList<>();
        this.isLastPage = false;
        this.version = b10;
        parseData(bArr);
    }
}
