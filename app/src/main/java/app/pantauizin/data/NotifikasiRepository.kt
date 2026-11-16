package app.pantauizin.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class NotifikasiRepository(private val dao: NotifikasiDao) {
    fun observeBetween(from: Instant, to: Instant): Flow<List<NotifikasiView>> =
        dao.observeBetween(from.toEpochMilli(), to.toEpochMilli()).map { list ->
            list.map { it.toView() }
        }

    fun observeUnreadCount(): Flow<Int> =
        dao.observeBetween(0L, Long.MAX_VALUE).map { list ->
            list.count { !it.notifikasi.dibaca }
        }

    suspend fun markRead(id: Long) {
        dao.markRead(id)
    }

    suspend fun markReadByIzin(izinId: Long) {
        dao.markReadByIzin(izinId)
    }

    suspend fun markAllRead() {
        dao.markAllRead()
    }
}

