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

package eu.europa.ec.dashboardfeature.model

import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

data class TransactionDetailsUi(
    val transactionId: String,
    val transactionDetailsCardData: TransactionDetailsCardData,
    val transactionDetailsDataShared: TransactionDetailsDataSharedHolder,
    val transactionDetailsDataSigned: TransactionDetailsDataSignedHolder?,
)

data class TransactionDetailsCardData(
    val transactionTypeLabel: String,
    val transactionStatusLabel: String,
    val transactionIsCompleted: Boolean,
    val transactionDate: String,
    val relyingPartyName: String?,
    val relyingPartyIsVerified: Boolean?,
)

data class TransactionDetailsDataSharedHolder(
    val dataSharedItems: List<ExpandableListItem.NestedListItemData>,
)

data class TransactionDetailsDataSignedHolder(
    val dataSignedItems: List<ExpandableListItem.NestedListItemData>,
)