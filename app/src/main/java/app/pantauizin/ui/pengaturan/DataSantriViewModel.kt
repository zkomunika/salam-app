package app.pantauizin.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.SantriRepository
import app.pantauizin.data.SantriResult
import app.pantauizin.domain.SantriItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataSantriUiState(
    val query: String = "",
    val items: List<SantriItem> = emptyList(),
    val errorMessage: String? = null
)

class DataSantriViewModel(
    private val santriRepo: SantriRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(DataSantriUiState())
    val uiState: StateFlow<DataSantriUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(santriRepo.observe(), _query) { all, q ->
                val trimmed = q.trim()
                val filtered = if (trimmed.isEmpty()) {
                    all
                } else {
                    all.filter {
                        it.nama.contains(trimmed, ignoreCase = true) ||
                            it.kamar.contains(trimmed, ignoreCase = true)
                    }
                }
                q to filtered
            }.collect { (q, filtered) ->
                _uiState.update {
                    it.copy(
                        query = q,
                        items = filtered
                    )
                }
            }
        }
    }

    fun ubahQuery(q: String) {
        _query.value = q
    }

    fun hapusPesanError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    suspend fun tambah(nama: String, kamar: String): Boolean {
        return handleResult(santriRepo.tambah(nama, kamar))
    }

    suspend fun ubah(id: Long, nama: String, kamar: String): Boolean {
        return handleResult(santriRepo.ubah(id, nama, kamar))
    }

    suspend fun setAktif(id: Long, aktif: Boolean) {
        santriRepo.setAktif(id, aktif)
    }

    private fun handleResult(res: SantriResult): Boolean {
        return when (res) {
            SantriResult.Ok -> {
                _uiState.update { it.copy(errorMessage = null) }
                true
            }
            SantriResult.NamaKembar -> {
                _uiState.update { it.copy(errorMessage = "Nama sudah ada") }
                false
            }
            SantriResult.Invalid -> {
                _uiState.update { it.copy(errorMessage = "Nama dan kamar wajib diisi") }
                false
            }
        }
    }
}

