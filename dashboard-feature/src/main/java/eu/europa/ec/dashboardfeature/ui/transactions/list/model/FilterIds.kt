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

package eu.europa.ec.dashboardfeature.ui.transactions.list.model

object TransactionFilterIds {
    const val FILTER_SORT_GROUP_ID = "sort_group_id"
    const val FILTER_SORT_TRANSACTION_DATE = "sort_transaction_date"

    const val FILTER_BY_STATUS_GROUP_ID = "status_group_id"
    const val FILTER_BY_STATUS_COMPLETE = "status_complete"
    const val FILTER_BY_STATUS_FAILED = "status_failed"

    const val FILTER_BY_TRANSACTION_DATE_GROUP_ID = "transaction_date_group_id"
    const val FILTER_BY_TRANSACTION_DATE_RANGE = "by_transaction_date_range"

    const val FILTER_BY_RELYING_PARTY_GROUP_ID = "relying_party_group_id"
    const val FILTER_BY_RELYING_PARTY_WITHOUT_NAME = "by_transaction_without_relying_party"

    const val FILTER_BY_TRANSACTION_TYPE_GROUP_ID = "transaction_type_group_id"
    const val FILTER_BY_TRANSACTION_TYPE_PRESENTATION = "by_transaction_type_presentation"
    const val FILTER_BY_TRANSACTION_TYPE_ISSUANCE = "by_transaction_type_issuance"
    const val FILTER_BY_TRANSACTION_TYPE_SIGNING = "by_transaction_type_signing"
}