package com.github.xerootg.cryze.restreamer.interfaces

import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder

interface IRestreamingVideoDecoder : IVideoDecoder {
    var initialized: Boolean
}