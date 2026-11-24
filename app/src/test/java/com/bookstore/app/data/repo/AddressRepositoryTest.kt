package com.bookstore.app.data.repo

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.testutil.FakeAddressDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddressRepositoryTest {
    private lateinit var addressDao: FakeAddressDao
    private lateinit var session: SessionManager
    private lateinit var repo: AddressRepository

    private fun sampleAddress(label: String = "Home") = AddressEntity(
        id = 0, userId = 10, label = label, fullName = "Test User", phone = "9876543210",
        line1 = "123 Main St", line2 = "Apt 4B", city = "Bengaluru", state = "Karnataka",
        postalCode = "560001", isDefault = false
    )

    @Before
    fun setUp() {
        addressDao = FakeAddressDao()
        session = SessionManager(initialUserId = 10L)
        repo = AddressRepository(addressDao, session)
    }

    @Test
    fun signedOut_saveFails() = runTest {
        session.signOut()
        val result = repo.save(sampleAddress(), false)
        assertTrue(result is AppResult.Failure)
        assertEquals("Please sign in first", (result as AppResult.Failure).message)
    }

    @Test
    fun firstAddress_becomesDefaultAutomatically() = runTest {
        val result = repo.save(sampleAddress("Home"), makeDefault = false)
        assertTrue(result is AppResult.Success)
        val id = (result as AppResult.Success).value

        val saved = repo.find(id)
        assertNotNull(saved)
        assertTrue(saved!!.isDefault)
    }

    @Test
    fun secondAddress_withMakeDefaultTrue_becomesDefault() = runTest {
        val res1 = repo.save(sampleAddress("Home"), false)
        val id1 = (res1 as AppResult.Success).value

        val res2 = repo.save(sampleAddress("Office"), makeDefault = true)
        val id2 = (res2 as AppResult.Success).value

        val addr1 = repo.find(id1)
        val addr2 = repo.find(id2)

        assertFalse(addr1!!.isDefault)
        assertTrue(addr2!!.isDefault)
    }

    @Test
    fun editExistingAddress_updatesDetails() = runTest {
        val res = repo.save(sampleAddress("Home"), false)
        val id = (res as AppResult.Success).value

        val existing = repo.find(id)!!
        val updated = existing.copy(fullName = "Updated Name", city = "Mysuru")
        val editRes = repo.save(updated, false)
        assertTrue(editRes is AppResult.Success)

        val retrieved = repo.find(id)!!
        assertEquals("Updated Name", retrieved.fullName)
        assertEquals("Mysuru", retrieved.city)
    }

    @Test
    fun deleteDefaultAddress_promotesNextAddressToDefault() = runTest {
        val id1 = (repo.save(sampleAddress("Home"), false) as AppResult.Success).value
        val id2 = (repo.save(sampleAddress("Office"), false) as AppResult.Success).value

        // id1 was default
        assertTrue(repo.find(id1)!!.isDefault)
        assertFalse(repo.find(id2)!!.isDefault)

        // delete id1
        repo.delete(id1)
        assertNull(repo.find(id1))

        // id2 should now be default
        val remaining = repo.addresses().first()
        assertEquals(1, remaining.size)
        assertTrue(remaining.first().isDefault)
    }
}
