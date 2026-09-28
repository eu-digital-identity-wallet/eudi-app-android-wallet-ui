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

package eu.europa.ec.dashboardfeature.util

import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimRefDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.CredentialClaimsDomain
import eu.europa.ec.corelogic.model.CredentialRefDomain
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.InteractingPartyDomain
import eu.europa.ec.corelogic.model.IssuanceDetailsDomain
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.PresentationRegistrationDomain
import eu.europa.ec.corelogic.model.QualifiedIdentifierDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.testfeature.util.mockedFormattedExpirationDate
import eu.europa.ec.testfeature.util.mockedFormattedIssuanceDate
import eu.europa.ec.testfeature.util.mockedMdlDocName
import eu.europa.ec.testfeature.util.mockedMdlId
import eu.europa.ec.testfeature.util.mockedMdocMdlNameSpace
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import java.time.LocalDateTime

internal const val mockedBookmarkId = "mockedBookmarkId"
internal const val mockedChangeLogUrl = "https://example.com/changelog"

private const val mockedClaimIsRequired = false

internal val mockedFullPidUi = DocumentDetailsUi(
    documentId = mockedPidId,
    documentName = mockedPidDocName,
    issuerId = "",
    documentConfigId = "",
    documentIdentifier = DocumentIdentifier.MdocPid,
    documentClaims = emptyList(),
)

internal val mockedPendingPidUi = mockedFullPidUi

internal val mockedUnsignedPidUi = mockedFullPidUi.copy(
    documentName = mockedPidDocName,
    documentIdentifier = DocumentIdentifier.MdocPid,
)

