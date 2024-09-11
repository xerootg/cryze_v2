package com.github.xerootg.cryze.restreamer.rtspmuxer.sources

interface IRtspStreamSource {
    var postToMuxer : Boolean
    // called when the muxer is ready to stream
    fun startStream()
    fun stopStream()
}