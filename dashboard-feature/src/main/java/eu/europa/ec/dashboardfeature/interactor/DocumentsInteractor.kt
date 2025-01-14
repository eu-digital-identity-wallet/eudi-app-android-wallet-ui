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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.util.extractValueFromDocumentOrEmpty
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.MainContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.RadioButtonData

interface DocumentsInteractor {
    fun getAllDocuments(): List<ListItemData>
    fun searchDocuments(query: String): List<ListItemData>
    fun getFilters(): List<ExpandableListItemData>
    fun onFilterSelect(
        id: String,
        groupId: String,
        setStateAction: (List<ExpandableListItemData>) -> Unit,
    )
    fun clearFilters(setStateAction: (List<ExpandableListItemData>) -> Unit)
    fun applyFilters(setStateAction: (List<ExpandableListItemData>) -> Unit)
}

class DocumentsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val documentsController: WalletCoreDocumentsController,
) : DocumentsInteractor {

    private val documents: MutableList<ListItemData> = mutableListOf()

    //#region Filters
    private val expandableFilterByExpiryPeriod = ExpandableListItemData(
        collapsed = ListItemData(
            itemId = "",
            mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_by)),
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowDown
            )
        ),
        expanded = listOf(
            ListItemData(
                itemId = "def1",
                mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_default)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        groupId = "filter1",
                        isSelected = true,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = "issue1",
                mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_date_issued)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        groupId = "filter1",
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = "date1",
                mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_expiry_date)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        groupId = "filter1",
                        isSelected = false,
                        enabled = true
                    )
                )
            )
        )
    )

    private val expandableSortFilters = ExpandableListItemData(
        collapsed = ListItemData(
            itemId = "",
            mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_by)),
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowDown
            )
        ),
        expanded = listOf(
            ListItemData(
                itemId = "def",
                mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_default)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        groupId = "filter",
                        isSelected = true,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = "issue",
                mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_date_issued)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        groupId = "filter",
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = "date",
                mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_expiry_date)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        groupId = "filter",
                        isSelected = false,
                        enabled = true
                    )
                )
            )
        )
    )

    private val filterList = mutableListOf(expandableSortFilters, expandableFilterByExpiryPeriod)
    //#endregion

    override fun getAllDocuments(): List<ListItemData> {
        documents.clear()
        documents.addAll(
            documentsController.getAllDocuments().map {
                val documentExpirationDate: String = when (it) {
                    is IssuedDocument -> {
                        "${resourceProvider.getString(R.string.dashboard_document_has_not_expired)}: " +
                                extractValueFromDocumentOrEmpty(
                                    document = it,
                                    key = DocumentJsonKeys.EXPIRY_DATE
                                )
                    }

                    else -> ""
                }
                ListItemData(
                    itemId = it.id,
                    mainContentData = MainContentData.Text(text = it.name),
                    overlineText = "Hellenic Goverment", // TODO Here we want to show issuer name
                    supportingText = documentExpirationDate,
                    leadingContentData = ListItemLeadingContentData.Icon(
                        iconData = AppIcons.IssuerPlaceholder
                    ), // TODO Get the actual issuer image
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowRight
                    )
                )
            }
        )

        return documents
    }

    override fun searchDocuments(query: String): List<ListItemData> {
        val result = documents.filter {
            (it.mainContentData as MainContentData.Text).text.lowercase()
                .contains(query.lowercase())
        }

        return result.ifEmpty {
            listOf(
                ListItemData(
                    itemId = "",
                    mainContentData = MainContentData.Text(resourceProvider.getString(R.string.documents_screen_search_no_results)),
                    overlineText = null,
                    supportingText = null,
                    leadingContentData = null,
                    trailingContentData = null
                )
            )
        }
    }

    override fun getFilters(): List<ExpandableListItemData> {
        return filterList
    }

    override fun onFilterSelect(
        id: String,
        groupId: String,
        setStateAction: (List<ExpandableListItemData>) -> Unit,
    ) {

        filterSnapshot = filterSnapshot.map { checkIfSelected(it, id, groupId) }

        setStateAction(filterSnapshot)
    }

    override fun clearFilters(setStateAction: (List<ExpandableListItemData>) -> Unit) {
        filterSnapshot = filterList
        setStateAction(filterList)
    }

    override fun applyFilters(setStateAction: (List<ExpandableListItemData>) -> Unit) {
        filterList.clear().run { filterList.addAll(filterSnapshot) }
        setStateAction(filterList)
    }

    private fun checkIfSelected(
        expandableListItemData: ExpandableListItemData,
        id: String,
        groupId: String,
    ): ExpandableListItemData {
        return expandableListItemData.copy(
            expanded = expandableListItemData.expanded.map { listItemData ->

                if ((listItemData.trailingContentData as ListItemTrailingContentData.RadioButton).radioButtonData.groupId
                    != groupId && listItemData.itemId != id
                ) {
                    listItemData
                } else {
                    listItemData.copy(
                        trailingContentData = ListItemTrailingContentData.RadioButton(
                            radioButtonData = RadioButtonData(
                                groupId = groupId,
                                isSelected = listItemData.itemId == id,
                                enabled = true
                            )
                        )
                    )
                }
            }
        )
    }

    private var filterSnapshot: List<ExpandableListItemData> = listOf(
        expandableSortFilters,
        expandableFilterByExpiryPeriod
    )
}