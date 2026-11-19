package app.pantauizin.ui.form

import androidx.lifecycle.ViewModel
import app.pantauizin.data.IzinInput
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.IzinView
import app.pantauizin.data.SantriRepository
import app.pantauizin.data.SaveIzinResult
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.BatasPreview
import app.pantauizin.domain.BatasPreviewer
import app.pantauizin.domain.FormField
import app.pantauizin.domain.IzinFormState
import app.pantauizin.domain.IzinFormValidator
import app.pantauizin.domain.RekapPulangLogic
import app.pantauizin.domain.RiwayatPulangTerakhir
import app.pantauizin.domain.SantriItem
import app.pantauizin.domain.SantriSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class IzinFormUiState(
    val form: IzinFormState,
    val selectedKamar: String? = null,
    val kamarOptions: List<String> = emptyList(),
    val namaQuery: String = "",
    val saranSantri: List<SantriItem> = emptyList(),
    val activeIzinIdWarning: Long? = null,
    val recentPulangWarning: RiwayatPulangTerakhir? = null,
    val showRecentPulangPopup: Boolean = false,
    val recentPulangSantriIds: Set<Long> = emptySet(),
    val pemberiIzin: String? = null,
    val errors: Map<FormField, String> = emptyMap(),
    val preview: BatasPreview? = null,
    val isEditMode: Boolean = false
)

