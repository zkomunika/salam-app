package app.pantauizin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.pantauizin.domain.DefaultTemplates
import app.pantauizin.domain.TemplateVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate
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

    companion object {
        val DEFAULT_JAM: LocalTime = LocalTime.of(8, 0)
        private val KEY_JAM_PENGINGAT = stringPreferencesKey("jam_pengingat")
        private val KEY_TEMPLATE_A = stringPreferencesKey("template_a")
        private val KEY_TEMPLATE_B = stringPreferencesKey("template_b")
        private val KEY_LAST_SWEEP_DATE = stringPreferencesKey("last_sweep_date")

        private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

        private fun getOrCreateDataStore(appContext: Context): DataStore<Preferences> {
            val file = File(appContext.filesDir, "datastore/pantau_izin_settings.preferences_pb")
            return stores.getOrPut(file.absolutePath) {
                PreferenceDataStoreFactory.create(produceFile = { file })
            }
        }
    }
}

