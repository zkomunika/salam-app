package app.pantauizin.ui.terlambat

import app.pantauizin.data.IzinView
import app.pantauizin.data.KonfirmasiView
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TerlambatSortTest {
    private fun buatIzin(
        id: Long,
        nama: String,
        batasAwal: LocalDate,
        tanggalKembali: LocalDate? = null,
        konfirmasiTanggalBaru: List<LocalDate?> = emptyList()
    ): IzinView = IzinView(
        id = id,
        santriId = id,
        nama = nama,
        kamar = "1",
        tanggalPulang = batasAwal.minusDays(2),
        alasanJenis = AlasanJenis.SAKIT,
        alasanDetail = "Demam",
        batasJenis = BatasJenis.HARI_2,
        batasAwal = batasAwal,
        noWaWali = null,
        tanggalKembali = tanggalKembali,
        konfirmasi = konfirmasiTanggalBaru.mapIndexed { idx, tgl ->
            KonfirmasiView(
                id = idx.toLong() + 1L,
                tanggalKembaliBaru = tgl,
                catatan = null,
                buktiPath = null,
                dibuatPada = Instant.EPOCH
            )
        }
    )

    @Test
    fun sortTerlambatMenyaringYangBelumKembaliDanMengurutkanBelumKonfirmasiAtauHabisDiAtasTiebreakHariTerlambatTerbanyak() {
        val today = LocalDate.of(2026, 11, 10)

        // 1. Izin berjalan (batasAwal = 2026-11-10) -> tidak masuk
        val berjalan = buatIzin(1L, "BERJALAN", LocalDate.of(2026, 11, 10))
        // 2. Sudah kembali terlambat -> tidak masuk
        val kembaliTerlambat = buatIzin(2L, "KEMBALI", LocalDate.of(2026, 11, 5), tanggalKembali = LocalDate.of(2026, 11, 8))
        // 3. Terlambat terkonfirmasi, terlambat 6 hari (batasAwal Nov 4, konfirmasi Nov 12)
        val terkonfirmasi6Hari = buatIzin(3L, "TERKONFIRMASI_6", LocalDate.of(2026, 11, 4), konfirmasiTanggalBaru = listOf(LocalDate.of(2026, 11, 12)))
        // 4. Terlambat belum konfirmasi, terlambat 2 hari (batasAwal Nov 8)
        val belumKonfirmasi2Hari = buatIzin(4L, "BELUM_2", LocalDate.of(2026, 11, 8))
        // 5. Terlambat konfirmasi habis, terlambat 5 hari (batasAwal Nov 5, konfirmasi Nov 8)
        val konfirmasiHabis5Hari = buatIzin(5L, "HABIS_5", LocalDate.of(2026, 11, 5), konfirmasiTanggalBaru = listOf(LocalDate.of(2026, 11, 8)))
        // 6. Terlambat terkonfirmasi, terlambat 3 hari (batasAwal Nov 7, konfirmasi Nov 11)
        val terkonfirmasi3Hari = buatIzin(6L, "TERKONFIRMASI_3", LocalDate.of(2026, 11, 7), konfirmasiTanggalBaru = listOf(LocalDate.of(2026, 11, 11)))

        val hasil = sortTerlambat(
            listOf(berjalan, kembaliTerlambat, terkonfirmasi3Hari, belumKonfirmasi2Hari, terkonfirmasi6Hari, konfirmasiHabis5Hari),
            today
        )

        assertEquals(
            listOf(5L, 4L, 3L, 6L),
            hasil.map { it.id }
        )
    }
}

