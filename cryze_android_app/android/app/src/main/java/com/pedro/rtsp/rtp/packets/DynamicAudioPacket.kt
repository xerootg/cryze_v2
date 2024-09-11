package com.pedro.rtsp.rtp.packets

import android.media.MediaCodec
import com.pedro.common.removeInfo
import com.pedro.rtsp.rtsp.RtpFrame
import com.pedro.rtsp.utils.RtpConstants
import java.nio.ByteBuffer

// Example uses: PCMU and PCMA codecs that are not 8kHz or mono
// probably useful for any "raw" audio codec that doesn't have a specific packet type
class DynamicAudioPacket(
  sampleRate: Int
): BasePacket(
  sampleRate.toLong(),
  RtpConstants.payloadTypeDynamic + RtpConstants.trackAudio
) {

  init {
    channelIdentifier = RtpConstants.trackAudio
  }

  override fun createAndSendPacket(
    byteBuffer: ByteBuffer,
    bufferInfo: MediaCodec.BufferInfo,
    callback: (RtpFrame) -> Unit
  ) {
    val fixedBuffer = byteBuffer.removeInfo(bufferInfo)
    val length = fixedBuffer.remaining()
    val maxPayload = maxPacketSize - RtpConstants.RTP_HEADER_LENGTH
    val ts = bufferInfo.presentationTimeUs * 1000
    var sum = 0
    while (sum < length) {
      val size = if (length - sum < maxPayload) length - sum else maxPayload
      val buffer = getBuffer(size + RtpConstants.RTP_HEADER_LENGTH)
      fixedBuffer.get(buffer, RtpConstants.RTP_HEADER_LENGTH, size)
      markPacket(buffer)
      val rtpTs = updateTimeStamp(buffer, ts)
      updateSeq(buffer)
      val rtpFrame = RtpFrame(buffer, rtpTs, RtpConstants.RTP_HEADER_LENGTH + size, rtpPort, rtcpPort, channelIdentifier)
      sum += size
      callback(rtpFrame)
    }
  }
}