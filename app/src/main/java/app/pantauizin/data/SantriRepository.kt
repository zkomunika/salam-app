package app.pantauizin.data

import android.database.sqlite.SQLiteConstraintException
import app.pantauizin.data.firebase.FirestoreSyncManager
import app.pantauizin.domain.SantriItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SantriResult {
    data object Ok : SantriResult
    data object NamaKembar : SantriResult
    data object Invalid : SantriResult
}

class SantriRepository(
    private val dao: SantriDao,
    private val syncManager: FirestoreSyncManager? = null
) {
    fun observe(): Flow<List<SantriItem>> =
        dao.observeAll().map { list ->
            list.map { e ->
                SantriItem(id = e.id, nama = e.nama, kamar = e.kamar, aktif = e.aktif)
            }
        }

    suspend fun tambah(nama: String, kamar: String): SantriResult {
        val rapiNama = rapikan(nama)
        val rapiKamar = rapikan(kamar)
        if (rapiNama.isEmpty() || rapiKamar.isEmpty()) return SantriResult.Invalid
        if (dao.findByNama(rapiNama) != null) return SantriResult.NamaKembar

        return try {
            val entity = SantriEntity(nama = rapiNama, kamar = rapiKamar, aktif = true)
            val generatedId = dao.insert(entity)
            try {
                syncManager?.uploadSantri(entity.copy(id = generatedId))
            } catch (_: Exception) {}
            SantriResult.Ok
        } catch (_: SQLiteConstraintException) {
            SantriResult.NamaKembar
        }
    }

    suspend fun ubah(id: Long, nama: String, kamar: String): SantriResult {
        val rapiNama = rapikan(nama)
        val rapiKamar = rapikan(kamar)
        if (rapiNama.isEmpty() || rapiKamar.isEmpty()) return SantriResult.Invalid

        val current = dao.getById(id) ?: return SantriResult.Invalid
        val existingByName = dao.findByNama(rapiNama)
        if (existingByName != null && existingByName.id != id) return SantriResult.NamaKembar

        return try {
            val updated = current.copy(nama = rapiNama, kamar = rapiKamar)
            dao.update(updated)
            try {
                syncManager?.uploadSantri(updated)
            } catch (_: Exception) {}
            SantriResult.Ok
        } catch (_: SQLiteConstraintException) {
            SantriResult.NamaKembar
        }
    }

    suspend fun setAktif(id: Long, aktif: Boolean) {
        val current = dao.getById(id) ?: return
        val updated = current.copy(aktif = aktif)
        dao.update(updated)
        try {
            syncManager?.uploadSantri(updated)
        } catch (_: Exception) {}
    }

    private fun rapikan(s: String): String = s.trim().replace(Regex("\\s+"), " ")
}

