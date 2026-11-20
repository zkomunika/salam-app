package app.pantauizin.data.firebase

import androidx.annotation.Keep

@Keep
enum class UserRole {
    ADMIN,
    PENGURUS
}

private val PENGURUS_INTI_MAP = mapOf(
    "miftahudin@asas.id" to "MIFTAHUDIN",
    "mthoriqmusthafa@asas.id" to "M THORIQ MUSTHAFA",
    "mfikrihambali@asas.id" to "M FIKRI HAMBALI",
    "fikrimhilmi@asas.id" to "FIKRI M HILMI",
    "mfajrulhikam@asas.id" to "M FAJRUL HIKAM",
    "maulanahasyim@asas.id" to "MAULANA HASYIM",
    "mohghiyatsmaulanaaripin@asas.id" to "MOH GHIYATS MAULANA ARIPIN",
    "mazkaanggiri@asas.id" to "M AZKA ANGGIRI",
    "mazmanabdulaziz@asas.id" to "M AZMAN ABDUL AZIZ",
    "akmalmasykurhidayat@asas.id" to "AKMAL MASYKUR HIDAYAT"
)

internal val PENGURUS_INTI_NAMES: Set<String> = PENGURUS_INTI_MAP.values.toSet()

fun defaultNamaForEmail(email: String): String? = PENGURUS_INTI_MAP[email.trim().lowercase()]

@Keep
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nama: String = "",
    val role: String = UserRole.PENGURUS.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role.equals(UserRole.ADMIN.name, ignoreCase = true)
    val isPengurusInti: Boolean
        get() = email.trim().lowercase() in PENGURUS_INTI_MAP ||
            nama.trim().uppercase() in PENGURUS_INTI_NAMES
    val canManageSantri: Boolean
        get() = isAdmin || isPengurusInti
    val canManageIzin: Boolean
        get() = isAdmin || isPengurusInti
    val displayNama: String
        get() = nama.trim().ifBlank {
            defaultNamaForEmail(email)
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
}

@Keep
data class FirestoreKonfirmasi(
    val id: Long = 0,
    val tanggalKembaliBaru: String? = null,
    val catatan: String? = null,
    val buktiBase64: String? = null,
    val dibuatPada: Long = 0
)

@Keep
data class FirestoreIzin(
    val id: Long = 0,
    val santriId: Long = 0,
    val tanggalPulang: String = "",
    val alasanJenis: String = "",
    val alasanDetail: String = "",
    val batasJenis: String = "",
    val batasAwal: String = "",
    val noWaWali: String? = null,
    val tanggalKembali: String? = null,
    val dibuatPada: Long = 0,
    val pemberiIzin: String? = null,
    val terakhirWaPada: Long? = null,
    val terakhirWaOleh: String? = null,
    val konfirmasiList: List<FirestoreKonfirmasi> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class FirestoreSantri(
    val id: Long = 0,
    val nama: String = "",
    val kamar: String = "",
    val aktif: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
