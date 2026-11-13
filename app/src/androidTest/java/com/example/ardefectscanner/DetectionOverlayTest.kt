package com.example.ardefectscanner

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ardefectscanner.ar.*
import com.example.ardefectscanner.ui.theme.ARDefectScannerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetectionOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun objectAndDefectLabelsAreDisplayedTogether() {
        val box = DetectionBox(.1f, .1f, .9f, .9f)
        val objectDetection = Detection("object-1", DetectionKind.OBJECT, "Насос", box, .95f)
        val defect = Detection("defect-1", DetectionKind.DEFECT, "Коррозия", box)
        compose.setContent {
            ARDefectScannerTheme {
                Box(Modifier.size(300.dp, 400.dp)) {
                    DetectionOverlay(listOf(
                        ScreenDetection(objectDetection, RectF(20f, 100f, 500f, 700f)),
                        ScreenDetection(defect, RectF(120f, 350f, 220f, 450f))
                    ), Modifier.matchParentSize())
                }
            }
        }
        compose.onNodeWithText("Объект: Насос · 95%").assertIsDisplayed()
        compose.onNodeWithText("Дефект: Коррозия").assertIsDisplayed()
    }
}
