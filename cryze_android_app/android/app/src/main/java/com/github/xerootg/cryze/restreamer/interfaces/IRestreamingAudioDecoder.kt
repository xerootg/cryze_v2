package com.github.xerootg.cryze.restreamer.interfaces

import com.tencentcs.iotvideo.iotvideoplayer.codec.IAudioDecoder

interface IRestreamingAudioDecoder : IAudioDecoder {
    var initialized: Boolean
}