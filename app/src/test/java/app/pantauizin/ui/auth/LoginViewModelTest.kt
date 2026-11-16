package app.pantauizin.ui.auth

import app.pantauizin.data.firebase.AuthManager
import app.pantauizin.data.firebase.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeAuthManager(var shouldFail: Boolean = false) : AuthManager {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return if (shouldFail) {
            Result.failure(Exception("Kredensial tidak valid"))
        } else {
            val user = UserProfile(uid = "uid123", email = email)
            _currentUser.value = user
            Result.success(user)
        }
    }

    override fun signOut() {
        _currentUser.value = null
    }
}

@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {

    @Test
    fun validasiMenolakEmailKosong() {
        val fakeAuth = FakeAuthManager()
        val vm = LoginViewModel(fakeAuth)

        vm.onEmailChange("")
        vm.onKataSandiChange("password123")
        vm.login(onSuccess = {})

        assertEquals("Email tidak boleh kosong", vm.uiState.value.errorMessage)
    }

    @Test
    fun validasiMenolakKataSandiKosong() {
        val fakeAuth = FakeAuthManager()
        val vm = LoginViewModel(fakeAuth)

        vm.onEmailChange("pengurus@pesantren.id")
        vm.onKataSandiChange("")
        vm.login(onSuccess = {})

        assertEquals("Kata sandi tidak boleh kosong", vm.uiState.value.errorMessage)
    }

    @Test
    fun mengetikUlangMenghapusPesanError() {
        val fakeAuth = FakeAuthManager()
        val vm = LoginViewModel(fakeAuth)

        vm.onEmailChange("")
        vm.login(onSuccess = {})
        assertEquals("Email tidak boleh kosong", vm.uiState.value.errorMessage)

        vm.onEmailChange("baru@pesantren.id")
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun loginBerhasilMemanggilCallback() {
        val fakeAuth = FakeAuthManager(shouldFail = false)
        val vm = LoginViewModel(fakeAuth)

        var berhasil = false
        vm.onEmailChange("admin@pesantren.id")
        vm.onKataSandiChange("rahasia123")
        vm.login(onSuccess = {
            berhasil = true
        })

        assertTrue(berhasil)
        assertNull(vm.uiState.value.errorMessage)
    }
}
