/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkParagraphUi
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkUi
import eu.europa.ec.corelogic.controller.WalletCoreTrustMarkController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

sealed interface LoadTrustMarkPartialState {
    data class Success(val trustMark: TrustMarkUi) : LoadTrustMarkPartialState
    data class Failure(val error: String) : LoadTrustMarkPartialState
}

sealed interface CompleteTrustMarkIntroductionPartialState {
    data object Success : CompleteTrustMarkIntroductionPartialState
    data class Failure(val error: String) : CompleteTrustMarkIntroductionPartialState
}

interface TrustMarkInteractor {
    suspend fun getTrustMark(): LoadTrustMarkPartialState
    suspend fun completeIntroduction(): CompleteTrustMarkIntroductionPartialState
}

class TrustMarkInteractorImpl(
    private val walletCoreTrustMarkController: WalletCoreTrustMarkController,
    private val prefKeys: PrefKeys,
    private val resourceProvider: ResourceProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrustMarkInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override suspend fun getTrustMark(): LoadTrustMarkPartialState =
        withContext(dispatcher) {
            runCatching<LoadTrustMarkPartialState> {
                val trustMark = walletCoreTrustMarkController.getTrustMark().getOrThrow()
                LoadTrustMarkPartialState.Success(
                    trustMark = TrustMarkUi(
                        imageUrl = resolveImageUrl(trustMark.imageUrl, trustMark.resourceUrl),
                        text = trustMark.localisedText,
                        certifiedWalletsUrl = usableWebUrl(trustMark.certifiedWalletsUrl),
                        walletSolutionUrl = usableWebUrl(trustMark.walletSolutionUrl),
                        certificationDescription = linkedParagraph(
                            textRes = R.string.trust_mark_certification_description,
                            linkRes = R.string.trust_mark_certified_wallets_link,
                        ),
                        certificationInformationDescription = linkedParagraph(
                            textRes = R.string.trust_mark_certification_information_description,
                            linkRes = R.string.trust_mark_certification_information_link,
                        )
                    )
                )
            }.getOrElse {
                LoadTrustMarkPartialState.Failure(
                    error = resourceProvider.getString(R.string.trust_mark_load_error)
                )
            }
        }

    override suspend fun completeIntroduction(): CompleteTrustMarkIntroductionPartialState =
        withContext(dispatcher) {
            runCatching<CompleteTrustMarkIntroductionPartialState> {
                prefKeys.setTrustMarkIntroductionCompleted(value = true)
                CompleteTrustMarkIntroductionPartialState.Success
            }.getOrElse {
                CompleteTrustMarkIntroductionPartialState.Failure(
                    error = it.localizedMessage ?: genericErrorMsg
                )
            }
        }

    private fun linkedParagraph(textRes: Int, linkRes: Int): TrustMarkParagraphUi {
        val label = resourceProvider.getString(linkRes)
        val text = resourceProvider.getString(textRes, label)
        val start = text.indexOf(label)
        return TrustMarkParagraphUi(
            text = text,
            linkRange = if (label.isNotBlank() && start >= 0) {
                start until start + label.length
            } else {
                null
            }
        )
    }

    private fun resolveImageUrl(imageUrl: String, resourceUrl: String): String? {
        if (imageUrl.isBlank()) return null
        val resolved = runCatching {
            URI(resourceUrl).resolve(imageUrl).toString()
        }.getOrNull()
        return resolved?.let { url -> usableWebUrl(value = url) }
    }

    private fun usableWebUrl(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        return value.takeIf {
            (uri.scheme.equals("https", ignoreCase = true)
                    || uri.scheme.equals("http", ignoreCase = true))
                    && !uri.host.isNullOrBlank()
                    && uri.rawUserInfo == null
        }
    }
}