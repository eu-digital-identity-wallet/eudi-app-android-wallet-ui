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

package eu.europa.ec.eudi.wallet.ui.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import eu.europa.ec.eudi.wallet.ui.WalletUIBaseFragment
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.databinding.FragmentShareBinding
import eu.europa.ec.eudi.wallet.ui.util.NfcEngagementServiceImpl
import eu.europa.ec.eudi.wallet.ui.util.log
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService

class ShareFragment : WalletUIBaseFragment() {

    private val viewModel: ShareViewModel by viewModels()

    private var _binding: FragmentShareBinding? = null
    private val binding get() = _binding!!

    private var showQrDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentShareBinding.inflate(inflater)
        binding.apply {
            fragment = this@ShareFragment
            vm = viewModel
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onCancel()
                }
            })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.message.set(getString(R.string.scan_qr_code_with_mdoc_verifier_device))
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is TransferEvent.QrEngagementReady -> {
                    // when qr engagement is ready, show the qr code view on the screen
                    // calculate the qr code according to the available width
                    viewModel.message.set(getString(R.string.scan_qr_code_with_mdoc_verifier_device_or_tap_for_nfc))
                    showQrDialog = AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.qr_device_engagement))
                        .setMessage(event.qrCode.content)
                        .create()
                    binding.layoutEngagement.doOnLayout {
                        viewModel.showQrCode(event.qrCode.asView(requireContext(), it.width))
                    }
                }

                is TransferEvent.Connected -> {
                    // when connect to the reader device go to the transfer fragment
                    viewModel.message.set(getString(R.string.connected))
                    showQrDialog = null
                    findNavController().navigate(R.id.action_ShareFragment_to_transferFragment)
                }

                is TransferEvent.Error -> {
                    log("Error on presentation!", event.error)
                    viewModel.message.set(getString(R.string.error_on_presentation))
                }

                else -> {
                    log("Transfer event: $event")
                }
            }
        }

        // call startQrEngagement(), when the qr code become available
        // the status 'TransferStatus.QR_ENGAGEMENT_READY' will be returned
        viewModel.startQrEngagement()
    }

    fun showQRDeviceEngagementContent() {
        showQrDialog?.show()
    }

    fun onCancel() {
        viewModel.cancelPresentation()
        findNavController().navigateUp()
    }

    override fun onResume() {
        super.onResume()
        NfcEngagementService.enable(requireActivity(), NfcEngagementServiceImpl::class.java)
    }

    override fun onPause() {
        super.onPause()
        NfcEngagementService.disable(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}