package com.tranik.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tranik.app.ui.theme.TarAnikTheme
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `permission screen displays correctly`() {
        composeTestRule.setContent {
            TarAnikTheme {
                PermissionScreen(
                    onGranted = {},
                    onDeny = {}
                )
            }
        }

        // بررسی نمایش عنوان اپ
        composeTestRule.onNodeWithText("ترانیک").assertIsDisplayed()

        // بررسی نمایش دکمه اجازه
        composeTestRule.onNodeWithText("اجازه دسترسی").assertIsDisplayed()
    }

    @Test
    fun `permission screen button is clickable`() {
        var granted = false
        composeTestRule.setContent {
            TarAnikTheme {
                PermissionScreen(
                    onGranted = { granted = true },
                    onDeny = {}
                )
            }
        }

        // دکمه باید clickable باشه
        composeTestRule.onNodeWithText("اجازه دسترسی").assertHasClickAction()
    }
}
