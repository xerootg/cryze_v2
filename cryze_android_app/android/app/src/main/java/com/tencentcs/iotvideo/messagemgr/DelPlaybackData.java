package com.tencentcs.iotvideo.messagemgr;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
/* loaded from: classes2.dex */
public class DelPlaybackData implements Parcelable {
    public static final Parcelable.Creator<DelPlaybackData> CREATOR = new Parcelable.Creator<DelPlaybackData>() { // from class: com.tencentcs.iotvideo.messagemgr.DelPlaybackData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DelPlaybackData createFromParcel(Parcel parcel) {
            return new DelPlaybackData(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DelPlaybackData[] newArray(int i10) {
            return new DelPlaybackData[i10];
        }
    };
    public List<FailDeleteInfo> failList;
    public int totalDelCnt;
    public int totalFailCnt;

    public DelPlaybackData(int i10, int i11, List<FailDeleteInfo> list) {
        this.totalDelCnt = i10;
        this.totalFailCnt = i11;
        this.failList = list;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "DelPlaybackData{totalDelCnt=" + this.totalDelCnt + ", totalFailCnt=" + this.totalFailCnt + ", failList=" + this.failList + '}';
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i10) {
        parcel.writeInt(this.totalDelCnt);
        parcel.writeInt(this.totalFailCnt);
        parcel.writeTypedList(this.failList);
    }

    /* loaded from: classes2.dex */
    public static class FailDeleteInfo implements Parcelable {
        public static final Parcelable.Creator<FailDeleteInfo> CREATOR = new Parcelable.Creator<FailDeleteInfo>() { // from class: com.tencentcs.iotvideo.messagemgr.DelPlaybackData.FailDeleteInfo.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public FailDeleteInfo createFromParcel(Parcel parcel) {
                return new FailDeleteInfo(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public FailDeleteInfo[] newArray(int i10) {
                return new FailDeleteInfo[i10];
            }
        };
        public int errCode;
        public long fileTime;

        public FailDeleteInfo(long j10, int i10) {
            this.fileTime = j10;
            this.errCode = i10;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i10) {
            parcel.writeLong(this.fileTime);
            parcel.writeInt(this.errCode);
        }

        public FailDeleteInfo(Parcel parcel) {
            this.fileTime = parcel.readLong();
            this.errCode = parcel.readInt();
        }
    }

    public DelPlaybackData(Parcel parcel) {
        this.totalDelCnt = parcel.readInt();
        this.totalFailCnt = parcel.readInt();
        this.failList = parcel.createTypedArrayList(FailDeleteInfo.CREATOR);
    }
}
