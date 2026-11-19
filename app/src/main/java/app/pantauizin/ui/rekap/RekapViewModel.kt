package app.pantauizin.ui.rekap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.SantriRepository
import app.pantauizin.domain.RekapFilter
import app.pantauizin.domain.RekapPulangLogic
import app.pantauizin.domain.SantriRekapItem
import app.pantauizin.domain.SantriSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class RekapViewModel(
    private val santriRepo: SantriRepository,
    private val izinRepo: IzinRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _filter = MutableStateFlow(RekapFilter.SEMUA)
    val filter: StateFlow<RekapFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _kamarFilter = MutableStateFlow<String?>(null)
    val kamarFilter: StateFlow<String?> = _kamarFilter.asStateFlow()

    private val _kamarOptions = MutableStateFlow<List<String>>(emptyList())
    val kamarOptions: StateFlow<List<String>> = _kamarOptions.asStateFlow()

    private val _filterCounts = MutableStateFlow<Map<RekapFilter, Int>>(emptyMap())
    val filterCounts: StateFlow<Map<RekapFilter, Int>> = _filterCounts.asStateFlow()

    private val _items = MutableStateFlow<List<SantriRekapItem>>(emptyList())
    val items: StateFlow<List<SantriRekapItem>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                santriRepo.observe(),
                izinRepo.observe(),
                _filter,
                _searchQuery,
                _kamarFilter
            ) { allSantri, allIzin, currentFilter, query, kamar ->
                val today = todayProvider()
                val allRekap = RekapPulangLogic.buildRekap(allSantri, allIzin, today)
                val kamarOpts = SantriSearch.kamarOptions(allSantri)
                val counts = RekapPulangLogic.counts(allRekap, kamar)
                val filtered = RekapPulangLogic.filter(allRekap, currentFilter, query, kamar)
                RekapUiResult(kamarOpts, counts, filtered)
            }.collect { result ->
                _kamarOptions.value = result.kamarOptions
                _filterCounts.value = result.counts
                _items.value = result.items
            }
        }
    }

    private data class RekapUiResult(
        val kamarOptions: List<String>,
        val counts: Map<RekapFilter, Int>,
        val items: List<SantriRekapItem>
    )

    fun pilihFilter(newFilter: RekapFilter) {
        _filter.value = newFilter
    }

    fun ubahSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun pilihKamarFilter(kamar: String?) {
        _kamarFilter.value = kamar
    }
}
