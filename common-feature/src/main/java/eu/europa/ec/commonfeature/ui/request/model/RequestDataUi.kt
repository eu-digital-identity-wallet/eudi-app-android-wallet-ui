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

package eu.europa.ec.commonfeature.ui.request.model

/**
 * The request screen's state: still loading ([Initial]), nothing to disclose ([NoData]), one
 * combination ([Single]), or several to choose between ([Multiple]).
 */
sealed interface RequestDataUi {

    /**
     * Pre-load default: nothing resolved yet. The screen renders nothing (the loading overlay
     * covers this window) — distinct from [NoData] ("resolved, but nothing to disclose").
     */
    data object Initial : RequestDataUi

    /** No disclosable data — the screen shows the "no data" message and Share is disabled. */
    data object NoData : RequestDataUi

    /**
     * Exactly one combination — its documents render directly, with no radio or "Option N of M" title
     * since there is nothing to choose between.
     */
    data class Single(val combination: RequestCombinationUi) : RequestDataUi

    /**
     * Several combinations; [selectedIndex] is the chosen one, whose documents Share submits.
     */
    data class Multiple(
        val combinations: List<RequestCombinationUi>,
        val selectedIndex: Int,
    ) : RequestDataUi

    /**
     * The combination whose documents Share submits — the only one for [Single], the
     * selected one for [Multiple], `null` otherwise.
     */
    val selectedCombination: RequestCombinationUi?
        get() = when (this) {
            is Initial -> null
            is NoData -> null
            is Single -> combination
            is Multiple -> combinations.getOrNull(selectedIndex)
        }

    /** The selected combination's items, or empty for [NoData]. */
    val selectedDocuments: List<RequestDocumentItemUi>
        get() = selectedCombination?.documents.orEmpty()

    /**
     * Returns a copy with the selected combination's items replaced by [documents] — the
     * single write-back point for the view-model's toggle / expand edits. A no-op for
     * [NoData].
     */
    fun withSelectedDocuments(documents: List<RequestDocumentItemUi>): RequestDataUi = when (this) {
        is Initial -> this
        is NoData -> this
        is Single -> Single(combination = combination.copy(documents = documents))
        is Multiple -> Multiple(
            combinations = combinations.mapIndexed { index, combination ->
                if (index == selectedIndex) {
                    combination.copy(documents = documents)
                } else {
                    combination
                }
            },
            selectedIndex = selectedIndex,
        )
    }

    companion object {
        /**
         * Maps the controller's combinations onto the right variant: none → [NoData],
         * one → [Single], several → [Multiple] with the first preselected.
         */
        fun of(combinations: List<RequestCombinationUi>): RequestDataUi = when {
            combinations.isEmpty() -> NoData
            combinations.size == 1 -> Single(combination = combinations.first())
            else -> Multiple(combinations = combinations, selectedIndex = 0)
        }
    }
}