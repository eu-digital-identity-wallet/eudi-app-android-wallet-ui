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

package eu.europa.ec.eudi.wallet.ui.selectivedisclosure

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.theme.WalletUIAppTheme
import eu.europa.ec.eudi.wallet.ui.transfer.SendDocumentsResult
import eu.europa.ec.eudi.wallet.ui.transfer.TransferViewModel
import eu.europa.ec.eudi.wallet.ui.userauth.UserAuthPromptBuilder
import eu.europa.ec.eudi.wallet.ui.util.log
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.Document

class SelectiveDisclosureFragment : BottomSheetDialogFragment() {

    private val transferViewModel: TransferViewModel by activityViewModels()
    private var isSendingInProgress = mutableStateOf(false)
    private val args by navArgs<SelectiveDisclosureFragmentArgs>()
    private val requestDocuments
        get() = args.request.documents

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val sheetData = mapToSelectiveDisclosureSheetData(requestDocuments)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WalletUIAppTheme {
                    SelectiveDisclosureSheet(
                        modifier = Modifier.fillMaxSize(),
                        title = getSubtitle(),
                        isTrustedReader = requestDocuments.first().docRequest.readerAuth?.isSuccess() == true,
                        isSendingInProgress = isSendingInProgress.value,
                        sheetData = sheetData,
                        onDocItemToggled = { credentialName, element ->
                            transferViewModel.toggleDocItem(
                                credentialName, element
                            )
                        },
                        onConfirm = { sendResponse() },
                        onCancel = {
                            dismiss()
                            cancelAuthorization()
                        }
                    )
                }
            }
        }
    }

    private fun mapToSelectiveDisclosureSheetData(
        elementsToSelect: List<RequestDocument>,
    ): List<SelectiveDisclosureSheetData> {
        return elementsToSelect.map { documentData ->
            val doc: Document? = EudiWallet.getDocumentById(documentData.documentId)
            transferViewModel.addDocumentForSelection(documentData)
            val elements = documentData.docRequest.requestItems.map { element ->
                transferViewModel.toggleDocItem(documentData.documentId, element)
                val displayName = stringValueFor(element.elementIdentifier)
                val isPresent =
                    doc?.nameSpaces?.get(element.namespace)?.contains(element.elementIdentifier)
                        ?: false
                SelectiveDisclosureSheetData.DocumentItem(displayName, element, isPresent)
            }
            SelectiveDisclosureSheetData(
                documentData.documentId,
                documentData.docName,
                elements
            )
        }
    }

    private fun authenticationSucceeded() {
        try {
            transferViewModel.updateUserAuthStatus(UserAuthStatus.SUCCESS)
            findNavController().navigateUp()
        } catch (e: Exception) {
            val message = "Send response error: ${e.message}"
            log(message, e)
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        cancelAuthorization()
    }

    private fun cancelAuthorization() {
        transferViewModel.updateUserAuthStatus(UserAuthStatus.CANCELED)
    }

    private fun authenticationFailed() {
        transferViewModel.updateUserAuthStatus(UserAuthStatus.FAIL)
    }

    private fun sendResponse() {
        isSendingInProgress.value = true

        when (val result = transferViewModel.sendDocuments()) {
            is SendDocumentsResult.UserAuthRequired ->
                requestUserAuth(false, result.cryptoObject)

            is SendDocumentsResult.Success -> findNavController().navigateUp()

            is SendDocumentsResult.Failure -> {
                context?.let {
                    Toast.makeText(it,
                        getString(R.string.error_sending_response), Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            }
        }
    }

    private fun requestUserAuth(forceLskf: Boolean, cryptoObject: BiometricPrompt.CryptoObject?) {
        val userAuthRequest = UserAuthPromptBuilder.requestUserAuth(this)
            .withTitle(getString(R.string.bio_auth_title))
            .withNegativeButton(getString(R.string.bio_auth_use_pin))
            .withSuccessCallback { authenticationSucceeded() }
            .withCancelledCallback { retryForcingPinUse(cryptoObject) }
            .withFailureCallback { authenticationFailed() }
            .setForceLskf(forceLskf)
            .build()
        userAuthRequest.authenticate(cryptoObject)
    }

    private fun retryForcingPinUse(cryptoObject: BiometricPrompt.CryptoObject?) {
        val runnable = { requestUserAuth(true, cryptoObject) }
        // Without this delay, the prompt won't reshow
        Handler(Looper.getMainLooper()).postDelayed(runnable, 100)
    }

    private fun getSubtitle(): String {
        val readerCommonName = requestDocuments
            .first().docRequest.readerAuth?.readerCommonName
        val readerIsTrusted = (true == requestDocuments
            .first().docRequest.readerAuth?.isSuccess())
        return if (readerCommonName.isNullOrEmpty()) {
            getString(R.string.reader_auth_verifier_anonymous)
        } else {
            if (readerIsTrusted) {
                getString(R.string.reader_auth_verifier_trusted_with_name, readerCommonName)
            } else {
                getString(R.string.reader_auth_verifier_untrusted_with_name, readerCommonName)
            }
        }
    }

    private fun stringValueFor(element: String): String {
        val identifier = resources.getIdentifier(element, "string", requireContext().packageName)
        return if (identifier != 0) getString(identifier) else element
    }
}