class IzinFormViewModel(
    private val santriRepo: SantriRepository,
    private val izinRepo: IzinRepository,
    private val editIzinId: Long? = null,
    private val pemberiIzinNama: String? = null,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private var allSantri: List<SantriItem> = emptyList()
    private var allIzin: List<IzinView> = emptyList()

    private val _uiState = MutableStateFlow(
        IzinFormUiState(
            form = IzinFormState(
                santriId = null,
                tanggalPulang = todayProvider(),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "",
                batasJenis = BatasJenis.HARI_1,
                tanggalBatasPilihan = null,
                noWa = ""
            ),
            pemberiIzin = pemberiIzinNama?.trim()?.takeIf { it.isNotEmpty() },
            isEditMode = editIzinId != null
        ).withComputed(todayProvider())
    )
    val uiState: StateFlow<IzinFormUiState> = _uiState.asStateFlow()

    suspend fun loadInitial() {
        allSantri = santriRepo.observe().first()
        allIzin = izinRepo.observe().first()
        val kamarOpts = SantriSearch.kamarOptions(allSantri)
        val recentIds = RekapPulangLogic.santriIdsPulangDalam2Bulan(
            allIzin = allIzin,
            referenceDate = _uiState.value.form.tanggalPulang,
            excludeIzinId = editIzinId
        )

        if (editIzinId != null) {
            val existing = izinRepo.get(editIzinId)
            if (existing != null) {
                val formState = IzinFormState(
                    santriId = existing.santriId,
                    tanggalPulang = existing.tanggalPulang,
                    alasanJenis = existing.alasanJenis,
                    alasanDetail = existing.alasanDetail,
                    batasJenis = existing.batasJenis,
                    tanggalBatasPilihan = if (existing.batasJenis == BatasJenis.LEBIH_3_HARI) existing.batasAwal else null,
                    noWa = existing.noWaWali.orEmpty()
                )
                val editRecentIds = RekapPulangLogic.santriIdsPulangDalam2Bulan(
                    allIzin = allIzin,
                    referenceDate = existing.tanggalPulang,
                    excludeIzinId = editIzinId
                )
                val recentWarning = RekapPulangLogic.cekPulangDalam2Bulan(
                    allIzin = allIzin,
                    santriId = existing.santriId,
                    referenceDate = existing.tanggalPulang,
                    excludeIzinId = editIzinId
                )
                _uiState.value = _uiState.value.copy(
                    form = formState,
                    selectedKamar = existing.kamar,
                    kamarOptions = kamarOpts,
                    namaQuery = existing.nama,
                    saranSantri = emptyList(),
                    activeIzinIdWarning = null,
                    recentPulangWarning = recentWarning,
                    showRecentPulangPopup = false,
                    recentPulangSantriIds = editRecentIds,
                    pemberiIzin = existing.pemberiIzin
                ).withComputed(todayProvider())
                return
            }
        }

        _uiState.value = _uiState.value.copy(
            kamarOptions = kamarOpts,
            saranSantri = computeSaran(_uiState.value.namaQuery, _uiState.value.selectedKamar),
            recentPulangSantriIds = recentIds
        ).withComputed(todayProvider())
    }

    fun pilihKamar(kamar: String?) {
        val current = _uiState.value
        val selectedSantri = allSantri.find { it.id == current.form.santriId }
        val keepSantri = selectedSantri != null && (kamar == null || selectedSantri.kamar == kamar)
        val newSantriId = if (keepSantri) current.form.santriId else null
        val newQuery = if (keepSantri) current.namaQuery else ""

        _uiState.value = current.copy(
            form = current.form.copy(santriId = newSantriId),
            selectedKamar = kamar,
            namaQuery = newQuery,
            saranSantri = if (newSantriId != null) emptyList() else computeSaran(newQuery, kamar),
            activeIzinIdWarning = findActiveWarning(newSantriId),
            recentPulangWarning = if (keepSantri) current.recentPulangWarning else null,
            showRecentPulangPopup = if (keepSantri) current.showRecentPulangPopup else false
        ).withComputed(todayProvider())
    }

    fun ubahNamaQuery(query: String) {
        val current = _uiState.value
        val selectedSantri = allSantri.find { it.id == current.form.santriId }
        val isExactSame = selectedSantri != null && selectedSantri.nama.equals(query.trim(), ignoreCase = true)
        val newSantriId = if (isExactSame) selectedSantri!!.id else null

        _uiState.value = current.copy(
            form = current.form.copy(santriId = newSantriId),
            namaQuery = query,
            saranSantri = if (newSantriId != null) emptyList() else computeSaran(query, current.selectedKamar),
            activeIzinIdWarning = findActiveWarning(newSantriId),
            recentPulangWarning = if (newSantriId != null) current.recentPulangWarning else null,
            showRecentPulangPopup = if (newSantriId != null) current.showRecentPulangPopup else false
        ).withComputed(todayProvider())
    }

    fun pilihSantri(santriId: Long) {
        val santri = allSantri.find { it.id == santriId } ?: return
        val current = _uiState.value
        val recent = RekapPulangLogic.cekPulangDalam2Bulan(
            allIzin = allIzin,
            santriId = santri.id,
            referenceDate = current.form.tanggalPulang,
            excludeIzinId = editIzinId
        )
        _uiState.value = current.copy(
            form = current.form.copy(santriId = santri.id),
            selectedKamar = santri.kamar,
            namaQuery = santri.nama,
            saranSantri = emptyList(),
            activeIzinIdWarning = findActiveWarning(santri.id),
            recentPulangWarning = recent,
            showRecentPulangPopup = recent != null && !current.isEditMode,
            errors = current.errors - FormField.SANTRI
        ).withComputed(todayProvider())
    }

    fun tutupPopupRecentPulang() {
        _uiState.value = _uiState.value.copy(showRecentPulangPopup = false)
    }

    fun ubahTanggalPulang(tanggal: LocalDate) {
        val current = _uiState.value
        val recentIds = RekapPulangLogic.santriIdsPulangDalam2Bulan(
            allIzin = allIzin,
            referenceDate = tanggal,
            excludeIzinId = editIzinId
        )
        val recentWarning = current.form.santriId?.let { sId ->
            RekapPulangLogic.cekPulangDalam2Bulan(
                allIzin = allIzin,
                santriId = sId,
                referenceDate = tanggal,
                excludeIzinId = editIzinId
            )
        }
        _uiState.value = current.copy(
            form = current.form.copy(tanggalPulang = tanggal),
            recentPulangSantriIds = recentIds,
            recentPulangWarning = recentWarning
        ).withComputed(todayProvider())
    }

    fun ubahAlasanJenis(jenis: AlasanJenis) {
        val current = _uiState.value
        _uiState.value = current.copy(
            form = current.form.copy(alasanJenis = jenis)
        ).withComputed(todayProvider())
    }

    fun ubahAlasanDetail(detail: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            form = current.form.copy(alasanDetail = detail),
            errors = current.errors - FormField.ALASAN_DETAIL
        ).withComputed(todayProvider())
    }

    fun ubahBatasJenis(jenis: BatasJenis) {
        val current = _uiState.value
        _uiState.value = current.copy(
            form = current.form.copy(batasJenis = jenis),
            errors = current.errors - FormField.BATAS_TANGGAL
        ).withComputed(todayProvider())
    }

    fun ubahTanggalBatasPilihan(tanggal: LocalDate?) {
        val current = _uiState.value
        _uiState.value = current.copy(
            form = current.form.copy(tanggalBatasPilihan = tanggal),
            errors = current.errors - FormField.BATAS_TANGGAL
        ).withComputed(todayProvider())
    }

    fun ubahNoWa(noWa: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            form = current.form.copy(noWa = noWa),
            errors = current.errors - FormField.NO_WA
        ).withComputed(todayProvider())
    }

    suspend fun simpan(): Boolean {
        val current = _uiState.value
        val validationErrors = IzinFormValidator.validate(current.form)
        val activeWarning = findActiveWarning(current.form.santriId)
        if (validationErrors.isNotEmpty() || activeWarning != null) {
            _uiState.value = current.copy(
                errors = validationErrors,
                activeIzinIdWarning = activeWarning
            )
            return false
        }

        val f = current.form
        val input = IzinInput(
            santriId = f.santriId!!,
            tanggalPulang = f.tanggalPulang,
            alasanJenis = f.alasanJenis!!,
            alasanDetail = f.alasanDetail,
            batasJenis = f.batasJenis!!,
            tanggalBatasPilihan = f.tanggalBatasPilihan,
            noWaWali = f.noWa,
            pemberiIzin = current.pemberiIzin
        )
        val res = if (editIzinId != null) {
            izinRepo.ubah(editIzinId, input)
        } else {
            izinRepo.tambah(input)
        }
        return when (res) {
            is SaveIzinResult.Ok -> true
            is SaveIzinResult.SantriSudahPunyaIzinAktif -> {
                _uiState.value = current.copy(activeIzinIdWarning = res.izinId)
                false
            }
            is SaveIzinResult.BatasTidakValid -> {
                _uiState.value = current.copy(
                    errors = current.errors + (FormField.BATAS_TANGGAL to "Tanggal kembali tidak valid")
                )
                false
            }
            is SaveIzinResult.NomorWaTidakValid -> {
                _uiState.value = current.copy(
                    errors = current.errors + (FormField.NO_WA to "Format nomor WhatsApp tidak valid")
                )
                false
            }
        }
    }

    suspend fun hapus(): Boolean {
        val id = editIzinId ?: return false
        izinRepo.hapus(id)
        return true
    }

    private fun computeSaran(query: String, kamar: String?): List<SantriItem> {
        if (kamar == null && query.isBlank()) return emptyList()
        return SantriSearch.filter(allSantri, query, kamar)
    }

    private fun findActiveWarning(santriId: Long?): Long? {
        if (santriId == null) return null
        val active = allIzin.find { it.santriId == santriId && it.tanggalKembali == null && it.id != editIzinId }
        return active?.id
    }

    private fun IzinFormUiState.withComputed(today: LocalDate): IzinFormUiState =
        copy(preview = BatasPreviewer.of(form, today))
}

