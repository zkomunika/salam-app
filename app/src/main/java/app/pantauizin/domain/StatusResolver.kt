package app.pantauizin.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object StatusResolver {
    /** Terbesar dari batasAwal dan semua tanggalKembaliBaru non-null. */
    fun batasAktif(s: IzinSnapshot): LocalDate {
        var hasil = s.batasAwal
        for (k in s.konfirmasi) {
            val baru = k.tanggalKembaliBaru
            if (baru != null && baru.isAfter(hasil)) hasil = baru
        }
        return hasil
    }

    fun status(s: IzinSnapshot, today: LocalDate): IzinStatus {
        val kembali = s.tanggalKembali
        if (kembali != null) {
            return if (kembali.isAfter(s.batasAwal)) IzinStatus.KEMBALI_TERLAMBAT else IzinStatus.KEMBALI_TEPAT
        }
        if (!today.isAfter(s.batasAwal)) return IzinStatus.IZIN_BERJALAN
        if (s.konfirmasi.isEmpty()) return IzinStatus.TERLAMBAT_BELUM_KONFIRMASI
        return if (today.isAfter(batasAktif(s))) {
            IzinStatus.TERLAMBAT_KONFIRMASI_HABIS
        } else {
            IzinStatus.TERLAMBAT_TERKONFIRMASI
        }
    }

    /** (tanggalKembali ?: today) dikurangi batasAwal dalam hari, minimal 0. */
    fun hariTerlambat(s: IzinSnapshot, today: LocalDate): Int {
        val akhir = s.tanggalKembali ?: today
        return ChronoUnit.DAYS.between(s.batasAwal, akhir).toInt().coerceAtLeast(0)
    }
}

