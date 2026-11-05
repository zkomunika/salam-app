package app.pantauizin.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TemplateVariant { A, B }

object MessageTemplateBuilder {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))

    /** A: belum dikonfirmasi; B: terkonfirmasi atau konfirmasi habis. Status lain tidak punya template. */
    fun variantFor(status: IzinStatus): TemplateVariant = when (status) {
        IzinStatus.TERLAMBAT_BELUM_KONFIRMASI -> TemplateVariant.A
        IzinStatus.TERLAMBAT_TERKONFIRMASI,
        IzinStatus.TERLAMBAT_KONFIRMASI_HABIS -> TemplateVariant.B
        else -> throw IllegalArgumentException("Status $status tidak memakai template pesan")
    }

    fun formatTanggal(d: LocalDate): String = d.format(formatter)

    fun build(
        template: String,
        nama: String,
        kamar: String,
        batasAwal: LocalDate,
        batasAktif: LocalDate
    ): String = template
        .replace("{nama}", nama)
        .replace("{kamar}", kamar)
        .replace("{batas_awal}", formatTanggal(batasAwal))
        .replace("{batas_aktif}", formatTanggal(batasAktif))
}

