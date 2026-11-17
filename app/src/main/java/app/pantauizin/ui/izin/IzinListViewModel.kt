package app.pantauizin.ui.izin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.SantriRepository
import app.pantauizin.domain.IzinFilter
import app.pantauizin.domain.IzinFilterLogic
import app.pantauizin.domain.IzinListItem
import app.pantauizin.domain.SantriItem
import app.pantauizin.domain.StatusResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate

typealias IzinListItemUi = IzinListItem

class IzinListViewModel(
    private val izinRepo: IzinRepository,
    private val santriRepo: SantriRepository? = null,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _filter = MutableStateFlow(IzinFilter.SEMUA)
    val filter: StateFlow<IzinFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _kamarFilter = MutableStateFlow<String?>(null)
    val kamarFilter: StateFlow<String?> = _kamarFilter.asStateFlow()

    private val _kamarOptions = MutableStateFlow<List<String>>(emptyList())
    val kamarOptions: StateFlow<List<String>> = _kamarOptions.asStateFlow()

    private val _filterCounts = MutableStateFlow<Map<IzinFilter, Int>>(emptyMap())
    val filterCounts: StateFlow<Map<IzinFilter, Int>> = _filterCounts.asStateFlow()

    private val _items = MutableStateFlow<List<IzinListItemUi>>(emptyList())
    val items: StateFlow<List<IzinListItemUi>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            val santriFlow: Flow<List<SantriItem>> = santriRepo?.observe() ?: flowOf(emptyList())

            combine(izinRepo.observe(), santriFlow, _filter, _searchQuery, _kamarFilter) { list, allSantri, currentFilter, query, kamarFilter ->
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

                val kamarOptions = IzinFilterLogic.kamarOptions(allSantri, list)
                val counts = IzinFilterLogic.counts(allItems, kamarFilter)
                val filtered = IzinFilterLogic.filter(allItems, currentFilter, query, kamarFilter)

                FilteredResult(kamarOptions, counts, filtered)
            }.collect { result ->
                _kamarOptions.value = result.kamarOptions
                _filterCounts.value = result.counts
                _items.value = result.items
            }
        }
    }

    private data class FilteredResult(
        val kamarOptions: List<String>,
        val counts: Map<IzinFilter, Int>,
        val items: List<IzinListItemUi>
    )

    fun pilihFilter(newFilter: IzinFilter) {
        _filter.value = newFilter
    }

    fun ubahSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun pilihKamarFilter(kamar: String?) {
        _kamarFilter.value = kamar
    }

    suspend fun tandaiKembali(izinId: Long, tanggal: LocalDate) {
        izinRepo.tandaiKembali(izinId, tanggal)
    }

    suspend fun batalKembali(izinId: Long) {
        izinRepo.batalKembali(izinId)
    }
}
