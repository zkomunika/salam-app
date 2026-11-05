package app.pantauizin.domain

import java.time.LocalDate

data class ExistingNotif(val izinId: Long, val tanggalKey: LocalDate)

data class PlannedNotif(val izinId: Long, val jenis: NotifJenis)

object NotificationPlanner {
    /**
     * Merencanakan notifikasi sapuan harian (spec 7.1).
     * Hanya santri yang belum kembali dan sudah lewat batasAktif; paling banyak satu per hari.
     */
    fun plan(
        today: LocalDate,
        izin: List<IzinSnapshot>,
        existing: List<ExistingNotif>
    ): List<PlannedNotif> {
        val riwayatPerIzin = existing.groupBy({ it.izinId }, { it.tanggalKey })
        val hasil = mutableListOf<PlannedNotif>()
        for (s in izin) {
            if (s.tanggalKembali != null) continue
            val batasAktif = StatusResolver.batasAktif(s)
            if (!today.isAfter(batasAktif)) continue

            val riwayat = riwayatPerIzin[s.izinId].orEmpty()
            if (today in riwayat) continue

            val jenis = when {
                s.konfirmasi.isEmpty() && riwayat.isEmpty() -> NotifJenis.LEWAT_BATAS
                s.konfirmasi.isNotEmpty() && riwayat.none { it.isAfter(batasAktif) } -> NotifJenis.LEWAT_TANGGAL_BARU
                else -> NotifJenis.PENGINGAT_HARIAN
            }
            hasil += PlannedNotif(s.izinId, jenis)
        }
        return hasil
    }
}

