package com.example.ardefectscanner

import com.example.ardefectscanner.camera.QrDecoder
import com.example.ardefectscanner.data.EquipmentCatalog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class QrDecoderTest {
    @Test fun equipmentQrOpensMatchingCardFromPaddedCameraPlane() {
        val matrix = QRCodeWriter().encode("KST:PUMP-001", BarcodeFormat.QR_CODE, 256, 256)
        val rowStride = 288
        val bytes = ByteArray(rowStride * 256) { 0x7f.toByte() }
        for (y in 0 until 256) for (x in 0 until 256) {
            bytes[y * rowStride + x] = if (matrix[x, y]) 0 else 0xff.toByte()
        }
        val raw = QrDecoder().decode(ByteBuffer.wrap(bytes), 256, 256, rowStride, 1)
        assertEquals("KST:PUMP-001", raw)
        assertEquals("Насосный агрегат", EquipmentCatalog.resolveQr(raw!!)?.name)
    }

    @Test fun rotatedQrWorksWithPixelStrideAndBufferOffset() {
        val matrix = QRCodeWriter().encode("KST:MOTOR-002", BarcodeFormat.QR_CODE, 256, 256)
        val start = 13
        val rowStride = 544
        val bytes = ByteArray(start + rowStride * 256) { 0x7f.toByte() }
        for (y in 0 until 256) for (x in 0 until 256) {
            bytes[start + y * rowStride + x * 2] = if (matrix[y, 255 - x]) 0 else 0xff.toByte()
        }
        val buffer = ByteBuffer.wrap(bytes).apply { position(start) }
        val raw = QrDecoder().decode(buffer, 256, 256, rowStride, 2)
        assertEquals("KST:MOTOR-002", raw)
        assertEquals(start, buffer.position())
        assertEquals("MOTOR-002", EquipmentCatalog.resolveQr(raw!!)?.id)
    }

    @Test fun blankFrameDoesNotProduceAnEquipmentMatch() {
        val data = ByteArray(256 * 256) { 0xff.toByte() }
        assertNull(QrDecoder().decode(ByteBuffer.wrap(data), 256, 256, 256, 1))
    }

    @Test fun unknownIdentifiersAndUrlsAreNotTreatedAsEquipment() {
        assertNull(EquipmentCatalog.resolveQr("KST:UNKNOWN"))
        assertNull(EquipmentCatalog.resolveQr("https://example.com/PUMP-001"))
        assertNull(EquipmentCatalog.resolveQr(""))
    }
}
