package app.pantauizin.ui.terlambat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.IzinView
import app.pantauizin.data.SettingsStore
import app.pantauizin.domain.DefaultTemplates
import app.pantauizin.domain.IzinStatus
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.domain.SendAction
import app.pantauizin.domain.SendActionPlanner
import app.pantauizin.domain.StatusResolver
import app.pantauizin.domain.TemplateVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

fun sortTerlambat(items: List<IzinView>, today: LocalDate): List<IzinView> {
    return items
        .map { view ->
            val snap = view.toSnapshot()
            val status = StatusResolver.status(snap, today)
            val hari = StatusResolver.hariTerlambat(snap, today)
            Triple(view, status, hari)
        }
        .filter { (_, status, _) ->
            status == IzinStatus.TERLAMBAT_BELUM_KONFIRMASI ||
                status == IzinStatus.TERLAMBAT_KONFIRMASI_HABIS ||
                status == IzinStatus.TERLAMBAT_TERKONFIRMASI
        }
        .sortedWith(
            compareBy<Triple<IzinView, IzinStatus, Int>> { (_, status, _) ->
                when (status) {
                    IzinStatus.TERLAMBAT_BELUM_KONFIRMASI,
                    IzinStatus.TERLAMBAT_KONFIRMASI_HABIS -> 0
                    else -> 1
                }
            }.thenByDescending { (_, _, hari) -> hari }
                .thenBy { (view, _, _) -> view.id }
        )
        .map { (view, _, _) -> view }
}

data class TerlambatItemUi(
    val view: IzinView,
    val status: IzinStatus,
    val batasAktif: LocalDate,
    val hariTerlambat: Int
)

class TerlambatViewModel(
    private val izinRepo: IzinRepository,
    private val settingsStore: SettingsStore,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _items = MutableStateFlow<List<TerlambatItemUi>>(emptyList())
    val items: StateFlow<List<TerlambatItemUi>> = _items.asStateFlow()

    private var templateA: String = DefaultTemplates.A
    private var templateB: String = DefaultTemplates.B

    init {
        viewModelScope.launch {
            combine(
                izinRepo.observe(),
                settingsStore.templateA,
                settingsStore.templateB
            ) { list, tplA, tplB ->
                templateA = tplA
                templateB = tplB
                val today = todayProvider()
                sortTerlambat(list, today).map { view ->
                    val snap = view.toSnapshot()
                    TerlambatItemUi(
                        view = view,
                        status = StatusResolver.status(snap, today),
                        batasAktif = StatusResolver.batasAktif(snap),
                        hariTerlambat = StatusResolver.hariTerlambat(snap, today)
                    )
                }
            }.collect { sorted ->
                _items.value = sorted
            }
        }
    }

    fun buildSendAction(item: TerlambatItemUi): SendAction {
        val variant = MessageTemplateBuilder.variantFor(item.status)
        val rawTemplate = when (variant) {
            TemplateVariant.A -> templateA
            TemplateVariant.B -> templateB
        }
        val text = MessageTemplateBuilder.build(
            template = rawTemplate,
            nama = item.view.nama,
            kamar = item.view.kamar,
            batasAwal = item.view.batasAwal,
            batasAktif = item.batasAktif
        )
        return SendActionPlanner.plan(text, item.view.noWaWali)
    }

    suspend fun tandaiKembali(izinId: Long, tanggal: LocalDate) {
        izinRepo.tandaiKembali(izinId, tanggal)
    }
}

