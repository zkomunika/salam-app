package app.pantauizin.data.firebase

import androidx.annotation.Keep

@Keep
enum class UserRole {
    ADMIN,
    PENGURUS
}

@Keep
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nama: String = "",
    val role: String = UserRole.PENGURUS.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role.equals(UserRole.ADMIN.name, ignoreCase = true)
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
