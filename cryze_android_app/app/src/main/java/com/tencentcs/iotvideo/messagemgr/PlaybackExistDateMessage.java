package com.tencentcs.iotvideo.messagemgr;

import com.tencentcs.iotvideo.utils.ByteUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import com.tencentcs.iotvideo.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public class PlaybackExistDateMessage extends DataMessage {
    private static final String TAG = "PlaybackMessage";
    public int currentPage;
    private boolean isLastPage;
    public int pageCount;
    public ArrayList<DateListNode> playbackList;
    private byte version;

    /* loaded from: classes2.dex */
    public static class DateListNode {
        public long dateTime;

        public DateListNode(long j10) {
            this.dateTime = j10;
        }

        public long getDateTime() {
            return this.dateTime;
        }

        public void setDateTime(long j10) {
            this.dateTime = j10;
        }

        public String toString() {
            return "dateTime:" + Utils.timeFormatEndDay(this.dateTime);
        }
    }

    public PlaybackExistDateMessage(long j10, int i10, int i11) {
        super(j10, i10, i11, null);
        this.playbackList = new ArrayList<>();
        this.isLastPage = true;
    }

    private void parseData(byte[] bArr) {
        if (bArr.length < 17) {
            return;
        }
        int i10 = 0;
        byte b10 = bArr[0];
        if (255 == (ByteUtils.bytesToInt(bArr, 1) & 255)) {
            this.isLastPage = true;
        }
        this.currentPage = ByteUtils.bytesToInt(bArr, 5);
        this.pageCount = ByteUtils.bytesToInt(bArr, 9);
        int bytesToInt = ByteUtils.bytesToInt(bArr, 13);
        if (bytesToInt <= 0) {
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
        byte[][] bArr2 = (byte[][]) Array.newInstance(Byte.TYPE, bytesToInt, 17);
        for (int i13 = 0; i13 < i11; i13++) {
            System.arraycopy(bArr, (i13 * 17) + 26, bArr2[i13], 0, 17);
        }
        if (bArr.length < (bytesToInt * 9) + i12) {
            LogUtils.e(TAG, "data invalid: parse item data failure");
            return;
        }
        LogUtils.i(TAG, "baseTime:" + bytesTolong);
        if (2 == b10) {
            while (i10 < bytesToInt) {
                this.playbackList.add(new DateListNode((ByteUtils.bytesToInt(bArr, (i10 * 9) + i12) & 4294967295L) + bytesTolong));
                i10++;
            }
        } else if (3 == b10) {
            while (i10 < bytesToInt) {
                this.playbackList.add(new DateListNode(ByteUtils.bytesTolong(bArr, (i10 * 13) + i12) + bytesTolong));
                i10++;
            }
        }
    }

    public void addOnePageData(byte b10, long j10, int i10, int i11, byte[] bArr) {
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

    public String getDateListString() {
        StringBuilder sb2 = new StringBuilder();
        Iterator<DateListNode> it = this.playbackList.iterator();
        while (it.hasNext()) {
            sb2.append(it.next());
            sb2.append("\n");
        }
        return sb2.toString();
    }

    public boolean isLastPage() {
        return this.isLastPage;
    }

    @Override // com.tencentcs.iotvideo.messagemgr.DataMessage
    public String toString() {
        return "PlaybackExistDateMessage{currentPage=" + this.currentPage + ", pageCount=" + this.pageCount + ", playbackList=" + this.playbackList + '}';
    }

    public PlaybackExistDateMessage(byte b10, long j10, int i10, int i11, byte[] bArr) {
        super(j10, i10, i11, bArr);
        this.playbackList = new ArrayList<>();
        this.isLastPage = false;
        this.version = b10;
        parseData(bArr);
    }
}
