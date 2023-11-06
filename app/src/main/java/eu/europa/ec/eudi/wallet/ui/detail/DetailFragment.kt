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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import eu.europa.ec.eudi.wallet.ui.WalletUIBaseFragment
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.databinding.FragmentDetailBinding
import eu.europa.ec.eudi.wallet.document.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailFragment : WalletUIBaseFragment() {

    private val viewModel: DetailViewModel by viewModels()

    private val arguments by navArgs<DetailFragmentArgs>()

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater)
        binding.deleteBtn.setOnClickListener {
            binding.isLoading = true
            viewModel.document.removeObservers(viewLifecycleOwner)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteDocument()
                withContext(Dispatchers.Main) {
                    binding.isLoading = false
                    findNavController().navigateUp()
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.isLoading = true
        viewModel.document.observe(viewLifecycleOwner) { doc ->
            if (doc != null) {
                DocumentDataElementParser(requireContext())
                    .read(doc.docType, doc.nameSpacedData)
                    .let {
                        updateUI(doc, it)
                    }
                binding.isLoading = false
            } else {
                showToast(getString(R.string.document_not_found))
            }
        }
        viewModel.loadDocument(arguments.documentIdentityCredentialName)

    }

    private fun updateUI(doc: Document, documentDetails: DocumentDetails) {
        with(binding) {
            docEntity = doc
            textElements.text =
                HtmlCompat.fromHtml(documentDetails.textElements, HtmlCompat.FROM_HTML_MODE_LEGACY)
            portraitImg.setImageBitmap(documentDetails.portrait)
            signatureImg.setImageBitmap(documentDetails.signature)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}