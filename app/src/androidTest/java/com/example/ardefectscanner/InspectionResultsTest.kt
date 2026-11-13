package com.example.ardefectscanner

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ardefectscanner.ar.*
import com.example.ardefectscanner.ui.InspectionResults
import com.example.ardefectscanner.ui.InspectionSession
import com.example.ardefectscanner.ui.theme.ARDefectScannerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InspectionResultsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun detectedInformationSurvivesScreenRestoration() {
        val restoration = StateRestorationTester(compose)
        lateinit var session: InspectionSession
        restoration.setContent {
            session = rememberSaveable(saver = InspectionSession.Saver) { InspectionSession() }
            ARDefectScannerTheme { InspectionResults(session, onScan = {}) }
        }
        compose.runOnIdle {
            session.accept(DetectionFrame(640, 480, listOf(
                Detection("object", DetectionKind.OBJECT, "Насос",
                    DetectionBox(.1f, .2f, .8f, .9f), .95f),
                Detection("defect", DetectionKind.DEFECT, "Коррозия",
                    DetectionBox(.2f, .3f, .4f, .5f))
            ), modelAvailable = true))
            // Camera inactivity must not erase the frame before opening Results.
            session.accept(null)
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Насос").assertExists()
        compose.onNodeWithText("Коррозия").assertExists()
        compose.onNodeWithText("Уверенность модели: 95%").assertExists()
    }

    @Test fun unavailableModelDoesNotLookLikeAnEmptySuccessfulAnalysis() {
        val session = InspectionSession().apply {
            accept(DetectionFrame(640, 480, emptyList(), modelAvailable = false))
        }
        compose.setContent {
            ARDefectScannerTheme { InspectionResults(session, onScan = {}) }
        }
        compose.onNodeWithText("Модель пока не подключена").assertIsDisplayed()
        compose.onNodeWithText("Ничего не обнаружено").assertDoesNotExist()
    }
}
