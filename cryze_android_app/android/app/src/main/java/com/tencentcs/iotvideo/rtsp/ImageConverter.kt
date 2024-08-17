package com.tencentcs.iotvideo.rtsp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.renderscript.*
import com.tencentcs.iotvideo.utils.LogUtils
import kotlin.concurrent.fixedRateTimer

class ImageConverter(private val context: Context) {
    class ImageConvertFailed : Exception("Image converting failed")

    companion object {
        private const val HARDCODE_SCALE = 1.0f
    }

    // calculate frames per second
    private var count = 0
    private var logFps = false
    private var trackingStarted = false

    val logCallback: (Int) -> Unit = { count ->
        LogUtils.d(ImageConverter::class.simpleName,"Conversion FPS: $count")
    }

    private fun startTracking() {
        if (!trackingStarted) {
            trackingStarted = true
            fixedRateTimer("callbackTimer", initialDelay = 1000, period = 1000) {
                logCallback(count)
                if (count == 0) {
                    trackingStarted = false
                    LogUtils.d(ImageConverter::class.simpleName,"Tracking stopped")
                    cancel()
                }
                count = 0 // Reset count after each second
            }
        }
    }

    val onFrame: () -> Unit = {
        if (!trackingStarted) {
            startTracking()
        }
        count++
    }

    fun convert(img: Image, rotation: Int = 0): Bitmap {
        val frame = nv21ToBitmap(yuv420toNV21(img), img.width, img.height, rotation)
        if(logFps == true) onFrame() // track frames per second
        return frame
    }

    private fun nv21ToBitmap(bytes: ByteArray, width: Int, height: Int, rotate: Int): Bitmap {
        val rs = RenderScript.create(context)
        val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(bytes.size)
        val input = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)

        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        val output = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)

        input.copyFrom(bytes)

        yuvToRgbIntrinsic.setInput(input)
        yuvToRgbIntrinsic.forEach(output)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.copyTo(bitmap)

        input.destroy()
        output.destroy()
        yuvToRgbIntrinsic.destroy()
        rs.destroy()

        return bitmap.scaleAndRotate(HARDCODE_SCALE, rotate)
    }

    private fun Bitmap.scaleAndRotate(scale: Float, rotate: Int): Bitmap {
        val m = Matrix()
        m.postScale(scale, scale)
        if (rotate != 0) {
            m.postRotate(rotate.toFloat())
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    private fun yuv420toNV21(image: Image): ByteArray {
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }
}