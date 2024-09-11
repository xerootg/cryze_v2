package com.github.xerootg.cryze.restreamer.rtspmuxer.sources

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.MimeTypes

import com.pedro.common.VideoCodec
import com.pedro.common.isKeyframe
import com.pedro.rtsp.rtsp.RtspClient
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.github.xerootg.cryze.restreamer.interfaces.IRestreamingVideoDecoder
import com.github.xerootg.cryze.restreamer.rtspmuxer.ISourceReadyCallback
import com.tencentcs.iotvideo.utils.LogUtils
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.and

@SuppressLint("UnsafeOptInUsageError")
class RemuxerVideoSource(
    private val muxer: RtspClient,
    private val readyCallback: ISourceReadyCallback,
    private val debugLogging : Boolean = false
) : IRestreamingVideoDecoder, IRtspStreamSource {
    override var initialized: Boolean
        get() = initCalled && spsPpsSent.get()
        set(_) {
            throw UnsupportedOperationException("Directly setting not allowed")
        } // no-op
    private var initCalled = false
    private var framesProcessed = 0L

    private val mTaskThread: HandlerThread = HandlerThread("VideoSenderThread")
    private val mTaskTHandler: Handler by lazy { Handler(mTaskThread.looper) }

    override fun init(aVHeader: AVHeader?) {
        if (aVHeader == null) {
            LogUtils.w(TAG, "init: aVHeader is null")
            return
        }

        if(!initCalled) {

            val mime = MediaConstant.getVideoMimeByAVHeader(aVHeader)

            when (mime) {
                MimeTypes.VIDEO_H264 -> {
                    muxer.setVideoCodec(VideoCodec.H264)
                }

                // To support other video types, the sps/pps/vps must be sent to the muxer
                // right now, only H264 is implemented. see send_packet for more info
                MimeTypes.VIDEO_H265 -> muxer.setVideoCodec(VideoCodec.H265)
                MimeTypes.VIDEO_AV1 -> muxer.setVideoCodec(VideoCodec.AV1)
                else -> {
                    muxer.setOnlyAudio(true)
                    LogUtils.e(TAG, "init: Unsupported video type: $mime")
                }
            }

            initCalled = true

            LogUtils.i(TAG, "video init: $aVHeader")
        } else {
            LogUtils.w(TAG, "init: already initialized")
        }
    }

    override fun receive_frame(aVData: AVData?): Int {
        if (aVData?.extraData != null) {
            if(debugLogging)LogUtils.i(TAG, "receive_frame: extraData is not null: ${aVData.extraSize}")
        }
        return 0
    }

    // nothing to release
    override fun release() {}

    var lastSps: ByteArray? = null
    var lastPps: ByteArray? = null
    val spsPpsSent = AtomicBoolean(false)
    var doNotReconnect = AtomicBoolean(false)

    override fun send_packet(aVData: AVData?): Int {
        if(doNotReconnect.get())
        {
            if(debugLogging)LogUtils.i(TAG, "send_packet: doNotReconnect is set")
            return 0
        }
        if (aVData?.data != null && aVData.pts != 0L) {

            // make the frame here so the buffer is copied immediately,
            // not when the posting thread finally runs
            val frame = ByteArray(aVData.size)
            aVData.data.get(frame)

            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.presentationTimeUs = aVData.pts
            // TODO: aVData.size seems wrong. servers are complaining about the size
            bufferInfo.size = frame.size

            val nalUnits = extractNalUnits(frame)

            if (nalUnits.any(RemuxerVideoSource::rawNalUnitIsH264KeyFrame)) {
                bufferInfo.flags = BUFFER_FLAG_KEY_FRAME
            }

            if (!postToMuxer) {
                if(debugLogging) {
                    printNalTypes(frame)
                }

                for (nalUnit in nalUnits) {
                    // determine the type of nal
                    val nalType = getNalUnitTypeFromExtractedNalUnit(nalUnit).toInt()
                    when (nalType) {
                        MediaConstant.NAL_UNIT_TYPE_SPS -> {
                            lastSps = nalUnit
                        }

                        MediaConstant.NAL_UNIT_TYPE_PPS -> {
                            lastPps = nalUnit
                        }
                    }
                    LogUtils.i(TAG, "NAL type: $nalType")
                }

                if (lastSps != null && lastPps != null) {
                    val sps = lastSps!!
                    val pps = lastPps!!
                    LogUtils.i(TAG, "Got SPS and PPS")
                    // Send the SPS and PPS to the muxer
                    muxer.setVideoInfo(sps, pps, null)
                    spsPpsSent.set(true)
                    if (initialized && !doNotReconnect.get() // && MediaConstant.isH264KeyFrame(frameType)
                    ) {
                        LogUtils.i(TAG, "SPS/PPS set, starting muxer")
                        readyCallback.sourceReady()
                    } else {
                        LogUtils.i(TAG, "SPS/PPS set, but not starting muxer")
                        if(!initialized) {
                            LogUtils.i(TAG, "SPS/PPS set, but not starting muxer because not initialized")
                        }
                        if(doNotReconnect.get()) {
                            LogUtils.i(TAG, "SPS/PPS set, but not starting muxer because doNotReconnect is set")
                        }
                    }
                }

            }
            else {
                // we're ready to send the frame
                val buffer = ByteBuffer.wrap(frame)
                if(debugLogging)LogUtils.i(TAG, "bufferinfo: pts:${bufferInfo.presentationTimeUs}, size:${bufferInfo.size}")
                mTaskTHandler.post {
                    if (postToMuxer) // if we are still good to go, post the frame
                    {
                        if (bufferInfo.isKeyframe() && debugLogging)
                            LogUtils.i(TAG, "Sending key frame")
                        muxer.sendVideo(buffer, bufferInfo)
                    }
                }
            }
            framesProcessed++
        } else {
            LogUtils.w(TAG, "Ignoring null or timeless packet")
        }
        return 0
    }

    override var postToMuxer = false
    override fun startStream() {
        mTaskThread.start()
        if(doNotReconnect.get())
        {
            LogUtils.e(TAG, "startStream: doNotReconnect is set, stream state might be inconsistent")
            doNotReconnect.set(false)
        }
        postToMuxer = true
        LogUtils.i(TAG, "Starting stream")
    }

    override fun stopStream() {
        postToMuxer = false
        doNotReconnect.set(true)
        mTaskThread.quitSafely()
        LogUtils.i(TAG, "Stopping stream. doNotReconnect is set")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("$TAG: ")
        sb.append("initialized: $initialized")
        sb.append(", postToMuxer: $postToMuxer")
        sb.append(", framesProcessed: $framesProcessed")
        return sb.toString()
    }

    companion object {
        val TAG = RemuxerVideoSource::class.simpleName

        private fun getSeqParameterSetId(spsIn: ByteArray): Int {
            val sps = ByteBuffer.wrap(spsIn)
            // Rewind the buffer to ensure it is read from the beginning
            sps.rewind()

            // Skip the NAL unit header (1 byte for forbidden_zero_bit, nal_ref_idc, and nal_unit_type)
            sps.position(1)

            // Read the profile_idc (1 byte)
            sps.get()

            // Read the constraint_set_flags and reserved_zero_2bits (1 byte)
            sps.get()

            // Read the level_idc (1 byte)
            sps.get()

            // Read the seq_parameter_set_id (Exp-Golomb coded)
            val seqParameterSetId = readExpGolombCode(sps)

            return seqParameterSetId
        }

        fun extractNalUnits(data: ByteArray): List<ByteArray> {
            val nalUnits = mutableListOf<ByteArray>()
            var i = 0

            while (i < data.size - 4) {
                // Check for NAL unit start code (0x000001 or 0x00000001)
                if ((data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()) ||
                    (data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 1.toByte())
                ) {

                    val startCodeLength = if (data[i + 2] == 1.toByte()) 3 else 4
                    val nalStartIndex = i + startCodeLength

                    // Find the end of the NAL unit by looking for the next start code
                    var nalEndIndex = data.size
                    for (j in nalStartIndex until data.size - 4) {
                        if ((data[j] == 0.toByte() && data[j + 1] == 0.toByte() && data[j + 2] == 0.toByte() && data[j + 3] == 1.toByte()) ||
                            (data[j] == 0.toByte() && data[j + 1] == 0.toByte() && data[j + 2] == 1.toByte())
                        ) {
                            nalEndIndex = j
                            break
                        }
                    }

                    // Extract the NAL unit
                    val nalUnit = data.copyOfRange(nalStartIndex, nalEndIndex)
                    nalUnits.add(nalUnit)

                    // Move the index to the end of the current NAL unit
                    i = nalEndIndex
                } else {
                    i++
                }
            }

            return nalUnits
        }

        // these wont have the start code, so we need to find the first byte of the array. extractNalUnits
        // provides the start and end of the nal unit, which renders other methods useless
        fun getNalUnitTypeFromExtractedNalUnit(data: ByteArray): Byte{
            return data[0] and 0x1F
        }

        fun rawNalUnitIsH264KeyFrame(data: ByteArray): Boolean {
            val nalUnitType = getNalUnitTypeFromExtractedNalUnit(data).toInt()
            return nalUnitType == MediaConstant.NAL_UNIT_TYPE_PPS || nalUnitType == MediaConstant.NAL_UNIT_TYPE_SPS || nalUnitType == MediaConstant.NAL_UNIT_TYPE_IDR
        }

        fun printNalTypes(data: ByteArray) {
            var i = 0
            while (i < data.size - 4) {
                // Check for NAL unit start code (0x000001 or 0x00000001)
                if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()) {
                    // NAL unit header is the byte after the start code
                    val nalUnitHeader = data[i + 4]
                    // NAL unit type is the last 5 bits of the NAL unit header
                    val nalUnitType = nalUnitHeader.toInt() and 0x1F
                    LogUtils.i(TAG,"NAL Unit Type: $nalUnitType")
                    i += 4
                } else if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 1.toByte()) {
                    // Handle the case where the start code is 0x000001
                    val nalUnitHeader = data[i + 3]
                    val nalUnitType = nalUnitHeader.toInt() and 0x1F
                    LogUtils.i(TAG, "NAL Unit Type: $nalUnitType")
                    i += 3
                } else {
                    i++
                }
            }
        }

        private fun readExpGolombCode(buffer: ByteBuffer): Int {
            var leadingZeroBits = 0
            while (buffer.get().toInt() == 0) {
                leadingZeroBits++
            }
            buffer.position(buffer.position() - 1) // Move back one position

            val codeNum = (1 shl leadingZeroBits) - 1 + readBits(buffer, leadingZeroBits)
            return codeNum
        }

        private fun readBits(buffer: ByteBuffer, numBits: Int): Int {
            var value = 0
            for (i in 0 until numBits) {
                value = (value shl 1) or (buffer.get().toInt() and 0x01)
            }
            return value
        }

        // IF we need to create a PPS from an SPS, we can use this. it's a stub PPS, but it's enough to get the muxer going
        // Mostly here for worst case scenario where we can't get the PPS from the stream. This is a last resort.
        @Suppress("unused")
        fun createPpsFromSps(sps: ByteArray): ByteArray {

            val seqParameterSetId = getSeqParameterSetId(sps)

            val pps = ByteBuffer.allocate(128) // Allocate a buffer with enough space

            // Example values, adjust as needed based on SPS parsing
            val picParameterSetId = 0
            val entropyCodingModeFlag = false
            val bottomFieldPicOrderInFramePresentFlag = false
            val numSliceGroupsMinus1 = 0
            val numRefIdxL0DefaultActiveMinus1 = 0
            val numRefIdxL1DefaultActiveMinus1 = 0
            val weightedPredFlag = false
            val weightedBipredIdc = 0
            val picInitQpMinus26 = 0
            val picInitQsMinus26 = 0
            val chromaQpIndexOffset = 0
            val deblockingFilterControlPresentFlag = true
            val constrainedIntraPredFlag = false
            val redundantPicCntPresentFlag = false

            pps.put(0x68.toByte()) // NAL unit type for PPS
            pps.put((picParameterSetId shl 3 or seqParameterSetId).toByte())
            pps.put((if (entropyCodingModeFlag) 1 else 0).toByte())
            pps.put((if (bottomFieldPicOrderInFramePresentFlag) 1 else 0).toByte())
            pps.put(numSliceGroupsMinus1.toByte())
            pps.put(numRefIdxL0DefaultActiveMinus1.toByte())
            pps.put(numRefIdxL1DefaultActiveMinus1.toByte())
            pps.put((if (weightedPredFlag) 1 else 0).toByte())
            pps.put(weightedBipredIdc.toByte())
            pps.put(picInitQpMinus26.toByte())
            pps.put(picInitQsMinus26.toByte())
            pps.put(chromaQpIndexOffset.toByte())
            pps.put((if (deblockingFilterControlPresentFlag) 1 else 0).toByte())
            pps.put((if (constrainedIntraPredFlag) 1 else 0).toByte())
            pps.put((if (redundantPicCntPresentFlag) 1 else 0).toByte())

            pps.flip() // Prepare the buffer for reading
            return pps.array()
        }

    }

}