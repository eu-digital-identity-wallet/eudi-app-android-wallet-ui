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

package eu.europa.ec.businesslogic.controller.walletcore

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Controller for interacting with internal local storage of Core for CRUD operations on documents
 * */
interface WalletCoreDocumentsController {
    /**
     * Load sample document data taken from raw.xml
     * */
    fun loadSampleData(sampleDataByteArray: ByteArray): Flow<LoadSampleDataPartialState>

    /**
     * @return Sample documents from Database
     * */
    fun getSampleDocuments(): List<Document>

    fun getDocumentById(id: String): Document?
}

class WalletCoreDocumentsControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet
) : WalletCoreDocumentsController {

    private val genericErrorMessage
        get() = resourceProvider.genericErrorMessage()

    override fun loadSampleData(sampleDataByteArray: ByteArray): Flow<LoadSampleDataPartialState> =
        flow {
            when (val result = eudiWallet.loadSampleData(sampleDataByteArray)) {
                is LoadSampleResult.Error -> emit(LoadSampleDataPartialState.Failure(result.message))
                is LoadSampleResult.Success -> emit(LoadSampleDataPartialState.Success)
            }
        }.safeAsync {
            LoadSampleDataPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
        }

    override fun getSampleDocuments(): List<Document> = eudiWallet.getDocuments()

    override fun getDocumentById(id: String): Document? {
        return eudiWallet.getDocumentById(documentId = id)
    }
}