package app.pantauizin.domain

import app.pantauizin.data.IzinView
import java.time.LocalDate

data class IzinListItem(
    val view: IzinView,
    val status: IzinStatus,
    val batasAktif: LocalDate,
    val hariTerlambat: Int,
    val sudahKonfirmasi: Boolean
) {
    val tampilKotakKonfirmasi: Boolean
        get() = status == IzinStatus.TERLAMBAT_BELUM_KONFIRMASI ||
            status == IzinStatus.TERLAMBAT_TERKONFIRMASI ||
            status == IzinStatus.TERLAMBAT_KONFIRMASI_HABIS ||
            status == IzinStatus.KEMBALI_TERLAMBAT
}

object IzinFilterLogic {
    fun filter(
        items: List<IzinListItem>,
        filter: IzinFilter,
        query: String,
        kamar: String? = null
    ): List<IzinListItem> {
        val trimmed = query.trim()
        return items.filter { item ->
            val matchesFilter = filter.matches(item.status)
            val matchesKamar = kamar == null || item.view.kamar == kamar
            val matchesQuery = trimmed.isEmpty() ||
                item.view.nama.contains(trimmed, ignoreCase = true) ||
                item.view.kamar.contains(trimmed, ignoreCase = true) ||
                item.view.alasanDetail.contains(trimmed, ignoreCase = true) ||
                item.view.pemberiIzin?.contains(trimmed, ignoreCase = true) == true
            matchesFilter && matchesKamar && matchesQuery
        }
    }

    fun kamarOptions(allSantri: List<SantriItem>, allIzin: List<IzinView>): List<String> {
        return if (allSantri.isNotEmpty()) {
            SantriSearch.kamarOptions(allSantri)
        } else {
            val unik = allIzin.map { it.kamar }.distinct()
            val (angka, teks) = unik.partition { it.toLongOrNull() != null }
            angka.sortedBy { it.toLong() } + teks.sortedBy { it.lowercase() }
        }
    }

    fun counts(items: List<IzinListItem>, kamar: String? = null): Map<IzinFilter, Int> {
        val base = if (kamar == null) items else items.filter { it.view.kamar == kamar }
        return IzinFilter.entries.associateWith { f ->
            base.count { f.matches(it.status) }
        }
    }
}
