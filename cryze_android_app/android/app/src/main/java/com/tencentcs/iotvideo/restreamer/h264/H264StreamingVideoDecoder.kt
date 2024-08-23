package com.tencentcs.iotvideo.restreamer.h264

import com.tencentcs.iotvideo.StackTraceUtils
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVData
import com.tencentcs.iotvideo.iotvideoplayer.codec.AVHeader
import com.tencentcs.iotvideo.restreamer.interfaces.IRendererCallback
import com.tencentcs.iotvideo.restreamer.interfaces.IRestreamingVideoDecoder
import com.tencentcs.iotvideo.utils.LogUtils
import java.io.IOException
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean


class H264StreamingVideoDecoder(private val rawSocketPort: Int, private var onFrameCallback : IRendererCallback) :
    IRestreamingVideoDecoder {

    private val TAG = H264StreamingVideoDecoder::class.simpleName+"::"+rawSocketPort
    private val _initialized = AtomicBoolean(false)
    override var initialized: Boolean // latched to true
        get() = _initialized.get()
        set(value) = if(_initialized.get()) {} else _initialized.set(value)
    private var framesSent = 0L
    private var framesDropped = 0L

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null

    private var socketHandlerThread: Thread? = null
    private var parsingThread: Thread? = null

    private var released: Boolean = false

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

    var frames = 0L
    var previousFrames = 0L

    var watchdog : Thread? = Thread {
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

    override fun init(avHeader: AVHeader) {
        released = false
        running = true
        LogUtils.i(TAG , "init: header: $avHeader")

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

        parsingThread = Thread {
            LogUtils.i(TAG , "parsingHandler starting")
            while (!Thread.currentThread().isInterrupted) {
                process_queue()
                try {
                    Thread.sleep(100) // always be doing something
                } catch (_:InterruptedException){}
            }
            LogUtils.i(TAG , "parsingHandler exiting")
        }

        socketHandlerThread?.start()

        initialized = true

        parsingThread?.start()
        watchdog?.start()
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
                        // If you have a header, this is where you would send it
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

    override fun receive_frame(aVData: AVData?): Int {
        return 0
    }

    override fun send_packet(aVData: AVData?): Int {
        onFrameCallback.onFrame()

        val byteBufferContents = ByteArray(aVData?.size ?: 0)
        aVData?.data?.get(byteBufferContents)
        queue.add(byteBufferContents)

        frameReady = true

        return 0
    }


    private var frameReady = false

    private val queue = ArrayDeque<ByteArray>()

    private fun process_queue() {
        if (state != State.CONNECTED)
        {
            if (!queue.isEmpty()) {
                queue.clear()
            }
        }
        else if (frameReady)
        {
            while(!queue.isEmpty() && state == State.CONNECTED)
            {
                // if we didn't get anything, reevaluate the queue
                val data = queue.removeFirstOrNull() ?: continue
                try {
                    outputStream?.write(data)
                    outputStream?.flush() ?: throw Exception("outputStream is null")
                } catch (e: Exception) { // catch both our if output is null exception and any socket related exceptions
                    framesDropped += queue.size
                    outputStream?.close()
                    LogUtils.i(TAG , "tryFlushArrayToClient error: ${e.message}")
                    queue.clear()
                    frameReady = false
                    return // let the processing thread restart the processor
                }
                framesSent++
            }
            frameReady = false
        }

    }

    override fun release() {
        release(false)
    }

    fun release(force: Boolean = false) {
        StackTraceUtils.logStackTrace(TAG , "release")
        if(!initialized && !force) {
            LogUtils.i(TAG , "release: not initialized, skipping")
            return
        }
    }

    fun finalize() {
        release(true)

        released = true
        LogUtils.i(TAG , "finalizing")

        running = false
        initialized = false
        watchdog?.interrupt()
        LogUtils.i(TAG , "waiting for watchdog")
        watchdog?.join()
        LogUtils.i(TAG , "watchdog is closed")
        watchdog = null
        socketHandlerThread?.interrupt()
        LogUtils.i(TAG , "waiting for socketHandlerThread")
        socketHandlerThread?.join()
        LogUtils.i(TAG , "socketHandlerThread is closed")
        socketHandlerThread = null
        parsingThread?.interrupt()
        LogUtils.i(TAG , "waiting for parsingThread")
        parsingThread?.join()
        LogUtils.i(TAG , "parsingThread is closed")
        parsingThread = null

        outputStream?.close()
        clientSocket?.close()
        serverSocket?.close()
        serverSocket = null // construct a new one, this one is dead
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("${H264StreamingVideoDecoder::class.simpleName}{");
        sb.append("\n\t\tframesSent: $framesSent");
        sb.append("\n\t\tframesDropped: $framesDropped");
        sb.append("\n\t\tconnectionState: ${state.name}");
        sb.append("\n\t\tparsingThreadState: ${parsingThread?.state}")
        sb.append("\n\t\tsocketHandlerThreadState: ${socketHandlerThread?.state}\n\t");
        return sb.toString()
    }
}

