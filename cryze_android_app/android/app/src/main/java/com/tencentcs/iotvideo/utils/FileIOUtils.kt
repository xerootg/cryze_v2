package com.tencentcs.iotvideo.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileIOUtils {
    private const val TAG = "FileIOUtils"


    @JvmStatic
    fun readFile2BytesByStream(str: String): ByteArray {
        return readFile2BytesByStream(File(str))
    }

    @JvmStatic
    fun writeFileFromBytesByStream(str: String, data: ByteArray?): Boolean {
        return writeFileFromBytesByStream(File(str), data, false)
    }

    @Throws(IOException::class)
    fun writeFileFromBytesByStream(file: File, data: ByteArray?, append: Boolean): Boolean {
        if (data == null) {
            LogUtils.e(TAG, "Data is null")
            return false
        }

        val parentFile = file.parentFile
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                LogUtils.e(TAG, "Unable to create directory: ${parentFile.absolutePath}")
                return false
            }
        }

        return try {
            FileOutputStream(file, append).use { fileOutputStream ->
                fileOutputStream.write(data)
            }
            LogUtils.i(TAG, "Successfully wrote to file: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            LogUtils.e(
                TAG,
                "Unable to writeFileFromBytesByStream: ${file.name}, error: ${e.localizedMessage}"
            )
            false
        }
    }

    @Throws(IOException::class)
    fun readFile2BytesByStream(file: File): ByteArray {

        if (!file.exists()) {
            LogUtils.e(TAG, "File does not exist: ${file.absolutePath}")
            return ByteArray(0)
        }

        return try {
            FileInputStream(file).use { fileInputStream ->
                val fileBytes = ByteArray(file.length().toInt())
                val bytesRead = fileInputStream.read(fileBytes)

                if (bytesRead < file.length()) {
                    throw IOException("Could not read the entire file: ${file.name}")
                }

                LogUtils.i(TAG, "Successfully read file: ${file.absolutePath}")
                fileBytes
            }
        } catch (e: IOException) {
            LogUtils.e(
                TAG,
                "Unable to readFile2BytesByStream: ${file.name}, error: ${e.localizedMessage}"
            )
            ByteArray(0)
        }
    }
}
