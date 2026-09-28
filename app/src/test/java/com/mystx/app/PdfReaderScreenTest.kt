package com.mystx.app

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mystx.app.ui.PdfReaderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PdfReaderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPdfReaderScreenDoesNotCrash() {
        val uri = Uri.parse("content://dummy/uri")
        composeTestRule.setContent {
            PdfReaderScreen(pdfUri = uri, onClose = {})
        }
    }
}
