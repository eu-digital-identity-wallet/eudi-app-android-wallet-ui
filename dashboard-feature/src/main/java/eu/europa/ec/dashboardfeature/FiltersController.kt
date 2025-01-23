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

package eu.europa.ec.dashboardfeature

import eu.europa.ec.businesslogic.extension.addOrReplace
import eu.europa.ec.dashboardfeature.extensions.getEmptyUIifEmptyList
import eu.europa.ec.dashboardfeature.extensions.isBeyondNextDays
import eu.europa.ec.dashboardfeature.extensions.isExpired
import eu.europa.ec.dashboardfeature.extensions.isWithinNextDays
import eu.europa.ec.dashboardfeature.extensions.search
import eu.europa.ec.dashboardfeature.extensions.sortByOrder
import eu.europa.ec.dashboardfeature.model.FilterableDocuments
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.RadioButtonData

private const val FILTER_BY_STATE_GROUP_ID = "state_group_id"
private const val FILTER_BY_STATE_VALID = "state_valid"
private const val FILTER_BY_STATE_EXPIRED = "state_expired"
private const val FILTER_BY_ISSUER_GROUP_ID = "issuer_group_id"
private const val FILTER_BY_PERIOD_GROUP_ID = "by_period_group_id"
private const val FILTER_BY_PERIOD_NEXT_7 = "by_period_next_7"
private const val FILTER_BY_PERIOD_NEXT_30 = "by_period_next_30"
private const val FILTER_BY_PERIOD_BEYOND_30 = "by_period_beyond_30"
private const val FILTER_BY_PERIOD_EXPIRED = "by_period_expired"
private const val FILTER_SORT_GROUP_ID = "sort_group_id"
private const val FILTER_SORT_DEFAULT = "sort_default"
private const val FILTER_SORT_DATE_ISSUED = "sort_date_issued"
private const val FILTER_SORT_EXPIRY_DATE = "sort_expiry_date"

interface FiltersController {
    /**
     * Applies a search query to the filterable documents.
     *
     * @param filterableDocuments The currently displayed documents.
     * @param appliedFilters The currently applied filters.
     * @param newQuery The search query.
     * @return A pair of updated documents and updated filters after applying the search query.
     */
    fun applySearch(
        filterableDocuments: FilterableDocuments,
        appliedFilters: List<ExpandableListItemData>,
        newQuery: String,
    ): Pair<FilterableDocuments, List<ExpandableListItemData>>

    /**
     * Applies the selected filters to the filterable documents.
     *
     * @param filterableDocuments The currently displayed documents.
     * @param selectedFilters The selected filters to apply.
     * @return A pair of updated documents and updated filters after applying the selected filters.
     */
    fun applyFilters(
        filterableDocuments: FilterableDocuments,
        selectedFilters: List<ExpandableListItemData>,
    ): Pair<FilterableDocuments, List<ExpandableListItemData>>

    /**
     * Resets the filters to their initial state.
     *
     * @param filteredDocuments The currently filtered documents.
     * @param initialFilters The initial filters.
     * @return A pair of updated documents and updated filters after resetting the filters.
     */
    fun resetFilters(
        filteredDocuments: FilterableDocuments,
        initialFilters: List<ExpandableListItemData>,
    ): Pair<FilterableDocuments, List<ExpandableListItemData>>

    /**
     * Updates the selection state of a filter.
     *
     * @param filterId The ID of the filter to update.
     * @param groupId The ID of the group the filter belongs to.
     * @param appliedFilters The currently applied filters.
     * @return The updated list of filters.
     */
    fun updateFilter(
        filterId: String,
        groupId: String,
        appliedFilters: List<ExpandableListItemData>,
    ): List<ExpandableListItemData>

    /**
     * Gets all available filters.
     *
     * @param filteredDocuments The currently filtered documents.
     * @param appliedFilters The currently applied filters or empty if no filters are applied or a filter reset is required.
     * @return The list of all available filters.
     */
    fun getAllFilter(
        filteredDocuments: FilterableDocuments,
        appliedFilters: MutableList<ExpandableListItemData> = mutableListOf(),
    ): List<ExpandableListItemData>
}

