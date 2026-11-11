package app.pantauizin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.pantauizin.domain.DefaultTemplates
import app.pantauizin.domain.TemplateVariant
import app.pantauizin.notify.SweepSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

class SettingsStore(context: Context) {
    private val dataStore: DataStore<Preferences> = getOrCreateDataStore(context.applicationContext)

    val jamPengingat: Flow<LocalTime> = dataStore.data.map { prefs ->
        prefs[KEY_JAM_PENGINGAT]?.let(LocalTime::parse) ?: DEFAULT_JAM
    }

    val templateA: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TEMPLATE_A] ?: DefaultTemplates.A
    }

    val templateB: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TEMPLATE_B] ?: DefaultTemplates.B
    }

    val lastSweepDate: Flow<LocalDate?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SWEEP_DATE]?.let(LocalDate::parse)
    }

    suspend fun setJamPengingat(t: LocalTime) {
        dataStore.edit { prefs ->
            prefs[KEY_JAM_PENGINGAT] = t.toString()
        }
    }

    suspend fun setTemplate(v: TemplateVariant, text: String) {
        dataStore.edit { prefs ->
            val key = when (v) {
                TemplateVariant.A -> KEY_TEMPLATE_A
                TemplateVariant.B -> KEY_TEMPLATE_B
            }
            prefs[key] = text
        }
    }

    suspend fun resetTemplate(v: TemplateVariant) {
        dataStore.edit { prefs ->
            when (v) {
                TemplateVariant.A -> prefs.remove(KEY_TEMPLATE_A)
                TemplateVariant.B -> prefs.remove(KEY_TEMPLATE_B)
            }
        }
    }

    suspend fun setLastSweepDate(d: LocalDate) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SWEEP_DATE] = d.toString()
        }
    }

    val lastRunAt: Flow<LocalDateTime?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_RUN_AT]?.let(LocalDateTime::parse)
    }

    val lastRunSource: Flow<SweepSource?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_RUN_SOURCE]?.let { name -> SweepSource.entries.firstOrNull { it.name == name } }
    }

    val lastAlarmFiredAt: Flow<LocalDateTime?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_ALARM_FIRED]?.let(LocalDateTime::parse)
    }

    val nextAlarmAt: Flow<LocalDateTime?> = dataStore.data.map { prefs ->
        prefs[KEY_NEXT_ALARM]?.let(LocalDateTime::parse)
    }

    suspend fun recordRun(at: LocalDateTime, source: SweepSource) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_RUN_AT] = at.toString()
            prefs[KEY_LAST_RUN_SOURCE] = source.name
            if (source == SweepSource.ALARM) prefs[KEY_LAST_ALARM_FIRED] = at.toString()
        }
    }

    suspend fun recordAlarmFired(at: LocalDateTime) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_ALARM_FIRED] = at.toString()
        }
    }

    suspend fun setNextAlarmAt(at: LocalDateTime) {
        dataStore.edit { prefs ->
            prefs[KEY_NEXT_ALARM] = at.toString()
        }
    }

    companion object {
        val DEFAULT_JAM: LocalTime = LocalTime.of(8, 0)
        private val KEY_JAM_PENGINGAT = stringPreferencesKey("jam_pengingat")
        private val KEY_TEMPLATE_A = stringPreferencesKey("template_a")
        private val KEY_TEMPLATE_B = stringPreferencesKey("template_b")
        private val KEY_LAST_SWEEP_DATE = stringPreferencesKey("last_sweep_date")
        private val KEY_LAST_RUN_AT = stringPreferencesKey("last_run_at")
        private val KEY_LAST_RUN_SOURCE = stringPreferencesKey("last_run_source")
        private val KEY_LAST_ALARM_FIRED = stringPreferencesKey("last_alarm_fired")
        private val KEY_NEXT_ALARM = stringPreferencesKey("next_alarm")

        private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

        private fun getOrCreateDataStore(appContext: Context): DataStore<Preferences> {
            val file = File(appContext.filesDir, "datastore/pantau_izin_settings.preferences_pb")
            return stores.getOrPut(file.absolutePath) {
                PreferenceDataStoreFactory.create(produceFile = { file })
            }
        }
    }
}

