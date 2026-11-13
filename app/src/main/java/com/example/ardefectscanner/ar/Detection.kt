package com.example.ardefectscanner.ar

import androidx.camera.core.ImageProxy

enum class DetectionKind { OBJECT, DEFECT }

/** Normalized bounds in the upright, cropped camera image, after undoing model letterboxing. */
data class DetectionBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f })
        require(left < right && top < bottom)
    }
}

data class Detection(
    val id: String,
    val kind: DetectionKind,
    val label: String,
    val box: DetectionBox,
    val confidence: Float? = null
) {
    init {
        require(id.isNotBlank() && label.isNotBlank())
        require(confidence == null || (confidence.isFinite() && confidence in 0f..1f))
    }
}

data class DetectionFrame(
    val imageWidth: Int,
    val imageHeight: Int,
    val detections: List<Detection>,
    val modelAvailable: Boolean
) {
    init {
        require(imageWidth > 0 && imageHeight > 0)
        require(modelAvailable || detections.isEmpty())
        require(detections.map { it.id }.distinct().size == detections.size)
    }
}

/** A future LiteRT adapter owns preprocessing, inference and decoding on the analysis thread. */
interface FrameDetector : AutoCloseable {
    fun detect(image: ImageProxy): DetectionFrame
    override fun close() {}
}

class UnavailableDetector : FrameDetector {
    override fun detect(image: ImageProxy): DetectionFrame {
        val rotated = image.imageInfo.rotationDegrees % 180 != 0
        val crop = image.cropRect
        return DetectionFrame(
            if (rotated) crop.height() else crop.width(),
            if (rotated) crop.width() else crop.height(),
            emptyList(), modelAvailable = false
        )
    }
}
