package com.example.ardefectscanner

import com.example.ardefectscanner.ar.*
import org.junit.Assert.*
import org.junit.Test

class DetectionContractTest {
    private val box = DetectionBox(.1f, .2f, .8f, .9f)

    @Test fun supportsObjectAndDefectWithoutComponentRelation() {
        val frame = DetectionFrame(640, 480, listOf(
            Detection("o", DetectionKind.OBJECT, "Насос", box, .9f),
            Detection("d", DetectionKind.DEFECT, "Коррозия", box)
        ), modelAvailable = true)
        assertEquals(2, DetectionKind.entries.size)
        assertEquals(listOf(DetectionKind.OBJECT, DetectionKind.DEFECT), frame.detections.map { it.kind })
        assertNull(frame.detections[1].confidence)
    }

    @Test fun rejectsInvalidGeometryConfidenceAndUnavailableResults() {
        assertThrows(IllegalArgumentException::class.java) { DetectionBox(.8f, 0f, .2f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { DetectionBox(Float.NaN, 0f, 1f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { DetectionBox(-.1f, 0f, 1f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { Detection("o", DetectionKind.OBJECT, "Насос", box, 1.1f) }
        val detection = Detection("o", DetectionKind.OBJECT, "Насос", box)
        assertThrows(IllegalArgumentException::class.java) { DetectionFrame(640, 480, listOf(detection), false) }
        assertThrows(IllegalArgumentException::class.java) { DetectionFrame(640, 480, listOf(detection, detection), true) }
    }
}
