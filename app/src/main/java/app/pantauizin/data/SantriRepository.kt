package app.pantauizin.data

import android.database.sqlite.SQLiteConstraintException
import app.pantauizin.domain.SantriItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SantriResult {
    data object Ok : SantriResult
    data object NamaKembar : SantriResult
    data object Invalid : SantriResult
}

class SantriRepository(private val dao: SantriDao) {
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
            dao.insert(SantriEntity(nama = rapiNama, kamar = rapiKamar, aktif = true))
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
            dao.update(current.copy(nama = rapiNama, kamar = rapiKamar))
            SantriResult.Ok
        } catch (_: SQLiteConstraintException) {
            SantriResult.NamaKembar
        }
    }

    suspend fun setAktif(id: Long, aktif: Boolean) {
        val current = dao.getById(id) ?: return
        dao.update(current.copy(aktif = aktif))
    }

    private fun rapikan(s: String): String = s.trim().replace(Regex("\\s+"), " ")
}

