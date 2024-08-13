package com.tencentcs.iotvideo.utils;

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData;
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader;
import com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion;
import com.tencentcs.iotvideo.iotvideoplayer.codec.VideoRenderInfo;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AVDataUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "AVDataUtils";

    public static boolean checkAVHeaderChange(AVHeader aVHeader, AVHeader aVHeader2) {
        return aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 0) != aVHeader2.getInteger(AVHeader.KEY_AUDIO_SAMPLE_RATE, 0) ||
                aVHeader.getInteger(AVHeader.KEY_VIDEO_TYPE, 0) != aVHeader2.getInteger(AVHeader.KEY_VIDEO_TYPE, 0) ||
                aVHeader.getInteger(AVHeader.KEY_WIDTH, 0) != aVHeader2.getInteger(AVHeader.KEY_WIDTH, 0) ||
                aVHeader.getInteger(AVHeader.KEY_HEIGHT, 0) != aVHeader2.getInteger(AVHeader.KEY_HEIGHT, 0) ||
                aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 0) != aVHeader2.getInteger(AVHeader.KEY_FRAME_RATE, 0) ||
                aVHeader.getInteger(AVHeader.KEY_PLAYBACK_SPEED, 0) != aVHeader2.getInteger(AVHeader.KEY_PLAYBACK_SPEED, 0) ||
                aVHeader.getInteger(AVHeader.KEY_BIT_RATE, 0) != aVHeader2.getInteger(AVHeader.KEY_BIT_RATE, 0) ||
                aVHeader.getInteger(AVHeader.KEY_AUDIO_TYPE, 0) != aVHeader2.getInteger(AVHeader.KEY_AUDIO_TYPE, 0) ||
                aVHeader.getInteger(AVHeader.KEY_AUDIO_CODEC_OPTION, 0) != aVHeader2.getInteger(AVHeader.KEY_AUDIO_CODEC_OPTION, 0) ||
                aVHeader.getInteger(AVHeader.KEY_AUDIO_MODE, 0) != aVHeader2.getInteger(AVHeader.KEY_AUDIO_MODE, 0) ||
                aVHeader.getInteger(AVHeader.KEY_AUDIO_BIT_WIDTH, 0) != aVHeader2.getInteger(AVHeader.KEY_AUDIO_BIT_WIDTH, 0) ||
                aVHeader.getInteger(AVHeader.KEY_AUDIO_SAMPLE_NUM_PERFRAME, 0) != aVHeader2.getInteger(AVHeader.KEY_AUDIO_SAMPLE_NUM_PERFRAME, 0);
    }

    public static Map<Long, AVData> createRegionAVDataByAVHeader(AVHeader avHeader, Long[] regionIds) {
        if (avHeader == null) {
            LogUtils.e(TAG, "createRegionAVDataByAVHeader(avHeader), avHeader == null");
            return null;
        }
        VideoRenderInfo videoRenderInfo = avHeader.getVideoRenderInfo();
        if (videoRenderInfo == null) {
            LogUtils.e(TAG, "createRegionAVDataByAVHeader(avHeader), renderInfo == null");
            return null;
        }
        int regionListSize = videoRenderInfo.getRegionListSize();
        if (regionListSize <= 0) {
            LogUtils.e(TAG, "createRegionAVDataByAVHeader(avHeader), regionNum <= 0");
            return null;
        }
        if (regionIds == null || regionIds.length <= 0) {
            regionIds = new Long[regionListSize];
            for (int i = 0; i < regionListSize; i++) {
                regionIds[i] = videoRenderInfo.getRenderRegionByIndex(0).getCameraId();
            }
        }
        HashMap<Long, AVData> regionAVDataMap = new HashMap<>();
        for (Long regionId : regionIds) {
            CameraRenderRegion renderRegion = videoRenderInfo.getRenderRegionById(regionId);
            if (renderRegion != null) {
                AVData avData = new AVData();
                int dataSize = renderRegion.getHeight() * renderRegion.getWidth();
                avData.size = dataSize;
                avData.data = ByteBuffer.allocateDirect(dataSize);
                int quarterSize = avData.size / 4;
                avData.size1 = quarterSize;
                avData.data1 = ByteBuffer.allocateDirect(quarterSize);
                avData.size2 = quarterSize;
                avData.data2 = ByteBuffer.allocateDirect(quarterSize);
                regionAVDataMap.put(regionId, avData);
            }
        }
        return regionAVDataMap;
    }

    private static native void nativeSetChildrenAVData(int index, AVData parentAVData, List<AVData> childAVDataList, List<CameraRenderRegion> renderRegionList);

    public static void setChildrenAVData(int index, AVData parentAVData, List<AVData> childAVDataList, List<CameraRenderRegion> renderRegionList) {
        if (parentAVData != null && childAVDataList != null && !childAVDataList.isEmpty() && renderRegionList != null && renderRegionList.size() == childAVDataList.size()) {
            parentAVData.data.position(0);
            parentAVData.data1.position(0);
            parentAVData.data2.position(0);
            for (AVData childAVData : childAVDataList) {
                if (childAVData != null) {
                    childAVData.data.position(0);
                    childAVData.data1.position(0);
                    childAVData.data2.position(0);
                }
            }
            nativeSetChildrenAVData(index, parentAVData, childAVDataList, renderRegionList);
            parentAVData.data.position(0);
            parentAVData.data1.position(0);
            parentAVData.data2.position(0);
            for (AVData childAVData : childAVDataList) {
                if (childAVData != null) {
                    childAVData.data.position(0);
                    childAVData.data1.position(0);
                    childAVData.data2.position(0);
                }
            }
        }
    }

}
