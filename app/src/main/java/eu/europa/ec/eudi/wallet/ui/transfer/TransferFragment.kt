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

package eu.europa.ec.eudi.wallet.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import eu.europa.ec.eudi.wallet.ui.WalletUIBaseFragment
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.databinding.FragmentTransferBinding
import eu.europa.ec.eudi.wallet.ui.selectivedisclosure.UserAuthStatus
import eu.europa.ec.eudi.wallet.ui.util.log
import eu.europa.ec.eudi.iso18013.transfer.Request
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent

class TransferFragment : WalletUIBaseFragment() {

    companion object {
        const val CLOSE_AFTER_RESPONSE_KEY = "closeAfterResponse"
        const val OPENID4VP_URI_KEY = "openId4VpUri"
    }

    private val transferViewModel: TransferViewModel by activityViewModels()

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!

    private val closeAfterResponse by lazy {
        arguments?.getBoolean(CLOSE_AFTER_RESPONSE_KEY, false) ?: false
    }

    private val openId4VpURI by lazy {
        arguments?.getString(OPENID4VP_URI_KEY, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferBinding.inflate(inflater)
        binding.fragment = this
        binding.vm = transferViewModel

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closeConnection()
                }
            })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        openId4VpURI?.let {
            transferViewModel.handleOpenId4Vp(it)
        }

        transferViewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is TransferEvent.QrEngagementReady -> log("Engagement Ready")
                is TransferEvent.Connected -> log("Connected")
                is TransferEvent.RequestReceived -> onTransferRequested(event.request)
                is TransferEvent.ResponseSent -> showToast(getString(R.string.response_has_been_sent))
                is TransferEvent.Disconnected -> onTransferDisconnected()
                is TransferEvent.Error -> onTransferError(event.error)
                else -> {
                    log("Transfer event: $event")
                }
            }
        }
        transferViewModel.userAuthStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                UserAuthStatus.CANCELED -> {
                    // on auth cancel close the connection
                    transferViewModel.onUserAuthConsumed()
                    closeConnection()
                }

                UserAuthStatus.SUCCESS -> {
                    // on success authentication
                    // send selected items response
                    transferViewModel.onUserAuthConsumed()
                    transferViewModel.sendDocuments()
                }

                UserAuthStatus.FAIL -> {
                    // on auth fail close the connection
                    showToast(getString(R.string.bio_auth_failed))
                    transferViewModel.onUserAuthConsumed()
                    closeConnection()
                }

                else -> {
                    log("User auth status: $status")
                }
            }
        }
    }

    // on request received
    private fun onTransferRequested(requestedDocumentData: Request) {
        log("Request received")
        binding.txtConnectionStatus.append("\n" + getString(R.string.request_received_please_wait))
        try {
            // get the Requested Document Data
            if (requestedDocumentData.documents.isEmpty()) {
                // if no documents found to return - send a response with no documents
                transferViewModel.sendDocuments()
                showToast("No documents found to return.")
            } else {
                // show auth confirm dialog: user will be authenticated (if required) and select the response items
                // for each document
                findNavController().navigate(
                    TransferFragmentDirections.navigateToSelectiveDisclosure(
                        requestedDocumentData
                    )
                )
            }
        } catch (e: Exception) {
            showToast("On request received error: ${e.message}")
            binding.txtConnectionStatus.append("\n${e.message}")
        }
    }

    // on connection lost
    private fun onTransferDisconnected() {
        showToast("Disconnected.")
        closeConnection()
    }

    // on transfer error
    private fun onTransferError(error: Throwable) {
        showToast("An error occurred.")
        log("Transfer error: ${error.message}", error)
        closeConnection()
    }

    fun closeConnection() {
        transferViewModel.cancelPresentation()
        hideButtons()
        findNavController().navigateUp()
        if (closeAfterResponse) {
            requireActivity().finishAndRemoveTask()
        }
    }

    private fun hideButtons() {
        binding.txtConnectionStatus.text = getString(R.string.connection_mdoc_closed)
        binding.btClose.visibility = View.GONE
        binding.btOk.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}