package com.tencentcs.iotvideo.restreamer.mjpeg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.MediaCodec.INFO_TRY_AGAIN_LATER
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
import android.media.MediaCodecList
import android.media.MediaFormat
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant
import com.tencentcs.iotvideo.iotvideoplayer.mediacodec.MediaConstant.INPUT_BUFFER_ERROR
import com.tencentcs.iotvideo.restreamer.interfaces.IRendererCallback
import com.tencentcs.iotvideo.restreamer.interfaces.IRestreamingVideoDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean


class ImageServerVideoStreamDecoder (private val rawSocketPort : Int, private val onFrameCallback: IRendererCallback, baseContext : Context) :
    IRestreamingVideoDecoder {
        
    private val TAG = ImageServerVideoStreamDecoder::class.simpleName
    private val _initialized = AtomicBoolean(false)
    override var initialized: Boolean // latched to true
        get() = _initialized.get()
        set(value) = if(_initialized.get()) {} else _initialized.set(value)
    
    private var codec : MediaCodec? = null
    private var mediaFormat : MediaFormat? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null

    private var socketHandlerThread: Thread? = null

    @Volatile private var running = true

    private enum class State {
        WAITING_FOR_CONNECTION,
        CONNECTED,
        DISCONNECTED
    }

    @Volatile private var state = State.WAITING_FOR_CONNECTION
    @Volatile private var previousState = State.WAITING_FOR_CONNECTION

    private var converter : ImageConverter = ImageConverter(baseContext)

    init {
        serverSocket = ServerSocket(rawSocketPort)
        LogUtils.i(TAG, "Server started on port $rawSocketPort")

        socketHandlerThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                connectionHandler()
                LogUtils.i(TAG, "connectionHandler is restarting")
            }
            LogUtils.i(TAG, "connectionHandler exiting")
        }

        socketHandlerThread?.start()

        // track FPS for toString
        converter.logFps = true
    }

    val boundary = "--frameboundary--"

    private fun connectionHandler() {

        while (running && !Thread.currentThread().isInterrupted) {
            var stateChange = false
            if (previousState != state) {
                LogUtils.i(
                    TAG,
                    "connectionHandler state: ${state.name}"
                )
                previousState = state
                stateChange = true
            }
            when (state) {
                State.WAITING_FOR_CONNECTION -> {
                    if(stateChange)LogUtils.i(
                        TAG,
                        "Waiting for client connection"
                    )
                    try {
                        if (serverSocket == null) {
                            // take the socket if it is already bound
                            if (clientSocket?.isClosed == false) {
                                clientSocket?.close()
                            }
                            try {
                                serverSocket = ServerSocket(rawSocketPort)
                                serverSocket?.soTimeout = 10_000
                            } catch (ex: IOException)
                            {
                                LogUtils.e(TAG , "connectionHandler: clientSocket: error: ${ex.message} ${ex.stackTrace}")
                                serverSocket?.close()
                                serverSocket = null
                                Thread.yield()
                            }
                            serverSocket?.reuseAddress = true //allow us to rebind to the same port after we close the server
                            if(stateChange)LogUtils.i(
                                TAG,
                                "connectionHandler: serverSocket: $serverSocket"
                            )
                        }
                        clientSocket = serverSocket?.accept()
                        clientSocket?.keepAlive = false
                        clientSocket?.soTimeout = 10_000
                        clientSocket?.tcpNoDelay = true
                        if(stateChange)LogUtils.i(
                            TAG,
                            "connectionHandler: socketInfo: $clientSocket"
                        )
                        outputStream = clientSocket?.getOutputStream()
                        outputStream?.write("HTTP/1.1 200 OK\r\n".toByteArray())
                        val header = "Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n"
                        outputStream?.write(header.toByteArray())
                        outputStream?.write("\r\n".toByteArray())
                        if(stateChange) LogUtils.i(
                            TAG,
                            "connectionHandler: Client connected"
                        )
                        state = State.CONNECTED
                    }
                    catch (_: SocketTimeoutException)
                    {
                        //swallow, this isn't a bug, it's a feature.
                        //Basically, don't block if we can't connect to the client
                    }
                    catch (e: Exception) {
                        LogUtils.e(
                            TAG,
                            "connectionHandler: Error accepting client: ${e.message} ${e.stackTrace}"
                        )
                    }
                }

                State.CONNECTED -> {
                    if(stateChange)LogUtils.i(
                        TAG,
                        "connectionHandler: Client connected"
                    )
                    if (clientSocket?.isClosed == true || clientSocket?.isConnected == false || clientSocket?.isOutputShutdown == true) {
                        LogUtils.i(
                            TAG,
                            "connectionHandler: Client disconnected"
                        )
                        state = State.DISCONNECTED
                    }
                }

                State.DISCONNECTED -> {
                    if(stateChange)LogUtils.i(
                        TAG,
                        "connectionHandler: Client disconnected"
                    )
                    try {
                        clientSocket?.close()
                    } catch (e: Exception) {
                        LogUtils.i(
                            TAG,
                            "connectionHandler: Error closing connection: ${e.message}"
                        )
                    }
                    state = State.WAITING_FOR_CONNECTION
                }
            }
            Thread.yield()
        }
        LogUtils.i(TAG , "connectionHandler stopped")
    }

    private var watchdog : Thread? = Thread {
        while(!Thread.currentThread().isInterrupted)
        {
            try {
                Thread.sleep(60_000)
            } catch (e: InterruptedException) {
                return@Thread
            }
            if(frames == previousFrames)
            {
                onFrameCallback.onRenderWatchdogRequestsRestart()
            } else {
                previousFrames = frames
            }

        }

    }

    @SuppressLint("WrongConstant")
    override fun init(aVHeader: AVHeader?) {
        val videoMime = MediaConstant.getVideoMimeByAVHeader(aVHeader)
        codec = MediaCodec.createDecoderByType(videoMime)
        val imageWidth = aVHeader!!.getInteger("width", 0)
        val imageHeight = aVHeader.getInteger("height", 0)
        val frameRate = aVHeader.getInteger(AVHeader.KEY_FRAME_RATE, 20)

        val incomingFormat = MediaFormat.createVideoFormat(videoMime, imageWidth, imageHeight)
        incomingFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        incomingFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2 ) // default: suggested I-frame interval for webcams and h264
        incomingFormat.setInteger("color-format", COLOR_FormatYUV420Flexible)

        // clamp the bitrate https://stackoverflow.com/questions/26110337/what-are-valid-bit-rates-to-set-for-mediacodec
        var bitRate: Int = aVHeader.getInteger(AVHeader.KEY_BIT_RATE, (4.8*imageHeight*imageWidth).toInt())
        val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in mcl.codecInfos) {
            if (!info.isEncoder) {
                continue
            }
            try {
                val caps = info.getCapabilitiesForType(videoMime)
                if (caps != null && caps.isFormatSupported(incomingFormat)) {
                    bitRate = caps.videoCapabilities.bitrateRange.clamp(bitRate)
                    incomingFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                    break
                }
            } catch (e: IllegalArgumentException) {
                // type is not supported
            }
        }

        // Configure the codec to decode only, we are passing the image to the VideoSource which will pass it to the surface to encode
        codec?.configure(incomingFormat, null, null, 8)
        mediaFormat = codec?.outputFormat
        codec?.start()

        watchdog?.start()
    }

    var previousFrames = 0L
    var frames = 0L

    override fun receive_frame(aVData: AVData?): Int {
        var bufferInfo: MediaCodec.BufferInfo? = null
        var dequeueStatus = 0
        try {
            bufferInfo = MediaCodec.BufferInfo()
            dequeueStatus = codec!!.dequeueOutputBuffer(bufferInfo, 20000L)
        }
        catch (_ : IOException)
        {
            close()
        }
        catch (exception: Exception)
        {
            LogUtils.e(TAG, "receive_frame exception:" + exception.message)
        }

        if (dequeueStatus < 0) {
            if (dequeueStatus != INFO_TRY_AGAIN_LATER && dequeueStatus == INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = codec!!.outputFormat
            }
            return MediaConstant.SEND_PACKET_ERROR
        }

        val outputImage = codec!!.getOutputImage(dequeueStatus)

        if (outputImage == null) {
            LogUtils.e(TAG, "receive_frame error, image is null")
            return MediaConstant.SEND_PACKET_ERROR
        }

        if (state == State.CONNECTED) {
            try {
                // flush the image to the surface to encode
                val bitmap = converter.convert(outputImage)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // mjpeg "frame"
                outputStream?.write("$boundary\r\n".toByteArray())
                outputStream?.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                outputStream?.write(byteArray)
                outputStream?.write("\r\n".toByteArray())

                // flush the stream
                outputStream?.flush()
            }
            catch (_ : IOException)
            {
                close()
            }
            catch (e: Exception) {
                LogUtils.e(
                    TAG,
                    "receive_frame exception:" + e.message
                )
            }
        }

        val rtpTimestamp = bufferInfo!!.presentationTimeUs
        aVData!!.pts = rtpTimestamp
        aVData.dts = rtpTimestamp
        aVData.keyFrame = bufferInfo.flags and 1

        codec?.releaseOutputBuffer(dequeueStatus, false)

        onFrameCallback.onFrame()
        frames++

        return 0
    }

    fun close() {
        running = false
        try {
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            LogUtils.i(TAG, "Error closing server: ${e.message}")
        }
        LogUtils.i(TAG, "Server closed")
    }

    override fun release() {
        // if we are initialized then this is a real release
        release(initialized)
    }

    fun release(force: Boolean)
    {
        if(!initialized && !force)
            return

        close()
        watchdog?.interrupt()
        watchdog?.join()
        watchdog = null

        socketHandlerThread?.interrupt()
        socketHandlerThread?.join()
        socketHandlerThread = null
        LogUtils.i(TAG, "release")
        codec?.stop()
        codec?.release()
    }

    fun finalize() {
        release()
    }

    override fun send_packet(aVData: AVData?): Int {
        if(aVData == null)
            return MediaConstant.SEND_PACKET_ERROR

        try {
            val mediaCodec = codec
            if (mediaCodec == null) {
                LogUtils.e(TAG, "send_packet error:codec is null")
                return MediaConstant.SEND_PACKET_ERROR
            }
            val dequeueInputBuffer = mediaCodec.dequeueInputBuffer(20000L)
            if (dequeueInputBuffer >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(dequeueInputBuffer)
                if (inputBuffer == null) {
                    LogUtils.e(TAG, "send_packet error, inputBuffer is null")
                    return MediaConstant.SEND_PACKET_ERROR
                }
                inputBuffer.clear()
                inputBuffer.put(aVData.data)
                mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, aVData.size, aVData.pts, 0)

                return 0
            }
            LogUtils.e(
                TAG,
                "send_packet error, try again later " + aVData.pts
            )
            return INPUT_BUFFER_ERROR
        } catch (sendingException: java.lang.Exception) {
            LogUtils.e(TAG, "send_packet exception:" +sendingException::class.simpleName + sendingException.message + sendingException.stackTraceToString())
            return MediaConstant.SEND_PACKET_ERROR
        }
    }

    override fun toString(): String {
        return "ImageServerVideoStreamDecoder{SocketState: ${state.name}, FPS: ${converter.lastFps}}"
    }

}