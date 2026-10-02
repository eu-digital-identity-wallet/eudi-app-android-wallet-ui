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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.businesslogic.util.FULL_DATETIME_PATTERN
import eu.europa.ec.businesslogic.util.formatLocalDateTime
import eu.europa.ec.commonfeature.ui.request.transformer.TransactionDataTransformer
import eu.europa.ec.corelogic.controller.RecordTransactionPartialState
import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogController
import eu.europa.ec.corelogic.controller.WalletCoreTransactionRecordingController
import eu.europa.ec.corelogic.extension.preferredDataDeletionContact
import eu.europa.ec.corelogic.extension.toPrivacyContactOrNull
import eu.europa.ec.corelogic.extension.toPrivacyContacts
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimRefDomain
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.CredentialClaimsDomain
import eu.europa.ec.corelogic.model.CredentialRefDomain
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.InteractingPartyDomain
import eu.europa.ec.corelogic.model.IssuanceDetailsDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.PrivacyContactDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.dashboardfeature.ui.transactions.data_deletion.model.DataDeletionRequestUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PendingTransactionActionUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PresentationActionCountsUiState
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionContactUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsBodyUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsCardUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsFieldUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsGroupUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsItemUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsMetadataUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsSectionUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsUi
import eu.europa.ec.dashboardfeature.ui.transactions.dpa_report.model.DpaReportContactUi
import eu.europa.ec.dashboardfeature.ui.transactions.dpa_report.model.DpaReportUi
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionStatusUi
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionStatusUi.Companion.toUiText
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionTypeUi.Companion.toUiText
import eu.europa.ec.dashboardfeature.ui.transactions.model.toTransactionPartyName
import eu.europa.ec.dashboardfeature.ui.transactions.model.toTransactionStatusUi
import eu.europa.ec.dashboardfeature.ui.transactions.model.toTransactionTypeUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.ThemeColorKey
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI
import java.net.URLEncoder

sealed class TransactionDetailsInteractorPartialState {
    data class Success(
        val transactionDetailsUi: TransactionDetailsUi,
    ) : TransactionDetailsInteractorPartialState()

    data class Failure(
        val error: String
    ) : TransactionDetailsInteractorPartialState()
}

sealed class TransactionDetailsInteractorDeleteTransactionPartialState {
    data object Success : TransactionDetailsInteractorDeleteTransactionPartialState()
    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorDeleteTransactionPartialState()
}

sealed class TransactionDetailsInteractorDataProtectionPartialState {
    data class Success(
        val pendingAction: PendingTransactionActionUi
    ) : TransactionDetailsInteractorDataProtectionPartialState()

    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorDataProtectionPartialState()
}

sealed class TransactionDetailsInteractorDataDeletionPartialState {
    data class Success(
        val request: DataDeletionRequestUi
    ) : TransactionDetailsInteractorDataDeletionPartialState()

    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorDataDeletionPartialState()
}

sealed class TransactionDetailsInteractorDpaReportPartialState {
    data class Success(
        val report: DpaReportUi
    ) : TransactionDetailsInteractorDpaReportPartialState()

    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorDpaReportPartialState()
}

interface TransactionDetailsInteractor {
    fun getTransactionDetails(
        transactionId: String
    ): Flow<TransactionDetailsInteractorPartialState>

    fun getDataDeletionRequest(transactionId: String): Flow<TransactionDetailsInteractorDataDeletionPartialState>

    fun getDpaReport(transactionId: String): Flow<TransactionDetailsInteractorDpaReportPartialState>

    fun observePresentationActionCounts(presentationId: String): Flow<PresentationActionCountsUiState>

    fun deleteTransaction(transactionId: String): Flow<TransactionDetailsInteractorDeleteTransactionPartialState>

    fun prepareDataProtectionAction(
        transactionId: String,
        action: TransactionDataProtectionAction,
        contactUrl: String,
    ): Flow<TransactionDetailsInteractorDataProtectionPartialState>

    fun recordDataProtectionAction(
        pendingAction: PendingTransactionActionUi,
    ): Flow<RecordTransactionPartialState>
}

