package app.pantauizin.domain

import app.pantauizin.data.IzinView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RekapPulangLogicTest {

    private fun buatIzin(
        id: Long,
        santriId: Long,
        nama: String = "SANTRI $santriId",
        kamar: String = "10",
        tanggalPulang: LocalDate,
        alasanJenis: AlasanJenis = AlasanJenis.SAKIT,
        alasanDetail: String = "Demam",
        pemberiIzin: String? = "MIFTAHUDIN"
    ) = IzinView(
        id = id,
        santriId = santriId,
        nama = nama,
        kamar = kamar,
        tanggalPulang = tanggalPulang,
        alasanJenis = alasanJenis,
        alasanDetail = alasanDetail,
        batasJenis = BatasJenis.HARI_2,
        batasAwal = tanggalPulang.plusDays(1),
        noWaWali = null,
        tanggalKembali = tanggalPulang.plusDays(1),
        konfirmasi = emptyList(),
        pemberiIzin = pemberiIzin
    )

    @Test
    fun cekPulangDalam2BulanMenghitungSemuaAlasanDanMengembalikanDetail() {
        val today = LocalDate.of(2026, 11, 19)
        val duaMingguLalu = LocalDate.of(2026, 11, 5)
        val izins = listOf(
            buatIzin(
                id = 1L,
                santriId = 10L,
                tanggalPulang = duaMingguLalu,
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam berdarah",
                pemberiIzin = "M THORIQ MUSTHAFA"
            ),
            buatIzin(
                id = 2L,
                santriId = 20L,
                tanggalPulang = duaMingguLalu,
                alasanJenis = AlasanJenis.LAIN_LAIN,
                alasanDetail = "Nikahan kakak",
                pemberiIzin = "MAULANA HASYIM"
            )
        )

        val wSakit = RekapPulangLogic.cekPulangDalam2Bulan(izins, santriId = 10L, referenceDate = today)
        assertNotNull(wSakit)
        assertEquals(duaMingguLalu, wSakit!!.tanggalPulang)
        assertEquals(AlasanJenis.SAKIT, wSakit.alasanJenis)
        assertEquals("Demam berdarah", wSakit.alasanDetail)
        assertEquals("M THORIQ MUSTHAFA", wSakit.pemberiIzin)
        assertEquals(LocalDate.of(2027, 1, 5), wSakit.bolehPulangLagiMulai)
        assertEquals(14, wSakit.hariSejakPulang)
        assertEquals(47, wSakit.sisaHariKe2Bulan)
        assertEquals(1, wSakit.jumlahPulang2Bulan)

        val wLain = RekapPulangLogic.cekPulangDalam2Bulan(izins, santriId = 20L, referenceDate = today)
        assertNotNull(wLain)
        assertEquals(AlasanJenis.LAIN_LAIN, wLain!!.alasanJenis)
        assertEquals("Nikahan kakak", wLain.alasanDetail)
    }

    @Test
    fun batasTepat2BulanKalenderSudahAman() {
        val pulang = LocalDate.of(2026, 11, 5)
        val izins = listOf(buatIzin(id = 1L, santriId = 10L, tanggalPulang = pulang))

        // Sehari sebelum genap 2 bulan masih ditandai
        assertNotNull(
            RekapPulangLogic.cekPulangDalam2Bulan(
                izins,
                santriId = 10L,
                referenceDate = LocalDate.of(2027, 1, 4)
            )
        )
        // Tepat genap 2 bulan (5 Jan 2027) sudah aman
        assertNull(
            RekapPulangLogic.cekPulangDalam2Bulan(
                izins,
                santriId = 10L,
                referenceDate = LocalDate.of(2027, 1, 5)
            )
        )
    }

    @Test
    fun beberapaIzinDalam2BulanMemilihTerbaruDanExcludeSaatEdit() {
        val today = LocalDate.of(2026, 11, 19)
        val izins = listOf(
            buatIzin(id = 1L, santriId = 10L, tanggalPulang = LocalDate.of(2026, 10, 10), alasanDetail = "Sakit lama"),
            buatIzin(id = 2L, santriId = 10L, tanggalPulang = LocalDate.of(2026, 11, 10), alasanDetail = "Acara baru")
        )

        val latest = RekapPulangLogic.cekPulangDalam2Bulan(izins, 10L, today)!!
        assertEquals(2L, latest.izinId)
        assertEquals("Acara baru", latest.alasanDetail)
        assertEquals(2, latest.jumlahPulang2Bulan)

        // Saat mengedit izin id 2, hanya izin id 1 yang dihitung
        val excluded = RekapPulangLogic.cekPulangDalam2Bulan(izins, 10L, today, excludeIzinId = 2L)!!
        assertEquals(1L, excluded.izinId)
        assertEquals("Sakit lama", excluded.alasanDetail)
        assertEquals(1, excluded.jumlahPulang2Bulan)
    }

    @Test
    fun buildRekapFilterKamarDanSortirBerfungsiBenar() {
        val today = LocalDate.of(2026, 11, 19)
        val santri = listOf(
            SantriItem(1L, "AHMAD", "10", true),
            SantriItem(2L, "BUDI", "10", true),
            SantriItem(3L, "CITRA", "12", true),
            SantriItem(4L, "NONAKTIF", "10", false)
        )
        val izins = listOf(
            buatIzin(id = 101L, santriId = 2L, nama = "BUDI", kamar = "10", tanggalPulang = LocalDate.of(2026, 11, 10)),
            buatIzin(id = 102L, santriId = 3L, nama = "CITRA", kamar = "12", tanggalPulang = LocalDate.of(2026, 8, 1))
        )

        val rekap = RekapPulangLogic.buildRekap(santri, izins, today)
        // Nonaktif tidak masuk; BUDI (pulang < 2 bln) di urutan pertama; AHMAD & CITRA aman
        assertEquals(listOf("BUDI", "AHMAD", "CITRA"), rekap.map { it.santri.nama })
        assertTrue(rekap[0].sudahPulangDalam2Bulan)
        assertFalse(rekap[1].sudahPulangDalam2Bulan)
        assertNull(rekap[1].riwayatTerakhir)
        assertFalse(rekap[2].sudahPulangDalam2Bulan)
        assertNotNull(rekap[2].riwayatTerakhir)

        val countsKamar10 = RekapPulangLogic.counts(rekap, kamar = "10")
        assertEquals(2, countsKamar10[RekapFilter.SEMUA])
        assertEquals(1, countsKamar10[RekapFilter.SUDAH_PULANG_2_BULAN])
        assertEquals(1, countsKamar10[RekapFilter.BELUM_PULANG_2_BULAN])

        val filtered = RekapPulangLogic.filter(rekap, RekapFilter.SUDAH_PULANG_2_BULAN, query = "", kamar = "10")
        assertEquals(listOf("BUDI"), filtered.map { it.santri.nama })
    }
}
