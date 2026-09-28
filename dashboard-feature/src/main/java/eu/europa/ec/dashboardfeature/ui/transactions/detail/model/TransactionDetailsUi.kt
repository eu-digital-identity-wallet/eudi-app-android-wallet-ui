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

package eu.europa.ec.dashboardfeature.ui.transactions.detail.model

import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

data class TransactionDetailsUi(
    val transactionId: String,
    val transactionDetailsCardUi: TransactionDetailsCardUi,
    val body: TransactionDetailsBodyUi,
)

data class TransactionDetailsCardUi(
    val transactionTypeLabel: String,
    val transactionStatusLabel: String,
    val transactionIsCompleted: Boolean,
    val transactionDate: String,
    val partyName: String?,
    val providerType: String?,
    val nonCompletionReason: String?,
    val metadata: List<TransactionDetailsMetadataUi>,
)

data class TransactionDetailsMetadataUi(
    val fields: List<TransactionDetailsFieldUi>,
)

sealed interface PresentationActionCountsUiState {
    data object Loading : PresentationActionCountsUiState
    data class Content(
        val dataDeletionRequests: Int,
        val dpaReports: Int,
    ) : PresentationActionCountsUiState

    data class Failure(val errorMessage: String) : PresentationActionCountsUiState
}

sealed interface TransactionDetailsBodyUi {
    val sections: List<TransactionDetailsSectionUi>

    data class Presentation(
        val requested: TransactionDetailsSectionUi,
        val shared: TransactionDetailsSectionUi,
        val transactionData: TransactionDetailsSectionUi?,
        val deletionContacts: List<TransactionContactUi>,
        val reportContacts: List<TransactionContactUi>,
        val actionCounts: PresentationActionCountsUiState,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> =
            listOfNotNull(requested, shared, transactionData)
    }

    data class Issuance(
        val credentials: TransactionDetailsSectionUi?,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> = listOfNotNull(credentials)
    }

    data class Reissuance(
        val credentials: TransactionDetailsSectionUi?,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> = listOfNotNull(credentials)
    }

    data class Deletion(
        val credential: TransactionDetailsSectionUi?,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> = listOfNotNull(credential)
    }

    data class Signing(
        val document: TransactionDetailsSectionUi?,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> = listOfNotNull(document)
    }

    data class DataDeletionRequest(
        val party: TransactionDetailsSectionUi,
        val claims: TransactionDetailsSectionUi,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> = listOf(party, claims)
    }

    data class DpaReport(
        val authority: TransactionDetailsSectionUi,
    ) : TransactionDetailsBodyUi {
        override val sections: List<TransactionDetailsSectionUi> = listOf(authority)
    }
}

data class TransactionDetailsSectionUi(
    val title: String,
    val items: List<TransactionDetailsItemUi>,
    val groups: List<TransactionDetailsGroupUi>,
    val emptyItem: ListItemDataUi?,
)

data class TransactionDetailsGroupUi(
    val header: ListItemDataUi,
    val items: List<ExpandableListItemUi>,
)

data class TransactionDetailsItemUi(
    val item: ListItemDataUi,
    val url: String?,
)

data class TransactionDetailsFieldUi(
    val id: String,
    val label: String?,
    val value: String,
    val url: String?,
) {
    val item: ListItemDataUi = ListItemDataUi(
        itemId = id,
        overlineText = label,
        mainContentData = ListItemMainContentDataUi.Text(value),
        trailingContentData = url?.let {
            ListItemTrailingContentDataUi.Icon(iconData = AppIcons.OpenNew)
        },
    )
}