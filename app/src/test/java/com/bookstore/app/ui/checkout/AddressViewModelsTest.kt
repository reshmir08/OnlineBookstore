package com.bookstore.app.ui.checkout

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.repo.AddressRepository
import com.bookstore.app.testutil.FakeAddressDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var addressDao: FakeAddressDao
    private lateinit var session: SessionManager
    private lateinit var repo: AddressRepository

    @Before
    fun setUp() {
        addressDao = FakeAddressDao()
        session = SessionManager(initialUserId = 1L)
        repo = AddressRepository(addressDao, session)
    }

    @Test
    fun addressSelectViewModel_selectsDefaultInitially() = runTest {
        repo.save(
            AddressEntity(
                id = 0, userId = 1, label = "Home", fullName = "John Doe", phone = "9876543210",
                line1 = "123 Main St", line2 = "", city = "Bengaluru", state = "Karnataka",
                postalCode = "560001", isDefault = true
            ),
            makeDefault = true
        )
        val vm = AddressSelectViewModel(repo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(1, state.addresses.size)
        assertEquals(state.addresses.first().id, state.selectedId)

        collectJob.cancel()
    }

    @Test
    fun addressEditViewModel_validationAndSave() = runTest {
        val vm = AddressEditViewModel(AddressEditViewModel.NEW, repo)
        assertTrue(vm.isNew)

        // Try to save empty form
        vm.save()
        assertTrue(vm.errors.isNotEmpty())
        assertFalse(vm.saved)

        // Fill in valid data
        vm.onLabel("Office")
        vm.onFullName("John Doe")
        vm.onPhone("9876543210")
        vm.onLine1("Tech Park 4th Floor")
        vm.onCity("Bengaluru")
        vm.onState("Karnataka")
        vm.onPostalCode("560103")
        vm.onMakeDefault(true)

        vm.save()
        assertTrue(vm.saved)

        val saved = repo.addresses().first()
        assertEquals(1, saved.size)
        assertEquals("Office", saved.first().label)
        assertTrue(saved.first().isDefault)
    }
}
