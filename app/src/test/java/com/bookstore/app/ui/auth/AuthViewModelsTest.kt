package com.bookstore.app.ui.auth

import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.testutil.FakeUserDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userDao: FakeUserDao
    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository

    @Before
    fun setUp() {
        userDao = FakeUserDao()
        session = SessionManager()
        authRepo = AuthRepository(userDao, session, dispatcher = mainDispatcherRule.testDispatcher)
    }

    @Test
    fun loginViewModel_validationFailsOnEmptyFields() {
        val vm = LoginViewModel(authRepo)
        vm.submit()
        assertNotNull(vm.emailError)
        assertNotNull(vm.passwordError)
    }

    @Test
    fun loginViewModel_successfulLoginUpdatesSession() = runTest {
        authRepo.register("User", "user@test.com", "Password123")
        session.signOut()

        val vm = LoginViewModel(authRepo)
        vm.onEmail("user@test.com")
        vm.onPassword("Password123")
        vm.submit()

        assertNull(vm.formError)
        assertNotNull(session.userId.value)
    }

    @Test
    fun loginViewModel_fillDemoCredentialsAndSubmit() = runTest {
        val vm = LoginViewModel(authRepo)
        vm.fillDemoCredentials()
        assertEquals(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_EMAIL, vm.email)
        assertEquals(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD, vm.password)

        vm.submit()
        assertNull(vm.formError)
        assertNotNull(session.userId.value)
    }

    @Test
    fun loginViewModel_wrongCredentialsShowsFormError() = runTest {
        val vm = LoginViewModel(authRepo)
        vm.onEmail("unknown@test.com")
        vm.onPassword("WrongPass123")
        vm.submit()

        assertEquals("Incorrect email or password", vm.formError)
    }

    @Test
    fun registerViewModel_validationFailsOnInvalidInput() {
        val vm = RegisterViewModel(authRepo)
        vm.onName("")
        vm.onEmail("notanemail")
        vm.onPassword("short")
        vm.onConfirm("different")
        vm.submit()

        assertNotNull(vm.nameError)
        assertNotNull(vm.emailError)
        assertNotNull(vm.passwordError)
        assertNotNull(vm.confirmError)
    }

    @Test
    fun registerViewModel_successfulRegistration() = runTest {
        val vm = RegisterViewModel(authRepo)
        vm.onName("Alice Smith")
        vm.onEmail("alice@test.com")
        vm.onPassword("Password123")
        vm.onConfirm("Password123")
        vm.submit()

        assertNull(vm.formError)
        assertNotNull(session.userId.value)
    }
}
