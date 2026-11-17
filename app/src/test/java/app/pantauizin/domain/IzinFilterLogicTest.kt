package app.pantauizin.domain

import app.pantauizin.data.IzinView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class IzinFilterLogicTest {

    private fun buatItem(
        id: Long,
        nama: String,
        kamar: String,
        status: IzinStatus = IzinStatus.IZIN_BERJALAN,
        alasan: String = "Sakit"
    ): IzinListItem {
        val view = IzinView(
            id = id,
            santriId = id,
            nama = nama,
            kamar = kamar,
            tanggalPulang = LocalDate.of(2026, 11, 10),
            alasanJenis = AlasanJenis.SAKIT,
            alasanDetail = alasan,
            batasJenis = BatasJenis.HARI_3,
            batasAwal = LocalDate.of(2026, 11, 12),
            noWaWali = null,
            tanggalKembali = if (status == IzinStatus.KEMBALI_TEPAT) LocalDate.of(2026, 11, 12) else null,
            konfirmasi = emptyList()
        )
        return IzinListItem(
            view = view,
            status = status,
            batasAktif = view.batasAwal,
            hariTerlambat = if (status == IzinStatus.TERLAMBAT_BELUM_KONFIRMASI) 2 else 0,
            sudahKonfirmasi = false
        )
    }

    @Test
    fun filterMenyaringBerdasarkanKamar() {
        val items = listOf(
            buatItem(1L, "AHMAD", "10"),
            buatItem(2L, "BUDI", "12"),
            buatItem(3L, "CITRA", "10")
        )

        // Filter kamar 10
        val hasil10 = IzinFilterLogic.filter(items, IzinFilter.SEMUA, query = "", kamar = "10")
        assertEquals(listOf(1L, 3L), hasil10.map { it.view.id })

        // Filter kamar 12
        val hasil12 = IzinFilterLogic.filter(items, IzinFilter.SEMUA, query = "", kamar = "12")
        assertEquals(listOf(2L), hasil12.map { it.view.id })

        // Filter semua kamar (null)
        val hasilSemua = IzinFilterLogic.filter(items, IzinFilter.SEMUA, query = "", kamar = null)
        assertEquals(3, hasilSemua.size)
    }

    @Test
    fun filterMengkombinasikanStatusFilterKamarDanPencarian() {
        val items = listOf(
            buatItem(1L, "AHMAD FAUZI", "10", status = IzinStatus.IZIN_BERJALAN),
            buatItem(2L, "AHMAD DANI", "10", status = IzinStatus.TERLAMBAT_BELUM_KONFIRMASI),
            buatItem(3L, "AHMAD KURNIA", "12", status = IzinStatus.IZIN_BERJALAN),
            buatItem(4L, "BUDI SANTOSO", "10", status = IzinStatus.IZIN_BERJALAN)
        )

        // Status IZIN_BERJALAN + Kamar 10 + Query "ahmad" -> Hanya Ahmad Fauzi
        val hasil = IzinFilterLogic.filter(
            items = items,
            filter = IzinFilter.BERJALAN,
            query = "ahmad",
            kamar = "10"
        )
        assertEquals(listOf(1L), hasil.map { it.view.id })
    }

    @Test
    fun kamarOptionsMenyortirNumerikDanAbjad() {
        val santri = listOf(
            SantriItem(1L, "S1", "10", true),
            SantriItem(2L, "S2", "2", true),
            SantriItem(3L, "S3", "1", true),
            SantriItem(4L, "S4", "B", true),
            SantriItem(5L, "S5", "A", true)
        )

        val opsi = IzinFilterLogic.kamarOptions(allSantri = santri, allIzin = emptyList())
        assertEquals(listOf("1", "2", "10", "A", "B"), opsi)
    }

    @Test
    fun countsMenghitungJumlahSesuaiFilterKamar() {
        val items = listOf(
            buatItem(1L, "A", "10", status = IzinStatus.IZIN_BERJALAN),
            buatItem(2L, "B", "10", status = IzinStatus.TERLAMBAT_BELUM_KONFIRMASI),
            buatItem(3L, "C", "12", status = IzinStatus.IZIN_BERJALAN)
        )

        // Hitung semua kamar
        val countsSemua = IzinFilterLogic.counts(items, kamar = null)
        assertEquals(3, countsSemua[IzinFilter.SEMUA])
        assertEquals(2, countsSemua[IzinFilter.BERJALAN])
        assertEquals(1, countsSemua[IzinFilter.TERLAMBAT])

        // Hitung kamar 10 saja
        val countsKamar10 = IzinFilterLogic.counts(items, kamar = "10")
        assertEquals(2, countsKamar10[IzinFilter.SEMUA])
        assertEquals(1, countsKamar10[IzinFilter.BERJALAN])
        assertEquals(1, countsKamar10[IzinFilter.TERLAMBAT])

        // Hitung kamar 12 saja
        val countsKamar12 = IzinFilterLogic.counts(items, kamar = "12")
        assertEquals(1, countsKamar12[IzinFilter.SEMUA])
        assertEquals(1, countsKamar12[IzinFilter.BERJALAN])
        assertEquals(0, countsKamar12[IzinFilter.TERLAMBAT])
    }
}