class FiltersControllerImpl(
    val resourceProvider: ResourceProvider,
) : FiltersController {

    //#region Filters
    private val initialFilters = mutableListOf(
        // Filter by expiry period
        ExpandableListItemData(
            collapsed = ListItemData(
                itemId = FILTER_BY_PERIOD_GROUP_ID,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period)),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                )
            ),
            expanded = listOf(
                ListItemData(
                    itemId = FILTER_BY_PERIOD_NEXT_7,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_1)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.filter { document ->
                            document.filterableAttributes.expiryDate?.isWithinNextDays(7) == true
                        }.toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                ),
                ListItemData(
                    itemId = FILTER_BY_PERIOD_NEXT_30,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_2)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.filter { document ->
                            document.filterableAttributes.expiryDate?.isWithinNextDays(30) == true
                        }.toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                ),
                ListItemData(
                    itemId = FILTER_BY_PERIOD_BEYOND_30,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_3)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.filter { document ->
                            document.filterableAttributes.expiryDate?.isBeyondNextDays(30) == true
                        }.toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                ),
                ListItemData(
                    itemId = FILTER_BY_PERIOD_EXPIRED,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_4)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.filter { document ->
                            document.filterableAttributes.expiryDate?.isExpired() == true
                        }.toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                )
            )
        ),

        // Sort Filters
        ExpandableListItemData(
            collapsed = ListItemData(
                itemId = FILTER_SORT_GROUP_ID,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_by)),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                )
            ),
            expanded = listOf(
                ListItemData(
                    itemId = FILTER_SORT_DEFAULT,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_sort_default)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.sortByOrder(filterable.sortingOrder) { (it.itemUi.uiData.mainContentData as ListItemMainContentData.Text).text.lowercase() }
                            .toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = true,
                            enabled = true
                        )
                    )
                ),
                ListItemData(
                    itemId = FILTER_SORT_DATE_ISSUED,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_sort_date_issued)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.sortByOrder(filterable.sortingOrder) { it.filterableAttributes.issuedDate }
                            .toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                ),
                ListItemData(
                    itemId = FILTER_SORT_EXPIRY_DATE,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_sort_expiry_date)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.sortByOrder(filterable.sortingOrder) { it.filterableAttributes.expiryDate }
                            .toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                )
            )
        ),

        // Filter by Issuer
        ExpandableListItemData(
            collapsed = ListItemData(
                itemId = FILTER_BY_ISSUER_GROUP_ID,
                mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                    resourceProvider.getString(R.string.documents_screen_filters_filter_by_issuer)
                ) { filterable ->
                    filterable.copy(documents = filterable.documents.filter { document ->
                        document.itemUi.uiData.overlineText == document.filterableAttributes.issuer

                    }.toMutableList())
                },
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                )
            ),
            expanded = listOf()
        ),

        // Filter by state
        ExpandableListItemData(
            collapsed = ListItemData(
                itemId = FILTER_BY_STATE_GROUP_ID,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_state)),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                )
            ),
            expanded = listOf(
                ListItemData(
                    itemId = FILTER_BY_STATE_VALID,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_valid)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.filter { document ->
                            document.filterableAttributes.expiryDate?.isExpired() == false
                        }.toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                ),
                ListItemData(
                    itemId = FILTER_BY_STATE_EXPIRED,
                    mainContentData = ListItemMainContentData.Actionable<FilterableDocuments>(
                        resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_expired)
                    ) { filterable ->
                        filterable.copy(documents = filterable.documents.filter { document ->
                            document.filterableAttributes.expiryDate?.isExpired() == true
                        }.toMutableList())
                    },
                    trailingContentData = ListItemTrailingContentData.RadioButton(
                        radioButtonData = RadioButtonData(
                            isSelected = false,
                            enabled = true
                        )
                    )
                )
            )
        )
    )

    //#endregion

    override fun applySearch(
        filterableDocuments: FilterableDocuments,
        appliedFilters: List<ExpandableListItemData>,
        newQuery: String,
    ): Pair<FilterableDocuments, List<ExpandableListItemData>> {
        val searchedDocuments = filterableDocuments.search(newQuery)
        val updatedFilters = getAllFilter(searchedDocuments, appliedFilters.toMutableList())
        return Pair(searchedDocuments.getEmptyUIifEmptyList(resourceProvider), updatedFilters)
    }

    override fun applyFilters(
        filterableDocuments: FilterableDocuments,
        selectedFilters: List<ExpandableListItemData>,
    ): Pair<FilterableDocuments, List<ExpandableListItemData>> {
        // Filter only the selected filters
        val activeFilters = selectedFilters
            .flatMap { it.expanded }
            .filter { (it.trailingContentData as ListItemTrailingContentData.RadioButton).radioButtonData.isSelected }
            .mapNotNull { it.mainContentData as? ListItemMainContentData.Actionable<FilterableDocuments> }

        // If no filters are selected, return the original list
        if (activeFilters.isEmpty()) {
            return Pair(filterableDocuments, selectedFilters)
        }

        // Chain the filtering actions
        val documentsWithAppliedFilters =
            activeFilters.fold(filterableDocuments) { currentList, filter ->
                filter.action.invoke(currentList)
            }
        return Pair(
            documentsWithAppliedFilters.getEmptyUIifEmptyList(resourceProvider),
            selectedFilters
        )
    }

    override fun resetFilters(
        initialDocuments: FilterableDocuments,
        initialFilters: List<ExpandableListItemData>,
    ): Pair<FilterableDocuments, List<ExpandableListItemData>> {
        val searchAppliedFilters = applyFilters(initialDocuments, initialFilters).second
        return applyFilters(
            initialDocuments.getEmptyUIifEmptyList(resourceProvider),
            searchAppliedFilters
        )
    }

    override fun getAllFilter(
        filteredDocuments: FilterableDocuments,
        appliedFilters: MutableList<ExpandableListItemData>,
    ): List<ExpandableListItemData> {
        // If applied filters are present take those in consideration in order to preserve the filter state
        val filtersToApplyAgainst = appliedFilters.ifEmpty { initialFilters }
        val issuerFilter =
            filtersToApplyAgainst.find { it.collapsed.itemId == FILTER_BY_ISSUER_GROUP_ID }
                ?.copy(
                    expanded = filteredDocuments.documents
                        .distinctBy { it.filterableAttributes.issuer }
                        .mapNotNull { document ->
                            document.filterableAttributes.issuer?.let {
                                ListItemData(
                                    itemId = document.filterableAttributes.issuer,
                                    mainContentData = ListItemMainContentData.Text(document.filterableAttributes.issuer),
                                    trailingContentData = ListItemTrailingContentData.RadioButton(
                                        radioButtonData = RadioButtonData(
                                            isSelected = false,
                                            enabled = true
                                        )
                                    )
                                )
                            }
                        })

        // Update the issuer placeholder filter
        issuerFilter?.let {
            filtersToApplyAgainst.addOrReplace(issuerFilter) { it.collapsed.itemId == FILTER_BY_ISSUER_GROUP_ID }
        }
        return filtersToApplyAgainst
    }

    override fun updateFilter(
        filterId: String,
        groupId: String,
        appliedFilters: List<ExpandableListItemData>,
    ): List<ExpandableListItemData> {
        return appliedFilters.map { appliedFilter ->
            appliedFilter.copy(expanded = appliedFilter.expanded.map { filterItem ->
                if (appliedFilter.collapsed.itemId != groupId && filterItem.itemId != filterId) {
                    filterItem
                } else {
                    filterItem.copy(
                        trailingContentData = ListItemTrailingContentData.RadioButton(
                            radioButtonData = RadioButtonData(
                                isSelected = filterItem.itemId == filterId,
                                enabled = true
                            )
                        )
                    )
                }
            })
        }
    }
}