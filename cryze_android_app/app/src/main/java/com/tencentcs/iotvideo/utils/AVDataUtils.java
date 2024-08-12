package com.tencentcs.iotvideo.utils;

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion;
import com.tencentcs.iotvideo.iotvideoplayer.codec.VideoRenderInfo;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public class AVDataUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "AVDataUtils";

    public static boolean checkAVHeaderChange(AVHeader aVHeader, AVHeader aVHeader2) {
        if (aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 0) == aVHeader2.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 0) && aVHeader.getInteger(AVHeader.KEY_VIDEO_TYPE, 0) == aVHeader2.getInteger(AVHeader.KEY_VIDEO_TYPE, 0) && aVHeader.getInteger(AVHeader.KEY_WIDTH, 0) == aVHeader2.getInteger(AVHeader.KEY_WIDTH, 0) && aVHeader.getInteger(AVHeader.KEY_HEIGHT, 0) == aVHeader2.getInteger(AVHeader.KEY_HEIGHT, 0) && aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 0) == aVHeader2.getInteger(AVHeader.KEY_FRAME_RATE, 0) && aVHeader.getInteger(AVHeader.KEY_PLAYBACK_SPEED, 0) == aVHeader2.getInteger(AVHeader.KEY_PLAYBACK_SPEED, 0) && aVHeader.getInteger(AVHeader.KEY_BIT_RATE, 0) == aVHeader2.getInteger(AVHeader.KEY_BIT_RATE, 0) && aVHeader.getInteger(AVHeader.KEY_AUDIO_TYPE, 0) == aVHeader2.getInteger(AVHeader.KEY_AUDIO_TYPE, 0) && aVHeader.getInteger(AVHeader.KEY_AUDIO_CODEC_OPTION, 0) == aVHeader2.getInteger(AVHeader.KEY_AUDIO_CODEC_OPTION, 0) && aVHeader.getInteger(AVHeader.KEY_AUDIO_MODE, 0) == aVHeader2.getInteger(AVHeader.KEY_AUDIO_MODE, 0) && aVHeader.getInteger(AVHeader.KEY_AUDIO_BIT_WIDTH, 0) == aVHeader2.getInteger(AVHeader.KEY_AUDIO_BIT_WIDTH, 0) && aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_NUM_PERFRAME, 0) == aVHeader2.getInteger(AVHeader.KEY_AUDIO_SAMPLE_NUM_PERFRAME, 0)) {
            return false;
        }
        return true;
    }

    public static Map<Long, AVData> createRegionAVDataByAVHeader(AVHeader aVHeader, Long[] lArr) {
        if (aVHeader == null) {
            LogUtils.e(TAG, "createRegionAVDataByAVHeader(avHeader), avHeader == null");
            return null;
        }
        VideoRenderInfo videoRenderInfo = aVHeader.getVideoRenderInfo();
        if (videoRenderInfo == null) {
            LogUtils.e(TAG, "createRegionAVDataByAVHeader(avHeader), renderInfo == null");
            return null;
        }
        int regionListSize = aVHeader.getVideoRenderInfo().getRegionListSize();
        if (regionListSize <= 0) {
            LogUtils.e(TAG, "createRegionAVDataByAVHeader(avHeader), regionNum <= 0");
            return null;
        }
        if (lArr == null || lArr.length <= 0) {
            lArr = new Long[regionListSize];
            for (int i10 = 0; i10 < regionListSize; i10++) {
                lArr[i10] = Long.valueOf(videoRenderInfo.getRenderRegionByIndex(0).getCameraId());
            }
        }
        HashMap hashMap = new HashMap();
        for (Long l10 : lArr) {
            CameraRenderRegion renderRegionById = videoRenderInfo.getRenderRegionById(l10.longValue());
            if (renderRegionById != null) {
                AVData aVData = new AVData();
                int height = renderRegionById.getHeight() * renderRegionById.getWidth();
                aVData.size = height;
                aVData.data = ByteBuffer.allocateDirect(height);
                int i11 = aVData.size / 4;
                aVData.size1 = i11;
                aVData.data1 = ByteBuffer.allocateDirect(i11);
                int i12 = aVData.size / 4;
                aVData.size2 = i12;
                aVData.data2 = ByteBuffer.allocateDirect(i12);
                hashMap.put(l10, aVData);
            }
        }
        return hashMap;
    }

    private static native void nativeSetChildrenAVData(int i10, AVData aVData, List<AVData> list, List<CameraRenderRegion> list2);

    public static void setChildrenAVData(int i10, AVData aVData, List<AVData> list, List<CameraRenderRegion> list2) {
        if (aVData != null && list != null && list.size() > 0 && list2 != null && list2.size() == list.size()) {
            aVData.data.position(0);
            aVData.data1.position(0);
            aVData.data2.position(0);
            for (AVData aVData2 : list) {
                if (aVData2 != null) {
                    aVData2.data.position(0);
                    aVData2.data1.position(0);
                    aVData2.data2.position(0);
                }
            }
            nativeSetChildrenAVData(i10, aVData, list, list2);
            aVData.data.position(0);
            aVData.data1.position(0);
            aVData.data2.position(0);
            for (AVData aVData3 : list) {
                if (aVData3 != null) {
                    aVData3.data.position(0);
                    aVData3.data1.position(0);
                    aVData3.data2.position(0);
                }
            }
        }
    }
}
