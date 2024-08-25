package com.tencentcs.iotvideo.utils

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.CameraRenderRegion

// JNI wants this I'm pretty sure
//TODO: Try to remove this
private external fun nativeSetChildrenAVData(
    index: Int,
    parentAVData: AVData,
    childAVDataList: List<AVData>,
    renderRegionList: List<CameraRenderRegion>
)