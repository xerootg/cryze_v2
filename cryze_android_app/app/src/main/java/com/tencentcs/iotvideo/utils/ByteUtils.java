package com.tencentcs.iotvideo.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
public class ByteUtils {
    public static short byte2ToShort(byte[] byteArray, int offset) {
        return (short) (((short) (((short) (byteArray[offset + 1] & 255)) << 8)) | ((short) (byteArray[offset] & 255)));
    }

    public static int bytesToInt(byte[] byteArray, int offset) {
        return ((byteArray[offset + 3] & 255) << 24) | (byteArray[offset] & 255) | ((byteArray[offset + 1] & 255) << 8) | ((byteArray[offset + 2] & 255) << 16);
    }

    public static long bytesToLong(byte[] byteArray, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(byteArray, offset, 8);
        buffer.flip();
        return buffer.getLong();
    }

    public static byte[] intToBytes(int i10) {
        return new byte[]{(byte) (i10 & 255), (byte) ((i10 >> 8) & 255), (byte) ((i10 >> 16) & 255), (byte) ((i10 >> 24) & 255)};
    }

    public static byte[] longToBytes(long j10) {
        return new byte[]{(byte) (j10 & 255), (byte) ((j10 >> 8) & 255), (byte) ((j10 >> 16) & 255), (byte) ((j10 >> 24) & 255), (byte) ((j10 >> 32) & 255), (byte) ((j10 >> 40) & 255), (byte) ((j10 >> 48) & 255), (byte) ((j10 >> 56) & 255)};
    }

    public static final long MAX_UNSIGNED_INT = 4294967295L;
    public static long lowBit32(long j10) {
        return j10 & MAX_UNSIGNED_INT;
    }

    public static byte[] shortToByte2(short s10) {
        return new byte[]{(byte) (s10 & 255), (byte) ((s10 >> 8) & 255)};
    }

    public static int unsignedByteToInt(byte b10) {
        return b10 & 255;
    }
}
