package com.tencentcs.iotvideo.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes2.dex */
public class ByteUtils {
    public static short byte2ToShort(byte[] bArr, int i10) {
        return (short) (((short) (((short) (bArr[i10 + 1] & 255)) << 8)) | ((short) (bArr[i10] & 255)));
    }

    public static int bytesToInt(byte[] bArr, int i10) {
        return ((bArr[i10 + 3] & 255) << 24) | (bArr[i10] & 255) | ((bArr[i10 + 1] & 255) << 8) | ((bArr[i10 + 2] & 255) << 16);
    }

    public static long bytesTolong(byte[] bArr, int i10) {
        ByteBuffer allocate = ByteBuffer.allocate(8);
        allocate.order(ByteOrder.LITTLE_ENDIAN);
        allocate.put(bArr, i10, 8);
        allocate.flip();
        return allocate.getLong();
    }

    public static byte[] intToBytes(int i10) {
        return new byte[]{(byte) (i10 & 255), (byte) ((i10 >> 8) & 255), (byte) ((i10 >> 16) & 255), (byte) ((i10 >> 24) & 255)};
    }

    public static byte[] longToBytes(long j10) {
        return new byte[]{(byte) (j10 & 255), (byte) ((j10 >> 8) & 255), (byte) ((j10 >> 16) & 255), (byte) ((j10 >> 24) & 255), (byte) ((j10 >> 32) & 255), (byte) ((j10 >> 40) & 255), (byte) ((j10 >> 48) & 255), (byte) ((j10 >> 56) & 255)};
    }

    public static long lowBit32(long j10) {
        return j10 & 4294967295L;
    }

    public static byte[] shortToByte2(short s10) {
        return new byte[]{(byte) (s10 & 255), (byte) ((s10 >> 8) & 255)};
    }

    public static int unsignedByteToInt(byte b10) {
        return b10 & 255;
    }
}
