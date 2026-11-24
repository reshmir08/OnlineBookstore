package com.bookstore.app.data.auth

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.core.util.PasswordHasher
import com.bookstore.app.data.local.UserDao
import com.bookstore.app.data.local.UserEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    private val session: SessionManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    val currentUserId = session.userId

    suspend fun currentUser(): UserEntity? = session.userId.value?.let { userDao.findById(it) }

    suspend fun register(name: String, email: String, password: String): AppResult<UserEntity> =
        withContext(dispatcher) {
            val normalizedEmail = email.trim().lowercase()
            if (userDao.findByEmail(normalizedEmail) != null) {
                return@withContext AppResult.Failure("An account with this email already exists")
            }
            val salt = PasswordHasher.newSalt()
            val user = UserEntity(
                name = name.trim(),
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                createdAt = nowMillis()
            )
            try {
                val id = userDao.insert(user)
                session.signIn(id)
                AppResult.Success(user.copy(id = id))
            } catch (e: Exception) {
                AppResult.Failure("An account with this email already exists")
            }
        }

    suspend fun login(email: String, password: String): AppResult<UserEntity> =
        withContext(dispatcher) {
            val normalizedEmail = email.trim().lowercase()
            var user = userDao.findByEmail(normalizedEmail)

            // Auto-provision or reconcile demo user on login to prevent startup race conditions
            if (normalizedEmail == com.bookstore.app.data.seed.DatabaseSeeder.DEMO_EMAIL.lowercase()) {
                if (user == null) {
                    val salt = PasswordHasher.newSalt()
                    val demoUser = UserEntity(
                        name = "Demo Reader",
                        email = com.bookstore.app.data.seed.DatabaseSeeder.DEMO_EMAIL,
                        passwordHash = PasswordHasher.hash(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD, salt),
                        salt = salt,
                        createdAt = nowMillis()
                    )
                    try {
                        val id = userDao.insert(demoUser)
                        user = demoUser.copy(id = id)
                    } catch (e: Exception) {
                        user = userDao.findByEmail(normalizedEmail)
                    }
                } else if (password == com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD &&
                    !PasswordHasher.verify(password, user.salt, user.passwordHash)
                ) {
                    val salt = PasswordHasher.newSalt()
                    val updated = user.copy(
                        passwordHash = PasswordHasher.hash(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD, salt),
                        salt = salt
                    )
                    userDao.update(updated)
                    user = updated
                }
            }

            // Identical message for unknown email and wrong password to avoid account enumeration.
            if (user == null || !PasswordHasher.verify(password, user.salt, user.passwordHash)) {
                return@withContext AppResult.Failure("Incorrect email or password")
            }
            session.signIn(user.id)
            AppResult.Success(user)
        }

    fun logout() = session.signOut()
}
