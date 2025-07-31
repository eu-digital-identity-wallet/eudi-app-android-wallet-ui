/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.issuancefeature.ui.add.issuer

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.content.edit

const val ISSUER_CRUD_PREFS = "IssuerCrudPrefs" // TODO share with WalletCoreConfigImpl
const val selectedIssuerKey = "selected_issuer" // TODO share with WalletCoreConfigImpl
private const val separator = ":!:"

// TODO use ModelViewInteractor
class IssuerViewModel(context: Context) {
    private val sharedPrefs = context.getSharedPreferences(ISSUER_CRUD_PREFS, Context.MODE_PRIVATE)
    private val issuerSetKey = "issuer_set"

    var items: SnapshotStateList<Pair<String, String>> = loadItems().toMutableStateList()
    var selectedItem by mutableStateOf(sharedPrefs.getString(selectedIssuerKey, null))

    private fun loadItems(): List<Pair<String, String>> {
        val stringSet = sharedPrefs.getStringSet(issuerSetKey, setOf()) ?: setOf()
        return stringSet.map {
            val parts = it.split(separator, limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }.filter { it.first.isNotBlank() }
    }

    private fun saveItems(items: List<Pair<String, String>>) {
        val stringSet = items.map { "${it.first}$separator${it.second}" }.toSet()
        sharedPrefs.edit() { putStringSet(issuerSetKey, stringSet) }
    }

    fun addItem(key: String, value: String) {
        if (key.isBlank()) return
        items.add(key to value)
        saveItems(items)
    }

    fun updateItem(index: Int, newKey: String, newValue: String) {
        if (newKey.isBlank()) return
        items[index] = newKey to newValue
        saveItems(items)
    }

    fun deleteItem(index: Int) {
        // If deleting the selected item, clear selection
        if (items[index].first == selectedItem) {
            selectedItem = null
            sharedPrefs.edit() { remove(selectedIssuerKey) }
        }
        items.removeAt(index)
        saveItems(items)
    }

    fun selectItem(key: String, address: String) {
        selectedItem = key
        sharedPrefs.edit() { putString(selectedIssuerKey, address) }
    }
}