package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IzinFormValidatorTest {
    private val validBase = IzinFormState(
        santriId = 1L,
        tanggalPulang = LocalDate.of(2026, 11, 1),
        alasanJenis = AlasanJenis.SAKIT,
        alasanDetail = "Demam",
        batasJenis = BatasJenis.HARI_3,
        tanggalBatasPilihan = null,
        noWa = ""
    )

    @Test
    fun stateValidMenghasilkanMapKosong() {
        assertTrue(IzinFormValidator.validate(validBase).isEmpty())
    }

    @Test
    fun menolakSantriNullDanAlasanDetailKosong() {
        val errors = IzinFormValidator.validate(
            validBase.copy(santriId = null, alasanDetail = "   ")
        )
        assertTrue(errors.containsKey(FormField.SANTRI))
        assertTrue(errors.containsKey(FormField.ALASAN_DETAIL))
    }

    @Test
    fun menolakLebihTigaHariTanpaTanggalAtauTidakSetelahHariKetiga() {
        val e1 = IzinFormValidator.validate(
            validBase.copy(batasJenis = BatasJenis.LEBIH_3_HARI, tanggalBatasPilihan = null)
        )
        assertTrue(e1.containsKey(FormField.BATAS_TANGGAL))

        val e2 = IzinFormValidator.validate(
            validBase.copy(
                batasJenis = BatasJenis.LEBIH_3_HARI,
                tanggalBatasPilihan = LocalDate.of(2026, 11, 3)
            )
        )
        assertTrue(e2.containsKey(FormField.BATAS_TANGGAL))

        val e3 = IzinFormValidator.validate(
            validBase.copy(
                batasJenis = BatasJenis.LEBIH_3_HARI,
                tanggalBatasPilihan = LocalDate.of(2026, 11, 4)
            )
        )
        assertTrue(e3.isEmpty())
    }

    @Test
    fun menolakNomorWaTidakValidDanMenerimaKosong() {
        val e1 = IzinFormValidator.validate(validBase.copy(noWa = "abc"))
        assertTrue(e1.containsKey(FormField.NO_WA))

        val e2 = IzinFormValidator.validate(validBase.copy(noWa = ""))
        assertTrue(e2.isEmpty())

        val e3 = IzinFormValidator.validate(validBase.copy(noWa = "0812-3456-7890"))
        assertTrue(e3.isEmpty())
    }

    @Test
    fun batasPreviewerMenghitungBatasDanHariLewat() {
        val s = validBase.copy(
            tanggalPulang = LocalDate.of(2026, 10, 28),
            batasJenis = BatasJenis.HARI_3
        )
        val preview = BatasPreviewer.of(s, LocalDate.of(2026, 11, 5))
        assertEquals(BatasPreview(LocalDate.of(2026, 10, 30), 6), preview)

        val belumLewat = BatasPreviewer.of(validBase, LocalDate.of(2026, 11, 3))
        assertEquals(BatasPreview(LocalDate.of(2026, 11, 3), 0), belumLewat)

        val belumCukup = BatasPreviewer.of(
            validBase.copy(batasJenis = BatasJenis.LEBIH_3_HARI, tanggalBatasPilihan = null),
            LocalDate.of(2026, 11, 5)
        )
        assertNull(belumCukup)
    }
}

