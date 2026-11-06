package app.pantauizin.ui.izin

import androidx.compose.ui.graphics.Color
import app.pantauizin.domain.IzinStatus
import app.pantauizin.ui.theme.StatusColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStyleTest {
    @Test
    fun styleOfMengembalikanLabelIkonDanWarnaUntukSemuaStatusTanpaWarnaUtama() {
        val primary = Color(0xFF551F20)
        val styles = IzinStatus.entries.associateWith { styleOf(it) }

        assertEquals(6, styles.values.map { it.label }.distinct().size)
        for ((_, style) in styles) {
            assertTrue(style.label.isNotBlank())
            assertNotEquals(primary, style.color)
        }

        assertEquals(StatusColors.Biru, styles.getValue(IzinStatus.IZIN_BERJALAN).color)
        assertEquals(StatusColors.Merah, styles.getValue(IzinStatus.TERLAMBAT_BELUM_KONFIRMASI).color)
        assertEquals(StatusColors.Amber, styles.getValue(IzinStatus.TERLAMBAT_TERKONFIRMASI).color)
        assertEquals(StatusColors.Merah, styles.getValue(IzinStatus.TERLAMBAT_KONFIRMASI_HABIS).color)
        assertEquals(StatusColors.Hijau, styles.getValue(IzinStatus.KEMBALI_TEPAT).color)
        assertEquals(StatusColors.Hijau, styles.getValue(IzinStatus.KEMBALI_TERLAMBAT).color)
    }
}

