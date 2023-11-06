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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import eu.europa.ec.eudi.wallet.ui.WalletUIBaseFragment
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.databinding.FragmentWalletBinding
import eu.europa.ec.eudi.wallet.ui.permissions.BlePermissionsFragment
import eu.europa.ec.eudi.wallet.ui.util.EuPidIssuance
import eu.europa.ec.eudi.wallet.ui.util.log
import eu.europa.ec.eudi.wallet.ui.util.logError
import eu.europa.ec.eudi.wallet.ui.wallet.ui.DocumentAdapter
import eu.europa.ec.eudi.web.lightIssuing.EudiPidIssuer
import eu.europa.ec.eudi.wallet.EudiWalletSDK
import eu.europa.ec.eudi.wallet.document.Document
import kotlinx.coroutines.launch
import javax.crypto.AEADBadTagException

class WalletFragment : WalletUIBaseFragment() {

    private val viewModel: WalletViewModel by viewModels {
        WalletViewModel.Factory(requireActivity().application)
    }

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater)

        val docAdapter = DocumentAdapter()

        try {

            viewModel.documents.observe(viewLifecycleOwner) { documents ->
                updateUI(docAdapter, documents)
            }
            viewModel.loadDocuments()

            binding.viewModel = viewModel

            binding.rvDocuments.apply {
                this.adapter = docAdapter
                layoutManager = LinearLayoutManager(context)
            }
            binding.btnShare.setOnClickListener {
                // check for ble permissions
                BlePermissionsFragment.showDialog(
                    parentFragmentManager,
                    centralModeEnabled = EudiWalletSDK.config.bleCentralClientModeEnabled,
                    onSuccess = {
                        // when ble permissions have been enabled go to share screen
                        findNavController().navigate(R.id.action_WalletFragment_to_ShareFragment)
                    },
                    onCancelled = {})
            }
            if (viewModel.canCreateSampleData) {
                binding.btnAddSampleDocuments.visibility = View.VISIBLE
                binding.btnAddSampleDocuments.setOnClickListener { viewModel.createSampleData() }
            } else {
                binding.btnAddSampleDocuments.visibility = View.GONE
            }
        } catch (e: Exception) {
            when (e) {
                // Handle exception by users
                is AEADBadTagException -> showToast("Invalid passcode. Please try again!")
                else -> showToast("Error. Please try again later.")
            }
            requireContext().logError(e.printStackTrace().toString())
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnIssueUtopiaPid.setOnClickListener {
            lifecycleScope.launch {
                when (val r = viewModel.issueEuPid(requireActivity(), EudiPidIssuer.Country.FC)) {
                    is EuPidIssuance.Result.Failure -> {
                        log("Failed to issue document", r.throwable)
                        showToast("Failed to issue document\n${r.throwable.message}")
                    }

                    is EuPidIssuance.Result.Success -> viewModel.loadDocuments()
                }
            }
        }
        binding.btnIssuePortugalPid.setOnClickListener {
            lifecycleScope.launch {
                when (val r = viewModel.issueEuPid(requireActivity(), EudiPidIssuer.Country.PT)) {
                    is EuPidIssuance.Result.Failure -> {
                        log("Failed to issue document", r.throwable)
                        showToast("Failed to issue document\n${r.throwable.message}")
                    }

                    is EuPidIssuance.Result.Success -> viewModel.loadDocuments()
                }
            }
        }
        binding.btnIssueEidasPid.setOnClickListener {
            lifecycleScope.launch {
                when (val r = viewModel.issueEuPid(requireActivity(), EudiPidIssuer.Country.CW)) {
                    is EuPidIssuance.Result.Failure -> {
                        log("Failed to issue document", r.throwable)
                        showToast("Failed to issue document\n${r.throwable.message}")
                    }

                    is EuPidIssuance.Result.Success -> viewModel.loadDocuments()
                }
            }
        }
    }

    private fun updateUI(
        adapter: DocumentAdapter,
        documentsList: List<Document>,
    ) {
        if (documentsList.isEmpty()) {
            viewModel.noDocuments.set(true)
        } else {
            adapter.submitList(documentsList)
            viewModel.noDocuments.set(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}