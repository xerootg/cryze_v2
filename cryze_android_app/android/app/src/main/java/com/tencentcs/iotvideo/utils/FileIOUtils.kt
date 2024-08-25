package com.tencentcs.iotvideo.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileIOUtils {
    private const val TAG = "FileIOUtils"

    @JvmStatic
    fun writeFileFromBytesByStream(str: String, data: ByteArray?): Boolean {
        try {
            return writeFileFromBytesByStream(getFileByPath(str), data, false)
        } catch (e: IOException) {
            LogUtils.e(TAG, "Unable to writeFileFromBytesByStream: $str")
            return false
        }
    }

    @Throws(IOException::class)
    fun writeFileFromBytesByStream(file: File?, data: ByteArray?, append: Boolean): Boolean {
        var fileOutputStream: FileOutputStream? = null

        // make sure the directory exists
        val parentFile = file!!.parentFile
        if (parentFile != null) {
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                            LogUtils.e(TAG, "Unable to create directory: " + parentFile.absolutePath)
                            return false
                        }
            }
        }

        try {
            fileOutputStream = FileOutputStream(file, append)
            fileOutputStream.write(data)
        } catch (err: IOException) {
            LogUtils.e(TAG, "Unable to writeFileFromBytesByStream: " + file.name)
            return false
        } finally {
            // Close the FileOutputStream in a finally block to ensure it's always closed
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    @JvmStatic
    fun readFile2BytesByStream(str: String): ByteArray {
        LogUtils.i(TAG, "readFile2BytesByStream file: $str")
        try {
            return readFile2BytesByStream(getFileByPath(str))
        } catch (e: IOException) {
            LogUtils.e(
                TAG,
                "readFile2BytesByStream error reading: " + str + ", error: " + e.localizedMessage
            )
            return ByteArray(0)
        }
    }

    private fun getFileByPath(str: String): File? {
        if (isSpace(str)) {
            return null
        }
        return File(str)
    }

    private fun isSpace(str: String?): Boolean {
        if (str == null) {
            return true
        }
        val length = str.length
        for (i10 in 0 until length) {
            if (!Character.isWhitespace(str[i10])) {
                return false
            }
        }
        return true
    }

    @Throws(IOException::class)
    fun readFile2BytesByStream(file: File?): ByteArray {
        var fileInputStream: FileInputStream? = null

        try {
            fileInputStream = FileInputStream(file)
            val fileBytes = ByteArray(file!!.length().toInt())

            // Read file content into byte array
            val bytesRead = fileInputStream.read(fileBytes)

            // Check if the entire file is read
            if (bytesRead < file.length()) {
                throw IOException("Could not read the entire file: " + file.name)
            }

            return fileBytes
        } finally {
            // Close the FileInputStream in a finally block to ensure it's always closed
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
