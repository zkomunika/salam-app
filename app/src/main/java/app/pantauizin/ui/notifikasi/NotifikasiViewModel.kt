package app.pantauizin.ui.notifikasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.NotifikasiRepository
import app.pantauizin.data.NotifikasiView
import app.pantauizin.domain.NotifRange
import app.pantauizin.domain.toInstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NotifikasiViewModel(
    private val notifRepo: NotifikasiRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() }
) : ViewModel() {

    private val _range = MutableStateFlow<NotifRange>(NotifRange.TujuhHari)
    val range: StateFlow<NotifRange> = _range.asStateFlow()

    private val _items = MutableStateFlow<List<NotifikasiView>>(emptyList())
    val items: StateFlow<List<NotifikasiView>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            _range.flatMapLatest { r ->
                val (from, to) = r.toInstants(todayProvider(), zoneProvider())
                notifRepo.observeBetween(from, to)
            }.collect { list ->
                _items.value = list
            }
        }
    }

    fun pilihRange(newRange: NotifRange) {
        _range.value = newRange
    }

    suspend fun tandaiDibaca(id: Long) {
        notifRepo.markRead(id)
    }
}

