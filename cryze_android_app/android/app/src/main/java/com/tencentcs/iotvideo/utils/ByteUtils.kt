package com.tencentcs.iotvideo.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ByteUtils {
    @JvmStatic
    fun byte2ToShort(byteArray: ByteArray, offset: Int): Short {
        return (((byteArray[offset + 1].toInt() and 255).toShort()
            .toInt() shl 8).toShort().toInt() or (byteArray[offset].toInt() and 255).toShort()
            .toInt()).toShort()
    }

    @JvmStatic
    fun bytesToInt(byteArray: ByteArray, offset: Int): Int {
        return ((byteArray[offset + 3].toInt() and 255) shl 24) or (byteArray[offset].toInt() and 255) or ((byteArray[offset + 1].toInt() and 255) shl 8) or ((byteArray[offset + 2].toInt() and 255) shl 16)
    }

    @JvmStatic
    fun bytesToLong(byteArray: ByteArray?, offset: Int): Long {
        if (byteArray == null) {
            return 0
        }
        val buffer = ByteBuffer.allocate(8)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(byteArray, offset, 8)
        buffer.flip()
        return buffer.getLong()
    }
}
