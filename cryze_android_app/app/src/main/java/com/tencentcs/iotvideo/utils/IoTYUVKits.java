package com.tencentcs.iotvideo.utils;
/* loaded from: classes2.dex */
public final class IoTYUVKits {
    private IoTYUVKits() {
    }

    public static void NV21toI420SemiPlanar(byte[] bArr, byte[] bArr2, int i10, int i11) {
        int i12 = i10 * i11;
        int i13 = i12 / 4;
        System.arraycopy(bArr, 0, bArr2, 0, i12);
        for (int i14 = 0; i14 < i13; i14++) {
            int i15 = (i14 * 2) + i12;
            bArr2[i12 + i14] = bArr[i15];
            bArr2[i12 + i13 + i14] = bArr[i15 + 1];
        }
    }

    public static void swapYV12toNV12(byte[] bArr, byte[] bArr2, int i10, int i11) {
        int i12 = i10 * i11;
        int i13 = i12 / 4;
        System.arraycopy(bArr, 0, bArr2, 0, i12);
        for (int i14 = 0; i14 < i13; i14++) {
            int i15 = (i14 * 2) + i12;
            bArr2[i15 + 1] = bArr[i12 + i14];
            bArr2[i15] = bArr[i12 + i13 + i14];
        }
    }

    public static void yv12ToI420(byte[] bArr, byte[] bArr2, int i10, int i11) {
        int i12 = i10 * i11;
        System.arraycopy(bArr, 0, bArr2, 0, i12);
        int i13 = i12 / 4;
        int i14 = i12 + i13;
        System.arraycopy(bArr, i14, bArr2, i12, i13);
        System.arraycopy(bArr, i12, bArr2, i14, i13);
    }

    public static void yv12ToNv21(byte[] bArr, byte[] bArr2, int i10, int i11) {
        int i12 = i10 * i11;
        int i13 = i12 / 4;
        System.arraycopy(bArr, 0, bArr2, 0, i12);
        for (int i14 = 0; i14 < i13; i14++) {
            int i15 = (i14 * 2) + i12;
            bArr2[i15 + 1] = bArr[i12 + i13 + i14];
            bArr2[i15] = bArr[i12 + i14];
        }
    }
}
