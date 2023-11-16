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

package eu.europa.ec.eudi.wallet.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.europa.ec.eudi.wallet.ui.util.log
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeleteDocumentResult
import eu.europa.ec.eudi.wallet.document.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DetailViewModel"

class DetailViewModel : ViewModel() {

    private val _document = MutableLiveData<Document?>()
    val document: LiveData<Document?>
        get() = _document

    fun loadDocument(documentId: String) {
        val doc = EudiWallet.getDocumentById(documentId)
        viewModelScope.launch(Dispatchers.Main) { _document.value = doc }
    }

    fun deleteDocument() {
        when (val result = document.value?.let {
            EudiWallet.deleteDocumentById(it.id)
        }) {
            is DeleteDocumentResult.Success -> viewModelScope.launch(Dispatchers.Main) {
                _document.value = null
            }

            is DeleteDocumentResult.Failure -> log(
                "Error deleting document",
                result.throwable
            )

            else -> log("No document to delete")

        }
    }
}