class TransactionDetailsInteractorImpl(
    private val walletCoreTransactionLogController: WalletCoreTransactionLogController,
    private val walletCoreTransactionRecordingController: WalletCoreTransactionRecordingController,
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
) : TransactionDetailsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getTransactionDetails(transactionId: String): Flow<TransactionDetailsInteractorPartialState> =
        flow {
            val transaction =
                walletCoreTransactionLogController.getTransactionLog(id = transactionId)
            if (transaction == null) {
                emit(TransactionDetailsInteractorPartialState.Failure(error = genericErrorMsg))
                return@flow
            }

            val transactionUiStatus = transaction.result.toTransactionStatusUi()
            val transactionUiDate = transaction.time.formatLocalDateTime(
                pattern = FULL_DATETIME_PATTERN
            )
            val transactionDetailsUi = TransactionDetailsUi(
                transactionId = transaction.id,
                transactionDetailsCardUi = TransactionDetailsCardUi(
                    transactionTypeLabel = transaction
                        .toTransactionTypeUi()
                        .toUiText(resourceProvider),
                    transactionStatusLabel = transactionUiStatus.toUiText(resourceProvider),
                    transactionIsCompleted = transactionUiStatus == TransactionStatusUi.Completed,
                    transactionDate = transactionUiDate,
                    partyName = transaction.toTransactionPartyName(),
                    providerType = transaction.toProviderType(),
                    metadata = transaction.toCardMetadata(),
                    nonCompletionReason = when (val result = transaction.result) {
                        is TransactionResultDomain.Completed -> null
                        is TransactionResultDomain.NotCompleted ->
                            result.reason?.takeIf { reason -> reason.isNotBlank() }
                    },
                ),
                body = transaction.toDetailsBody(),
            )
            emit(
                TransactionDetailsInteractorPartialState.Success(
                    transactionDetailsUi = transactionDetailsUi
                )
            )
        }.safeAsync {
            TransactionDetailsInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun getDataDeletionRequest(
        transactionId: String,
    ): Flow<TransactionDetailsInteractorDataDeletionPartialState> = flow {
        val presentation = walletCoreTransactionLogController.getTransactionLog(id = transactionId)
                as? TransactionLogDomain.Presentation

        val contact = presentation
            ?.takeIf { parent -> parent.canRequestDataDeletion }
            ?.party
            ?.contacts
            ?.toPrivacyContacts()
            ?.preferredDataDeletionContact()

        if (presentation == null || contact == null) {
            emit(
                TransactionDetailsInteractorDataDeletionPartialState.Failure(
                    errorMessage = resourceProvider.getString(R.string.transaction_details_action_unavailable),
                )
            )
            return@flow
        }
        emit(
            TransactionDetailsInteractorDataDeletionPartialState.Success(
                request = contact.toDataDeletionRequestUi(
                    relyingPartyName = presentation.party.name?.text
                        ?.takeIf { name -> name.isNotBlank() }
                        ?: resourceProvider.getString(R.string.data_deletion_relying_party_default_name),
                )
            )
        )
    }.safeAsync {
        TransactionDetailsInteractorDataDeletionPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg,
        )
    }

    override fun getDpaReport(
        transactionId: String,
    ): Flow<TransactionDetailsInteractorDpaReportPartialState> = flow {
        val presentation = walletCoreTransactionLogController.getTransactionLog(id = transactionId)
                as? TransactionLogDomain.Presentation

        val authority = presentation?.registration?.dpa

        val contacts = authority?.contacts.orEmpty().toPrivacyContacts().sortedBy { contact ->
            when (contact.method) {
                CommunicationMethodDomain.Phone -> 0
                CommunicationMethodDomain.Email -> 1
                CommunicationMethodDomain.Website -> 2
            }
        }

        if (contacts.isEmpty()) {
            emit(
                TransactionDetailsInteractorDpaReportPartialState.Failure(
                    errorMessage = resourceProvider.getString(R.string.transaction_details_action_unavailable),
                )
            )
            return@flow
        }

        emit(
            TransactionDetailsInteractorDpaReportPartialState.Success(
                report = DpaReportUi(
                    authority = authority?.name?.text?.takeIf { name -> name.isNotBlank() },
                    responsibility = resourceProvider.getString(R.string.dpa_report_responsibility),
                    followUp = resourceProvider.getString(R.string.dpa_report_follow_up),
                    contacts = contacts.map { contact -> contact.toDpaReportContactUi() },
                )
            )
        )
    }.safeAsync {
        TransactionDetailsInteractorDpaReportPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg,
        )
    }

    private fun PrivacyContactDomain.toDataDeletionRequestUi(
        relyingPartyName: String,
    ): DataDeletionRequestUi {
        val (descriptionRes, responsibilityRes, buttonRes) = when (method) {
            CommunicationMethodDomain.Website -> Triple(
                R.string.data_deletion_website_description,
                R.string.data_deletion_website_responsibility,
                R.string.data_deletion_website_button,
            )

            CommunicationMethodDomain.Email -> Triple(
                R.string.data_deletion_email_description,
                R.string.data_deletion_email_responsibility,
                R.string.data_deletion_email_button,
            )

            CommunicationMethodDomain.Phone -> Triple(
                R.string.data_deletion_phone_description,
                R.string.data_deletion_phone_responsibility,
                R.string.data_deletion_phone_button,
            )
        }
        return DataDeletionRequestUi(
            description = resourceProvider.getString(descriptionRes, relyingPartyName),
            responsibility = resourceProvider.getString(responsibilityRes),
            retentionNotice = resourceProvider.getString(
                R.string.data_deletion_retention_notice,
                relyingPartyName,
            ),
            buttonText = resourceProvider.getString(buttonRes, relyingPartyName),
            contactUrl = url,
        )
    }

    private fun PrivacyContactDomain.toDpaReportContactUi(): DpaReportContactUi {
        val (icon, actionTextRes) = when (method) {
            CommunicationMethodDomain.Phone -> AppIcons.Call to R.string.dpa_report_call_button
            CommunicationMethodDomain.Email -> AppIcons.Email to R.string.dpa_report_email_button
            CommunicationMethodDomain.Website -> AppIcons.Link to R.string.dpa_report_website_button
        }
        return DpaReportContactUi(
            item = ListItemDataUi(
                itemId = url,
                mainContentData = ListItemMainContentDataUi.Text(text = displayValue),
                leadingContentData = ListItemLeadingContentDataUi.Icon(
                    iconData = icon,
                    tint = ThemeColorKey.OnSurfaceVariant,
                ),
                trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                    text = resourceProvider.getString(actionTextRes),
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            url = url,
        )
    }

    override fun observePresentationActionCounts(
        presentationId: String,
    ): Flow<PresentationActionCountsUiState> = flow {
        emit(PresentationActionCountsUiState.Loading)

        walletCoreTransactionLogController.observePresentationActions(presentationId = presentationId)
            .collect { actions ->
                var dataDeletionRequests = 0
                var dpaReports = 0
                actions.forEach { action ->
                    when (action) {
                        is TransactionLogDomain.DataDeletionRequest -> dataDeletionRequests++
                        is TransactionLogDomain.DpaReport -> dpaReports++
                    }
                }

                emit(
                    PresentationActionCountsUiState.Content(
                        dataDeletionRequests = dataDeletionRequests,
                        dpaReports = dpaReports,
                    )
                )
            }
    }.safeAsync {
        PresentationActionCountsUiState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun deleteTransaction(transactionId: String): Flow<TransactionDetailsInteractorDeleteTransactionPartialState> =
        flow {
            walletCoreTransactionLogController.deleteTransactionLog(id = transactionId)
            emit(TransactionDetailsInteractorDeleteTransactionPartialState.Success)
        }.safeAsync {
            TransactionDetailsInteractorDeleteTransactionPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun prepareDataProtectionAction(
        transactionId: String,
        action: TransactionDataProtectionAction,
        contactUrl: String,
    ): Flow<TransactionDetailsInteractorDataProtectionPartialState> = flow {
        val presentation = walletCoreTransactionLogController.getTransactionLog(id = transactionId)
                as? TransactionLogDomain.Presentation

        if (presentation == null
            || presentation
                .actionContacts(action)
                .none { transactionContactUi ->
                    transactionContactUi.url == contactUrl
                }
        ) {
            emit(
                TransactionDetailsInteractorDataProtectionPartialState.Failure(
                    errorMessage = resourceProvider.getString(R.string.transaction_details_action_unavailable),
                )
            )
            return@flow
        }

        val communicationMethod = when (URI(contactUrl).scheme?.lowercase()) {
            "http", "https" -> CommunicationMethodDomain.Website
            "mailto" -> CommunicationMethodDomain.Email
            "tel" -> CommunicationMethodDomain.Phone
            else -> {
                emit(
                    TransactionDetailsInteractorDataProtectionPartialState.Failure(
                        errorMessage = resourceProvider.getString(R.string.transaction_details_action_unavailable),
                    )
                )
                return@flow
            }
        }

        emit(
            TransactionDetailsInteractorDataProtectionPartialState.Success(
                pendingAction = PendingTransactionActionUi(
                    id = uuidProvider.provideUuid(),
                    presentation = presentation,
                    action = action,
                    contactUrl = contactUrl,
                    launchUrl = presentation.actionUrl(action = action, contactUrl = contactUrl),
                    communicationMethod = communicationMethod,
                    authority = if (action == TransactionDataProtectionAction.ReportSuspiciousTransaction) {
                        presentation.registration?.dpa
                    } else {
                        null
                    },
                    launchedAt = null,
                ),
            )
        )
    }.safeAsync {
        TransactionDetailsInteractorDataProtectionPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg,
        )
    }

    override fun recordDataProtectionAction(
        pendingAction: PendingTransactionActionUi,
    ): Flow<RecordTransactionPartialState> = flow {
        val launchedAt = pendingAction.launchedAt
        if (launchedAt == null) {
            emit(RecordTransactionPartialState.Failure(genericErrorMsg))
            return@flow
        }

        val result = when (pendingAction.action) {
            TransactionDataProtectionAction.RequestDataDeletion ->
                walletCoreTransactionRecordingController.recordDataDeletionRequest(
                    id = pendingAction.id,
                    time = launchedAt,
                    presentation = pendingAction.presentation,
                    communicationMethod = pendingAction.communicationMethod,
                )

            TransactionDataProtectionAction.ReportSuspiciousTransaction -> {
                val authority = pendingAction.authority
                if (authority == null) {
                    emit(RecordTransactionPartialState.Failure(genericErrorMsg))
                    return@flow
                }

                walletCoreTransactionRecordingController.recordDpaReport(
                    id = pendingAction.id,
                    time = launchedAt,
                    parentPresentationId = pendingAction.presentation.id,
                    authority = authority,
                    communicationMethod = pendingAction.communicationMethod,
                )
            }
        }

        emit(result)
    }.safeAsync {
        RecordTransactionPartialState.Failure(it.localizedMessage ?: genericErrorMsg)
    }

    private fun TransactionLogDomain.Presentation.actionContacts(
        action: TransactionDataProtectionAction,
    ): List<TransactionContactUi> {
        val contacts = when (action) {
            TransactionDataProtectionAction.RequestDataDeletion ->
                if (canRequestDataDeletion) party.contacts else emptyList()

            TransactionDataProtectionAction.ReportSuspiciousTransaction ->
                registration?.dpa?.contacts.orEmpty()
        }
        return contacts.mapNotNull { contact ->
            contact.toContactUrlOrNull()?.let { url ->
                TransactionContactUi(label = contact.trim(), url = url)
            }
        }.distinctBy { contact -> contact.url }
    }

    private fun TransactionLogDomain.Presentation.actionUrl(
        action: TransactionDataProtectionAction,
        contactUrl: String,
    ): String {
        if (!contactUrl.startsWith("mailto:")) return contactUrl

        val partyName = party.name?.text?.takeIf { name -> name.isNotBlank() }
            ?: party.identifier?.value?.takeIf { identifier -> identifier.isNotBlank() }
            ?: resourceProvider.getString(R.string.transaction_details_no_information)

        val subjectRes = when (action) {
            TransactionDataProtectionAction.RequestDataDeletion ->
                R.string.transaction_details_deletion_email_subject

            TransactionDataProtectionAction.ReportSuspiciousTransaction ->
                R.string.transaction_details_report_email_subject
        }

        val bodyRes = when (action) {
            TransactionDataProtectionAction.RequestDataDeletion ->
                R.string.transaction_details_deletion_email_body

            TransactionDataProtectionAction.ReportSuspiciousTransaction ->
                R.string.transaction_details_report_email_body
        }

        val subject = resourceProvider.getString(
            subjectRes, partyName.replace(Regex("[\\r\\n]+"), " "),
        )

        val identity = listOfNotNull(
            party.name?.text?.takeIf { name -> name.isNotBlank() },
            party.identifier?.value?.takeIf { identifier -> identifier.isNotBlank() },
            party.identifier?.schemeUri?.takeIf { scheme -> scheme.isNotBlank() },
        ).joinToString(separator = "\n")
            .ifBlank { partyName }

        val body = buildString {
            append(
                resourceProvider.getString(
                    bodyRes, identity, time.formatLocalDateTime(pattern = FULL_DATETIME_PATTERN),
                )
            )
            if (action == TransactionDataProtectionAction.ReportSuspiciousTransaction) {
                intermediary?.name?.text?.takeIf { name -> name.isNotBlank() }?.let { name ->
                    append(
                        resourceProvider.getString(
                            R.string.transaction_details_report_email_intermediary,
                            name
                        )
                    )
                }
            }
        }

        return "$contactUrl?subject=${subject.encodeMailParameter()}&body=${body.encodeMailParameter()}"
    }

    private fun String.encodeMailParameter(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun TransactionLogDomain.toProviderType(): String? {
        return when (this) {
            is TransactionLogDomain.Presentation -> partyType
            is TransactionLogDomain.CredentialIssuance -> details.issuerType
            is TransactionLogDomain.CredentialReissuance -> details.issuerType
            is TransactionLogDomain.SigningSealing -> serviceType
            is TransactionLogDomain.CredentialDeletion,
            is TransactionLogDomain.DataDeletionRequest,
            is TransactionLogDomain.DpaReport -> null
        }?.takeIf { type -> type.isNotBlank() }
    }

    private fun TransactionLogDomain.toCardMetadata(): List<TransactionDetailsMetadataUi> {
        return when (this) {
            is TransactionLogDomain.Presentation -> listOfNotNull(
                metadataGroup(
                    fields = listOfNotNull(
                        textField(
                            "purpose",
                            R.string.transaction_details_purpose_label,
                            registration?.purpose
                        ),
                    ),
                ),
                metadataGroup(
                    fields = registration?.privacyPolicyUrls.orEmpty()
                        .mapIndexedNotNull { index, url ->
                            webField(
                                "privacy:$index",
                                R.string.transaction_details_privacy_policy_label,
                                url
                            )
                        },
                ),
                metadataGroup(
                    fields = contactFields(
                        prefix = "party",
                        contacts = party.contacts,
                        labelRes = R.string.transaction_details_contact_label,
                    ),
                ),
                metadataGroup(
                    fields = listOfNotNull(
                        textField(
                            "intermediary:name",
                            R.string.transaction_details_intermediary_name_label,
                            intermediary?.name?.text
                        ),
                    ) + contactFields(
                        prefix = "intermediary",
                        contacts = intermediary?.contacts.orEmpty(),
                        labelRes = R.string.transaction_details_intermediary_contact_label,
                    ),
                ),
            )

            is TransactionLogDomain.CredentialIssuance -> listOfNotNull(
                metadataGroup(fields = details.toCountFields()),
                metadataGroup(
                    fields = contactFields(
                        prefix = "issuer",
                        contacts = details.issuer.contacts,
                        labelRes = R.string.transaction_details_contact_label,
                    ),
                ),
            )

            is TransactionLogDomain.CredentialReissuance -> listOfNotNull(
                metadataGroup(fields = details.toCountFields()),
                metadataGroup(
                    fields = listOfNotNull(details.toTriggerField()),
                ),
                metadataGroup(
                    fields = contactFields(
                        prefix = "issuer",
                        contacts = details.issuer.contacts,
                        labelRes = R.string.transaction_details_contact_label,
                    ),
                ),
            )

            is TransactionLogDomain.SigningSealing -> listOfNotNull(
                metadataGroup(
                    fields = listOfNotNull(
                        textField(
                            id = "signing-transaction",
                            labelRes = R.string.transaction_details_signing_identifier_label,
                            value = signingTransactionId,
                        ),
                    ),
                ),
            )

            is TransactionLogDomain.CredentialDeletion,
            is TransactionLogDomain.DataDeletionRequest,
            is TransactionLogDomain.DpaReport -> emptyList()
        }
    }

    private fun metadataGroup(
        fields: List<TransactionDetailsFieldUi>,
    ): TransactionDetailsMetadataUi? {
        return fields
            .takeIf { availableFields -> availableFields.isNotEmpty() }
            ?.let { availableFields ->
                TransactionDetailsMetadataUi(fields = availableFields)
            }
    }

    private fun TransactionLogDomain.toDetailsBody(): TransactionDetailsBodyUi = when (this) {
        is TransactionLogDomain.Presentation -> TransactionDetailsBodyUi.Presentation(
            requested = claimsSection(
                prefix = "requested",
                titleRes = R.string.transaction_details_data_requested_section_title,
                claims = claimsRequested,
                emptyRes = R.string.transaction_details_no_data_requested,
            ),
            shared = claimsSection(
                prefix = "shared",
                titleRes = R.string.transaction_details_data_shared_section_title,
                claims = claimsPresented,
                emptyRes = R.string.transaction_details_no_data_shared,
            ),
            transactionData = transactionDataSection(transactions = transactionData),
            deletionContacts = actionContacts(TransactionDataProtectionAction.RequestDataDeletion),
            reportContacts = actionContacts(TransactionDataProtectionAction.ReportSuspiciousTransaction),
            actionCounts = PresentationActionCountsUiState.Loading,
        )

        is TransactionLogDomain.CredentialIssuance -> TransactionDetailsBodyUi.Issuance(
            credentials = credentialsSection(
                titleRes = R.string.transaction_details_credentials_issued_section_title,
                credentials = details.credentials,
            ),
        )

        is TransactionLogDomain.CredentialReissuance -> TransactionDetailsBodyUi.Reissuance(
            credentials = credentialsSection(
                titleRes = R.string.transaction_details_credentials_issued_section_title,
                credentials = details.credentials,
            ),
        )

        is TransactionLogDomain.CredentialDeletion -> TransactionDetailsBodyUi.Deletion(
            credential = credentialsSection(
                titleRes = R.string.transaction_details_credentials_section_title,
                credentials = listOf(credential),
            ),
        )

        is TransactionLogDomain.SigningSealing -> TransactionDetailsBodyUi.Signing(
            document = textField("filename", R.string.transaction_details_filename_label, fileName)
                ?.let { filename ->
                    fieldsSection(
                        titleRes = R.string.transaction_details_data_signed_section_title,
                        fields = listOf(filename),
                    )
                },
        )

        is TransactionLogDomain.DataDeletionRequest -> TransactionDetailsBodyUi.DataDeletionRequest(
            party = fieldsSection(
                titleRes = R.string.transaction_details_relying_party_section_title,
                fields = party.toDetailsFields(prefix = "party"),
            ),
            claims = claimsSection(
                prefix = "deletion",
                titleRes = R.string.transaction_details_data_deletion_section_title,
                claims = claims,
                emptyRes = R.string.transaction_details_no_claims,
            ),
        )

        is TransactionLogDomain.DpaReport -> TransactionDetailsBodyUi.DpaReport(
            authority = DpaContactDomain(
                name = dpaName,
                country = dpaCountry,
                contacts = emptyList()
            ).toAuthoritySection()
        )
    }

    private fun transactionDataSection(
        transactions: List<PresentationTransactionDataDomain>,
    ): TransactionDetailsSectionUi? {
        if (transactions.isEmpty()) return null
        val sectionId = "transaction-data"
        return TransactionDetailsSectionUi(
            title = resourceProvider.getString(R.string.transaction_details_signing_request_title),
            items = emptyList(),
            groups = listOf(
                TransactionDetailsGroupUi(
                    header = ListItemDataUi(
                        itemId = sectionId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(R.string.request_transaction_details_title),
                        ),
                        supportingContentData = ListItemSupportingContentDataUi.Text(
                            text = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowDown,
                        ),
                    ),
                    items = TransactionDataTransformer(resourceProvider = resourceProvider)
                        .transformRecordedToUi(transactions = transactions, sectionId = sectionId),
                )
            ),
            emptyItem = null,
        )
    }

    private fun InteractingPartyDomain.toDetailsFields(prefix: String): List<TransactionDetailsFieldUi> {
        return listOfNotNull(
            textField("$prefix:name", R.string.transaction_details_name_label, name?.text),
            textField(
                "$prefix:id",
                R.string.transaction_details_identifier_label,
                identifier?.value
            ),
            textField(
                "$prefix:scheme",
                R.string.transaction_details_identifier_scheme_label,
                identifier?.schemeUri
            ),
        ) + contactFields(
            prefix = prefix,
            contacts = contacts,
            labelRes = R.string.transaction_details_contact_label,
        )
    }

    private fun DpaContactDomain.toAuthoritySection(): TransactionDetailsSectionUi {
        return fieldsSection(
            titleRes = R.string.transaction_details_authority_section_title,
            fields = listOfNotNull(
                textField("authority:name", R.string.transaction_details_name_label, name?.text),
                textField(
                    "authority:country",
                    R.string.transaction_details_country_label,
                    country?.text
                ),
            ) + contactFields(
                prefix = "authority",
                contacts = contacts,
                labelRes = R.string.transaction_details_contact_label,
            ),
        )
    }

    private fun IssuanceDetailsDomain.toCountFields(): List<TransactionDetailsFieldUi> {
        return listOfNotNull(
            textField(
                id = "requested-count",
                labelRes = R.string.transaction_details_requested_count_label,
                value = requestedCount.toString(),
            ),
            textField(
                id = "issued-count",
                labelRes = R.string.transaction_details_issued_count_label,
                value = issuedCount.toString(),
            ),
        )
    }

    private fun IssuanceDetailsDomain.toTriggerField(): TransactionDetailsFieldUi? {
        val triggerLabel = when (isUserTriggered) {
            true -> resourceProvider.getString(R.string.transaction_details_requested_by_you)
            false -> resourceProvider.getString(R.string.transaction_details_renewed_by_wallet)
            null -> null
        }
        return textField("trigger", R.string.transaction_details_trigger_label, triggerLabel)
    }

    private fun credentialsSection(
        titleRes: Int,
        credentials: List<CredentialRefDomain>,
    ): TransactionDetailsSectionUi? {
        val fields = credentials.mapIndexedNotNull { index, credential ->
            credential.identifier.takeIf { identifier -> identifier.isNotBlank() }
                ?.let { identifier ->
                    TransactionDetailsFieldUi(
                        id = "credential:$index",
                        label = null,
                        value = identifier,
                        url = null,
                    )
                }
        }
        return fields.takeIf { availableFields -> availableFields.isNotEmpty() }
            ?.let { availableFields ->
                fieldsSection(
                    titleRes = titleRes,
                    fields = availableFields,
                )
            }
    }

    private fun claimsSection(
        prefix: String,
        titleRes: Int,
        claims: List<CredentialClaimsDomain>,
        emptyRes: Int,
    ): TransactionDetailsSectionUi {
        return TransactionDetailsSectionUi(
            title = resourceProvider.getString(titleRes),
            items = emptyList(),
            groups = claims.mapIndexed { credentialIndex, credentialClaims ->
                val groupId = "$prefix:$credentialIndex"
                TransactionDetailsGroupUi(
                    header = ListItemDataUi(
                        itemId = groupId,
                        mainContentData = ListItemMainContentDataUi.Text(credentialClaims.credential.identifier),
                        supportingContentData = ListItemSupportingContentDataUi.Text(
                            resourceProvider.getString(R.string.transaction_details_collapsed_supporting_text),
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowDown),
                    ),
                    items = credentialClaims.claims.mapIndexed { claimIndex, claim ->
                        ExpandableListItemUi.SingleListItem(
                            header = ListItemDataUi(
                                itemId = "$groupId:$claimIndex",
                                mainContentData = ListItemMainContentDataUi.Text(claim.toIdentifierPath()),
                            ),
                        )
                    }.ifEmpty {
                        listOf(
                            ExpandableListItemUi.SingleListItem(
                                header = ListItemDataUi(
                                    itemId = "$groupId:empty",
                                    mainContentData = ListItemMainContentDataUi.Text(
                                        resourceProvider.getString(R.string.transaction_details_no_claims),
                                    ),
                                ),
                            ),
                        )
                    },
                )
            },
            emptyItem = if (claims.isEmpty()) {
                ListItemDataUi(
                    itemId = "$prefix:empty",
                    mainContentData = ListItemMainContentDataUi.Text(
                        resourceProvider.getString(emptyRes),
                    ),
                )
            } else {
                null
            },
        )
    }

    private fun ClaimRefDomain.toIdentifierPath(): String {
        if (segments.isEmpty()) {
            return resourceProvider.getString(R.string.transaction_details_unknown_claim)
        }
        return segments.joinToString(separator = "") { segment ->
            when (segment) {
                is ClaimPathSegment.Key -> "[${JsonPrimitive(segment.name)}]"
                is ClaimPathSegment.Index -> "[${segment.index}]"
                is ClaimPathSegment.AllElements -> "[*]"
            }
        }
    }

    private fun fieldsSection(
        titleRes: Int,
        fields: List<TransactionDetailsFieldUi>,
    ): TransactionDetailsSectionUi {
        return TransactionDetailsSectionUi(
            title = resourceProvider.getString(titleRes),
            items = fields.map { field ->
                TransactionDetailsItemUi(item = field.item, url = field.url)
            },
            groups = emptyList(),
            emptyItem = if (fields.isEmpty()) {
                ListItemDataUi(
                    itemId = "section:$titleRes:empty",
                    mainContentData = ListItemMainContentDataUi.Text(
                        resourceProvider.getString(R.string.transaction_details_no_information),
                    ),
                )
            } else {
                null
            },
        )
    }

    private fun textField(
        id: String,
        labelRes: Int,
        value: String?
    ): TransactionDetailsFieldUi? {
        return value
            ?.takeIf { value -> value.isNotBlank() }
            ?.let { text ->
                TransactionDetailsFieldUi(
                    id = id,
                    label = resourceProvider.getString(labelRes),
                    value = text,
                    url = null,
                )
            }
    }

    private fun webField(
        id: String,
        labelRes: Int,
        value: String?
    ): TransactionDetailsFieldUi? {
        return textField(
            id = id,
            labelRes = labelRes,
            value = value
        )?.let { field ->
            field.copy(url = field.value.toWebUrlOrNull())
        }
    }

    private fun contactFields(
        prefix: String,
        contacts: List<String>,
        labelRes: Int,
    ): List<TransactionDetailsFieldUi> {
        return contacts.mapIndexedNotNull { index, contact ->
            textField(
                "$prefix:contact:$index",
                labelRes,
                contact
            )?.copy(url = contact.toContactUrlOrNull())
        }
    }

    private fun String.toWebUrlOrNull(): String? =
        toPrivacyContactOrNull()?.takeIf { contact ->
            contact.method == CommunicationMethodDomain.Website
        }?.url

    private fun String.toContactUrlOrNull(): String? = toPrivacyContactOrNull()?.url
}