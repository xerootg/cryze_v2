/*
 * Copyright (C) 2024 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.rtsp.rtsp.commands

import android.util.Log
import com.pedro.rtsp.utils.RtpConstants

/**
 * Created by pedro on 21/02/17.
 */
object SdpBody {

    /** supported sampleRates.  */
    private val AUDIO_SAMPLING_RATES = intArrayOf(
        96000,  // 0
        88200,  // 1
        64000,  // 2
        48000,  // 3
        44100,  // 4
        32000,  // 5
        24000,  // 6
        22050,  // 7
        16000,  // 8
        12000,  // 9
        11025,  // 10
        8000,  // 11
        7350,  // 12
        -1,  // 13
        -1,  // 14
        -1
    )

    /**
     * Opus only support sample rate 48khz and stereo channel but Android encoder accept others values.
     * The encoder internally transform the sample rate to 48khz and channels to stereo
     */
    fun createOpusBody(trackAudio: Int): String {
        val payload = RtpConstants.payloadTypeDynamic + trackAudio
        return "m=audio 0 RTP/AVP ${payload}\r\n" +
                "a=rtpmap:$payload OPUS/48000/2\r\n" +
                "a=control:streamid=$trackAudio\r\n"
    }

    // PCMU or PCMA are example uses of this body header
    fun createDynamicAudioBody(trackAudio: Int, sampleRate: Int, isStereo: Boolean, codec : String): String {
        val channel = if (isStereo) 2 else 1
        val payload = RtpConstants.payloadTypeDynamic + trackAudio
        return "m=audio 0 RTP/AVP $payload\r\n" +
                // spec says PCMU-WB but MediaMTX doesn't parse that.
                "a=rtpmap:$payload $codec/$sampleRate/$channel\r\n" +
                "a=control:streamid=$trackAudio\r\n"
    }

    // any width. 8khz, mono only, as long as it's on 20ms intervals
    fun createG711ABody(trackAudio: Int, sampleRate: Int, isStereo: Boolean): String {
        if (sampleRate != 8000) {
            Log.e(SdpBody::class.simpleName, "Sample rate of G711 must be 8000, not all decoders support other rates")
        }
        val channel = if (isStereo) 2 else 1
        val payload = RtpConstants.payloadTypeG711A
        return "m=audio 0 RTP/AVP ${payload}\r\n" +
                "a=rtpmap:$payload PCMA/$sampleRate/$channel\r\n" +
                "a=control:streamid=$trackAudio\r\n"
    }

    fun createAacBody(trackAudio: Int, sampleRate: Int, isStereo: Boolean): String {
        val sampleRateNum = AUDIO_SAMPLING_RATES.toList().indexOf(sampleRate)
        val channel = if (isStereo) 2 else 1
        val config = 2 and 0x1F shl 11 or (sampleRateNum and 0x0F shl 7) or (channel and 0x0F shl 3)
        val payload = RtpConstants.payloadTypeDynamic + trackAudio
        return "m=audio 0 RTP/AVP ${payload}\r\n" +
                "a=rtpmap:$payload MPEG4-GENERIC/$sampleRate/$channel\r\n" +
                "a=fmtp:$payload profile-level-id=1; mode=AAC-hbr; config=${
                    Integer.toHexString(
                        config
                    )
                }; sizelength=13; indexlength=3; indexdeltalength=3\r\n" +
                "a=control:streamid=$trackAudio\r\n"
    }

    fun createAV1Body(trackVideo: Int): String {
        val payload = RtpConstants.payloadTypeDynamic + trackVideo
        return "m=video 0 RTP/AVP $payload\r\n" +
                "a=rtpmap:$payload AV1/${RtpConstants.clockVideoFrequency}\r\n" +
                "a=fmtp:$payload profile=0; level-idx=0;\r\n" +
                "a=control:streamid=$trackVideo\r\n"
    }

    fun createH264Body(trackVideo: Int, sps: String, pps: String): String {
        val payload = RtpConstants.payloadTypeDynamic + trackVideo
        return "m=video 0 RTP/AVP $payload\r\n" +
                "a=rtpmap:$payload H264/${RtpConstants.clockVideoFrequency}\r\n" +
                // This has a max size constraint of 131072. If the size is greater than this, the server will not be able to parse the SDP
                "a=fmtp:$payload packetization-mode=1; sprop-parameter-sets=$sps,$pps\r\n" +
                "a=control:streamid=$trackVideo\r\n"
    }

    fun createH265Body(trackVideo: Int, sps: String, pps: String, vps: String): String {
        val payload = RtpConstants.payloadTypeDynamic + trackVideo
        return "m=video 0 RTP/AVP ${payload}\r\n" +
                "a=rtpmap:$payload H265/${RtpConstants.clockVideoFrequency}\r\n" +
                "a=fmtp:$payload packetization-mode=1; sprop-sps=$sps; sprop-pps=$pps; sprop-vps=$vps\r\n" +
                "a=control:streamid=$trackVideo\r\n"
    }
}