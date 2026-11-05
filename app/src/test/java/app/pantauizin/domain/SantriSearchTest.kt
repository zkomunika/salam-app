package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SantriSearchTest {
    private val semua = listOf(
        SantriItem(1, "DRIYA NUR 'AZMI", "12", true),
        SantriItem(2, "RAFI AHMAD", "10", true),
        SantriItem(3, "BRAM SAPUTRA", "2", true),
        SantriItem(4, "RAHMAT NONAKTIF", "10", false),
        SantriItem(5, "ARA BUDI", "1", true),
        SantriItem(6, "SARA CAHYA", "11", true)
    )

    @Test fun cocokSebagianTanpaBedaHuruf() {
        assertEquals(listOf(1L), SantriSearch.filter(semua, "azmi").map { it.id })
    }

    @Test fun queryDirapikanSpasiTepiDanGanda() {
        assertEquals(listOf(1L), SantriSearch.filter(semua, "  driya   nur ").map { it.id })
    }

    @Test fun nonaktifTidakMuncul() {
        val hasil = SantriSearch.filter(semua, "rahmat")
        assertTrue(hasil.isEmpty())
    }

    @Test fun tidakAdaHasil() {
        assertTrue(SantriSearch.filter(semua, "zzz").isEmpty())
    }

    @Test fun queryKosongSemuaAktifTerurutNama() {
        val hasil = SantriSearch.filter(semua, "")
        assertEquals(
            listOf("ARA BUDI", "BRAM SAPUTRA", "DRIYA NUR 'AZMI", "RAFI AHMAD", "SARA CAHYA"),
            hasil.map { it.nama }
        )
    }

    @Test fun filterKamar() {
        val hasil = SantriSearch.filter(semua, "ra", kamar = "10")
        assertEquals(listOf("RAFI AHMAD"), hasil.map { it.nama })
        assertTrue(hasil.all { it.kamar == "10" })
    }

    @Test fun kamarOptionsTerurutNumerik() {
        val items = listOf("10", "2", "1", "11").mapIndexed { i, k -> SantriItem(i + 1L, "N$i", k, true) }
        assertEquals(listOf("1", "2", "10", "11"), SantriSearch.kamarOptions(items))
    }

    @Test fun kamarOptionsUnikDanHanyaAktif() {
        val hasil = SantriSearch.kamarOptions(semua)
        assertEquals(listOf("1", "2", "10", "11", "12"), hasil)
    }

    @Test fun kamarOptionsNonAngkaSetelahAngka() {
        val items = listOf("B", "3", "A", "1").mapIndexed { i, k -> SantriItem(i + 1L, "N$i", k, true) }
        assertEquals(listOf("1", "3", "A", "B"), SantriSearch.kamarOptions(items))
    }
}

