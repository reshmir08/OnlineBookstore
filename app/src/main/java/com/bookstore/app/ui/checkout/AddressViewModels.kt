package com.bookstore.app.ui.checkout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.core.util.Validators
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.repo.AddressRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddressListState(
    val loading: Boolean = true,
    val addresses: List<AddressEntity> = emptyList(),
    val selectedId: Long? = null
) {
    val selected: AddressEntity? get() = addresses.firstOrNull { it.id == selectedId }
}

class AddressSelectViewModel(private val repo: AddressRepository) : BaseViewModel() {
    private val picked = MutableStateFlow<Long?>(null)

    /** Falls back to the default (then first) address until the customer picks one. */
    val state: StateFlow<AddressListState> = combine(repo.addresses(), picked) { list, pick ->
        val selected = list.firstOrNull { it.id == pick }
            ?: list.firstOrNull { it.isDefault }
            ?: list.firstOrNull()
        AddressListState(loading = false, addresses = list, selectedId = selected?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddressListState())

    fun select(id: Long) { picked.value = id }

    fun delete(address: AddressEntity) {
        viewModelScope.launch {
            repo.delete(address.id)
            say("Address deleted")
        }
    }

    fun makeDefault(address: AddressEntity) {
        viewModelScope.launch {
            repo.setDefault(address.id)
            say("\"${address.label}\" is now your default address")
        }
    }
}

class AddressEditViewModel(private val addressId: Long, private val repo: AddressRepository) : BaseViewModel() {
    var label by mutableStateOf("Home"); private set
    var fullName by mutableStateOf(""); private set
    var phone by mutableStateOf(""); private set
    var line1 by mutableStateOf(""); private set
    var line2 by mutableStateOf(""); private set
    var city by mutableStateOf(""); private set
    var state by mutableStateOf(""); private set
    var postalCode by mutableStateOf(""); private set
    var makeDefault by mutableStateOf(false); private set

    var errors by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var loading by mutableStateOf(addressId != NEW); private set
    var saving by mutableStateOf(false); private set
    var saved by mutableStateOf(false); private set
    var notFound by mutableStateOf(false); private set

    val isNew get() = addressId == NEW

    init {
        if (!isNew) {
            viewModelScope.launch {
                val existing = repo.find(addressId)
                if (existing == null) notFound = true else {
                    label = existing.label; fullName = existing.fullName; phone = existing.phone
                    line1 = existing.line1; line2 = existing.line2; city = existing.city
                    state = existing.state; postalCode = existing.postalCode; makeDefault = existing.isDefault
                }
                loading = false
            }
        }
    }

    private fun clear(field: String) { if (field in errors) errors = errors - field }

    fun onLabel(v: String) { label = v; clear("label") }
    fun onFullName(v: String) { fullName = v; clear("fullName") }
    fun onPhone(v: String) { phone = v; clear("phone") }
    fun onLine1(v: String) { line1 = v; clear("line1") }
    fun onLine2(v: String) { line2 = v }
    fun onCity(v: String) { city = v; clear("city") }
    fun onState(v: String) { state = v; clear("state") }
    fun onPostalCode(v: String) { postalCode = v.filter { it.isDigit() }.take(6); clear("postalCode") }
    fun onMakeDefault(v: Boolean) { makeDefault = v }

    fun save() {
        if (saving) return
        val found = buildMap {
            Validators.required(label, "Label")?.let { put("label", it) }
            Validators.name(fullName)?.let { put("fullName", it) }
            Validators.phone(phone)?.let { put("phone", it) }
            Validators.required(line1, "Address line")?.let { put("line1", it) }
            Validators.required(city, "City")?.let { put("city", it) }
            Validators.required(state, "State")?.let { put("state", it) }
            Validators.postalCode(postalCode)?.let { put("postalCode", it) }
        }
        errors = found
        if (found.isNotEmpty()) return
        saving = true
        viewModelScope.launch {
            val entity = AddressEntity(
                id = if (isNew) 0 else addressId, userId = 0, label = label.trim(), fullName = fullName.trim(),
                phone = phone.trim(), line1 = line1.trim(), line2 = line2.trim(), city = city.trim(),
                state = state.trim(), postalCode = postalCode.trim(), isDefault = false
            )
            when (val result = repo.save(entity, makeDefault)) {
                is AppResult.Success -> saved = true
                is AppResult.Failure -> say(result.message)
            }
            saving = false
        }
    }

    companion object {
        const val NEW = 0L
    }
}
