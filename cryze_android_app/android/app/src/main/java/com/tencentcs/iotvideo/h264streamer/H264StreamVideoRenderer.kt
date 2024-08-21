package com.tencentcs.iotvideo.h264streamer

import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.iotvideoplayer.codec.IVideoDecoder
import com.tencentcs.iotvideo.rtsp.IOnFrameCallback
import com.tencentcs.iotvideo.utils.LogUtils
import java.io.IOException

import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

class H264StreamVideoRenderer(private val rawSocketPort: Int, private var onFrameCallback : IOnFrameCallback) : IVideoDecoder {

    private val TAG = H264StreamVideoRenderer::class.simpleName+"::"+rawSocketPort
    private var initialized = false
    private var framesSent = 0L
    private var framesDropped = 0L

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null

    private var socketHandlerThread: Thread? = null

    @Volatile
    private var running = true

    private enum class State {
        WAITING_FOR_CONNECTION,
        CONNECTED,
        DISCONNECTED
    }

    @Volatile
    private var state = State.WAITING_FOR_CONNECTION
    @Volatile
    private var previousState = State.WAITING_FOR_CONNECTION

    override fun init(avHeader1: AVHeader) {
        LogUtils.i(TAG , "init: header: $avHeader1")

        socketHandlerThread = Thread {
            LogUtils.i(TAG , "connectionHandler starting")
            while (!Thread.currentThread().isInterrupted && running) {
                connectionHandler()
                LogUtils.i(
                    TAG,
                    "connectionHandler is restarting"
                )
            }
            LogUtils.i(TAG , "connectionHandler exiting")
        }

        socketHandlerThread?.start()

        initialized = true
    }

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
                    LogUtils.i(
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
                                try {
                                    Thread.sleep(10_000L) // let the OS clear the port
                                    throw ex
                                } catch (_: InterruptedException) {} // the task is probably done
                            }
                            serverSocket?.reuseAddress = true //allow us to rebind to the same port after we close the server
                            LogUtils.i(
                                TAG,
                                "connectionHandler: serverSocket: $serverSocket"
                            )
                        }
                        clientSocket = serverSocket?.accept()
                        clientSocket?.keepAlive = true
                        LogUtils.i(
                            TAG,
                            "connectionHandler: socketInfo: $clientSocket"
                        )
                        outputStream = clientSocket?.getOutputStream()
                        // this is probably the wrong header but web2rtc and ffmpeg both seem to be happy with it
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: video/avc;\r\nTransfer-Encoding: chunked\r\n"
                        outputStream?.write(header.toByteArray())
                        outputStream?.write("\r\n".toByteArray())
                        LogUtils.i(
                            TAG,
                            "connectionHandler: Client connected"
                        )
                        state = State.CONNECTED
                    }
                    catch (_: SocketTimeoutException)
                    {
                        //swallow, this isn't a bug, it's a feature
                    }
                    catch (e: Exception) {
                        LogUtils.e(
                            TAG,
                            "connectionHandler: Error accepting client: ${e.message} ${e.stackTrace}"
                        )
                    }
                }

                State.CONNECTED -> {
                    LogUtils.i(
                        TAG,
                        "connectionHandler: Client connected"
                    )
                    while (true) {
                        if (clientSocket?.isClosed == true || clientSocket?.isConnected == false || clientSocket?.isOutputShutdown == true) {
                            if (stateChange) LogUtils.i(
                                TAG,
                                "connectionHandler: Client disconnected"
                            )
                            state = State.DISCONNECTED
                            break
                        }
                        try {
                            Thread.sleep(1000L) // give the threadpool some breathing room
                        } catch (_: Exception) {} // swallow
                    }
                }

                State.DISCONNECTED -> {
                    LogUtils.i(
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
            try {
                Thread.sleep(1000L) // give the threadpool some breathing room
            } catch (e: Exception) {
                LogUtils.i(TAG , "connectionHandler sleep error: ${e.message}")
            }
        }
        LogUtils.i(TAG , "connectionHandler stopped")
    }

    private var mByteBufferU: ByteBuffer? = null
    private var mByteBufferV: ByteBuffer? = null
    private var mByteBufferY: ByteBuffer? = null

    // This is the YUV component of the frame - the I frame
    override fun receive_frame(aVData: AVData?): Int {
        onFrameCallback.onFrame()
        try {
            // duplicate each buffer so we can isolate this frame now
            synchronized (this) {
                mByteBufferY = aVData?.data;
                mByteBufferU = aVData?.data1;
                mByteBufferV = aVData?.data2;
            }

            // drain the buffers into a single array
            val buffer0 = ByteArray(mByteBufferY?.remaining() ?: 0)
            val buffer1 = ByteArray(mByteBufferU?.remaining() ?: 0)
            val buffer2 = ByteArray(mByteBufferV?.remaining() ?: 0)
            mByteBufferY?.get(buffer0)
            mByteBufferU?.get(buffer1)
            mByteBufferV?.get(buffer2)

            val buffer = ByteArray(buffer0.size + buffer1.size + buffer2.size)
            System.arraycopy(buffer0, 0, buffer, 0, buffer0.size)
            System.arraycopy(buffer1, 0, buffer, buffer0.size, buffer1.size)
            System.arraycopy(buffer2, 0, buffer, buffer0.size + buffer1.size, buffer2.size)
            tryFlushArrayToClient(buffer)
        }
        catch (err: BufferUnderflowException)
        {
            //framesDropped++
            LogUtils.i(TAG , "receive_frame buffer underflow: ${err.message}")
            return 1 // allow this frame to be reprocessed
        }
        catch (err: Exception)
        {
            framesDropped++
            LogUtils.i(TAG , "receive_frame error: ${err::class.simpleName} ${err.stackTrace}")
        }
        return 0
    }

    // this is header/b/p frames
    override fun send_packet(aVData: AVData?): Int {
        try {
            val byteBufferContents = ByteArray(aVData?.size ?: 0)
            aVData?.data?.get(byteBufferContents)
            tryFlushArrayToClient(byteBufferContents)
        } catch (err: Exception)
        {
            LogUtils.i(TAG , "send_packet error: ${err.message}")
        }

        return 0
    }

    private fun tryFlushArrayToClient(valueToFlush: ByteArray) {
        if (state == State.CONNECTED) {
            try {
                outputStream?.write(valueToFlush)
                outputStream?.flush()
                // whew! we managed to send the frame! you have no idea how painful it was to get here.
                framesSent++
            } catch (e: Exception) {
                framesDropped++
                outputStream?.close()
                LogUtils.i(TAG , "tryFlushArrayToClient error: ${e.message}")
            }

        }
    }

    override fun release() {
        LogUtils.i(TAG , "release")
        try {
            running = false
            initialized = false
            socketHandlerThread?.interrupt()
            LogUtils.i(TAG , "waiting for socketHandlerThread")
            socketHandlerThread?.join()
            LogUtils.i(TAG , "socketHandlerThread is closed")
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
            serverSocket = null // construct a new one, this one is dead
            state = State.WAITING_FOR_CONNECTION
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        return "${H264StreamVideoRenderer::class.simpleName}={ framesSent: $framesSent framesDropped: $framesDropped connectionState: ${state.name} }"
    }
}

