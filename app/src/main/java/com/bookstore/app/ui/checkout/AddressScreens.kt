package com.bookstore.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.AppUi
import com.bookstore.app.ui.common.LocalAppUi
import com.bookstore.app.ui.common.BackTopBar
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.FormField
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.MessageView
import com.bookstore.app.ui.theme.BookstoreTheme

/** Checkout step 1: choose, add, edit or delete a delivery address. */
@Composable
fun AddressSelectScreen(
    onBack: () -> Unit,
    onAddOrEdit: (Long) -> Unit,
    onContinue: (Long) -> Unit
) {
    val vm = appViewModel { AddressSelectViewModel(it.addressRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)
    AddressSelectContent(
        state = state,
        onBack = onBack,
        onAddOrEdit = onAddOrEdit,
        onContinue = onContinue,
        onSelect = vm::select,
        onDelete = vm::delete,
        onMakeDefault = vm::makeDefault
    )
}

@Composable
private fun AddressSelectContent(
    state: AddressListState,
    onBack: () -> Unit,
    onAddOrEdit: (Long) -> Unit,
    onContinue: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    onDelete: (AddressEntity) -> Unit,
    onMakeDefault: (AddressEntity) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<AddressEntity?>(null) }

    Scaffold(
        topBar = { BackTopBar("Delivery address", onBack) },
        snackbarHost = { AppSnackbarHost() },
        bottomBar = {
            state.selected?.let { selected ->
                Surface(tonalElevation = 3.dp, shadowElevation = 6.dp, modifier = Modifier.padding(bottom = 46.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Delivering to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${selected.fullName} · ${selected.label}", fontWeight = FontWeight.SemiBold)
                        Text(selected.formatted(), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onContinue(selected.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("Continue to payment")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            state.addresses.isEmpty() -> MessageView(
                "No saved addresses", "Add a delivery address to continue.", Icons.Filled.LocationOn,
                Modifier.padding(padding), "Add address", { onAddOrEdit(AddressEditViewModel.NEW) }
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.addresses, key = { it.id }) { address ->
                    AddressCard(
                        address = address,
                        selected = address.id == state.selectedId,
                        onSelect = { onSelect(address.id) },
                        onEdit = { onAddOrEdit(address.id) },
                        onDelete = { pendingDelete = address },
                        onMakeDefault = { onMakeDefault(address) }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { onAddOrEdit(AddressEditViewModel.NEW) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add a new address")
                    }
                }
            }
        }
    }

    pendingDelete?.let { address ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete address?") },
            text = { Text("\"${address.label}\" will be removed from your saved addresses.") },
            confirmButton = { TextButton(onClick = { onDelete(address); pendingDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddressSelectScreenPreview() {
    val addresses = listOf(
        AddressEntity(
            id = 1, userId = 1, label = "Home", fullName = "Asha Nair", phone = "9876543210",
            line1 = "12 MG Road", line2 = "Near City Park", city = "Kochi", state = "Kerala",
            postalCode = "682001", isDefault = true
        ),
        AddressEntity(
            id = 2, userId = 1, label = "Office", fullName = "Asha Nair", phone = "9876501234",
            line1 = "5th Floor, Tech Park", line2 = "", city = "Bengaluru", state = "Karnataka",
            postalCode = "560001", isDefault = false
        )
    )
    CompositionLocalProvider(
        LocalAppUi provides AppUi(remember { SnackbarHostState() }, openBasket = {}, logout = {})
    ) {
        BookstoreTheme {
            AddressSelectContent(
                state = AddressListState(loading = false, addresses = addresses, selectedId = 1),
                onBack = {},
                onAddOrEdit = {},
                onContinue = {},
                onSelect = {},
                onDelete = {},
                onMakeDefault = {}
            )
        }
    }
}

@Composable
private fun AddressCard(
    address: AddressEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMakeDefault: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(12.dp)) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(address.label, fontWeight = FontWeight.Bold)
                    if (address.isDefault) AssistChip(onClick = {}, label = { Text("Default") }, enabled = false)
                }
                Text(address.fullName)
                Text(address.formatted(), style = MaterialTheme.typography.bodyMedium)
                Text("Phone: ${address.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                    if (!address.isDefault) TextButton(onClick = onMakeDefault) { Text("Set default") }
                }
            }
        }
    }
}

/** Add a new address, or edit an existing one when [addressId] is non-zero. */
@Composable
fun AddressEditScreen(addressId: Long, onDone: () -> Unit) {
    val vm = appViewModel { AddressEditViewModel(addressId, it.addressRepository) }
    CollectMessages(vm.messages)
    LaunchedEffect(vm.saved) { if (vm.saved) onDone() }

    Scaffold(
        topBar = { BackTopBar(if (vm.isNew) "Add address" else "Edit address", onDone) },
        snackbarHost = { AppSnackbarHost() }
    ) { padding ->
        when {
            vm.loading -> LoadingView(Modifier.padding(padding))
            vm.notFound -> MessageView(
                "Address not found", "This address may have been deleted.", Icons.Filled.LocationOn,
                Modifier.padding(padding), "Go back", onDone
            )
            else -> Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FormField(vm.label, vm::onLabel, "Label (e.g. Home, Office)", error = vm.errors["label"])
                    FormField(vm.fullName, vm::onFullName, "Full name", error = vm.errors["fullName"])
                    FormField(vm.phone, vm::onPhone, "Phone number", error = vm.errors["phone"], keyboardType = KeyboardType.Phone)
                    FormField(vm.line1, vm::onLine1, "Address line 1", error = vm.errors["line1"])
                    FormField(vm.line2, vm::onLine2, "Address line 2 (optional)")
                    FormField(vm.city, vm::onCity, "City", error = vm.errors["city"])
                    FormField(vm.state, vm::onState, "State", error = vm.errors["state"])
                    FormField(vm.postalCode, vm::onPostalCode, "PIN code", error = vm.errors["postalCode"], keyboardType = KeyboardType.Number)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(vm.makeDefault, vm::onMakeDefault)
                        Text("Make this my default address")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::save, enabled = !vm.saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (vm.saving) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Save address")
                    }
                }
            }
        }
    }
}
