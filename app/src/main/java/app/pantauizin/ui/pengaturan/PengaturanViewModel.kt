package app.pantauizin.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.SettingsStore
import app.pantauizin.data.firebase.FirebaseAuthManager
import app.pantauizin.data.firebase.FirestoreSyncManager
import app.pantauizin.data.firebase.UserProfile
import app.pantauizin.domain.DefaultTemplates
import app.pantauizin.domain.TemplateVariant
import app.pantauizin.notify.SweepSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

data class PengaturanUiState(
    val jamPengingat: LocalTime = SettingsStore.DEFAULT_JAM,
    val templateA: String = DefaultTemplates.A,
    val templateB: String = DefaultTemplates.B,
    val pesanErrorTemplateA: String? = null,
    val pesanErrorTemplateB: String? = null,
    val lastRunAt: LocalDateTime? = null,
    val lastRunSource: SweepSource? = null,
    val lastAlarmFiredAt: LocalDateTime? = null,
    val nextAlarmAt: LocalDateTime? = null,
    val testAlarmAt: LocalDateTime? = null,
    val currentUser: UserProfile? = null,
    val isMigrating: Boolean = false,
    val migrationProgress: String? = null,
    val migrationResult: String? = null
)

class PengaturanViewModel(
    private val settings: SettingsStore,
    private val authManager: FirebaseAuthManager? = null,
    private val syncManager: FirestoreSyncManager? = null,
    private val onTesNotifikasi: () -> Unit = {},
    private val onTesAlarm: () -> LocalDateTime? = { null },
    private val onReschedule: suspend () -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(PengaturanUiState())
    val uiState: StateFlow<PengaturanUiState> = _uiState.asStateFlow()

    init {
        authManager?.let { auth ->
            viewModelScope.launch {
                auth.currentUser.collect { user ->
                    _uiState.update { it.copy(currentUser = user) }
                }
            }
        }
        viewModelScope.launch {
            combine(
                settings.jamPengingat,
                settings.templateA,
                settings.templateB
            ) { jam, tplA, tplB ->
                Triple(jam, tplA, tplB)
            }.collect { (jam, tplA, tplB) ->
                _uiState.update {
                    it.copy(
                        jamPengingat = jam,
                        templateA = tplA,
                        templateB = tplB
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                settings.lastRunAt,
                settings.lastRunSource,
                settings.lastAlarmFiredAt,
                settings.nextAlarmAt
            ) { runAt, source, fired, next ->
                _uiState.update {
                    it.copy(
                        lastRunAt = runAt,
                        lastRunSource = source,
                        lastAlarmFiredAt = fired,
                        nextAlarmAt = next
                    )
                }
            }.collect { }
        }
    }

    fun logout() {
        authManager?.signOut()
    }

    fun migrasiDataKeCloud() {
        val sm = syncManager ?: return
        _uiState.update { it.copy(isMigrating = true, migrationResult = null) }
        viewModelScope.launch {
            val result = sm.migrateAllLocalToCloud { cur, total ->
                _uiState.update { it.copy(migrationProgress = "$cur / $total terunggah") }
            }
            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isMigrating = false,
                            migrationProgress = null,
                            migrationResult = "Berhasil mengunggah $count data ke Firestore!"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isMigrating = false,
                            migrationProgress = null,
                            migrationResult = "Gagal migrasi: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun tesNotifikasi() {
        onTesNotifikasi()
    }

    fun tesAlarm(): LocalDateTime? {
        val scheduledAt = onTesAlarm()
        _uiState.update { it.copy(testAlarmAt = scheduledAt) }
        return scheduledAt
    }

    suspend fun ubahJam(t: LocalTime) {
        settings.setJamPengingat(t)
        onReschedule()
    }

    suspend fun simpanTemplate(v: TemplateVariant, text: String): Boolean {
        if (text.isBlank()) {
            _uiState.update {
                when (v) {
                    TemplateVariant.A -> it.copy(pesanErrorTemplateA = "Template tidak boleh kosong")
                    TemplateVariant.B -> it.copy(pesanErrorTemplateB = "Template tidak boleh kosong")
                }
            }
            return false
        }
        _uiState.update {
            when (v) {
                TemplateVariant.A -> it.copy(pesanErrorTemplateA = null)
                TemplateVariant.B -> it.copy(pesanErrorTemplateB = null)
            }
        }
        settings.setTemplate(v, text)
        return true
    }

    suspend fun kembalikanTemplate(v: TemplateVariant) {
        _uiState.update {
            when (v) {
                TemplateVariant.A -> it.copy(pesanErrorTemplateA = null)
                TemplateVariant.B -> it.copy(pesanErrorTemplateB = null)
            }
        }
        settings.resetTemplate(v)
    }
}

