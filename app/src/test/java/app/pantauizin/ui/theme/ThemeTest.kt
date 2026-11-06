package app.pantauizin.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTest {
    @Test
    fun lightSchemePrimaryAdalahMerahMarunIdentitas() {
        assertEquals(Color(0xFF551F20), LightColorScheme.primary)
        assertEquals(Color.White, LightColorScheme.onPrimary)
    }

    @Test
    fun darkSchemePrimaryLebihTerangDariLightScheme() {
        assertNotEquals(LightColorScheme.primary, DarkColorScheme.primary)
    }

    @Test
    fun statusColorsSesuaiSpec() {
        assertEquals(Color(0xFF2E7D32), StatusColors.Hijau)
        assertEquals(Color(0xFFEF6C00), StatusColors.Oranye)
        assertEquals(Color(0xFF1565C0), StatusColors.Biru)
        assertEquals(Color(0xFFF9A825), StatusColors.Amber)
        assertEquals(Color(0xFFD32F2F), StatusColors.Merah)
    }
}

