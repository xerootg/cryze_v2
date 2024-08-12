package com.tencentcs.iotvideo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/* loaded from: classes2.dex */
public final class FileIOUtils {
    private static final String TAG = "FileIOUtils";
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static int sBufferSize = 8192;

    public static boolean writeFileFromBytesByStream(String str, byte[] bArr) {
        try {
            return writeFileFromBytesByStream(getFileByPath(str), bArr, false);
        } catch (IOException e) {
            LogUtils.e(TAG, "Unable to writeFileFromBytesByStream: " + str);
            return false;        }
    }

    public static boolean writeFileFromBytesByStream(String str, byte[] bArr, boolean z10) {
        try {
            return writeFileFromBytesByStream(getFileByPath(str), bArr, z10);
        } catch (IOException e) {
            LogUtils.e(TAG, "Unable to writeFileFromBytesByStream: " + str);
            return false;        }
    }

    public static boolean writeFileFromBytesByStream(File file, byte[] bArr) {
        try {
            return writeFileFromBytesByStream(file, bArr, false);
        } catch (IOException e) {
            LogUtils.e(TAG, "Unable to writeFileFromBytesByStream: " + file.getName());
            return false;
        }
    }

    public static boolean writeFileFromBytesByStream(File file, byte[] data, boolean append) throws IOException {
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file, append);
            fileOutputStream.write(data);
        } catch (IOException err)
        {
            LogUtils.e(TAG, "Unable to writeFileFromBytesByStream: " + file.getName());
            return false;
        } finally {
            // Close the FileOutputStream in a finally block to ensure it's always closed
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
    public static byte[] readFile2BytesByStream(String str) {
        LogUtils.i(TAG, "readFile2BytesByStream file: " + str);
        try {
            return readFile2BytesByStream(getFileByPath(str));
        } catch (IOException e) {
            LogUtils.e(TAG, "readFile2BytesByStream error reading: " + str + ", error: " + e.getLocalizedMessage());
            return new byte[0];
        }
    }

    private static File getFileByPath(String str) {
        if (isSpace(str)) {
            return null;
        }
        return new File(str);
    }

    private static boolean isSpace(String str) {
        if (str == null) {
            return true;
        }
        int length = str.length();
        for (int i10 = 0; i10 < length; i10++) {
            if (!Character.isWhitespace(str.charAt(i10))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFileExists(File file) {
        return file != null && file.exists();
    }

    public static byte[] readFile2BytesByStream(File file) throws IOException {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];

            // Read file content into byte array
            int bytesRead = fileInputStream.read(fileBytes);

            // Check if the entire file is read
            if (bytesRead < file.length()) {
                throw new IOException("Could not read the entire file: " + file.getName());
            }

            return fileBytes;
        } finally {
            // Close the FileInputStream in a finally block to ensure it's always closed
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
