package com.tencentcs.iotvideo.utils;
/* loaded from: classes2.dex */
public final class IoTYUVKits {
    private IoTYUVKits() {
    }

    public static void NV21toI420SemiPlanar(byte[] inputArray, byte[] outputArray, int width, int height) {
        int frameSize = width * height;
        int quarterFrameSize = frameSize / 4;
        System.arraycopy(inputArray, 0, outputArray, 0, frameSize);
        for (int i = 0; i < quarterFrameSize; i++) {
            int outputIndex = (i * 2) + frameSize;
            outputArray[frameSize + i] = inputArray[outputIndex];
            outputArray[frameSize + quarterFrameSize + i] = inputArray[outputIndex + 1];
        }
    }
    public static void swapYV12toNV12(byte[] inputArray, byte[] outputArray, int width, int height) {
        int frameSize = width * height;
        int quarterFrameSize = frameSize / 4;
        System.arraycopy(inputArray, 0, outputArray, 0, frameSize);
        for (int i = 0; i < quarterFrameSize; i++) {
            int outputIndex = (i * 2) + frameSize;
            outputArray[outputIndex + 1] = inputArray[frameSize + i];
            outputArray[outputIndex] = inputArray[frameSize + quarterFrameSize + i];
        }
    }

    public static void yv12ToI420(byte[] inputArray, byte[] outputArray, int width, int height) {
        int frameSize = width * height;
        System.arraycopy(inputArray, 0, outputArray, 0, frameSize);
        int quarterFrameSize = frameSize / 4;
        int uStartIndex = frameSize + quarterFrameSize;
        System.arraycopy(inputArray, uStartIndex, outputArray, frameSize, quarterFrameSize);
        System.arraycopy(inputArray, frameSize, outputArray, uStartIndex, quarterFrameSize);
    }

    public static void yv12ToNv21(byte[] inputArray, byte[] outputArray, int width, int height) {
        int frameSize = width * height;
        int quarterFrameSize = frameSize / 4;
        System.arraycopy(inputArray, 0, outputArray, 0, frameSize);
        for (int i = 0; i < quarterFrameSize; i++) {
            int outputIndex = (i * 2) + frameSize;
            outputArray[outputIndex + 1] = inputArray[frameSize + quarterFrameSize + i];
            outputArray[outputIndex] = inputArray[frameSize + i];
        }
    }
}
