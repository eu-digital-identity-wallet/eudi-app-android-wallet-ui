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

package eu.europa.ec.eudi.wallet.ui.wallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.databinding.ListItemDocumentBinding
import eu.europa.ec.eudi.wallet.ui.wallet.WalletFragmentDirections
import eu.europa.ec.eudi.wallet.document.Document

class DocumentAdapter :
    ListAdapter<Document, RecyclerView.ViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return DocumentViewHolder(
            ListItemDocumentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val document = getItem(position)
        (holder as DocumentViewHolder).bind(document)
    }

    class DocumentViewHolder(
        private val binding: ListItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickDetailListener {
                binding.document?.let { doc ->
                    navigateToDetail(doc.id, it)
                }
            }
        }

        fun bind(item: Document) {
            binding.apply {
                document = item
                executePendingBindings()
            }
        }

        private fun navigateToDetail(identityCredentialName: String, view: View) {
            val direction =
                WalletFragmentDirections.actionWalletFragmentToDetailFragment(identityCredentialName)
            if (R.id.WalletFragment == view.findNavController().currentDestination?.id) {
                view.findNavController().navigate(direction)
            }
        }
    }

    private class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {

        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem == newItem
        }
    }
}