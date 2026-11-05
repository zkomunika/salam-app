package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MessageTemplateBuilderTest {
    @Test fun formatTanggalIndonesia() {
        assertEquals("5 Januari 2026", MessageTemplateBuilder.formatTanggal(LocalDate.of(2026, 1, 5)))
        assertEquals("4 November 2026", MessageTemplateBuilder.formatTanggal(LocalDate.of(2026, 11, 4)))
    }

    @Test fun varianAMengisiSemuaPenampung() {
        val teks = MessageTemplateBuilder.build(
            DefaultTemplates.A, "ZAKY", "11", LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4)
        )
        assertTrue(teks.contains("ZAKY (kamar 11)"))
        assertTrue(teks.contains("4 November 2026"))
        assertFalse(teks.contains("{"))
    }

    @Test fun varianBMemuatBatasAktif() {
        val teks = MessageTemplateBuilder.build(
            DefaultTemplates.B, "ZAKY", "11", LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 8)
        )
        assertTrue(teks.contains("paling lambat 8 November 2026"))
        assertFalse(teks.contains("{"))
    }

    @Test fun pemilihanVarian() {
        assertEquals(TemplateVariant.A, MessageTemplateBuilder.variantFor(IzinStatus.TERLAMBAT_BELUM_KONFIRMASI))
        assertEquals(TemplateVariant.B, MessageTemplateBuilder.variantFor(IzinStatus.TERLAMBAT_TERKONFIRMASI))
        assertEquals(TemplateVariant.B, MessageTemplateBuilder.variantFor(IzinStatus.TERLAMBAT_KONFIRMASI_HABIS))
    }

    @Test fun statusLainMelempar() {
        assertThrows(IllegalArgumentException::class.java) {
            MessageTemplateBuilder.variantFor(IzinStatus.KEMBALI_TEPAT)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MessageTemplateBuilder.variantFor(IzinStatus.IZIN_BERJALAN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MessageTemplateBuilder.variantFor(IzinStatus.KEMBALI_TERLAMBAT)
        }
    }
}

