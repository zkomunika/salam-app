package app.pantauizin.ui.izin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.IzinView
import app.pantauizin.domain.IzinFilter
import app.pantauizin.domain.IzinStatus
import app.pantauizin.domain.StatusResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class IzinListItemUi(
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

class IzinListViewModel(
    private val izinRepo: IzinRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _filter = MutableStateFlow(IzinFilter.SEMUA)
    val filter: StateFlow<IzinFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCounts = MutableStateFlow<Map<IzinFilter, Int>>(emptyMap())
    val filterCounts: StateFlow<Map<IzinFilter, Int>> = _filterCounts.asStateFlow()

    private val _items = MutableStateFlow<List<IzinListItemUi>>(emptyList())
    val items: StateFlow<List<IzinListItemUi>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            combine(izinRepo.observe(), _filter, _searchQuery) { list, currentFilter, query ->
                val today = todayProvider()
                val allItems = list.map { view ->
                    val snap = view.toSnapshot()
                    val status = StatusResolver.status(snap, today)
                    IzinListItemUi(
                        view = view,
                        status = status,
                        batasAktif = StatusResolver.batasAktif(snap),
                        hariTerlambat = StatusResolver.hariTerlambat(snap, today),
                        sudahKonfirmasi = view.konfirmasi.isNotEmpty()
                    )
                }
                val counts = IzinFilter.entries.associateWith { f ->
                    allItems.count { f.matches(it.status) }
                }
                val trimmed = query.trim()
                val filtered = allItems.filter { item ->
                    currentFilter.matches(item.status) && (
                        trimmed.isEmpty() ||
                            item.view.nama.contains(trimmed, ignoreCase = true) ||
                            item.view.kamar.contains(trimmed, ignoreCase = true) ||
                            item.view.alasanDetail.contains(trimmed, ignoreCase = true)
                        )
                }
                counts to filtered
            }.collect { (counts, filtered) ->
                _filterCounts.value = counts
                _items.value = filtered
            }
        }
    }

    fun pilihFilter(newFilter: IzinFilter) {
        _filter.value = newFilter
    }

    fun ubahSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun tandaiKembali(izinId: Long, tanggal: LocalDate) {
        izinRepo.tandaiKembali(izinId, tanggal)
    }

    suspend fun batalKembali(izinId: Long) {
        izinRepo.batalKembali(izinId)
    }
}
