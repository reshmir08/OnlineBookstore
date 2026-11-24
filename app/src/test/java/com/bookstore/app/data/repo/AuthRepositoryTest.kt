package com.bookstore.app.data.repo

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.testutil.FakeUserDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {
    private lateinit var userDao: FakeUserDao
    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository

    @Before
    fun setUp() {
        userDao = FakeUserDao()
        session = SessionManager()
        authRepo = AuthRepository(userDao, session)
    }

    @Test
    fun registerSuccess_createsUserAndSignsIn() = runTest {
        val result = authRepo.register("John Doe", "john@example.com", "Password123")
        assertTrue(result is AppResult.Success)
        val user = (result as AppResult.Success).value
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals(user.id, session.userId.value)

        val current = authRepo.currentUser()
        assertNotNull(current)
        assertEquals(user.id, current?.id)
    }

    @Test
    fun registerDuplicateEmail_returnsFailure() = runTest {
        authRepo.register("John Doe", "john@example.com", "Password123")
        val duplicate = authRepo.register("Another Name", "  JOHN@example.com ", "DifferentPass1")
        assertTrue(duplicate is AppResult.Failure)
        assertEquals("An account with this email already exists", (duplicate as AppResult.Failure).message)
    }

    @Test
    fun loginSuccess_setsSession() = runTest {
        authRepo.register("Alice", "alice@test.com", "AlicePass123")
        session.signOut()
        assertNull(session.userId.value)

        val result = authRepo.login("alice@test.com", "AlicePass123")
        assertTrue(result is AppResult.Success)
        assertEquals("Alice", (result as AppResult.Success).value.name)
        assertNotNull(session.userId.value)
    }

    @Test
    fun loginWrongPassword_returnsFailureWithoutExposingAccount() = runTest {
        authRepo.register("Alice", "alice@test.com", "AlicePass123")
        val result = authRepo.login("alice@test.com", "WrongPassword1")
        assertTrue(result is AppResult.Failure)
        assertEquals("Incorrect email or password", (result as AppResult.Failure).message)
    }

    @Test
    fun loginNonExistentEmail_returnsSameFailureMessage() = runTest {
        val result = authRepo.login("nonexistent@test.com", "AnyPassword1")
        assertTrue(result is AppResult.Failure)
        assertEquals("Incorrect email or password", (result as AppResult.Failure).message)
    }

    @Test
    fun logout_clearsSession() = runTest {
        authRepo.register("Bob", "bob@test.com", "BobPass123")
        assertNotNull(session.userId.value)

        authRepo.logout()
        assertNull(session.userId.value)
        assertNull(authRepo.currentUser())
    }

    @Test
    fun loginDemoCredentials_succeedsEvenIfDatabaseNotSeededYet() = runTest {
        val result = authRepo.login(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_EMAIL, com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD)
        assertTrue(result is AppResult.Success)
        val user = (result as AppResult.Success).value
        assertEquals("Demo Reader", user.name)
        assertEquals(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_EMAIL, user.email)
        assertNotNull(session.userId.value)
    }

    @Test
    fun loginDemoCredentials_caseInsensitive() = runTest {
        val result = authRepo.login("  DEMO@PageTurner.com ", com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD)
        assertTrue(result is AppResult.Success)
        assertNotNull(session.userId.value)
    }
}
