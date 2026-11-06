package app.pantauizin.ui.nav

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.pantauizin.ui.theme.PantauIzinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menampilkanEmpatTabDanBerpindahKeTerlambatSaatDiketuk() {
        composeTestRule.setContent {
            PantauIzinTheme {
                AppNav()
            }
        }

        composeTestRule.onNodeWithText("Izin").assertExists().assertIsSelected()
        composeTestRule.onNodeWithText("Terlambat").assertExists()
        composeTestRule.onNodeWithText("Notifikasi").assertExists()
        composeTestRule.onNodeWithText("Pengaturan").assertExists()

        composeTestRule.onNodeWithText("Terlambat").performClick()
        composeTestRule.onNodeWithText("Terlambat").assertIsSelected()
    }
}

