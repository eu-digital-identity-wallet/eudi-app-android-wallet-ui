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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimRefDomain
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.CredentialClaimsDomain
import eu.europa.ec.corelogic.model.CredentialRefDomain
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.InteractingPartyDomain
import eu.europa.ec.corelogic.model.IssuanceDetailsDomain
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.PresentationRegistrationDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.QualifiedIdentifierDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.corelogic.util.mockedClaimName
import eu.europa.ec.corelogic.util.mockedClaimSegments
import eu.europa.ec.corelogic.util.mockedCredentialType
import eu.europa.ec.corelogic.util.mockedEnglishLocale
import eu.europa.ec.corelogic.util.mockedGreekLocale
import eu.europa.ec.corelogic.util.mockedTransactionId
import eu.europa.ec.corelogic.util.mockedTransactionTime
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.Policy
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionalData
import eu.europa.ec.eudi.wallet.transactionLogging.toJson
import eu.europa.ec.eudi.wallet.transactionLogging.toTransactionEntryOrNull
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalRequest
import eu.europa.ec.storagelogic.model.TransactionLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class TestTransactionEntryExtensions {

    private val mockedMethod = CommunicationMethodDomain.Email
    private val mockedStoredMethod = mockedMethod.toStoredCommunicationMethod()

    //region toTransactionLogDomain
    // Case 1:
    // 1. A completed presentation with registration and raw claim identifiers.
    //
    // Case 1 Expected Result:
    // The complete domain is mapped.
    @Test
    fun `Given a completed presentation with registration and raw claim identifiers, When toTransactionLogDomain is called, Then the complete domain is mapped`() {
        // Given
        val entry = mockedPresentation.copy(
            interactingPartyIdentifier = mockedIdentifier,
            interactingPartyContact = mockedContacts,
            interactingPartyType = "RecordedProviderType",
            registrarURL = mockedUrl,
            purpose = mockedPurposes,
            privacyPolicy = listOf(Policy(type = Policy.PRIVACY_STATEMENT, policyURI = mockedUrl)),
            dpaName = mockedName,
            dpaCountry = mockedCountry,
            dpaContact = mockedContacts,
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedGreekLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        )

        // Then
        assertEquals(
            TransactionLogDomain.Presentation(
                transactionData = emptyList(),
                id = mockedTransactionId,
                time = mockedLocalTime,
                result = TransactionResultDomain.Completed,
                party = mockedParty,
                partyType = "RecordedProviderType",
                intermediary = null,
                registration = PresentationRegistrationDomain(
                    registrarUrl = mockedUrl,
                    purpose = mockedGreekPurpose,
                    privacyPolicyUrls = listOf(mockedUrl),
                    dpa = DpaContactDomain(mockedNameDomain, mockedCountryDomain, mockedContacts),
                ),
                claimsRequested = listOf(
                    CredentialClaimsDomain(
                        CredentialRefDomain(mockedCredentialType),
                        listOf(ClaimRefDomain(mockedClaimSegments)),
                    )
                ),
                claimsPresented = emptyList(),
            ),
            result,
        )
    }

    // Case 2:
    // 1. A non-completed presentation with no party or reason.
    //
    // Case 2 Expected Result:
    // Absent data stays absent.
    @Test
    fun `Given a non-completed presentation with no party or reason, When toTransactionLogDomain is called, Then absent data stays absent`() {
        // Given
        val entry = mockedPresentation.copy(
            transactionResult = TransactionResult.NotCompleted(reason = null),
            interactingPartyName = null,
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.Presentation

        // Then
        assertEquals(TransactionResultDomain.NotCompleted(reason = null), result.result)
        assertEquals(InteractingPartyDomain(null, null, emptyList()), result.party)
        assertNull(result.intermediary)
        assertNull(result.registration)
        assertEquals(mockedRawClaims, result.claimsRequested)
        assertEquals(emptyList<CredentialClaimsDomain>(), result.claimsPresented)
    }

    // Case 3:
    // 1. A presentation records intermediary identity and contacts without registration details.
    // 2. Its intermediary flag may be true, false or absent.
    //
    // Case 3 Expected Result:
    // The recorded intermediary is retained separately from the relying party in every case.
    @Test
    fun `Given recorded intermediary data, When toTransactionLogDomain is called, Then it is retained independently of the flag`() {
        // Given
        val entries = listOf(true, false, null).map { flag ->
            mockedPresentation.copy(
                isIntermediary = flag,
                intermediaryName = mockedIntermediaryName,
                intermediaryIdentifier = mockedIdentifier,
                intermediaryContact = mockedContacts,
            )
        }

        // When
        val results = entries.map { entry ->
            entry.toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            )
        }
        val withoutIntermediary = mockedPresentation.toTransactionLogDomain(
            mockedTransactionId,
            mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.Presentation

        // Then
        results.forEach { result ->
            assertEquals(withoutIntermediary.copy(intermediary = mockedIntermediary), result)
        }
    }

    // Case 4:
    // 1. Presented paths containing keys indices and wildcards.
    //
    // Case 4 Expected Result:
    // All segment kinds survive without values.
    @Test
    fun `Given presented paths containing keys indices and wildcards, When toTransactionLogDomain is called, Then all segment kinds survive without values`() {
        // Given
        val entry = mockedPresentation.copy(
            listOfClaimsRequested = emptyList(),
            listOfClaimsPresented = listOf(
                ClaimInfo(
                    credentialIdentifier = mockedCredentialType,
                    claims = listOf(
                        ClaimPath(
                            listOf(
                                ClaimPath.Segment.Key("items"),
                                ClaimPath.Segment.Index(2),
                                ClaimPath.Segment.Key("0")
                            )
                        ),
                        ClaimPath(
                            listOf(
                                ClaimPath.Segment.Key("items"),
                                ClaimPath.Segment.Wildcard,
                                ClaimPath.Segment.Key("null")
                            )
                        ),
                    ),
                )
            ),
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.Presentation

        // Then
        assertEquals(emptyList<CredentialClaimsDomain>(), result.claimsRequested)
        assertEquals(
            listOf(
                listOf(
                    ClaimPathSegment.Key("items"),
                    ClaimPathSegment.Index(2),
                    ClaimPathSegment.Key("0")
                ),
                listOf(
                    ClaimPathSegment.Key("items"),
                    ClaimPathSegment.AllElements,
                    ClaimPathSegment.Key("null")
                ),
            ),
            result.claimsPresented.single().claims.map { claim -> claim.segments },
        )
    }

    // Case 5:
    // 1. Issuance and re-issuance entries with batch and trigger information.
    //
    // Case 5 Expected Result:
    // Their types and details remain distinct.
    @Test
    fun `Given issuance and re-issuance entries with batch and trigger information, When toTransactionLogDomain is called, Then their types and details remain distinct`() {
        // Given
        val triggers = listOf(true, false, null)
        val details = TransactionEntry.CredentialIssuanceDetails(
            interactingPartyType = "PIDProvider",
            credentialNumberRequested = 3,
            credentialNumberIssued = 1,
            credentialIdentifier = listOf(mockedCredentialType),
            interactingPartyName = mockedName,
            interactingPartyIdentifier = mockedIdentifier,
            interactingPartyContact = mockedContacts,
        )
        val entries = triggers.flatMap { trigger ->
            val payload = details.copy(isUserTriggered = trigger)
            listOf(
                TransactionEntry.CredentialIssuance(
                    mockedTransactionId,
                    mockedTransactionTime,
                    TransactionResult.Completed,
                    payload
                ),
                TransactionEntry.CredentialReissuance(
                    mockedTransactionId,
                    mockedTransactionTime,
                    TransactionResult.Completed,
                    payload
                ),
            )
        }

        // When
        val results = entries.map { entry ->
            entry.toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            )
        }

        // Then
        val expected = triggers.flatMap { trigger ->
            val payload = IssuanceDetailsDomain(
                issuer = mockedParty,
                issuerType = "PIDProvider",
                requestedCount = 3,
                issuedCount = 1,
                credentials = listOf(CredentialRefDomain(mockedCredentialType)),
                isUserTriggered = trigger,
            )
            listOf(
                TransactionLogDomain.CredentialIssuance(
                    mockedTransactionId,
                    mockedLocalTime,
                    TransactionResultDomain.Completed,
                    payload
                ),
                TransactionLogDomain.CredentialReissuance(
                    mockedTransactionId,
                    mockedLocalTime,
                    TransactionResultDomain.Completed,
                    payload
                ),
            )
        }
        assertEquals(expected, results)
    }

    // Case 6:
    // 1. An issuance without issued credentials or issuer metadata.
    //
    // Case 6 Expected Result:
    // Counts and the non-completion reason survive.
    @Test
    fun `Given an issuance without issued credentials or issuer metadata, When toTransactionLogDomain is called, Then counts and the non-completion reason survive`() {
        // Given
        val entry = TransactionEntry.CredentialIssuance(
            mockedTransactionId,
            mockedTransactionTime,
            TransactionResult.NotCompleted(mockedReason),
            TransactionEntry.CredentialIssuanceDetails(2, 0, emptyList()),
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.CredentialIssuance

        // Then
        assertEquals(TransactionResultDomain.NotCompleted(mockedReason), result.result)
        assertEquals(
            IssuanceDetailsDomain(
                InteractingPartyDomain(null, null, emptyList()),
                null,
                2,
                0,
                emptyList(),
                null
            ), result.details
        )
    }

    // Case 7:
    // 1. A deleted credential with and without issuer information.
    //
    // Case 7 Expected Result:
    // The recorded identifier remains readable.
    @Test
    fun `Given a deleted credential with and without issuer information, When toTransactionLogDomain is called, Then the recorded identifier remains readable`() {
        // Given
        val entry = TransactionEntry.CredentialDeletion(
            mockedTransactionId,
            mockedTransactionTime,
            TransactionResult.Completed,
            mockedCredentialType,
            credentialIssuerIdentifier = mockedIdentifier,
            credentialIssuerName = mockedName,
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        )
        val anonymous = entry.copy(credentialIssuerIdentifier = null, credentialIssuerName = null)
            .toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.CredentialDeletion

        // Then
        assertEquals(
            TransactionLogDomain.CredentialDeletion(
                mockedTransactionId, mockedLocalTime, TransactionResultDomain.Completed,
                CredentialRefDomain(mockedCredentialType), mockedParty.copy(contacts = emptyList()),
            ),
            result,
        )
        assertEquals(InteractingPartyDomain(null, null, emptyList()), anonymous.issuer)
    }

    // Case 8:
    // 1. A signing record with file certificate and service details.
    //
    // Case 8 Expected Result:
    // Signing fields and failure reason are retained.
    @Test
    fun `Given a signing record with file certificate and service details, When toTransactionLogDomain is called, Then signing fields and failure reason are retained`() {
        // Given
        val entry = mockedSigning.copy(
            transactionResult = TransactionResult.NotCompleted(mockedReason),
            certificateIdentifier = "certificate-serial",
            interactingPartyType = "RecordedSigningProvider",
            signingTransactionIdentifier = "signing-session-123",
            fileName = "signed.pdf",
            fileSize = "4294967296",
            dtbsr = "digest",
            interactingPartyName = mockedName,
            interactingPartyIdentifier = mockedIdentifier,
            interactingPartyContact = mockedContacts,
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        )

        // Then
        assertEquals(
            TransactionLogDomain.SigningSealing(
                mockedTransactionId,
                mockedLocalTime,
                TransactionResultDomain.NotCompleted(mockedReason),
                mockedParty,
                "RecordedSigningProvider",
                "signing-session-123",
                "certificate-serial",
                "signed.pdf",
                4294967296L,
                "digest",
            ),
            result,
        )
    }

    // Case 9:
    // 1. Missing malformed and overflowing file sizes.
    //
    // Case 9 Expected Result:
    // Signing remains readable without a size.
    @Test
    fun `Given missing malformed and overflowing file sizes, When toTransactionLogDomain is called, Then signing remains readable without a size`() {
        // Given
        val sizes = listOf(null, "", "not-a-number", "9223372036854775808")

        // When
        val results = sizes.map { size ->
            mockedSigning.copy(fileSize = size).toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.SigningSealing
        }

        // Then
        results.forEach { result ->
            assertEquals(InteractingPartyDomain(null, null, emptyList()), result.service)
            assertNull(result.fileSizeBytes)
            assertNull(result.fileName)
            assertNull(result.certificateSerialNumber)
            assertNull(result.dtbsr)
            assertNull(result.signingTransactionId)
        }
    }

    // Case 10:
    // 1. A data deletion request with claim paths and its initiating presentation ID.
    //
    // Case 10 Expected Result:
    // Own ID, parent ID, party and claims are mapped independently.
    @Test
    fun `Given a data deletion request with claim paths, When toTransactionLogDomain is called, Then party and claims are mapped`() {
        // Given
        val entry = TransactionEntry.DataDeletionRequest(
            mockedActionId, mockedTransactionTime, TransactionResult.Completed,
            listOfClaims = mockedPresentation.listOfClaimsRequested,
            interactingPartyIdentifier = mockedIdentifier,
            interactingPartyName = mockedName,
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedActionId,
            mockedEnglishLocale,
            parentPresentationId = mockedTransactionId,
            communicationMethod = mockedStoredMethod,
            transactionDataTypes = emptyList(),
        )
        val anonymous = entry.copy(interactingPartyIdentifier = null, interactingPartyName = null)
            .toTransactionLogDomain(
                mockedActionId,
                mockedEnglishLocale,
                parentPresentationId = mockedTransactionId,
                communicationMethod = mockedStoredMethod,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.DataDeletionRequest

        // Then
        assertEquals(
            TransactionLogDomain.DataDeletionRequest(
                id = mockedActionId,
                time = mockedLocalTime,
                result = TransactionResultDomain.Completed,
                parentPresentationId = mockedTransactionId,
                communicationMethod = mockedMethod,
                party = mockedParty.copy(contacts = emptyList()),
                claims = mockedRawClaims,
            ),
            result,
        )
        assertEquals(InteractingPartyDomain(null, null, emptyList()), anonymous.party)
    }

    // Case 11:
    // 1. A linked report with and without authority information.
    //
    // Case 11 Expected Result:
    // Own and parent IDs remain distinct; only recorded authority details are used.
    @Test
    fun `Given a report with and without authority information, When toTransactionLogDomain is called, Then only recorded authority details are used`() {
        // Given
        val entry = TransactionEntry.DPAReport(
            mockedActionId, mockedTransactionTime, TransactionResult.Completed,
            dpaName = mockedName, dpaCountry = mockedCountry,
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedActionId,
            mockedEnglishLocale,
            parentPresentationId = mockedTransactionId,
            communicationMethod = mockedStoredMethod,
            transactionDataTypes = emptyList(),
        )
        val anonymous = entry.copy(dpaName = null, dpaCountry = null)
            .toTransactionLogDomain(
                mockedActionId,
                mockedEnglishLocale,
                parentPresentationId = mockedTransactionId,
                communicationMethod = mockedStoredMethod,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.DpaReport

        // Then
        assertEquals(
            TransactionLogDomain.DpaReport(
                id = mockedActionId,
                time = mockedLocalTime,
                result = TransactionResultDomain.Completed,
                parentPresentationId = mockedTransactionId,
                communicationMethod = mockedMethod,
                dpaName = mockedNameDomain,
                dpaCountry = mockedCountryDomain,
            ),
            result,
        )
        assertNull(anonymous.dpaName)
        assertNull(anonymous.dpaCountry)
    }

    // Case 12:
    // 1. Purposes without the user language and a DPA country alone.
    //
    // Case 12 Expected Result:
    // Fallback text and partial registration data remain available.
    @Test
    fun `Given purposes without the user language and a DPA country alone, When toTransactionLogDomain is called, Then fallback text and partial registration data remain available`() {
        // Given
        val entry = mockedPresentation.copy(purpose = mockedPurposes, dpaCountry = mockedCountry)

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            Locale.FRENCH,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.Presentation

        // Then
        assertEquals(
            PresentationRegistrationDomain(
                null,
                mockedPurposes.first().content,
                emptyList(),
                DpaContactDomain(null, mockedCountryDomain, emptyList())
            ),
            result.registration,
        )
    }

    // Case 13:
    // 1. An unidentified party name supplied by the SDK.
    //
    // Case 13 Expected Result:
    // The recorded text is preserved.
    @Test
    fun `Given an unidentified party name supplied by the SDK, When toTransactionLogDomain is called, Then the recorded text is preserved`() {
        // Given
        val entry = mockedPresentation.copy(
            interactingPartyName = MultiLangString(
                "en",
                "Unidentified Relying Party"
            )
        )

        // When
        val result = entry.toTransactionLogDomain(
            mockedTransactionId,
            mockedGreekLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.Presentation

        // Then
        assertEquals(LocalizedTextDomain("en", "Unidentified Relying Party"), result.party.name)
    }

    // Case 14:
    // 1. Supported entries mixed with a model-only type.
    //
    // Case 14 Expected Result:
    // Unsupported entries do not hide the supported rows.
    @Test
    fun `Given supported entries mixed with a model-only type, When toTransactionLogDomain is called, Then unsupported entries do not hide the supported rows`() {
        // Given
        val entries = listOf(
            mockedPresentation,
            TransactionEntry.CertificateIssuance(
                mockedTransactionId,
                mockedTransactionTime,
                TransactionResult.Completed
            ),
            TransactionEntry.OtherTransaction(
                mockedTransactionId,
                mockedTransactionTime,
                TransactionResult.Completed,
                listOf("Other")
            ),
            mockedSigning,
        )

        // When
        val results = entries.mapNotNull { entry ->
            entry.toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            )
        }

        // Then
        assertEquals(
            listOf(
                TransactionLogDomain.Presentation::class,
                TransactionLogDomain.SigningSealing::class
            ),
            results.map { result -> result::class },
        )
    }


    // Case 17:
    // 1. Requested and shared groups contain a credential type distinct from its namespace and a mixed-case vct.
    //
    // Case 17 Expected Result:
    // Credential identifiers, namespaces and typed paths survive mapping exactly in both locales.
    @Test
    fun `Given raw credential and claim identifiers, When toTransactionLogDomain is called, Then identifiers stay exact across locales`() {
        // Given
        val mdocIdentifier = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"
        val vct = "urn:example:Identity:V1"
        val claims = listOf(
            ClaimInfo(mdocIdentifier, listOf(ClaimPath.ofKeys(namespace, mockedClaimName))),
            ClaimInfo(
                vct, listOf(
                    ClaimPath(
                        listOf(
                            ClaimPath.Segment.Key("address"),
                            ClaimPath.Segment.Key("street_address")
                        )
                    ),
                    ClaimPath(
                        listOf(
                            ClaimPath.Segment.Key("nationalities"),
                            ClaimPath.Segment.Index(0)
                        )
                    ),
                    ClaimPath(
                        listOf(
                            ClaimPath.Segment.Key("nationalities"),
                            ClaimPath.Segment.Wildcard
                        )
                    ),
                )
            ),
        )
        val entry =
            mockedPresentation.copy(listOfClaimsRequested = claims, listOfClaimsPresented = claims)
        val expected = listOf(
            CredentialClaimsDomain(
                CredentialRefDomain(mdocIdentifier),
                listOf(
                    ClaimRefDomain(
                        listOf(
                            ClaimPathSegment.Key(namespace),
                            ClaimPathSegment.Key(mockedClaimName)
                        )
                    )
                ),
            ),
            CredentialClaimsDomain(
                CredentialRefDomain(vct),
                listOf(
                    ClaimRefDomain(
                        listOf(
                            ClaimPathSegment.Key("address"),
                            ClaimPathSegment.Key("street_address")
                        )
                    ),
                    ClaimRefDomain(
                        listOf(
                            ClaimPathSegment.Key("nationalities"),
                            ClaimPathSegment.Index(0)
                        )
                    ),
                    ClaimRefDomain(
                        listOf(
                            ClaimPathSegment.Key("nationalities"),
                            ClaimPathSegment.AllElements
                        )
                    ),
                ),
            ),
        )

        // When
        val results = listOf(mockedEnglishLocale, mockedGreekLocale).map { locale ->
            entry.toTransactionLogDomain(
                mockedTransactionId,
                locale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.Presentation
        }

        // Then
        results.forEach { result ->
            assertEquals(expected, result.claimsRequested)
            assertEquals(expected, result.claimsPresented)
        }
    }

    // Case 18:
    // 1. Both action types have a missing, empty, whitespace or self-referencing parent ID.
    //
    // Case 18 Expected Result:
    // No action domain is produced and no parent is inferred from its payload.
    @Test
    fun `Given actions without a valid parent reference, When toTransactionLogDomain is called, Then they are skipped`() {
        // Given
        val entries = listOf(
            mockedHostPresentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime),
            mockedHostPresentation.registration?.dpa.toDpaReportEntry(
                mockedActionId,
                mockedActionTime
            ),
        )
        val parentIds = listOf(null, "", " \t\n", mockedActionId)

        // When
        val results = entries.flatMap { entry ->
            parentIds.map { parentId ->
                entry.toTransactionLogDomain(
                    mockedActionId,
                    mockedEnglishLocale,
                    parentPresentationId = parentId,
                    communicationMethod = mockedStoredMethod,
                    transactionDataTypes = emptyList(),
                )
            }
        }

        // Then
        results.forEach { result -> assertNull(result) }
    }

    // Case 19:
    // 1. Incomplete actions have identical payloads across distinct parent metadata, with and without a reason.
    //
    // Case 19 Expected Result:
    // Each mapping retains the exact supplied parent ID, its own ID, and the incomplete outcome.
    @Test
    fun `Given incomplete actions with distinct stored parents, When toTransactionLogDomain is called, Then exact associations and outcomes are retained`() {
        // Given
        val entries = listOf(mockedReason, null).flatMap { reason ->
            listOf(
                mockedHostPresentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime)
                    .copy(transactionResult = TransactionResult.NotCompleted(reason)),
                mockedHostPresentation.registration?.dpa.toDpaReportEntry(
                    mockedActionId,
                    mockedActionTime
                )
                    .copy(transactionResult = TransactionResult.NotCompleted(reason)),
            )
        }
        val parentIds = listOf(mockedTransactionId, "another-presentation")

        parentIds.forEach { parentId ->
            // When
            val results = entries.map { entry ->
                entry.toTransactionLogDomain(
                    mockedActionId,
                    mockedEnglishLocale,
                    parentPresentationId = parentId,
                    communicationMethod = mockedStoredMethod,
                    transactionDataTypes = emptyList(),
                ) as TransactionLogDomain.PresentationAction
            }

            // Then
            assertEquals(
                listOf(
                    TransactionResultDomain.NotCompleted(mockedReason),
                    TransactionResultDomain.NotCompleted(mockedReason),
                    TransactionResultDomain.NotCompleted(null),
                    TransactionResultDomain.NotCompleted(null),
                ),
                results.map { result -> result.result },
            )
            results.forEach { action ->
                assertEquals(mockedActionId, action.id)
                assertEquals(parentId, action.parentPresentationId)
            }
        }
    }

    // Case 20:
    // 1. Only an intermediary name, identifier or contact list is recorded.
    //
    // Case 20 Expected Result:
    // Each available field maps independently without requiring other intermediary data.
    @Test
    fun `Given partial intermediary data, When toTransactionLogDomain is called, Then each available field is retained`() {
        // Given
        val entries = listOf(
            mockedPresentation.copy(intermediaryName = mockedIntermediaryName) to
                    InteractingPartyDomain(mockedIntermediaryNameDomain, null, emptyList()),
            mockedPresentation.copy(intermediaryIdentifier = mockedIdentifier) to
                    InteractingPartyDomain(null, mockedIntermediary.identifier, emptyList()),
            mockedPresentation.copy(intermediaryContact = mockedContacts) to
                    InteractingPartyDomain(null, null, mockedContacts),
        )

        // When
        val results = entries.map { (entry, _) ->
            entry.toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.Presentation
        }

        // Then
        assertEquals(
            entries.map { (_, intermediary) -> intermediary },
            results.map { presentation -> presentation.intermediary })
        results.forEach { presentation ->
            assertEquals(mockedNameDomain, presentation.party.name)
            assertNull(presentation.registration)
        }
    }

    // Case 21:
    // 1. The intermediary flag is true, false or absent, but no intermediary fields are recorded.
    // 2. The contact list is null or empty.
    //
    // Case 21 Expected Result:
    // No intermediary identity is invented from the flag or relying-party data.
    @Test
    fun `Given no intermediary fields, When toTransactionLogDomain is called, Then the intermediary is absent regardless of the flag`() {
        // Given
        val entries = listOf(null, emptyList<String>()).flatMap { contacts ->
            listOf(true, false, null).map { flag ->
                mockedPresentation.copy(isIntermediary = flag, intermediaryContact = contacts)
            }
        }

        // When
        val results = entries.map { entry ->
            entry.toTransactionLogDomain(
                mockedTransactionId,
                mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.Presentation
        }

        // Then
        results.forEach { presentation ->
            assertNull(presentation.intermediary)
            assertEquals(mockedNameDomain, presentation.party.name)
        }
    }

    //endregion

    //region action method metadata

    // Case 1:
    // 1. Both action types have valid parents but missing or invalid method metadata.
    //
    // Case 1 Expected Result:
    // The actions are skipped without guessing a channel from their payload.
    @Test
    fun `Given actions without valid method metadata, When mapped, Then they are skipped`() {
        // Given
        val entries = listOf(
            mockedHostPresentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime),
            mockedHostPresentation.registration?.dpa.toDpaReportEntry(
                mockedActionId,
                mockedActionTime
            ),
        )
        val methods = listOf(null, "", "email ", "Email", "fax")

        // When
        val results = entries.flatMap { entry ->
            methods.map { method ->
                entry.toTransactionLogDomain(
                    id = mockedActionId,
                    userLocale = mockedEnglishLocale,
                    parentPresentationId = mockedTransactionId,
                    communicationMethod = method,
                    transactionDataTypes = emptyList(),
                )
            }
        }

        // Then
        results.forEach { result -> assertNull(result) }
    }

    // Case 2:
    // 1. Each method accompanies both serialized action types.
    //
    // Case 2 Expected Result:
    // Payload, exact parent and selected method survive storage-model reconstruction.
    @Test
    fun `Given each action and method, When stored and mapped, Then payload and metadata survive`() {
        // Given
        val entries = listOf(
            mockedHostPresentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime),
            mockedHostPresentation.registration?.dpa.toDpaReportEntry(
                mockedActionId,
                mockedActionTime
            ),
        )
        val inputs = CommunicationMethodDomain.entries.flatMap { method ->
            entries.map { entry -> method to entry }
        }

        inputs.forEach { (method, entry) ->
            // When
            val stored = TransactionLog(
                identifier = entry.transactionIdentifier,
                value = entry.toJson(),
                parentPresentationId = mockedHostPresentation.id,
                communicationMethod = method.toStoredCommunicationMethod(),
            )
            val decoded = stored.toCoreTransactionLog()
            val domain = decoded?.toTransactionLogDomain(
                id = stored.identifier,
                userLocale = mockedEnglishLocale,
                parentPresentationId = stored.parentPresentationId,
                communicationMethod = stored.communicationMethod,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.PresentationAction

            // Then
            assertEquals(entry, decoded)
            assertEquals(method, domain.communicationMethod)
            assertEquals(mockedHostPresentation.id, domain.parentPresentationId)
            assertEquals(mockedActionId, domain.id)
        }
    }

    //endregion

    //region toDataDeletionRequestEntry

    // Case 1:
    // 1. A presentation has different requested and shared claims, with distinct raw paths.
    //
    // Case 1 Expected Result:
    // Only shared paths and party identity are copied, with the action's id and time.
    @Test
    fun `Given a presentation with shared claims, When toDataDeletionRequestEntry is called, Then only shared paths and party identity are recorded`() {
        // Given
        val presentation = mockedHostPresentation

        // When
        val result = presentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime)

        // Then
        assertEquals(
            TransactionEntry.DataDeletionRequest(
                transactionIdentifier = mockedActionId,
                time = mockedActionTime,
                transactionResult = TransactionResult.Completed,
                interactingPartyIdentifier = mockedIdentifier,
                interactingPartyName = mockedName,
                listOfClaims = listOf(
                    ClaimInfo(
                        credentialIdentifier = mockedCredentialType,
                        claims = listOf(
                            ClaimPath.ofKeys(mockedCredentialType, mockedClaimName),
                            ClaimPath(
                                listOf(
                                    ClaimPath.Segment.Key("nationalities"),
                                    ClaimPath.Segment.Index(0)
                                )
                            ),
                            ClaimPath(
                                listOf(
                                    ClaimPath.Segment.Key("nationalities"),
                                    ClaimPath.Segment.Wildcard
                                )
                            ),
                            ClaimPath.ofKeys("0"),
                        ),
                    )
                ),
            ),
            result,
        )
    }

    // Case 2:
    // 1. Requested claims exist but none were shared, and the party has no name or identifier.
    //
    // Case 2 Expected Result:
    // No requested claims or invented party identity are recorded.
    @Test
    fun `Given a presentation without shared claims or party identity, When toDataDeletionRequestEntry is called, Then optional data remains absent`() {
        // Given
        val presentation = mockedHostPresentation.copy(
            party = mockedParty.copy(name = null, identifier = null),
            claimsPresented = emptyList(),
        )

        // When
        val result = presentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime)

        // Then
        assertEquals(emptyList<ClaimInfo>(), result.listOfClaims)
        assertNull(result.interactingPartyName)
        assertNull(result.interactingPartyIdentifier)
        assertEquals(TransactionResult.Completed, result.transactionResult)
    }

    //endregion

    //region toDpaReportEntry

    // Case 1:
    // 1. Registration data includes an authority name, country and contacts.
    //
    // Case 1 Expected Result:
    // The authority name and country are recorded with the new action's id and time.
    @Test
    fun `Given a presentation with authority data, When toDpaReportEntry is called, Then the authority is recorded with a new identity`() {
        // Given
        val presentation = mockedHostPresentation

        // When
        val result =
            presentation.registration?.dpa.toDpaReportEntry(mockedActionId, mockedActionTime)

        // Then
        assertEquals(
            TransactionEntry.DPAReport(
                transactionIdentifier = mockedActionId,
                time = mockedActionTime,
                transactionResult = TransactionResult.Completed,
                dpaName = mockedName,
                dpaCountry = mockedCountry,
            ),
            result,
        )
    }

    // Case 2:
    // 1. Registration or its authority details are absent.
    //
    // Case 2 Expected Result:
    // Both optional authority fields stay null.
    @Test
    fun `Given missing registration or authority data, When toDpaReportEntry is called, Then no authority details are invented`() {
        // Given
        val presentations = listOf(
            mockedHostPresentation.copy(registration = null),
            mockedHostPresentation.copy(registration = mockedHostRegistration.copy(dpa = null)),
        )

        // When
        val results = presentations.map { presentation ->
            presentation.registration?.dpa.toDpaReportEntry(
                mockedActionId,
                mockedActionTime
            )
        }

        // Then
        results.forEach { result ->
            assertNull(result.dpaName)
            assertNull(result.dpaCountry)
            assertEquals(TransactionResult.Completed, result.transactionResult)
        }
    }

    // Case 3:
    // 1. Only one of the authority's name and country is available.
    //
    // Case 3 Expected Result:
    // Each field maps independently without requiring the other.
    @Test
    fun `Given partial authority data, When toDpaReportEntry is called, Then available fields are preserved independently`() {
        // Given
        val authorities = listOf(
            DpaContactDomain(mockedNameDomain, null, emptyList()),
            DpaContactDomain(null, mockedCountryDomain, emptyList()),
        )

        // When
        val results = authorities.map { authority ->
            authority.toDpaReportEntry(mockedActionId, mockedActionTime)
        }

        // Then
        assertEquals(listOf(mockedName, null), results.map { report -> report.dpaName })
        assertEquals(listOf(null, mockedCountry), results.map { report -> report.dpaCountry })
    }

    //endregion

    //region toCoreTransactionLog

    // Case 1:
    // 1. A stored entry contains requested and presented paths and intermediary identity and contacts.
    //
    // Case 1 Expected Result:
    // The persisted payload decodes and maps without losing its paths or intermediary data.
    @Test
    fun `Given a stored presentation, When toCoreTransactionLog is called, Then it remains mappable`() {
        // Given
        val entry = mockedPresentation.copy(
            listOfClaimsPresented = mockedPresentation.listOfClaimsRequested,
            intermediaryName = mockedIntermediaryName,
            intermediaryIdentifier = mockedIdentifier,
            intermediaryContact = mockedContacts,
        )
        val stored = TransactionLog(
            identifier = mockedTransactionId,
            value = entry.toJson(),
            parentPresentationId = null,
            communicationMethod = null,
        )

        // When
        val decoded = stored.toCoreTransactionLog()
        val domain = decoded?.toTransactionLogDomain(
            stored.identifier,
            mockedEnglishLocale,
            stored.parentPresentationId,
            stored.communicationMethod,
            transactionDataTypes = emptyList(),
        ) as TransactionLogDomain.Presentation

        // Then
        assertEquals(entry, decoded)
        assertEquals(mockedIntermediary, domain.intermediary)
        assertEquals(mockedRawClaims, domain.claimsRequested)
        assertEquals(mockedRawClaims, domain.claimsPresented)
    }

    // Case 2:
    // 1. A stored row is malformed, legacy, or from an unknown future type.
    //
    // Case 2 Expected Result:
    // The row returns null without failing the read.
    @Test
    fun `Given unreadable stored rows, When toCoreTransactionLog is called, Then they are skipped`() {
        // Given
        val payloads = listOf(
            "not json",
            """{"status":"Completed","type":"Presentation","documents":[]}""",
            mockedSigning.toJson().replace("SigningSealing", "FutureTransaction"),
        )

        // When
        val results = payloads.map { payload ->
            TransactionLog(
                identifier = mockedTransactionId,
                value = payload,
                parentPresentationId = null,
                communicationMethod = null,
            ).toCoreTransactionLog()
        }

        // Then
        results.forEach { result -> assertNull(result) }
    }

    // Case 3:
    // 1. Both host action mappers produce entries stored with a separate parent reference.
    //
    // Case 3 Expected Result:
    // The SDK codec preserves each payload, including typed claim paths, and metadata supplies the domain parent.
    @Test
    fun `Given mapped host actions, When toCoreTransactionLog is called after serialization, Then both entries round trip`() {
        // Given
        val entries = listOf(
            mockedHostPresentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime),
            mockedHostPresentation.registration?.dpa.toDpaReportEntry(
                mockedActionId,
                mockedActionTime
            ),
        )

        // When
        val storedActions = entries.map { entry ->
            TransactionLog(
                identifier = entry.transactionIdentifier,
                value = entry.toJson(),
                parentPresentationId = mockedHostPresentation.id,
                communicationMethod = mockedStoredMethod,
            )
        }
        val results = storedActions.map { storedAction -> storedAction.toCoreTransactionLog() }
        val domains = storedActions.map { stored ->
            stored.toCoreTransactionLog()?.toTransactionLogDomain(
                id = stored.identifier,
                userLocale = mockedEnglishLocale,
                parentPresentationId = stored.parentPresentationId,
                communicationMethod = stored.communicationMethod,
                transactionDataTypes = emptyList(),
            ) as TransactionLogDomain.PresentationAction
        }

        // Then
        assertEquals(entries, results)
        domains.forEach { action ->
            assertEquals(mockedActionId, action.id)
            assertEquals(mockedHostPresentation.id, action.parentPresentationId)
            assertEquals(
                mockedActionTime.atZone(ZoneId.systemDefault()).toLocalDateTime(),
                action.time
            )
        }
        assertEquals(
            mockedHostPresentation.claimsPresented,
            (domains[0] as TransactionLogDomain.DataDeletionRequest).claims
        )
        assertEquals(mockedNameDomain, (domains[1] as TransactionLogDomain.DpaReport).dpaName)
    }

    // Case 4:
    // 1. Stored presentations contain mixed language tags or explicitly undetermined text.
    // 2. Each presentation is read in English and Greek before creating its DDR and DPAR.
    //
    // Case 4 Expected Result:
    // Both stored actions retain the original text and tags, independently of the app locale.
    @Test
    fun `Given language tagged presentation data, When actions are recorded and read, Then source text and tags survive`() {
        // Given
        val entries = listOf(
            mockedPresentation.copy(
                interactingPartyName = MultiLangString("fr-CA", "Organisme"),
                dpaName = MultiLangString("el-GR", "Αρχή"),
                dpaCountry = MultiLangString("en-GB", mockedCountry.content),
            ),
            mockedPresentation.copy(
                interactingPartyName = mockedName.copy(lang = "und"),
                dpaName = mockedName.copy(lang = "und"),
                dpaCountry = mockedCountry.copy(lang = "und"),
            ),
        )
        val locales = listOf(mockedEnglishLocale, mockedGreekLocale)

        entries.forEach { entry ->
            locales.forEach { locale ->
                // When
                val storedPresentation = TransactionLog(
                    identifier = entry.transactionIdentifier,
                    value = entry.toJson(),
                    parentPresentationId = null,
                    communicationMethod = null,
                )
                val presentation =
                    storedPresentation.toCoreTransactionLog()?.toTransactionLogDomain(
                        id = storedPresentation.identifier,
                        userLocale = locale,
                        parentPresentationId = null,
                        communicationMethod = null,
                        transactionDataTypes = emptyList(),
                    ) as TransactionLogDomain.Presentation
                val actions = listOf(
                    presentation.toDataDeletionRequestEntry(mockedActionId, mockedActionTime),
                    presentation.registration?.dpa.toDpaReportEntry(
                        mockedActionId,
                        mockedActionTime
                    ),
                )
                val restoredActions = actions.map { action ->
                    TransactionLog(
                        identifier = action.transactionIdentifier,
                        value = action.toJson(),
                        parentPresentationId = presentation.id,
                        communicationMethod = mockedStoredMethod,
                    ).toCoreTransactionLog()
                }
                val deletion = restoredActions[0] as TransactionEntry.DataDeletionRequest
                val report = restoredActions[1] as TransactionEntry.DPAReport
                val deletionDomain = deletion.toTransactionLogDomain(
                    id = deletion.transactionIdentifier,
                    userLocale = locale,
                    parentPresentationId = presentation.id,
                    communicationMethod = mockedStoredMethod,
                    transactionDataTypes = emptyList(),
                ) as TransactionLogDomain.DataDeletionRequest
                val reportDomain = report.toTransactionLogDomain(
                    id = report.transactionIdentifier,
                    userLocale = locale,
                    parentPresentationId = presentation.id,
                    communicationMethod = mockedStoredMethod,
                    transactionDataTypes = emptyList(),
                ) as TransactionLogDomain.DpaReport

                // Then
                assertEquals(entry.interactingPartyName, deletion.interactingPartyName)
                assertEquals(entry.dpaName, report.dpaName)
                assertEquals(entry.dpaCountry, report.dpaCountry)
                assertEquals(presentation.party.name, deletionDomain.party.name)
                assertEquals(presentation.registration?.dpa?.name, reportDomain.dpaName)
                assertEquals(presentation.registration?.dpa?.country, reportDomain.dpaCountry)
            }
        }
    }
    //endregion

    //region recorded transaction data

    // Case 1:
    // 1. A presentation with transaction data is serialized and read back for each result.
    //
    // Case 1 Expected Result:
    // Recorded fields survive without changing its presentation kind, claims or result.
    @Test
    fun `Given a stored signing request, When restored, Then it remains a presentation with its recorded payload`() {
        // Given
        val mockedFileName = "contract.pdf"
        val recorded = TransactionalData(
            Json.parseToJsonElement(
                """[{
                    "type":"${QesApprovalRequest.TYPE}",
                    "credential_ids":["query_0"],
                    "credentialID":"signing-credential",
                    "numSignatures":1,
                    "documentDigests":[{"label":"$mockedFileName","hash":"AQID"}],
                    "hashAlgorithmOID":"2.16.840.1.101.3.4.2.1"
                }]"""
            ).jsonArray
        )
        val results = listOf(
            TransactionResult.Completed to TransactionResultDomain.Completed,
            TransactionResult.NotCompleted(mockedReason) to TransactionResultDomain.NotCompleted(mockedReason),
        )

        results.forEach { (outcome, expectedResult) ->
            val entry = mockedPresentation.copy(transactionalData = recorded, transactionResult = outcome)

            // When
            val result = entry.toJson().toTransactionEntryOrNull()?.toTransactionLogDomain(
                id = mockedTransactionId,
                userLocale = mockedEnglishLocale,
                parentPresentationId = null,
                communicationMethod = null,
                transactionDataTypes = listOf(TransactionDataType.QES_APPROVAL),
            ) as TransactionLogDomain.Presentation

            // Then
            assertEquals(mockedTransactionId, result.id)
            assertEquals(expectedResult, result.result)
            assertEquals(mockedRawClaims, result.claimsRequested)
            assertEquals(emptyList<CredentialClaimsDomain>(), result.claimsPresented)
            val approval = result.transactionData.single() as PresentationTransactionDataDomain.QesApproval
            assertNull(approval.displayName)
            assertEquals(mockedFileName, approval.documentDigests.single().label)
            assertEquals("AQID", approval.documentDigests.single().hash)
            assertNull(approval.signatureQualifier)
        }
    }

    //endregion

    //region mocked objects
    private val mockedLocalTime =
        mockedTransactionTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
    private val mockedName = MultiLangString(lang = "en", content = "Example party")
    private val mockedIntermediaryName =
        MultiLangString(lang = "en", content = "Example intermediary")
    private val mockedCountry = MultiLangString(lang = "en", content = "Greece")
    private val mockedNameDomain =
        LocalizedTextDomain(languageTag = mockedName.lang, text = mockedName.content)
    private val mockedIntermediaryNameDomain = LocalizedTextDomain(
        languageTag = mockedIntermediaryName.lang,
        text = mockedIntermediaryName.content
    )
    private val mockedCountryDomain =
        LocalizedTextDomain(languageTag = mockedCountry.lang, text = mockedCountry.content)
    private val mockedIdentifier = QualifiedIdentifier(type = "urn:register", value = "identifier")
    private val mockedUrl = "https://party.example"
    private val mockedContacts = listOf("GR", mockedUrl)
    private val mockedReason = "Transaction stopped"
    private val mockedGreekPurpose = "Ταυτοποίηση"
    private val mockedPurposes = listOf(
        MultiLangString("en", "Identification"),
        MultiLangString("el", mockedGreekPurpose),
    )
    private val mockedParty = InteractingPartyDomain(
        name = mockedNameDomain,
        identifier = QualifiedIdentifierDomain(mockedIdentifier.type, mockedIdentifier.value),
        contacts = mockedContacts,
    )
    private val mockedIntermediary = mockedParty.copy(name = mockedIntermediaryNameDomain)
    private val mockedRawClaims = listOf(
        CredentialClaimsDomain(
            CredentialRefDomain(mockedCredentialType),
            listOf(ClaimRefDomain(mockedClaimSegments)),
        )
    )
    private val mockedPresentation = TransactionEntry.Presentation(
        transactionIdentifier = mockedTransactionId,
        time = mockedTransactionTime,
        transactionResult = TransactionResult.Completed,
        listOfClaimsRequested = listOf(
            ClaimInfo(
                mockedCredentialType,
                listOf(ClaimPath.ofKeys(mockedCredentialType, mockedClaimName))
            )
        ),
        listOfClaimsPresented = emptyList(),
        interactingPartyName = mockedName,
    )
    private val mockedSigning = TransactionEntry.SigningSealing(
        transactionIdentifier = mockedTransactionId,
        time = mockedTransactionTime,
        transactionResult = TransactionResult.Completed,
    )

    private val mockedActionId = "action-transaction"
    private val mockedActionTime = mockedTransactionTime.plusSeconds(60)
    private val mockedHostRegistration = PresentationRegistrationDomain(
        registrarUrl = mockedUrl,
        purpose = mockedGreekPurpose,
        privacyPolicyUrls = listOf(mockedUrl),
        dpa = DpaContactDomain(mockedNameDomain, mockedCountryDomain, mockedContacts),
    )
    private val mockedHostPresentation = TransactionLogDomain.Presentation(
        transactionData = emptyList(),
        id = mockedTransactionId,
        time = mockedLocalTime,
        result = TransactionResultDomain.NotCompleted(mockedReason),
        party = mockedParty,
        partyType = null,
        intermediary = null,
        registration = mockedHostRegistration,
        claimsRequested = mockedRawClaims,
        claimsPresented = listOf(
            CredentialClaimsDomain(
                credential = CredentialRefDomain(mockedCredentialType),
                claims = listOf(
                    ClaimRefDomain(mockedClaimSegments),
                    ClaimRefDomain(
                        listOf(
                            ClaimPathSegment.Key("nationalities"),
                            ClaimPathSegment.Index(0)
                        )
                    ),
                    ClaimRefDomain(
                        listOf(
                            ClaimPathSegment.Key("nationalities"),
                            ClaimPathSegment.AllElements
                        )
                    ),
                    ClaimRefDomain(listOf(ClaimPathSegment.Key("0"))),
                ),
            )
        ),
    )
    //endregion
}