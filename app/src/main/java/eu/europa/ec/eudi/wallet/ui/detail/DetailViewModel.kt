/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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