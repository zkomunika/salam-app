package app.pantauizin.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class FormField { SANTRI, ALASAN_DETAIL, BATAS_TANGGAL, NO_WA }

data class IzinFormState(
    val santriId: Long?,
    val tanggalPulang: LocalDate,
    val alasanJenis: AlasanJenis?,
    val alasanDetail: String,
    val batasJenis: BatasJenis?,
    val tanggalBatasPilihan: LocalDate?,
    val noWa: String
)

object IzinFormValidator {
    fun validate(s: IzinFormState): Map<FormField, String> {
        val errors = mutableMapOf<FormField, String>()
        if (s.santriId == null) {
            errors[FormField.SANTRI] = "Pilih santri dari daftar"
        }
        if (s.alasanJenis == null || s.alasanDetail.trim().isEmpty()) {
            errors[FormField.ALASAN_DETAIL] = "Alasan wajib diisi"
        }
        val batas = s.batasJenis
        if (batas == null) {
            errors[FormField.BATAS_TANGGAL] = "Pilih batas waktu izin"
        } else if (batas == BatasJenis.LEBIH_3_HARI) {
            try {
                DeadlineCalculator.batasAwal(s.tanggalPulang, batas, s.tanggalBatasPilihan)
            } catch (_: IllegalArgumentException) {
                errors[FormField.BATAS_TANGGAL] = "Tanggal kembali harus setelah tanggal pulang + 3 hari"
            }
        }
        if (PhoneNumberNormalizer.normalize(s.noWa) is PhoneResult.Invalid) {
            errors[FormField.NO_WA] = "Format nomor WhatsApp tidak valid"
        }
        return errors
    }
}

data class BatasPreview(val batasAwal: LocalDate, val hariLewat: Int)

object BatasPreviewer {
    fun of(s: IzinFormState, today: LocalDate): BatasPreview? {
        val jenis = s.batasJenis ?: return null
        val batasAwal = try {
            DeadlineCalculator.batasAwal(s.tanggalPulang, jenis, s.tanggalBatasPilihan)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val hariLewat = ChronoUnit.DAYS.between(batasAwal, today).toInt().coerceAtLeast(0)
        return BatasPreview(batasAwal, hariLewat)
    }
}

