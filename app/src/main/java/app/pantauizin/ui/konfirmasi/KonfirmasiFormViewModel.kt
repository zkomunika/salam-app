package app.pantauizin.ui.konfirmasi

import android.net.Uri
import androidx.lifecycle.ViewModel
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.KonfirmasiView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class KonfirmasiFormUiState(
    val izinId: Long,
    val namaSantri: String = "",
    val kamarSantri: String = "",
    val perluTanggalBaru: Boolean = true,
    val tanggalKembaliBaru: LocalDate? = null,
    val catatan: String = "",
    val buktiUri: Uri? = null,
    val buktiNama: String? = null,
    val tanggalError: String? = null,
    val buktiError: String? = null,
    val riwayat: List<KonfirmasiView> = emptyList()
)

class KonfirmasiFormViewModel(
    private val izinRepo: IzinRepository,
    private val izinId: Long,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(KonfirmasiFormUiState(izinId = izinId))
    val uiState: StateFlow<KonfirmasiFormUiState> = _uiState.asStateFlow()

    suspend fun load() {
        val izin = izinRepo.get(izinId) ?: return
        val belumKembali = izin.tanggalKembali == null
        _uiState.update {
            it.copy(
                namaSantri = izin.nama,
                kamarSantri = izin.kamar,
                perluTanggalBaru = belumKembali,
                tanggalKembaliBaru = if (belumKembali) it.tanggalKembaliBaru else null,
                riwayat = izin.konfirmasi
            )
        }
    }

    fun ubahTanggalBaru(tanggal: LocalDate?) {
        _uiState.update {
            it.copy(
                tanggalKembaliBaru = tanggal,
                tanggalError = null
            )
        }
    }

    fun ubahCatatan(catatan: String) {
        _uiState.update {
            it.copy(catatan = catatan)
        }
    }

    fun pilihBukti(uri: Uri?, nama: String? = null) {
        _uiState.update {
            it.copy(
                buktiUri = uri,
                buktiNama = nama ?: uri?.lastPathSegment,
                buktiError = null
            )
        }
    }

    suspend fun simpan(): Boolean {
        val current = _uiState.value
        val today = todayProvider()

        if (current.perluTanggalBaru) {
            val tgl = current.tanggalKembaliBaru
            if (tgl == null) {
                _uiState.update { it.copy(tanggalError = "Tanggal kembali baru wajib diisi") }
                return false
            }
            if (tgl.isBefore(today)) {
                _uiState.update { it.copy(tanggalError = "Tanggal kembali baru harus hari ini atau setelahnya") }
                return false
            }
        }

        return try {
            izinRepo.tambahKonfirmasi(
                izinId = izinId,
                tanggalBaru = if (current.perluTanggalBaru) current.tanggalKembaliBaru else null,
                catatan = current.catatan,
                buktiUri = current.buktiUri
            )
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(buktiError = "Gagal menyalin file bukti. Silakan coba lagi.")
            }
            false
        }
    }
}

