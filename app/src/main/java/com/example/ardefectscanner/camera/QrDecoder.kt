package com.example.ardefectscanner.camera

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer

class QrDecoder {
    private val reader = QRCodeReader()
    private var luminance = ByteArray(0)
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)

    fun decode(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int): String? {
        require(width > 0 && height > 0 && pixelStride > 0 && rowStride >= width * pixelStride)
        if (luminance.size != width * height) luminance = ByteArray(width * height)
        val start = buffer.position()
        for (y in 0 until height) {
            for (x in 0 until width) {
                luminance[y * width + x] = buffer.get(start + y * rowStride + x * pixelStride)
            }
        }
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: ReaderException) {
            null // No readable QR in this frame is an expected camera state.
        } finally {
            reader.reset()
        }
    }
}
