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

package eu.europa.ec.eudi.wallet.ui.wallet

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RawRes
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.util.EuPidIssuance
import eu.europa.ec.eudi.wallet.ui.util.logError
import eu.europa.ec.eudi.web.lightIssuing.EudiPidIssuer
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import org.json.JSONObject
import java.util.Base64

class WalletViewModel(
    private val app: Application,
) : AndroidViewModel(app) {

    private val context
        get() = app.applicationContext

    val noDocuments = ObservableBoolean(false)
    val isLoading = ObservableBoolean(false)
    private val _documents = MutableLiveData<List<Document>>()
    val documents: LiveData<List<Document>>
        get() = _documents

    fun loadDocuments() {
        isLoading.set(false)
        val docs = EudiWallet.getDocuments()
        _documents.value = docs
        noDocuments.set(docs.isEmpty())
    }

    val canCreateSampleData: Boolean
        get() = true

    suspend fun issueEuPid(
        activity: ComponentActivity,
        country: EudiPidIssuer.Country,
    ): EuPidIssuance.Result {
        return EuPidIssuance.issueDocument(activity, country)
    }

    /**
     * Stores sample data from the file raw/sample_data.json
     * into the document database and identity credential api
     */
    fun createSampleData() {

        isLoading.set(true)
        when (val result = EudiWallet.loadSampleData(
            Base64.getDecoder().decode(
                JSONObject(
                    context.getStringFromRaw(R.raw.sample_data)
                ).getString("Data")
            )
        )) {
            is LoadSampleResult.Success -> {
                EudiWallet.getDocuments().let {
                    _documents.value = it
                    noDocuments.set(it.isEmpty())
                    isLoading.set(false)
                }
            }

            is LoadSampleResult.Error -> {
                logError("Error: ${result.message}")
                showError(result.message)
                isLoading.set(false)
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val app: Application,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(app) as T
        }
    }
}

private fun Context.getStringFromRaw(@RawRes resId: Int): String =
    resources.openRawResource(resId).bufferedReader().use { it.readText() }