package app.pantauizin.domain

import app.pantauizin.data.IzinView
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RekapFilter {
    SEMUA,
    SUDAH_PULANG_2_BULAN,
    BELUM_PULANG_2_BULAN
}

data class RiwayatPulangTerakhir(
    val izinId: Long,
    val tanggalPulang: LocalDate,
    val alasanJenis: AlasanJenis,
    val alasanDetail: String,
    val pemberiIzin: String?,
    val bolehPulangLagiMulai: LocalDate,
    val hariSejakPulang: Int,
    val sisaHariKe2Bulan: Int,
    val jumlahPulang2Bulan: Int
)

data class SantriRekapItem(
    val santri: SantriItem,
    val sudahPulangDalam2Bulan: Boolean,
    val riwayatTerakhir: RiwayatPulangTerakhir?,
    val totalPulangSepanjangWaktu: Int,
    val jumlahPulang2Bulan: Int
)

object RekapPulangLogic {

    /**
     * Mengembalikan true jika [izin] berada di dalam jendela 2 bulan terakhir
     * terhadap [referenceDate] (semua alasan dihitung).
     */
    fun isDalam2Bulan(tanggalPulang: LocalDate, referenceDate: LocalDate): Boolean {
        return referenceDate.isBefore(tanggalPulang.plusMonths(2))
    }

    /**
     * Mengecek apakah santri pernah pulang dalam 2 bulan terakhir terhadap [referenceDate].
     * Mengembalikan null jika aman (belum pernah pulang dalam 2 bulan terakhir).
     */
    fun cekPulangDalam2Bulan(
        allIzin: List<IzinView>,
        santriId: Long,
        referenceDate: LocalDate,
        excludeIzinId: Long? = null
    ): RiwayatPulangTerakhir? {
        val recentList = allIzin
            .asSequence()
            .filter { it.santriId == santriId && it.id != excludeIzinId }
            .filter { isDalam2Bulan(it.tanggalPulang, referenceDate) }
            .sortedWith(compareByDescending<IzinView> { it.tanggalPulang }.thenByDescending { it.id })
            .toList()

        val latest = recentList.firstOrNull() ?: return null
        return toRiwayat(latest, referenceDate, jumlahPulang2Bulan = recentList.size)
    }

    /**
     * Kumpulan ID santri yang memiliki izin pulang dalam 2 bulan terakhir terhadap [referenceDate].
     */
    fun santriIdsPulangDalam2Bulan(
        allIzin: List<IzinView>,
        referenceDate: LocalDate,
        excludeIzinId: Long? = null
    ): Set<Long> {
        return allIzin
            .asSequence()
            .filter { it.id != excludeIzinId }
            .filter { isDalam2Bulan(it.tanggalPulang, referenceDate) }
            .map { it.santriId }
            .toSet()
    }

    /**
     * Membangun daftar rekap seluruh santri aktif beserta status kepulangan 2 bulan terakhirnya.
     * Santri yang sudah pulang dalam 2 bulan terakhir diurutkan paling atas.
     */
    fun buildRekap(
        allSantri: List<SantriItem>,
        allIzin: List<IzinView>,
        today: LocalDate
    ): List<SantriRekapItem> {
        val izinBySantri = allIzin.groupBy { it.santriId }

        return allSantri
            .asSequence()
            .filter { it.aktif }
            .map { santri ->
                val santriIzins = (izinBySantri[santri.id] ?: emptyList())
                    .sortedWith(compareByDescending<IzinView> { it.tanggalPulang }.thenByDescending { it.id })
                val recentList = santriIzins.filter { isDalam2Bulan(it.tanggalPulang, today) }
                val latest = santriIzins.firstOrNull()
                val riwayat = latest?.let {
                    toRiwayat(it, today, jumlahPulang2Bulan = recentList.size)
                }
                SantriRekapItem(
                    santri = santri,
                    sudahPulangDalam2Bulan = recentList.isNotEmpty(),
                    riwayatTerakhir = riwayat,
                    totalPulangSepanjangWaktu = santriIzins.size,
                    jumlahPulang2Bulan = recentList.size
                )
            }
            .sortedWith(
                compareByDescending<SantriRekapItem> { it.sudahPulangDalam2Bulan }
                    .thenByDescending { item ->
                        if (item.sudahPulangDalam2Bulan) item.riwayatTerakhir?.tanggalPulang else null
                    }
                    .thenBy { it.santri.nama.lowercase() }
            )
            .toList()
    }

    fun filter(
        items: List<SantriRekapItem>,
        filter: RekapFilter,
        query: String,
        kamar: String?
    ): List<SantriRekapItem> {
        val trimmed = query.trim()
        return items.filter { item ->
            val matchesFilter = when (filter) {
                RekapFilter.SEMUA -> true
                RekapFilter.SUDAH_PULANG_2_BULAN -> item.sudahPulangDalam2Bulan
                RekapFilter.BELUM_PULANG_2_BULAN -> !item.sudahPulangDalam2Bulan
            }
            val matchesKamar = kamar == null || item.santri.kamar == kamar
            val matchesQuery = trimmed.isEmpty() ||
                item.santri.nama.contains(trimmed, ignoreCase = true) ||
                item.santri.kamar.contains(trimmed, ignoreCase = true) ||
                item.riwayatTerakhir?.alasanDetail?.contains(trimmed, ignoreCase = true) == true
            matchesFilter && matchesKamar && matchesQuery
        }
    }

    fun counts(items: List<SantriRekapItem>, kamar: String?): Map<RekapFilter, Int> {
        val base = if (kamar == null) items else items.filter { it.santri.kamar == kamar }
        return mapOf(
            RekapFilter.SEMUA to base.size,
            RekapFilter.SUDAH_PULANG_2_BULAN to base.count { it.sudahPulangDalam2Bulan },
            RekapFilter.BELUM_PULANG_2_BULAN to base.count { !it.sudahPulangDalam2Bulan }
        )
    }

    fun formatSelisihHari(hari: Int): String = when {
        hari <= 0 -> "hari ini"
        hari < 14 -> "$hari hari yang lalu"
        else -> {
            val minggu = hari / 7
            if (hari % 7 == 0) "$minggu minggu yang lalu" else "$minggu minggu yang lalu ($hari hari)"
        }
    }

    private fun toRiwayat(
        izin: IzinView,
        referenceDate: LocalDate,
        jumlahPulang2Bulan: Int
    ): RiwayatPulangTerakhir {
        val bolehPulangLagiMulai = izin.tanggalPulang.plusMonths(2)
        val hariSejakPulang = ChronoUnit.DAYS.between(izin.tanggalPulang, referenceDate).toInt().coerceAtLeast(0)
        val sisaHariKe2Bulan = ChronoUnit.DAYS.between(referenceDate, bolehPulangLagiMulai).toInt().coerceAtLeast(0)
        return RiwayatPulangTerakhir(
            izinId = izin.id,
            tanggalPulang = izin.tanggalPulang,
            alasanJenis = izin.alasanJenis,
            alasanDetail = izin.alasanDetail,
            pemberiIzin = izin.pemberiIzin,
            bolehPulangLagiMulai = bolehPulangLagiMulai,
            hariSejakPulang = hariSejakPulang,
            sisaHariKe2Bulan = sisaHariKe2Bulan,
            jumlahPulang2Bulan = jumlahPulang2Bulan
        )
    }
}
