package com.bookstore.app.data.repo

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressDao
import com.bookstore.app.data.local.AddressEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class AddressRepository(
    private val addressDao: AddressDao,
    private val session: SessionManager
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun addresses(): Flow<List<AddressEntity>> =
        session.userId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else addressDao.addresses(id) }

    suspend fun find(id: Long): AddressEntity? =
        session.userId.value?.let { addressDao.find(id, it) }

    /** Saves a new or edited address. The first address a customer adds becomes the default. */
    suspend fun save(address: AddressEntity, makeDefault: Boolean): AppResult<Long> {
        val userId = session.userId.value ?: return AppResult.Failure("Please sign in first")
        val toSave = address.copy(userId = userId)
        val id = if (toSave.id == 0L) {
            addressDao.insert(toSave.copy(isDefault = false))
        } else {
            val existing = addressDao.find(toSave.id, userId) ?: return AppResult.Failure("Address not found")
            addressDao.update(toSave.copy(isDefault = existing.isDefault))
            toSave.id
        }
        if (makeDefault || addressDao.count(userId) == 1) setDefault(id)
        return AppResult.Success(id)
    }

    suspend fun setDefault(id: Long) {
        val userId = session.userId.value ?: return
        addressDao.clearDefault(userId)
        addressDao.markDefault(id, userId)
    }

    suspend fun delete(id: Long) {
        val userId = session.userId.value ?: return
        val wasDefault = addressDao.find(id, userId)?.isDefault == true
        addressDao.delete(id, userId)
        if (wasDefault) addressDao.first(userId)?.let { setDefault(it.id) }
    }
}
