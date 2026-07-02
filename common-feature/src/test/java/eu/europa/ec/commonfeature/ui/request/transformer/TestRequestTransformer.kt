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

package eu.europa.ec.commonfeature.ui.request.transformer

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimItemId
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain.Companion.toClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.PresentationCombinationDomain
import eu.europa.ec.corelogic.model.PresentationMatchDomain
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransformToUiItemsStrings
import eu.europa.ec.testfeature.util.copy
import eu.europa.ec.testfeature.util.getMockedMdlWithBasicFields
import eu.europa.ec.testfeature.util.getMockedPidWithBasicFields
import eu.europa.ec.testfeature.util.getMockedSdJwtFullPid
import eu.europa.ec.testfeature.util.mockedMdlId
import eu.europa.ec.testfeature.util.mockedMdocMdlDocType
import eu.europa.ec.testfeature.util.mockedMdocMdlNameSpace
import eu.europa.ec.testfeature.util.mockedMdocPidDocType
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedMdocPresentationMatch
import eu.europa.ec.testfeature.util.mockedNonSelectableClaims
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedSdJwtPidId
import eu.europa.ec.testfeature.util.mockedSdJwtPidVct
import eu.europa.ec.testfeature.util.mockedSdJwtVcClaim
import eu.europa.ec.testfeature.util.mockedSelectableClaims
import eu.europa.ec.testfeature.util.mockedValidMdlWithBasicFieldsRequestMatch
import eu.europa.ec.testfeature.util.mockedValidPidWithBasicFieldsRequestMatch
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TestRequestTransformer {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uuidProvider: UuidProvider

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        // The transformer reads the locale + the boolean/gender value strings while
        // materialising the claim tree; this helper stubs all of them (and getLocale()).
        mockTransformToUiItemsStrings(resourceProvider)
        // Document header's collapsed supporting text (used by transformToUiItems).
        whenever(resourceProvider.getString(R.string.request_collapsed_supporting_text))
            .thenReturn(mockedRequestCollapsedSupportingText)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region transformToCombinationsUi

    // Case 1:
    // A single combination holding a single (non-revoked) PID match, against a storage PID
    // that holds only the "basic fields".
    //
    // Case 1 Expected Result:
    // One combination with one document item whose claim tree is the intersection
    // of the verifier's requested claims and the claims actually stored — the two requested
    // fields the document does NOT store ("portrait", "issuing_country") are filtered out.
    @Test
    fun `Given a single combination, When transformToCombinationsUi, Then it projects requested-and-stored claims only`() {
        coroutineRule.runTest {
            // Given
            val storageDocuments = listOf(getMockedPidWithBasicFields())
            val combinationsDomain = listOf(
                PresentationCombinationDomain(
                    matches = listOf(
                        mockedValidPidWithBasicFieldsRequestMatch
                    )
                )
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = storageDocuments,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertEquals(1, combinationsUi.size)
            val items = combinationsUi.single().documents
            assertEquals(1, items.size)

            val payload = items.single().domainPayload
            assertEquals(mockedPidId, payload.docId)
            assertEquals(mockedPidDocName, payload.docName)
            assertEquals(DocumentFormatDomain.MsoMdoc, payload.docFormatDomain)
            assertEquals(null, payload.queryId)

            // 8 of the 10 requested claims are stored on the basic-fields PID.
            assertEquals(8, payload.docClaimsDomain.countLeaves())
            val leafKeys = payload.docClaimsDomain.leafKeys()
            assertFalse("portrait" in leafKeys)
            assertFalse("issuing_country" in leafKeys)
        }
    }

    // Case 2:
    // Two combinations, each holding one match.
    //
    // Case 2 Expected Result:
    // The projection is 1:1 — two combinations, the first rendering only the PID and the second
    // only the mDL.
    @Test
    fun `Given multiple combinations, When transformToCombinationsUi, Then each projects to its own combination`() {
        coroutineRule.runTest {
            // Given
            val storageDocuments = listOf(
                getMockedPidWithBasicFields(),
                getMockedMdlWithBasicFields()
            )
            val combinationsDomain = listOf(
                PresentationCombinationDomain(
                    matches = listOf(
                        mockedValidPidWithBasicFieldsRequestMatch
                    )
                ),
                PresentationCombinationDomain(
                    matches = listOf(
                        mockedValidMdlWithBasicFieldsRequestMatch
                    )
                ),
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = storageDocuments,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertEquals(2, combinationsUi.size)
            assertEquals(mockedPidId, combinationsUi[0].documents.single().domainPayload.docId)
            assertEquals(mockedMdlId, combinationsUi[1].documents.single().domainPayload.docId)
        }
    }

    // Case 3:
    // A single combination holding two matches (PID + mDL).
    //
    // Case 3 Expected Result:
    // Both matches materialise into one combination, one item each — several matches
    // under one combination collapse into a single RequestCombinationUi, not several combinations.
    @Test
    fun `Given a combination with several matches, When transformToCombinationsUi, Then all matches share one combination`() {
        coroutineRule.runTest {
            // Given
            val storageDocuments = listOf(
                getMockedPidWithBasicFields(),
                getMockedMdlWithBasicFields()
            )
            val combinationsDomain = listOf(
                PresentationCombinationDomain(
                    matches = listOf(
                        mockedValidPidWithBasicFieldsRequestMatch,
                        mockedValidMdlWithBasicFieldsRequestMatch,
                    )
                )
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = storageDocuments,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertEquals(1, combinationsUi.size)
            val items = combinationsUi.single().documents
            assertEquals(2, items.size)
            assertEquals(mockedPidId, items[0].domainPayload.docId)
            assertEquals(mockedMdlId, items[1].domainPayload.docId)
        }
    }

    // Case 4:
    // A single combination that carries no matches.
    //
    // Case 4 Expected Result:
    // The combination is preserved but renders no items — the 1:1 projection never
    // drops a combination, only its content.
    @Test
    fun `Given a combination with no matches, When transformToCombinationsUi, Then the combination has no items`() {
        coroutineRule.runTest {
            // Given
            val storageDocuments = listOf(getMockedPidWithBasicFields())
            val combinationsDomain = listOf(
                PresentationCombinationDomain(matches = emptyList())
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = storageDocuments,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertEquals(1, combinationsUi.size)
            assertTrue(combinationsUi.single().documents.isEmpty())
        }
    }

    // Case 5:
    // No combinations at all.
    //
    // Case 5 Expected Result:
    // An empty list of combinations.
    @Test
    fun `Given no combinations, When transformToCombinationsUi, Then the result is empty`() {
        coroutineRule.runTest {
            // Given
            val storageDocuments = listOf(getMockedPidWithBasicFields())

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = storageDocuments,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = emptyList(),
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertTrue(combinationsUi.isEmpty())
        }
    }

    // Case 6:
    // A combination whose match references a document the wallet does not store.
    //
    // Case 6 Expected Result:
    // The unmatched document is skipped, so the combination renders no items (rather than crashing
    // or fabricating an empty document).
    @Test
    fun `Given a match for a non-stored document, When transformToCombinationsUi, Then it is skipped`() {
        coroutineRule.runTest {
            // Given — only the PID is in storage, but the combination asks for the mDL.
            val storageDocuments = listOf(getMockedPidWithBasicFields())
            val combinationsDomain = listOf(
                PresentationCombinationDomain(
                    matches = listOf(
                        mockedValidMdlWithBasicFieldsRequestMatch
                    )
                )
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = storageDocuments,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertEquals(1, combinationsUi.size)
            assertTrue(combinationsUi.single().documents.isEmpty())
        }
    }

    // Case 7:
    // 1. The PID is an SD-JWT VC whose claims include a nested "address" object.
    // 2. The verifier requests a single nested leaf, ["address", "country"].
    //
    // Case 7 Expected Result:
    // One combination whose claim tree contains an "address" group holding only the "country" leaf —
    // the sibling address leaves the verifier did not ask for are filtered out.
    @Test
    fun `Given a nested SD-JWT leaf request, When transformToCombinationsUi, Then only that leaf is grouped`() {
        coroutineRule.runTest {
            // Given
            val match = sdJwtPidMatch(sdJwtKeyPath("address", "country"))
            val combinationsDomain = listOf(PresentationCombinationDomain(matches = listOf(match)))

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedNonSelectableClaims,
            ).getOrThrow()

            // Then
            val claims = combinationsUi.single().documents.single().domainPayload.docClaimsDomain
            assertEquals(listOf("country"), claims.group("address").items.leafKeys())
        }
    }

    // Case 8:
    // 1. The PID is an SD-JWT VC whose claims include a nested "address" object.
    // 2. The verifier requests the parent ["address"] (no specific leaf).
    //
    // Case 8 Expected Result:
    // One combination whose "address" group holds every stored address leaf — an ancestor request
    // releases all of its descendants.
    @Test
    fun `Given an ancestor SD-JWT request, When transformToCombinationsUi, Then all descendant leaves are released`() {
        coroutineRule.runTest {
            // Given
            val match = sdJwtPidMatch(sdJwtKeyPath("address"))
            val combinationsDomain = listOf(PresentationCombinationDomain(matches = listOf(match)))

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedNonSelectableClaims,
            ).getOrThrow()

            // Then
            val claims = combinationsUi.single().documents.single().domainPayload.docClaimsDomain
            assertEquals(
                setOf(
                    "postal_code", "country", "region",
                    "house_number", "street_address", "locality",
                ),
                claims.group("address").items.leafKeys().toSet(),
            )
        }
    }

    // Case 9:
    // 1. The PID is an SD-JWT VC whose claims include a "nationalities" array.
    // 2. The verifier requests a specific array element, ["nationalities", 0].
    //
    // Case 9 Expected Result:
    // One combination whose "nationalities" group surfaces the addressed array element as a leaf.
    @Test
    fun `Given an SD-JWT array index request, When transformToCombinationsUi, Then the element is surfaced`() {
        coroutineRule.runTest {
            // Given
            val match = sdJwtPidMatch(
                sdJwtPath(ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)),
            )
            val combinationsDomain = listOf(PresentationCombinationDomain(matches = listOf(match)))

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedNonSelectableClaims,
            ).getOrThrow()

            // Then
            val claims = combinationsUi.single().documents.single().domainPayload.docClaimsDomain
            assertEquals(1, claims.group("nationalities").items.countLeaves())
        }
    }

    // Case 10:
    // 1. The PID is an SD-JWT VC whose claims include a "nationalities" array.
    // 2. The verifier requests the wildcard ["nationalities", null] (any element).
    //
    // Case 10 Expected Result:
    // The wildcard matches the stored array element, so the "nationalities" group still
    // surfaces it — wildcard-to-index resolution works end-to-end through the transformer.
    @Test
    fun `Given an SD-JWT array wildcard request, When transformToCombinationsUi, Then matching elements are surfaced`() {
        coroutineRule.runTest {
            // Given
            val match = sdJwtPidMatch(
                sdJwtPath(ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements),
            )
            val combinationsDomain = listOf(PresentationCombinationDomain(matches = listOf(match)))

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedNonSelectableClaims,
            ).getOrThrow()

            // Then
            val claims = combinationsUi.single().documents.single().domainPayload.docClaimsDomain
            assertEquals(1, claims.group("nationalities").items.countLeaves())
        }
    }

    // Case 11:
    // 1. A combination whose match carries an empty (malformed) claim path, against a
    //    stored PID of the same claim type.
    //
    // Case 11 Expected Result:
    // Nothing is surfaced — an empty path matches no stored claim, so the combination renders no items
    // instead of releasing every claim of that type.
    @Test
    fun `Given an empty claim path in a match, When transformToCombinationsUi, Then nothing is surfaced`() {
        coroutineRule.runTest {
            // Given
            val malformedMatch = PresentationMatchDomain(
                documentId = mockedPidId,
                credentialId = "$mockedPidId-cred",
                queryId = null,
                requestedClaims = listOf(
                    ClaimPathDomain(
                        segments = emptyList(),
                        type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace),
                    )
                ),
            )
            val combinationsDomain = listOf(
                PresentationCombinationDomain(
                    matches = listOf(malformedMatch)
                )
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = listOf(getMockedPidWithBasicFields()),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            assertEquals(1, combinationsUi.size)
            assertTrue(combinationsUi.single().documents.isEmpty())
        }
    }

    // Case 12:
    // 1. One combination holds two matches for the same document, from two different queries
    //    (different queryIds).
    //
    // Case 12 Expected Result:
    // The combination holds two document items whose document-header item ids are distinct — identity is
    // (docId, queryId), so expand/collapse state on one card cannot couple to the other.
    @Test
    fun `Given two queries on the same document, When transformToCombinationsUi, Then header ids are distinct`() {
        coroutineRule.runTest {
            // Given
            val matchQuery1 = pidMatch("family_name").copy(queryId = "query-1")
            val matchQuery2 = pidMatch("given_name").copy(queryId = "query-2")
            val combinationsDomain = listOf(
                PresentationCombinationDomain(matches = listOf(matchQuery1, matchQuery2))
            )

            // When
            val combinationsUi = RequestTransformer.transformToCombinationsUi(
                storageDocuments = listOf(getMockedPidWithBasicFields()),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            ).getOrThrow()

            // Then
            val items = combinationsUi.single().documents
            assertEquals(2, items.size)
            val headerIds = items.map { it.headerUi.header.itemId }
            assertEquals(2, headerIds.toSet().size)
        }
    }

    // Case 13:
    // 1. The two-nationalities PID stores BOTH array elements ("GR", "SE").
    // 2. The verifier requests a single concrete element, ["nationalities", 0].
    //
    // Case 13 Expected Result:
    // Exactly the addressed element renders — the sibling element is provably absent.
    @Test
    fun `Given a two-element array and an index request, When transformToCombinationsUi, Then only the addressed element surfaces`() {
        coroutineRule.runTest {
            // Given
            val match = twoNationalitiesPidMatch(
                sdJwtPath(
                    ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)
                ),
            )

            // When
            val combinations = transform(
                storageDocuments = listOf(mockedTwoNationalitiesSdJwtPid()),
                combinationsDomain = listOf(PresentationCombinationDomain(matches = listOf(match))),
                claimsAreSelectable = mockedNonSelectableClaims,
            )

            // Then
            val nationalities = combinations.single().documents.single().domainPayload
                .docClaimsDomain.group("nationalities").items
            val leaf = nationalities.single() as ClaimDomain.Primitive
            assertEquals("0", leaf.key)
            assertEquals("GR", leaf.value)
        }
    }

    // Case 14:
    // 1. The two-nationalities PID stores BOTH array elements ("GR", "SE").
    // 2. The verifier requests the trailing wildcard, ["nationalities", null].
    //
    // Case 14 Expected Result:
    // Every element renders — the wildcard releases the whole array.
    @Test
    fun `Given a two-element array and a wildcard request, When transformToCombinationsUi, Then every element surfaces`() {
        coroutineRule.runTest {
            // Given
            val match = twoNationalitiesPidMatch(
                sdJwtPath(
                    ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements
                ),
            )

            // When
            val combinations = transform(
                storageDocuments = listOf(mockedTwoNationalitiesSdJwtPid()),
                combinationsDomain = listOf(PresentationCombinationDomain(matches = listOf(match))),
                claimsAreSelectable = mockedNonSelectableClaims,
            )

            // Then
            val leaves = combinations.single().documents.single().domainPayload
                .docClaimsDomain.group("nationalities").items
                .filterIsInstance<ClaimDomain.Primitive>()
            assertEquals(listOf("GR", "SE"), leaves.map { it.value })
        }
    }

    // Case 15:
    // The same single-PID combination as Case 1, projected once selectable (BLE / DC-API) and
    // once read-only (OpenID4VP).
    //
    // Case 15 Expected Result:
    // Both builds materialise the identical claim tree, but only the selectable build carries
    // leaf checkboxes — the read-only build renders no checkbox anywhere.
    @Test
    fun `Given claimsAreSelectable is false, When transformToCombinationsUi, Then no leaf carries a checkbox`() {
        coroutineRule.runTest {
            // Given
            val storageDocuments = listOf(getMockedPidWithBasicFields())
            val combinationsDomain = listOf(
                PresentationCombinationDomain(
                    matches = listOf(
                        mockedValidPidWithBasicFieldsRequestMatch
                    )
                )
            )

            // When
            val selectable = transform(
                storageDocuments = storageDocuments,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedSelectableClaims,
            )
            val readOnly = transform(
                storageDocuments = storageDocuments,
                combinationsDomain = combinationsDomain,
                claimsAreSelectable = mockedNonSelectableClaims,
            )

            // Then — identical claim tree, but checkboxes only on the selectable build.
            assertEquals(
                selectable.single().documents.single().domainPayload.docClaimsDomain.countLeaves(),
                readOnly.single().documents.single().domainPayload.docClaimsDomain.countLeaves(),
            )
            assertTrue(selectable.single().documents.hasAnyCheckbox())
            assertFalse(readOnly.single().documents.hasAnyCheckbox())
        }
    }

    //endregion

    //region createSelectionsDomain

    // Case 1:
    // The user keeps every (default-checked) claim of a single rendered PID.
    //
    // Case 1 Expected Result:
    // One selection carrying the document identity and all three requested-and-stored claims.
    @Test
    fun `Given all claims checked, When createSelectionsDomain, Then every requested claim is selected`() {
        coroutineRule.runTest {
            // Given
            val match = pidMatch("family_name", "given_name", "age_over_18")
            val items = render(listOf(getMockedPidWithBasicFields()), listOf(match))

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertEquals(1, selections.size)
            val selection = selections.single()
            assertEquals(mockedPidId, selection.documentId)
            assertEquals("$mockedPidId-cred", selection.credentialId)
            assertEquals(null, selection.queryId)
            assertEquals(3, selection.selectedClaims.size)
            assertTrue(pidPath("family_name") in selection.selectedClaims)
            assertTrue(pidPath("given_name") in selection.selectedClaims)
            assertTrue(pidPath("age_over_18") in selection.selectedClaims)
        }
    }

    // Case 2:
    // The user de-selects exactly one claim ("family_name").
    //
    // Case 2 Expected Result:
    // The de-selected claim is absent from the selection; the others remain.
    @Test
    fun `Given one claim unchecked, When createSelectionsDomain, Then only that claim is dropped`() {
        coroutineRule.runTest {
            // Given
            val match = pidMatch("family_name", "given_name", "age_over_18")
            val familyNameItemId =
                pidPath("family_name").itemId(docId = mockedPidId, queryId = null)
            val items = render(listOf(getMockedPidWithBasicFields()), listOf(match))
                .uncheckLeavesWhere { it == familyNameItemId }

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            val selection = selections.single()
            assertEquals(2, selection.selectedClaims.size)
            assertFalse(pidPath("family_name") in selection.selectedClaims)
            assertTrue(pidPath("given_name") in selection.selectedClaims)
            assertTrue(pidPath("age_over_18") in selection.selectedClaims)
        }
    }

    // Case 3:
    // The user de-selects every claim of the only rendered document.
    //
    // Case 3 Expected Result:
    // No selection is emitted — an all-empty disclosure is never sent.
    @Test
    fun `Given all claims unchecked, When createSelectionsDomain, Then the document is dropped`() {
        coroutineRule.runTest {
            // Given
            val match = pidMatch("family_name", "given_name", "age_over_18")
            val items = render(listOf(getMockedPidWithBasicFields()), listOf(match))
                .uncheckLeavesWhere { true }

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertTrue(selections.isEmpty())
        }
    }

    // Case 4:
    // A document is rendered but no match exists for its (documentId, queryId).
    //
    // Case 4 Expected Result:
    // The document is dropped — selections are only produced for documents with a backing
    // match.
    @Test
    fun `Given a rendered document with no backing match, When createSelectionsDomain, Then it is dropped`() {
        coroutineRule.runTest {
            // Given — render the PID, but only offer an unrelated mDL match at recovery time.
            val pidMatch = pidMatch("family_name", "given_name")
            val items = render(listOf(getMockedPidWithBasicFields()), listOf(pidMatch))
            val unrelatedMdlMatch = mockedMdocPresentationMatch(
                documentId = mockedMdlId,
                credentialId = "$mockedMdlId-cred",
                docType = mockedMdocMdlDocType,
                namespace = mockedMdocMdlNameSpace,
                dataElements = listOf("family_name"),
            )

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = listOf(unrelatedMdlMatch),
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            assertTrue(selections.isEmpty())
        }
    }

    // Case 5:
    // Two queries target the same document (same documentId and credentialId) but carry
    // different queryIds and request different claims.
    //
    // Case 5 Expected Result:
    // Two independent selections, each keyed by its own queryId and carrying only its own
    // query's claim — proving the (documentId, queryId) addressing keeps the two from
    // colliding (a documentId-only key would silently merge or drop one).
    @Test
    fun `Given two queries on the same document, When createSelectionsDomain, Then each query yields its own selection`() {
        coroutineRule.runTest {
            // Given
            val matchQuery1 = pidMatch("family_name").copy(queryId = "query-1")
            val matchQuery2 = pidMatch("given_name").copy(queryId = "query-2")
            val items = render(
                storageDocuments = listOf(getMockedPidWithBasicFields()),
                matches = listOf(matchQuery1, matchQuery2),
            )
            // Two document items projected for the same physical document, one per query.
            assertEquals(2, items.size)

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = listOf(matchQuery1, matchQuery2),
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            assertEquals(2, selections.size)

            val selectionQuery1 = selections.single { it.queryId == "query-1" }
            assertEquals(mockedPidId, selectionQuery1.documentId)
            assertEquals(setOf(pidPath("family_name")), selectionQuery1.selectedClaims)

            val selectionQuery2 = selections.single { it.queryId == "query-2" }
            assertEquals(mockedPidId, selectionQuery2.documentId)
            assertEquals(setOf(pidPath("given_name")), selectionQuery2.selectedClaims)
        }
    }

    // Cases 6–12 and 16 pair SD-JWT with a selectable build on purpose: SD-JWT is non-selectable
    // in production, so this pairing can't occur, but it's the only way to exercise the selectable
    // re-encoding of nested/array claim paths. Mechanics tests, kept deliberately.

    // Case 6:
    // An SD-JWT nested-leaf request (["address", "country"]) is rendered and kept checked.
    //
    // Case 6 Expected Result:
    // One selection whose selectedClaims is exactly the requested nested path — the nested
    // storage leaf is re-encoded and matched back correctly.
    @Test
    fun `Given a kept nested SD-JWT claim, When createSelectionsDomain, Then the nested path is selected`() {
        coroutineRule.runTest {
            // Given
            val match = sdJwtPidMatch(sdJwtKeyPath("address", "country"))
            val items = render(listOf(getMockedSdJwtFullPid()), listOf(match))

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            val selection = selections.single()
            assertEquals(mockedSdJwtPidId, selection.documentId)
            assertEquals(setOf(sdJwtKeyPath("address", "country")), selection.selectedClaims)
        }
    }

    // Case 7:
    // An SD-JWT array wildcard request (["nationalities", null]) is rendered and the matched
    // array element is kept checked.
    //
    // Case 7 Expected Result:
    // The selection carries the requested wildcard path itself, not the concrete index.
    @Test
    fun `Given a kept SD-JWT array wildcard, When createSelectionsDomain, Then the wildcard claim is selected`() {
        coroutineRule.runTest {
            // Given
            val wildcard =
                sdJwtPath(ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements)
            val match = sdJwtPidMatch(wildcard)
            val items = render(listOf(getMockedSdJwtFullPid()), listOf(match))

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertEquals(setOf(wildcard), selections.single().selectedClaims)
        }
    }

    // Case 8:
    // An SD-JWT array wildcard request is rendered, then the user de-selects the array element.
    //
    // Case 8 Expected Result:
    // No selection is emitted — with nothing kept under the wildcard, the document is dropped.
    @Test
    fun `Given an unchecked SD-JWT array wildcard, When createSelectionsDomain, Then the document is dropped`() {
        coroutineRule.runTest {
            // Given
            val wildcard =
                sdJwtPath(ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements)
            val match = sdJwtPidMatch(wildcard)
            val items = render(listOf(getMockedSdJwtFullPid()), listOf(match))
                .uncheckLeavesWhere { true }

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertTrue(selections.isEmpty())
        }
    }

    // Case 9:
    // Two nested SD-JWT leaves under the same parent are requested (["address","country"],
    // ["address","locality"]); the user keeps only "country".
    //
    // Case 9 Expected Result:
    // The selection carries exactly the kept nested path; the de-selected sibling leaf's
    // requested path is correctly NOT returned.
    @Test
    fun `Given one of two nested SD-JWT leaves unchecked, When createSelectionsDomain, Then only the kept nested path is selected`() {
        coroutineRule.runTest {
            // Given
            val match = sdJwtPidMatch(
                sdJwtKeyPath("address", "country"),
                sdJwtKeyPath("address", "locality"),
            )
            val localityItemId = sdJwtKeyPath("address", "locality")
                .itemId(docId = mockedSdJwtPidId, queryId = null)
            val items = render(listOf(getMockedSdJwtFullPid()), listOf(match))
                .uncheckLeavesWhere { it == localityItemId }

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertEquals(
                setOf(sdJwtKeyPath("address", "country")),
                selections.single().selectedClaims
            )
        }
    }

    // Case 10:
    // An ANCESTOR SD-JWT request (["address"]) renders all six stored leaves; the user
    // keeps only one of them ("country").
    //
    // Case 10 Expected Result:
    // The selection carries the requested ANCESTOR path itself, not the kept leaf.
    @Test
    fun `Given an ancestor SD-JWT request with one kept leaf, When createSelectionsDomain, Then the ancestor path is selected`() {
        coroutineRule.runTest {
            // Given
            val ancestor = sdJwtKeyPath("address")
            val match = sdJwtPidMatch(ancestor)
            val countryItemId = sdJwtKeyPath("address", "country")
                .itemId(docId = mockedSdJwtPidId, queryId = null)
            val items = render(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                matches = listOf(match)
            ).uncheckLeavesWhere { it != countryItemId }

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertEquals(setOf(ancestor), selections.single().selectedClaims)
        }
    }

    // Case 11:
    // A concrete SD-JWT array-index request (["nationalities", 0]) is rendered and kept.
    //
    // Case 11 Expected Result:
    // The selection carries the typed Index path itself.
    @Test
    fun `Given a kept SD-JWT array index, When createSelectionsDomain, Then the index path is selected`() {
        coroutineRule.runTest {
            // Given
            val indexPath = sdJwtPath(
                ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)
            )
            val match = sdJwtPidMatch(indexPath)
            val items = render(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                matches = listOf(match)
            )

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertEquals(setOf(indexPath), selections.single().selectedClaims)
        }
    }

    // Case 12:
    // A concrete SD-JWT array-index request is rendered, then the element is de-selected.
    //
    // Case 12 Expected Result:
    // No selection — nothing kept under the only requested path drops the document.
    @Test
    fun `Given an unchecked SD-JWT array index, When createSelectionsDomain, Then the document is dropped`() {
        coroutineRule.runTest {
            // Given
            val indexPath = sdJwtPath(
                ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)
            )
            val match = sdJwtPidMatch(indexPath)
            val items = render(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                matches = listOf(match)
            ).uncheckLeavesWhere { true }

            // When
            val selections =
                RequestTransformer.createSelectionsDomain(
                    documentItemsUi = items,
                    matchesDomain = listOf(match),
                    claimsAreSelectable = mockedSelectableClaims,
                )

            // Then
            assertTrue(selections.isEmpty())
        }
    }

    // Case 13:
    // Two documents render (mdoc PID + mdoc mDL); the user de-selects EVERYTHING on the
    // PID card and keeps the mDL untouched.
    //
    // Case 13 Expected Result:
    // Exactly one selection — the mDL's; the fully-unchecked PID contributes nothing.
    @Test
    fun `Given one of two documents fully unchecked, When createSelectionsDomain, Then only the other document is selected`() {
        coroutineRule.runTest {
            // Given
            val pidMatch = pidMatch("family_name", "given_name")
            val mdlMatch = mdlMatch("family_name", "given_name")
            val items = render(
                storageDocuments = listOf(
                    getMockedPidWithBasicFields(),
                    getMockedMdlWithBasicFields()
                ),
                matches = listOf(pidMatch, mdlMatch),
            ).uncheckLeavesWhere { itemId ->
                itemId == pidPath("family_name").itemId(
                    docId = mockedPidId,
                    queryId = null
                ) || itemId == pidPath("given_name").itemId(
                    docId = mockedPidId,
                    queryId = null
                )
            }

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = listOf(pidMatch, mdlMatch),
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            val selection = selections.single()
            assertEquals(mockedMdlId, selection.documentId)
        }
    }

    // Case 14:
    // One query is satisfied by two different stored documents — both matches share the
    // queryId but carry different document ids.
    //
    // Case 14 Expected Result:
    // Two cards with distinct header ids, and two independent selections sharing the
    // queryId — the (documentId, queryId) key stays unique on the documentId axis.
    @Test
    fun `Given one query bundling two documents, When createSelectionsDomain, Then each document yields its own selection under the shared queryId`() {
        coroutineRule.runTest {
            // Given
            val secondMdl = getMockedMdlWithBasicFields()
                .copy(id = mockedSecondMdlId)
            val matches = listOf(
                mdlMatch("family_name", "given_name")
                    .copy(queryId = "mdl"),
                mdlMatch("family_name", "given_name")
                    .copy(
                        documentId = mockedSecondMdlId,
                        credentialId = "$mockedSecondMdlId-cred",
                        queryId = "mdl",
                    ),
            )
            val items = render(
                storageDocuments = listOf(getMockedMdlWithBasicFields(), secondMdl),
                matches = matches
            )
            assertEquals(2, items.map { it.headerUi.header.itemId }.toSet().size)

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = matches,
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            assertEquals(2, selections.size)
            assertEquals(setOf("mdl"), selections.map { it.queryId }.toSet())
            assertEquals(
                setOf(mockedMdlId, mockedSecondMdlId),
                selections.map { it.documentId }.toSet(),
            )
        }
    }

    // Case 15:
    // Two queries over the SAME document; the user de-selects EVERYTHING on one query's
    // card. The item ids are queryId-scoped, so the unchecks cannot leak onto the twin
    // card's identical-looking rows.
    //
    // Case 15 Expected Result:
    // Only the untouched query's selection survives.
    @Test
    fun `Given one of two same-document query cards fully unchecked, When createSelectionsDomain, Then only the untouched query survives`() {
        coroutineRule.runTest {
            // Given
            val nameQuery = pidMatch("family_name", "given_name")
                .copy(queryId = "pid_name")
            val numberQuery = pidMatch("expiry_date")
                .copy(queryId = "pid_doc_number")
            val matches = listOf(nameQuery, numberQuery)
            val nameItemIds = setOf(
                pidPath("family_name").itemId(docId = mockedPidId, queryId = "pid_name"),
                pidPath("given_name").itemId(docId = mockedPidId, queryId = "pid_name"),
            )
            val items = render(
                storageDocuments = listOf(getMockedPidWithBasicFields()),
                matches = matches
            ).uncheckLeavesWhere { it in nameItemIds }

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = matches,
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            assertEquals("pid_doc_number", selections.single().queryId)
        }
    }

    // Case 16:
    // Two queries over the SAME SD-JWT document both request the same NESTED claim
    // (place_of_birth.locality) plus one distinguishing leaf each.
    //
    // Case 16 Expected Result:
    // Every rendered item id — headers, nested groups and leaves — is pairwise distinct
    // across the two cards, and each query's selection carries its own claim set (the
    // shared nested path appears in BOTH).
    @Test
    fun `Given two queries sharing a nested claim, When createSelectionsDomain, Then ids and selections stay per-query`() {
        coroutineRule.runTest {
            // Given
            val familyQuery = sdJwtPidMatch(
                sdJwtKeyPath("place_of_birth", "locality"),
                sdJwtKeyPath("address", "country"),
            ).copy(queryId = "pid_family")
            val givenQuery = sdJwtPidMatch(
                sdJwtKeyPath("place_of_birth", "locality"),
                sdJwtKeyPath("address", "locality"),
            ).copy(queryId = "pid_given")
            val matches = listOf(familyQuery, givenQuery)
            val items = render(
                storageDocuments = listOf(getMockedSdJwtFullPid()),
                matches = matches
            )

            // Then — every id across both card trees is unique (headers, groups, leaves).
            val allItemIds = items.flatMap { item ->
                listOf(item.headerUi.header.itemId) +
                        item.headerUi.nestedItems.flatMap { it.collectItemIds() }
            }
            assertEquals(allItemIds.size, allItemIds.toSet().size)

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = matches,
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            assertEquals(2, selections.size)
            assertEquals(
                setOf(
                    sdJwtKeyPath("place_of_birth", "locality"),
                    sdJwtKeyPath("address", "country")
                ),
                selections.single { it.queryId == "pid_family" }.selectedClaims,
            )
            assertEquals(
                setOf(
                    sdJwtKeyPath("place_of_birth", "locality"),
                    sdJwtKeyPath("address", "locality")
                ),
                selections.single { it.queryId == "pid_given" }.selectedClaims,
            )
        }
    }

    // Case 17:
    // Two matches arrive with the SAME (documentId, queryId) identity but different
    // claims — possible only if the controller's identity guard upstream were bypassed
    // (it errors on conflicting duplicates before the transformer ever runs).
    //
    // Case 17 Expected Result:
    // The lookup is last-wins: the LAST match's claims drive the selection. This pins the
    // documented boundary behavior — the upstream guard, not this function, is the defense.
    @Test
    fun `Given two matches sharing one identity, When createSelectionsDomain, Then the lookup is last-wins behind the upstream guard`() {
        coroutineRule.runTest {
            // Given — two matches with an identical (documentId, queryId) identity.
            val first = pidMatch("family_name")
                .copy(queryId = "pid")
            val second = pidMatch("given_name")
                .copy(queryId = "pid")
            val items = render(
                storageDocuments = listOf(getMockedPidWithBasicFields()),
                matches = listOf(second)
            )

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = listOf(first, second),
                claimsAreSelectable = mockedSelectableClaims,
            )

            // Then
            assertEquals(setOf(pidPath("given_name")), selections.single().selectedClaims)
        }
    }

    // Case 18:
    // 1. A PID is rendered selectable and the user de-selects EVERY claim.
    // 2. createSelectionsDomain is invoked with claimsAreSelectable = false (OpenID4VP).
    //
    // Uncheck-all is the deliberate discriminator: under the selectable branch empty rows drop the
    // document, so asserting the full set is STILL disclosed proves the non-selectable branch
    // ignores row state.
    //
    // Case 18 Expected Result:
    // The verifier's full requested set is disclosed regardless of the (de)selected rows.
    @Test
    fun `Given claimsAreSelectable is false, When createSelectionsDomain, Then the full requested set is disclosed regardless of row state`() {
        coroutineRule.runTest {
            // Given
            val match = pidMatch("family_name", "given_name", "age_over_18")
            val items = render(listOf(getMockedPidWithBasicFields()), listOf(match))
                .uncheckLeavesWhere { true }

            // When
            val selections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = items,
                matchesDomain = listOf(match),
                claimsAreSelectable = mockedNonSelectableClaims,
            )

            // Then
            val selection = selections.single()
            assertEquals(mockedPidId, selection.documentId)
            assertEquals("$mockedPidId-cred", selection.credentialId)
            assertEquals(
                setOf(
                    pidPath("family_name"),
                    pidPath("given_name"),
                    pidPath("age_over_18"),
                ),
                selection.selectedClaims,
            )
        }
    }

    //endregion

    //region helpers

    /** The collapsed supporting text the document header shows on the request screen. */
    private val mockedRequestCollapsedSupportingText = "View details"

    /**
     * Builds the real UI rows the request screen would render for [matches] against
     * [storageDocuments], by running the genuine
     * [RequestTransformer.transformToDomainItems] + [RequestTransformer.transformToUiItems]
     * pipeline (the same path [RequestTransformer.transformToCombinationsUi] uses per combination).
     */
    private fun render(
        storageDocuments: List<IssuedDocument>,
        matches: List<PresentationMatchDomain>,
    ): List<RequestDocumentItemUi> {
        val domainItems = RequestTransformer.transformToDomainItems(
            storageDocuments = storageDocuments,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            requestMatchesDomain = matches,
        ).getOrThrow()
        return RequestTransformer.transformToUiItems(
            documentsDomain = domainItems,
            resourceProvider = resourceProvider,
            claimsAreSelectable = mockedSelectableClaims,
        )
    }

    /** Second-document id for the one-query-two-credentials case. */
    private val mockedSecondMdlId = "second-mdl-id"

    /** The document id of the claims-mocked two-nationalities SD-JWT PID. */
    private val mockedTwoNationalitiesSdJwtPidId = "two-nationalities-sd-jwt-pid-id"

    /** Shorthand: transformToCombinationsUi against [storageDocuments], unwrapped. */
    private fun transform(
        storageDocuments: List<IssuedDocument>,
        combinationsDomain: List<PresentationCombinationDomain>,
        claimsAreSelectable: Boolean,
    ): List<RequestCombinationUi> = RequestTransformer.transformToCombinationsUi(
        storageDocuments = storageDocuments,
        resourceProvider = resourceProvider,
        uuidProvider = uuidProvider,
        combinationsDomain = combinationsDomain,
        claimsAreSelectable = claimsAreSelectable,
    ).getOrThrow()

    /** A PID mso_mdoc match requesting the given data elements (queryId = null). */
    private fun pidMatch(vararg dataElements: String): PresentationMatchDomain =
        mockedMdocPresentationMatch(
            documentId = mockedPidId,
            credentialId = "$mockedPidId-cred",
            docType = mockedMdocPidDocType,
            namespace = mockedMdocPidNameSpace,
            dataElements = dataElements.toList(),
        )

    /** An mDL mso_mdoc match requesting the given data elements (queryId = null). */
    private fun mdlMatch(vararg dataElements: String): PresentationMatchDomain =
        mockedMdocPresentationMatch(
            documentId = mockedMdlId,
            credentialId = "$mockedMdlId-cred",
            docType = mockedMdocMdlDocType,
            namespace = mockedMdocMdlNameSpace,
            dataElements = dataElements.toList(),
        )

    /** A match against the claims-mocked two-nationalities SD-JWT PID (queryId = null). */
    private fun twoNationalitiesPidMatch(vararg requestedClaims: ClaimPathDomain): PresentationMatchDomain =
        PresentationMatchDomain(
            documentId = mockedTwoNationalitiesSdJwtPidId,
            credentialId = "$mockedTwoNationalitiesSdJwtPidId-cred",
            queryId = null,
            requestedClaims = requestedClaims.toList(),
        )

    private fun mockedTwoNationalitiesSdJwtPid(): IssuedDocument {
        val claims = listOf(
            mockedSdJwtVcClaim(
                pathElement = ClaimPathElement.Claim(name = "family_name"),
                value = "Smith"
            ),
            mockedSdJwtVcClaim(
                pathElement = ClaimPathElement.Claim(name = "nationalities"),
                children = listOf(
                    mockedSdJwtVcClaim(
                        pathElement = ClaimPathElement.ArrayElement(
                            index = 0
                        ),
                        value = "GR"
                    ),
                    mockedSdJwtVcClaim(
                        pathElement = ClaimPathElement.ArrayElement(
                            index = 1
                        ),
                        value = "SE"
                    ),
                ),
            ),
            mockedSdJwtVcClaim(
                pathElement = ClaimPathElement.Claim(name = "place_of_birth"),
                children = listOf(
                    mockedSdJwtVcClaim(
                        pathElement = ClaimPathElement.Claim(
                            name = "country"
                        ),
                        value = "Greece"
                    ),
                ),
            ),
        )

        val data = mock<SdJwtVcData>()
        whenever(data.claims).thenReturn(claims)

        val document = mock<IssuedDocument>()
        whenever(document.id).thenReturn(mockedTwoNationalitiesSdJwtPidId)
        whenever(document.name).thenReturn("Two-nationalities SD-JWT PID")
        whenever(document.format).thenReturn(SdJwtVcFormat(vct = mockedSdJwtPidVct))
        whenever(document.data).thenReturn(data)
        return document
    }

    /** Collects the item id of [this] and of every item nested under it, depth-first. */
    private fun ExpandableListItemUi.collectItemIds(): List<String> = when (this) {
        is ExpandableListItemUi.SingleListItem -> listOf(header.itemId)

        is ExpandableListItemUi.NestedListItem ->
            listOf(header.itemId) + nestedItems.flatMap { it.collectItemIds() }
    }

    /** The typed storage/disclosure path of a single PID mso_mdoc data element. */
    private fun pidPath(dataElement: String): ClaimPathDomain =
        listOf<ClaimPathSegment>(ClaimPathSegment.Key(dataElement))
            .toClaimPathDomain(ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace))

    /** The row id [ClaimItemId.Claim] produces for this claim path. */
    private fun ClaimPathDomain.itemId(docId: String, queryId: String?): String =
        ClaimItemId.Claim(docId = docId, queryId = queryId, path = this).encode()

    /** An SD-JWT VC match for the SD-JWT PID requesting the given claim paths (queryId = null). */
    private fun sdJwtPidMatch(vararg requestedClaims: ClaimPathDomain): PresentationMatchDomain =
        PresentationMatchDomain(
            documentId = mockedSdJwtPidId,
            credentialId = "$mockedSdJwtPidId-cred",
            queryId = null,
            requestedClaims = requestedClaims.toList(),
        )

    /** A typed SD-JWT VC path of plain object keys, e.g. `["address", "country"]`. */
    private fun sdJwtKeyPath(vararg keys: String): ClaimPathDomain =
        ClaimPathDomain.ofPlainKeys(names = keys.toList(), type = ClaimType.SdJwtVc)

    /** A typed SD-JWT VC path of arbitrary segments (keys, array indices, the wildcard). */
    private fun sdJwtPath(vararg segments: ClaimPathSegment): ClaimPathDomain =
        segments.toList().toClaimPathDomain(ClaimType.SdJwtVc)

    /** Finds the [ClaimDomain.Group] with the given [key] among these claims. */
    private fun List<ClaimDomain>.group(key: String): ClaimDomain.Group =
        filterIsInstance<ClaimDomain.Group>().first { it.key == key }

    /** Recursively counts the selectable (Primitive) leaves of a claim tree. */
    private fun List<ClaimDomain>.countLeaves(): Int = sumOf {
        when (it) {
            is ClaimDomain.Primitive -> 1
            is ClaimDomain.Group -> it.items.countLeaves()
        }
    }

    /** Recursively collects the keys of the selectable (Primitive) leaves of a claim tree. */
    private fun List<ClaimDomain>.leafKeys(): List<String> = flatMap {
        when (it) {
            is ClaimDomain.Primitive -> listOf(it.key)
            is ClaimDomain.Group -> it.items.leafKeys()
        }
    }

    /** True if any rendered leaf row across these documents carries a checkbox. */
    private fun List<RequestDocumentItemUi>.hasAnyCheckbox(): Boolean = any { document ->
        document.headerUi.nestedItems.any { it.hasAnyCheckbox() }
    }

    private fun ExpandableListItemUi.hasAnyCheckbox(): Boolean = when (this) {
        is ExpandableListItemUi.SingleListItem ->
            header.trailingContentData is ListItemTrailingContentDataUi.Checkbox

        is ExpandableListItemUi.NestedListItem ->
            nestedItems.any { it.hasAnyCheckbox() }
    }

    /**
     * Returns a copy of these rendered items with every leaf checkbox whose item id matches
     * [shouldUncheck] set to unchecked — emulating the user de-selecting those rows.
     */
    private fun List<RequestDocumentItemUi>.uncheckLeavesWhere(
        shouldUncheck: (itemId: String) -> Boolean,
    ): List<RequestDocumentItemUi> {
        fun ExpandableListItemUi.rewrite(): ExpandableListItemUi = when (this) {
            is ExpandableListItemUi.SingleListItem -> {
                val trailing = header.trailingContentData
                if (trailing is ListItemTrailingContentDataUi.Checkbox && shouldUncheck(header.itemId)) {
                    copy(
                        header = header.copy(
                            trailingContentData = trailing.copy(
                                checkboxData = trailing.checkboxData.copy(isChecked = false)
                            )
                        )
                    )
                } else {
                    this
                }
            }

            is ExpandableListItemUi.NestedListItem -> copy(nestedItems = nestedItems.map { it.rewrite() })
        }

        return map { item ->
            item.copy(
                headerUi = item.headerUi.copy(
                    nestedItems = item.headerUi.nestedItems.map { it.rewrite() }
                )
            )
        }
    }

    //endregion
}