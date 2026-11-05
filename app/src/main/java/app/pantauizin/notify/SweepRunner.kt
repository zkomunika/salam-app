package app.pantauizin.notify

import app.pantauizin.data.IzinRepository
import app.pantauizin.data.NotifikasiDao
import app.pantauizin.data.NotifikasiEntity
import app.pantauizin.data.SettingsStore
import app.pantauizin.domain.ExistingNotif
import app.pantauizin.domain.NotifJenis
import app.pantauizin.domain.NotificationPlanner
import app.pantauizin.domain.SweepSchedule
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

interface Notifier {
    fun show(izinId: Long, text: String)
}

interface Scheduler {
    fun schedule(at: LocalDateTime)
}

interface SweepRunnerProvider {
    val sweepRunner: SweepRunner
}

class SweepRunner(
    private val izin: IzinRepository,
    private val notif: NotifikasiDao,
    private val settings: SettingsStore,
    private val notifier: Notifier,
    private val scheduler: Scheduler,
    private val clock: () -> LocalDateTime
) {
    suspend fun runIfDue(): Boolean {
        val now = clock()
        val jam = settings.jamPengingat.first()
        val lastSweep = settings.lastSweepDate.first()

        if (!SweepSchedule.isDue(now, jam, lastSweep)) {
            scheduler.schedule(SweepSchedule.next(now, jam, lastSweep))
            return false
        }

        val today = now.toLocalDate()
        val izinViews = izin.observe().first()
        val izinById = izinViews.associateBy { it.id }
        val snapshots = izinViews.map { it.toSnapshot() }
        val existing = notif.getAll().map { ExistingNotif(it.izinId, it.tanggalKey) }

        val planned = NotificationPlanner.plan(today, snapshots, existing)
        val createdAt = now.atZone(ZoneId.systemDefault()).toInstant()
        for (p in planned) {
            val view = izinById[p.izinId] ?: continue
            val text = formatNotifText(p.jenis, view.nama, view.kamar)
            val rowId = notif.insertIgnore(
                NotifikasiEntity(
                    izinId = p.izinId,
                    jenis = p.jenis,
                    pesan = text,
                    tanggalKey = today,
                    dibuatPada = createdAt,
                    dibaca = false
                )
            )
            if (rowId != -1L) {
                notifier.show(p.izinId, text)
            }
        }

        settings.setLastSweepDate(today)
        scheduler.schedule(SweepSchedule.next(now, jam, today))
        return true
    }

    suspend fun rescheduleOnly() {
        val now = clock()
        val jam = settings.jamPengingat.first()
        val lastSweep = settings.lastSweepDate.first()
        scheduler.schedule(SweepSchedule.next(now, jam, lastSweep))
    }

    companion object {
        fun formatNotifText(jenis: NotifJenis, nama: String, kamar: String): String = when (jenis) {
            NotifJenis.LEWAT_BATAS -> "$nama kamar $kamar sudah melewati batas waktu izin"
            NotifJenis.LEWAT_TANGGAL_BARU -> "$nama kamar $kamar sudah melewati tanggal kembali yang dikonfirmasi"
            NotifJenis.PENGINGAT_HARIAN -> "$nama kamar $kamar belum kembali dan belum ada konfirmasi terbaru"
        }
    }
}