internal val mockedBasicPidDomain = DocumentDetailsDomain(
    docName = mockedPidDocName,
    docId = mockedPidId,
    issuerId = "",
    documentConfigId = "",
    documentIdentifier = DocumentIdentifier.MdocPid,
    documentClaims = listOf(
        ClaimDomain.Primitive(
            key = "family_name",
            value = "ANDERSSON",
            displayTitle = "family_name",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("family_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "given_name",
            value = "JAN",
            displayTitle = "given_name",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("given_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "age_over_18",
            value = "yes",
            displayTitle = "age_over_18",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("age_over_18"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "age_over_65",
            value = "no",
            displayTitle = "age_over_65",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("age_over_65"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "age_birth_year",
            value = "1985",
            displayTitle = "age_birth_year",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("age_birth_year"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "birth_city",
            value = "KATRINEHOLM",
            displayTitle = "birth_city",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("birth_city"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "gender",
            value = "Male",
            displayTitle = "gender",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("gender"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "expiry_date",
            value = "30 Mar 2050",
            displayTitle = "expiry_date",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("expiry_date"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        )
    ).sortedBy {
        it.displayTitle.lowercase()
    },
    documentIssuanceDate = mockedFormattedIssuanceDate,
    documentExpirationDate = mockedFormattedExpirationDate,
)

internal val mockedFullMdlUi = DocumentDetailsUi(
    documentId = mockedMdlId,
    documentName = mockedMdlDocName,
    issuerId = "",
    documentConfigId = "",
    documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
    documentClaims = emptyList(),
)

internal val mockedPendingMdlUi = mockedFullMdlUi

internal val mockedBasicMdlUi = mockedFullMdlUi.copy(
    documentClaims = listOf(
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "expiry_date",
                mainContentData = ListItemMainContentDataUi.Text("30 Mar 2050")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "sex",
                mainContentData = ListItemMainContentDataUi.Text("male")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "birth_place",
                mainContentData = ListItemMainContentDataUi.Text("SWEDEN")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "portrait",
                mainContentData = ListItemMainContentDataUi.Image("SE")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "given_name",
                mainContentData = ListItemMainContentDataUi.Text("JAN")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "family_name",
                mainContentData = ListItemMainContentDataUi.Text("ANDERSSON")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "signature_usual_mark",
                mainContentData = ListItemMainContentDataUi.Image("SE")
            )
        )
    )
)

internal val mockedBasicMdlDomain = DocumentDetailsDomain(
    docName = mockedMdlDocName,
    docId = mockedMdlId,
    issuerId = "",
    documentConfigId = "",
    documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
    documentClaims = listOf(
        ClaimDomain.Primitive(
            key = "family_name",
            value = "ANDERSSON",
            displayTitle = "family_name",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("family_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "given_name",
            value = "JAN",
            displayTitle = "given_name",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("given_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "birth_place",
            value = "SWEDEN",
            displayTitle = "birth_place",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("birth_place"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "expiry_date",
            value = "30 Mar 2050",
            displayTitle = "expiry_date",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("expiry_date"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "portrait",
            value = "SE",
            displayTitle = "portrait",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("portrait"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "signature_usual_mark",
            value = "SE",
            displayTitle = "signature_usual_mark",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("signature_usual_mark"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "sex",
            value = "Male",
            displayTitle = "sex",
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf("sex"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        )
    ).sortedBy {
        it.displayTitle.lowercase()
    },
    documentIssuanceDate = mockedFormattedIssuanceDate,
    documentExpirationDate = mockedFormattedExpirationDate,
)

internal val mockedMdlUiWithNoUserNameAndNoUserImage: DocumentDetailsUi = mockedFullMdlUi

internal val mockedFullDocumentsUi: List<DocumentDetailsUi> = listOf(
    mockedFullPidUi, mockedFullMdlUi
)

internal val mockedTransactionDateTime: LocalDateTime = LocalDateTime.of(2026, 3, 15, 14, 30)
internal const val mockedTransactionPartyName = "Example verifier"
internal const val mockedTransactionIntermediaryName = "Example intermediary"
internal const val mockedTransactionIssuerName = "Example issuer"
internal const val mockedTransactionServiceName = "Example signing service"
internal const val mockedTransactionDpaName = "Example authority"
internal const val mockedTransactionLanguageTag = "en"
internal val mockedTransactionParty = InteractingPartyDomain(
    LocalizedTextDomain(mockedTransactionLanguageTag, mockedTransactionPartyName),
    null,
    emptyList(),
)
internal val mockedTransactionIntermediary = InteractingPartyDomain(
    LocalizedTextDomain(mockedTransactionLanguageTag, mockedTransactionIntermediaryName),
    null,
    emptyList(),
)
internal val mockedTransactionCredential = CredentialRefDomain(mockedMdocPidNameSpace)
internal const val mockedTransactionClaimPath = "[\"eu.europa.ec.eudi.pid.1\"][\"family_name\"]"
internal val mockedTransactionClaims = listOf(
    CredentialClaimsDomain(
        credential = mockedTransactionCredential,
        claims = listOf(
            ClaimRefDomain(
                segments = listOf(
                    ClaimPathSegment.Key(mockedMdocPidNameSpace),
                    ClaimPathSegment.Key("family_name"),
                ),
            )
        ),
    )
)
internal val mockedIssuanceDetails = IssuanceDetailsDomain(
    issuer = mockedTransactionParty.copy(
        name = LocalizedTextDomain(
            mockedTransactionLanguageTag,
            mockedTransactionIssuerName
        )
    ),
    issuerType = "PIDProvider",
    requestedCount = 1,
    issuedCount = 1,
    credentials = listOf(mockedTransactionCredential),
    isUserTriggered = true,
)
internal val mockedPresentationLogDomain = TransactionLogDomain.Presentation(
    transactionData = emptyList(),
    id = "presentation",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    party = mockedTransactionParty,
    partyType = "ServiceProvider",
    intermediary = null,
    registration = null,
    claimsRequested = mockedTransactionClaims,
    claimsPresented = mockedTransactionClaims,
)
internal val mockedIssuanceLogDomain = TransactionLogDomain.CredentialIssuance(
    id = "issuance",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    details = mockedIssuanceDetails,
)
internal val mockedReissuanceLogDomain = TransactionLogDomain.CredentialReissuance(
    id = "reissuance",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    details = mockedIssuanceDetails.copy(isUserTriggered = false),
)
internal val mockedDeletionLogDomain = TransactionLogDomain.CredentialDeletion(
    id = "deletion",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    credential = mockedTransactionCredential,
    issuer = mockedIssuanceDetails.issuer,
)
internal val mockedSigningLogDomain = TransactionLogDomain.SigningSealing(
    id = "signing",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    service = mockedTransactionParty.copy(
        name = LocalizedTextDomain(
            mockedTransactionLanguageTag,
            mockedTransactionServiceName
        )
    ),
    serviceType = "ESigESealCreationProvider",
    signingTransactionId = null,
    certificateSerialNumber = "serial",
    fileName = "signed.pdf",
    fileSizeBytes = 1024,
    dtbsr = null,
)
internal val mockedDataDeletionLogDomain = TransactionLogDomain.DataDeletionRequest(
    id = "data-deletion",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    parentPresentationId = mockedPresentationLogDomain.id,
    communicationMethod = CommunicationMethodDomain.Email,
    party = mockedTransactionParty,
    claims = mockedTransactionClaims,
)
internal val mockedDpaReportLogDomain = TransactionLogDomain.DpaReport(
    id = "report",
    time = mockedTransactionDateTime,
    result = TransactionResultDomain.Completed,
    parentPresentationId = mockedPresentationLogDomain.id,
    communicationMethod = CommunicationMethodDomain.Phone,
    dpaName = LocalizedTextDomain(mockedTransactionLanguageTag, mockedTransactionDpaName),
    dpaCountry = LocalizedTextDomain(mockedTransactionLanguageTag, "Greece"),
)
internal val mockedTransactionLogDomains = listOf(
    mockedPresentationLogDomain,
    mockedIssuanceLogDomain,
    mockedReissuanceLogDomain,
    mockedDeletionLogDomain,
    mockedSigningLogDomain,
    mockedDataDeletionLogDomain,
    mockedDpaReportLogDomain,
)
internal const val mockedCredentialDeletionTypeLabel = "Deletion"
internal const val mockedDataDeletionTypeLabel = "Data Deletion Requests"
internal const val mockedDpaReportTypeLabel = "Suspicious Transaction Reports"
internal val mockedTransactionTypeLabels = listOf(
    "Presentation",
    "Issuance",
    "Re-issuance",
    mockedCredentialDeletionTypeLabel,
    "Signing",
    mockedDataDeletionTypeLabel,
    mockedDpaReportTypeLabel,
)

internal val mockedRequestedTransactionCredential = CredentialRefDomain(
    identifier = "urn:credential:requested",
)
internal val mockedPresentedTransactionCredential = CredentialRefDomain(
    identifier = "urn:credential:presented",
)
internal val mockedOtherTransactionCredential = CredentialRefDomain(
    identifier = "urn:credential:deleted",
)
internal val mockedSearchableTransactionLogDomains = listOf(
    mockedPresentationLogDomain.copy(
        intermediary = mockedTransactionIntermediary,
        claimsRequested = listOf(
            mockedTransactionClaims.single().copy(credential = mockedRequestedTransactionCredential)
        ),
        claimsPresented = listOf(
            mockedTransactionClaims.single().copy(credential = mockedPresentedTransactionCredential)
        ),
    ),
    mockedIssuanceLogDomain.copy(
        details = mockedIssuanceDetails.copy(
            requestedCount = 2,
            issuedCount = 2,
            credentials = listOf(mockedTransactionCredential, mockedRequestedTransactionCredential),
        )
    ),
    mockedReissuanceLogDomain.copy(
        details = mockedReissuanceLogDomain.details.copy(
            credentials = listOf(mockedPresentedTransactionCredential)
        )
    ),
    mockedDeletionLogDomain.copy(credential = mockedOtherTransactionCredential),
    mockedSigningLogDomain,
    mockedDataDeletionLogDomain.copy(
        claims = listOf(
            mockedTransactionClaims.single()
                .copy(credential = mockedPresentedTransactionCredential),
            mockedTransactionClaims.single().copy(credential = mockedOtherTransactionCredential),
        )
    ),
    mockedDpaReportLogDomain,
)
internal val mockedTransactionPartyNames = listOf(
    mockedTransactionPartyName,
    mockedTransactionIssuerName,
    mockedTransactionIssuerName,
    mockedTransactionIssuerName,
    mockedTransactionServiceName,
    mockedTransactionPartyName,
    mockedTransactionDpaName,
)
internal val mockedNotCompletedTransactionLogDomains = listOf(
    mockedPresentationLogDomain.copy(
        id = "incomplete-presentation",
        result = TransactionResultDomain.NotCompleted("Consent declined"),
    ),
    mockedIssuanceLogDomain.copy(
        id = "incomplete-issuance",
        result = TransactionResultDomain.NotCompleted("Awaiting deferred credential"),
    ),
    mockedReissuanceLogDomain.copy(
        id = "incomplete-reissuance",
        result = TransactionResultDomain.NotCompleted("Renewal stopped"),
    ),
    mockedDeletionLogDomain.copy(
        id = "incomplete-deletion",
        result = TransactionResultDomain.NotCompleted(null),
    ),
    mockedSigningLogDomain.copy(
        id = "incomplete-signing",
        result = TransactionResultDomain.NotCompleted("Signing stopped"),
    ),
    mockedDataDeletionLogDomain.copy(
        id = "incomplete-data-deletion",
        result = TransactionResultDomain.NotCompleted(" "),
    ),
    mockedDpaReportLogDomain.copy(
        id = "incomplete-report",
        result = TransactionResultDomain.NotCompleted(null),
    ),
)
internal val mockedUnnamedTransactionLogDomains = listOf(
    mockedPresentationLogDomain.copy(party = mockedTransactionParty.copy(name = null)),
    mockedIssuanceLogDomain.copy(
        details = mockedIssuanceDetails.copy(
            issuer = mockedTransactionParty.copy(
                name = LocalizedTextDomain(mockedTransactionLanguageTag, "")
            )
        )
    ),
    mockedReissuanceLogDomain.copy(
        details = mockedReissuanceLogDomain.details.copy(
            issuer = mockedTransactionParty.copy(
                name = null
            )
        )
    ),
    mockedDeletionLogDomain.copy(
        issuer = mockedTransactionParty.copy(
            name = LocalizedTextDomain(
                mockedTransactionLanguageTag,
                " "
            )
        ),
    ),
    mockedSigningLogDomain.copy(
        service = mockedTransactionParty.copy(
            name = LocalizedTextDomain(
                mockedTransactionLanguageTag,
                " "
            )
        ),
        fileName = " ",
    ),
    mockedDataDeletionLogDomain.copy(party = mockedTransactionParty.copy(name = null)),
    mockedDpaReportLogDomain.copy(dpaName = LocalizedTextDomain(mockedTransactionLanguageTag, "")),
)

internal val mockedTransactionQualifiedIdentifier = QualifiedIdentifierDomain(
    schemeUri = "urn:example:register",
    value = "registered-123",
)
internal val mockedTransactionPartyWithContacts = mockedTransactionParty.copy(
    identifier = mockedTransactionQualifiedIdentifier,
    contacts = listOf("GR", "https://example.com/support", "https://example.com/info"),
)
internal val mockedTransactionRegistration = PresentationRegistrationDomain(
    registrarUrl = "https://example.com/register",
    purpose = "Confirm eligibility",
    privacyPolicyUrls = listOf("https://example.com/privacy", "http://example.com/other-policy"),
    dpa = DpaContactDomain(
        name = LocalizedTextDomain(mockedTransactionLanguageTag, mockedTransactionDpaName),
        country = LocalizedTextDomain(mockedTransactionLanguageTag, "Greece"),
        contacts = listOf(
            "authority@example.com",
            "+30 (210) 123-4567",
            "https://example.com/authority"
        ),
    ),
)
internal val mockedDetailedPresentationLogDomain = mockedPresentationLogDomain.copy(
    party = mockedTransactionPartyWithContacts,
    registration = mockedTransactionRegistration,
)
internal val mockedNestedTransactionClaims = listOf(
    CredentialClaimsDomain(
        credential = mockedOtherTransactionCredential,
        claims = listOf(
            ClaimRefDomain(
                listOf(
                    ClaimPathSegment.Key("addresses"),
                    ClaimPathSegment.Index(0),
                    ClaimPathSegment.Key("street.name"),
                ),
            ),
            ClaimRefDomain(
                listOf(ClaimPathSegment.Key("addresses"), ClaimPathSegment.AllElements),
            ),
            ClaimRefDomain(emptyList()),
        ),
    )
)
internal val mockedTransactionDetailsStrings = mapOf(
    R.string.transaction_details_signing_request_title to "SIGNING REQUEST",
    R.string.transaction_details_data_requested_section_title to "DATA REQUESTED",
    R.string.transaction_details_relying_party_section_title to "RELYING PARTY",
    R.string.transaction_details_authority_section_title to "DATA PROTECTION AUTHORITY",
    R.string.transaction_details_credentials_section_title to "CREDENTIALS",
    R.string.transaction_details_credentials_issued_section_title to "CREDENTIALS ISSUED",
    R.string.transaction_details_data_deletion_section_title to "DATA REQUESTED FOR DELETION",
    R.string.transaction_details_no_data_requested to "No data requested",
    R.string.transaction_details_no_data_shared to "No data shared",
    R.string.transaction_details_no_claims to "No attributes recorded",
    R.string.transaction_details_no_information to "No information recorded",
    R.string.transaction_details_unknown_claim to "Attribute identifier unavailable",
    R.string.transaction_details_name_label to "Name",
    R.string.transaction_details_intermediary_name_label to "Intermediary name",
    R.string.transaction_details_identifier_label to "Identifier",
    R.string.transaction_details_identifier_scheme_label to "Identifier scheme",
    R.string.transaction_details_contact_label to "Contact",
    R.string.transaction_details_intermediary_contact_label to "Intermediary contact",
    R.string.transaction_details_country_label to "Country",
    R.string.transaction_details_purpose_label to "Intended use",
    R.string.transaction_details_privacy_policy_label to "Privacy policy",
    R.string.transaction_details_requested_count_label to "Credentials requested",
    R.string.transaction_details_issued_count_label to "Credentials issued",
    R.string.transaction_details_trigger_label to "Initiated by",
    R.string.transaction_details_requested_by_you to "Wallet holder",
    R.string.transaction_details_renewed_by_wallet to "The Wallet",
    R.string.transaction_details_filename_label to "File name",
    R.string.transaction_details_signing_identifier_label to "Signing transaction identifier",
    R.string.transactions_screen_filters_filter_by_transaction_type_presentation to "Presentation",
    R.string.transactions_screen_filters_filter_by_transaction_type_issuance to "Issuance",
    R.string.transactions_screen_filters_filter_by_transaction_type_reissuance to "Re-issuance",
    R.string.transactions_screen_filters_filter_by_transaction_type_deletion to mockedCredentialDeletionTypeLabel,
    R.string.transactions_screen_filters_filter_by_transaction_type_signing to "Signing",
    R.string.transaction_type_data_deletion_request to mockedDataDeletionTypeLabel,
    R.string.transaction_type_dpa_report to mockedDpaReportTypeLabel,
    R.string.transactions_filter_item_status_completed to "Completed",
    R.string.transactions_filter_item_status_not_completed to "Not completed",
    R.string.transaction_details_data_shared_section_title to "DATA SHARED",
    R.string.transaction_details_data_signed_section_title to "DATA SIGNED",
    R.string.transaction_details_collapsed_supporting_text to "View details",
    R.string.transaction_details_screen_title to "Transaction information",
)