package app.pantauizin.domain

sealed interface PhoneResult {
    data object Empty : PhoneResult
    data class Valid(val value: String) : PhoneResult
    data object Invalid : PhoneResult
}

object PhoneNumberNormalizer {
    /**
     * Normalisasi nomor WA (spec 6.2): buang pemisah, awalan 0 -> 62, awalan 62 tetap,
     * awalan 8 -> 628. Valid jika berawalan 62 dan 10 sampai 15 digit.
     * Input yang berisi huruf dianggap tidak valid; kolom kosong dianggap tidak ada nomor.
     */
    fun normalize(input: String?): PhoneResult {
        if (input == null || input.isBlank()) return PhoneResult.Empty
        if (input.any { it.isLetter() }) return PhoneResult.Invalid

        val digits = input.filter { it in '0'..'9' }
        if (digits.isEmpty()) return PhoneResult.Invalid

        val normalized = when {
            digits.startsWith("62") -> digits
            digits.startsWith("0") -> "62" + digits.substring(1)
            digits.startsWith("8") -> "62$digits"
            else -> return PhoneResult.Invalid
        }
        return if (normalized.length in 10..15) PhoneResult.Valid(normalized) else PhoneResult.Invalid
    }
}

