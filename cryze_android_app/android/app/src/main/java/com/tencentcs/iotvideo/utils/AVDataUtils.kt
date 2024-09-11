package com.tencentcs.iotvideo.utils

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion

// JNI validates it exists, however we do not use it.
@Suppress("unused")
private external fun nativeSetChildrenAVData(
    index: Int,
    parentAVData: AVData,
    childAVDataList: List<AVData>,
    renderRegionList: List<CameraRenderRegion>
)