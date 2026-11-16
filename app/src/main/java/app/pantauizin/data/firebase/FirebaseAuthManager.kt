package app.pantauizin.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface AuthManager {
    val currentUser: StateFlow<UserProfile?>
    val isLoading: StateFlow<Boolean>
    suspend fun signIn(email: String, password: String): Result<UserProfile>
    fun signOut()
}

class FirebaseAuthManager(
    private val context: Context? = null,
    private val authProvider: () -> FirebaseAuth? = {
        try {
            if (context != null && FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else null
        } catch (_: Exception) {
            null
        }
    },
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            if (context != null && FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (_: Exception) {
            null
        }
    },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AuthManager {
    private val auth: FirebaseAuth? get() = authProvider()
    private val firestore: FirebaseFirestore? get() = firestoreProvider()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val currentAuth = auth
        if (currentAuth == null) {
            _currentUser.value = null
            _isLoading.value = false
        } else {
            currentAuth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user == null) {
                    _currentUser.value = null
                    _isLoading.value = false
                } else {
                    scope.launch {
                        loadUserProfile(user.uid, user.email)
                    }
                }
            }
        }
    }

    private suspend fun loadUserProfile(uid: String, email: String?) {
        try {
            val db = firestore ?: run {
                _currentUser.value = createDefaultProfile(uid, email)
                return
            }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                _currentUser.value = profile ?: createDefaultProfile(uid, email)
            } else {
                val newProfile = createDefaultProfile(uid, email)
                docRef.set(newProfile).await()
                _currentUser.value = newProfile
            }
        } catch (e: Exception) {
            // Fallback if offline: assume current auth info
            _currentUser.value = createDefaultProfile(uid, email)
        } finally {
            _isLoading.value = false
        }
    }

    private fun createDefaultProfile(uid: String, email: String?): UserProfile {
        val safeEmail = email ?: ""
        // Auto-assign admin if owner email or starts with admin
        val role = if (safeEmail.equals("mzakyalmubarok9.4@gmail.com", ignoreCase = true) ||
            safeEmail.startsWith("admin", ignoreCase = true)
        ) {
            UserRole.ADMIN.name
        } else {
            UserRole.PENGURUS.name
        }
        val defaultName = safeEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        return UserProfile(
            uid = uid,
            email = safeEmail,
            nama = defaultName,
            role = role,
            createdAt = System.currentTimeMillis()
        )
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val currentAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth belum terhubung"))
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore belum terhubung"))

        return try {
            val cleanEmail = email.trim()
            val cleanPass = password.trim()
            if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
                return Result.failure(IllegalArgumentException("Email dan kata sandi tidak boleh kosong"))
            }

            val authResult = currentAuth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
            val user = authResult.user ?: throw IllegalStateException("Gagal memuat akun pengguna")

            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()

            val profile = if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java) ?: createDefaultProfile(user.uid, user.email)
            } else {
                val newProfile = createDefaultProfile(user.uid, user.email)
                docRef.set(newProfile).await()
                newProfile
            }

            _currentUser.value = profile
            Result.success(profile)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Akun tidak ditemukan atau telah dinonaktifkan"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email atau kata sandi salah"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Koneksi gagal. Periksa jaringan internet Anda"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Terjadi kesalahan saat masuk"))
        }
    }

    override fun signOut() {
        auth?.signOut()
        _currentUser.value = null
    }

    suspend fun createPengurusAccount(
        email: String,
        password: String,
        nama: String,
        role: UserRole = UserRole.PENGURUS
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore belum terhubung"))
        return try {
            val current = _currentUser.value
            if (current == null || !current.isAdmin) {
                return Result.failure(IllegalAccessException("Hanya admin yang dapat menambahkan pengurus"))
            }

            // Using secondary Firebase App or custom admin function to not replace current login
            // For simple client-side onboarding by admin:
            val tempDoc = db.collection("pengurus_invites").document()
            val inviteData = mapOf(
                "email" to email.trim().lowercase(),
                "nama" to nama.trim(),
                "role" to role.name,
                "createdAt" to System.currentTimeMillis()
            )
            tempDoc.set(inviteData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
