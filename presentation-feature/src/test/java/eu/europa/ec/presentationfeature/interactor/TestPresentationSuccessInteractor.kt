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

package eu.europa.ec.presentationfeature.interactor

import android.content.Intent
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.model.ClaimItemId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransformToUiItemsStrings
import eu.europa.ec.testfeature.util.getMockedFullPid
import eu.europa.ec.testfeature.util.getMockedSdJwtFullPid
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedMdocPresentationSelection
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedSdJwtPidId
import eu.europa.ec.testfeature.util.mockedSdJwtPresentationSelection
import eu.europa.ec.testfeature.util.mockedUuid
import eu.europa.ec.testfeature.util.mockedVerifierName
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI

class TestPresentationSuccessInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var walletCorePresentationController: WalletCorePresentationController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uuidProvider: UuidProvider

    @Mock
    private lateinit var pendingIntent: Intent

    private lateinit var interactor: PresentationSuccessInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = PresentationSuccessInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            walletCorePresentationController = walletCorePresentationController,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region initiatorRoute
    @Test
    fun `When initiatorRoute is accessed, Then walletCorePresentationController#initiatorRoute is returned`() {
        // Given
        whenever(walletCorePresentationController.initiatorRoute).thenReturn(mockedInitiatorRoute)

        // When
        val result = interactor.initiatorRoute

        // Then
        assertEquals(mockedInitiatorRoute, result)
    }
    //endregion

    //region redirectUri
    @Test
    fun `When redirectUri is accessed, Then walletCorePresentationController#redirectUri is returned`() {
        // Given
        val expectedUri = URI(mockedRedirectUriString)
        whenever(walletCorePresentationController.redirectUri).thenReturn(expectedUri)

        // When
        val result = interactor.redirectUri

        // Then
        assertEquals(expectedUri, result)
    }
    //endregion

    //region getPendingIntent
    @Test
    fun `When getPendingIntent is called, Then walletCorePresentationController#pendingIntent is returned`() {
        // Given
        whenever(walletCorePresentationController.pendingIntent).thenReturn(pendingIntent)

        // When
        val result = interactor.getPendingIntent()

        // Then
        assertEquals(pendingIntent, result)
    }
    //endregion

    //region stopPresentation
    @Test
    fun `When stopPresentation is called, Then walletCorePresentationController#stopPresentation is invoked`() {
        // When
        interactor.stopPresentation()

        // Then
        verify(walletCorePresentationController, times(1)).stopPresentation()
    }
    //endregion

    //region setScopeId
    @Test
    fun `Given a scopeId, When setScopeId is called, Then presentationScopeId is set to the provided scopeId`() {
        // Given
        val mockScopeId = "mockScopeId"

        // When
        interactor.setScopeId(mockScopeId)

        // Then
        assertEquals(mockScopeId, interactor.presentationScopeId)
    }
    //endregion

    //region constructor default
    // the null-controller default: construction must not trigger the lazy Koin lookup
    @Test
    fun `When constructed without walletCorePresentationController, Then construction does not throw`() {
        // When
        val newInteractor = PresentationSuccessInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals("DefaultPresentationScopeId", newInteractor.presentationScopeId)
    }
    //endregion

    //region getUiItems

    // Case 1:
    // 1. walletCorePresentationController.disclosedDocuments returns null.
    // 2. walletCorePresentationController.verifierName returns null.
    // 3. walletCorePresentationController.verifierIsTrusted returns null
    //    (the `== true` check will evaluate to false, so isVerified is false).

    // Case 1 Expected Result:
    // Success state with:
    //  - an empty documentsUi list,
    //  - headerConfig whose description is the error-case string,
    //  - relyingPartyData with the default name and isVerified = false.
    @Test
    fun `Given Case 1, When getUiItems is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.disclosedDocuments).thenReturn(null)
            whenever(walletCorePresentationController.verifierName).thenReturn(null)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(null)
            mockSuccessHeaderStrings()

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val expectedResult = PresentationSuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = emptyList(),
                    headerConfig = ContentHeaderConfig(
                        description = mockedErrorDescription,
                        relyingPartyData = RelyingPartyDataUi(
                            name = mockedDefaultRelyingPartyName,
                            isVerified = false,
                        )
                    )
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    }

    // Case 2:
    // 1. walletCorePresentationController.disclosedDocuments returns a list with one DisclosedDocument
    //    referencing the mocked PID with one non-empty disclosed item.
    // 2. walletCoreDocumentsController.getDocumentById returns the mocked PID IssuedDocument.
    // 3. walletCorePresentationController.verifierName returns a non-empty name.
    // 4. walletCorePresentationController.verifierIsTrusted returns true.

    // Case 2 Expected Result:
    // Success state with:
    //  - documentsUi containing one NestedListItem with the document's id/name/supporting text/icon
    //    and non-empty nested claims items,
    //  - headerConfig whose description is the standard string,
    //  - relyingPartyData with the verifier's name and isVerified = true.
    @Test
    fun `Given Case 2, When getUiItems is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val pid = getMockedFullPid()
            val disclosedDocument = mockedMdocPresentationSelection(
                documentId = mockedPidId,
                namespace = mockedMdocPidNameSpace,
                dataElements = listOf("family_name"),
            )

            whenever(walletCorePresentationController.disclosedDocuments)
                .thenReturn(mutableListOf(disclosedDocument))
            whenever(walletCoreDocumentsController.getDocumentById(documentId = mockedPidId))
                .thenReturn(pid)
            whenever(walletCorePresentationController.verifierName).thenReturn(mockedVerifierName)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(true)
            mockSuccessHeaderStrings()
            mockTransformToUiItemsStrings(resourceProvider)
            whenever(uuidProvider.provideUuid()).thenReturn(mockedUuid)

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is PresentationSuccessInteractorGetUiItemsPartialState.Success)
                result as PresentationSuccessInteractorGetUiItemsPartialState.Success

                assertEquals(1, result.documentsUi.size)
                val docUi = result.documentsUi.first()
                assertEquals(
                    ClaimItemId.DocumentHeader(
                        docId = mockedPidId,
                        queryId = null
                    ).encode(),
                    docUi.header.itemId
                )
                assertEquals(
                    ListItemMainContentDataUi.Text(text = pid.name),
                    docUi.header.mainContentData
                )
                assertEquals(mockedDocumentSupportingText, docUi.header.supportingText)
                assertEquals(
                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown),
                    docUi.header.trailingContentData
                )
                assertEquals(false, docUi.isExpanded)
                assertTrue(docUi.nestedItems.isNotEmpty())

                assertEquals(
                    ContentHeaderConfig(
                        description = mockedNormalDescription,
                        relyingPartyData = RelyingPartyDataUi(
                            name = mockedVerifierName,
                            isVerified = true,
                        )
                    ),
                    result.headerConfig
                )
            }
        }
    }

    // Case 3:
    // 1. walletCorePresentationController.disclosedDocuments returns a list with one DisclosedDocument
    //    whose disclosedItems is empty (so transformPathsToDomainClaims yields no claims and
    //    the document is not added to documentsUi).
    // 2. walletCoreDocumentsController.getDocumentById returns the mocked PID IssuedDocument.

    // Case 3 Expected Result:
    // Success state with:
    //  - an empty documentsUi list,
    //  - headerConfig whose description is the error-case string.
    @Test
    fun `Given Case 3, When getUiItems is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val pid = getMockedFullPid()
            val disclosedDocument = mockedMdocPresentationSelection(
                documentId = mockedPidId,
                namespace = mockedMdocPidNameSpace,
                dataElements = emptyList(),
            )

            whenever(walletCorePresentationController.disclosedDocuments)
                .thenReturn(mutableListOf(disclosedDocument))
            whenever(walletCoreDocumentsController.getDocumentById(documentId = mockedPidId))
                .thenReturn(pid)
            whenever(walletCorePresentationController.verifierName).thenReturn(mockedVerifierName)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(true)
            mockSuccessHeaderStrings()

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val expectedResult = PresentationSuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = emptyList(),
                    headerConfig = ContentHeaderConfig(
                        description = mockedErrorDescription,
                        relyingPartyData = RelyingPartyDataUi(
                            name = mockedVerifierName,
                            isVerified = true,
                        )
                    )
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    }

    // Case 4:
    // 1. walletCorePresentationController.disclosedDocuments returns a list with one DisclosedDocument.
    // 2. walletCoreDocumentsController.getDocumentById throws (so the per-document try/catch is exercised
    //    and the document is silently skipped).

    // Case 4 Expected Result:
    // Success state with:
    //  - an empty documentsUi list,
    //  - headerConfig whose description is the error-case string.
    @Test
    fun `Given Case 4, When getUiItems is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val disclosedDocument = mockedMdocPresentationSelection(
                documentId = mockedPidId,
                namespace = mockedMdocPidNameSpace,
                dataElements = listOf("family_name"),
            )

            whenever(walletCorePresentationController.disclosedDocuments)
                .thenReturn(mutableListOf(disclosedDocument))
            whenever(walletCoreDocumentsController.getDocumentById(documentId = mockedPidId))
                .thenThrow(mockedExceptionWithMessage)
            whenever(walletCorePresentationController.verifierName).thenReturn(mockedVerifierName)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(false)
            mockSuccessHeaderStrings()

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val expectedResult = PresentationSuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = emptyList(),
                    headerConfig = ContentHeaderConfig(
                        description = mockedErrorDescription,
                        relyingPartyData = RelyingPartyDataUi(
                            name = mockedVerifierName,
                            isVerified = false,
                        )
                    )
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    }

    // Case 5:
    // 1. The outer Flow body throws an exception with a localized message
    //    (here, walletCorePresentationController.verifierName access throws).

    // Case 5 Expected Result:
    // Failed state with the thrown exception's localized message.
    @Test
    fun `Given Case 5, When getUiItems is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.disclosedDocuments).thenReturn(null)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(false)
            whenever(walletCorePresentationController.verifierName)
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val expectedResult = PresentationSuccessInteractorGetUiItemsPartialState.Failed(
                    errorMessage = mockedExceptionWithMessage.localizedMessage!!
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    }

    // Case 6:
    // 1. The outer Flow body throws an exception with no message.

    // Case 6 Expected Result:
    // Failed state with the generic error message.
    @Test
    fun `Given Case 6, When getUiItems is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.disclosedDocuments).thenReturn(null)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(false)
            whenever(walletCorePresentationController.verifierName)
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val expectedResult = PresentationSuccessInteractorGetUiItemsPartialState.Failed(
                    errorMessage = mockedGenericErrorMessage
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    }

    // Case 7:
    // 1. walletCorePresentationController.disclosedDocuments returns two selections for the
    //    SAME physical document, originating from two different DCQL queries.
    // 2. walletCoreDocumentsController.getDocumentById returns the mocked PID for both.

    // Case 7 Expected Result:
    // Success state with two documentsUi cards whose header item ids are distinct —
    // identity is (docId, queryId), so expand/collapse state cannot couple across the two cards.
    @Test
    fun `Given Case 7, When getUiItems is called, Then two same-document cards get distinct header ids`() {
        coroutineRule.runTest {
            // Given
            val pid = getMockedFullPid()
            val selectionQuery1 = mockedMdocPresentationSelection(
                documentId = mockedPidId,
                namespace = mockedMdocPidNameSpace,
                dataElements = listOf("family_name"),
                queryId = "query-1",
            )
            val selectionQuery2 = mockedMdocPresentationSelection(
                documentId = mockedPidId,
                namespace = mockedMdocPidNameSpace,
                dataElements = listOf("given_name"),
                queryId = "query-2",
            )

            whenever(walletCorePresentationController.disclosedDocuments)
                .thenReturn(mutableListOf(selectionQuery1, selectionQuery2))
            whenever(walletCoreDocumentsController.getDocumentById(documentId = mockedPidId))
                .thenReturn(pid)
            whenever(walletCorePresentationController.verifierName).thenReturn(mockedVerifierName)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(true)
            mockSuccessHeaderStrings()
            mockTransformToUiItemsStrings(resourceProvider)
            whenever(uuidProvider.provideUuid()).thenReturn(mockedUuid)

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is PresentationSuccessInteractorGetUiItemsPartialState.Success)
                result as PresentationSuccessInteractorGetUiItemsPartialState.Success

                assertEquals(2, result.documentsUi.size)
                assertEquals(
                    listOf(
                        ClaimItemId.DocumentHeader(
                            docId = mockedPidId,
                            queryId = "query-1"
                        ).encode(),
                        ClaimItemId.DocumentHeader(
                            docId = mockedPidId,
                            queryId = "query-2"
                        ).encode(),
                    ),
                    result.documentsUi.map { it.header.itemId }
                )
            }
        }
    }

    // Case 8:
    // 1. walletCorePresentationController.disclosedDocuments returns two selections for the
    //    SAME SD-JWT document, originating from two different DCQL queries, both disclosing
    //    the same nested claim (place_of_birth -> locality).
    // 2. walletCoreDocumentsController.getDocumentById returns the mocked SD-JWT PID for both.

    // Case 8 Expected Result:
    // Success state with two documentsUi cards where EVERY rendered item id — card headers,
    // nested group headers AND claim leaves — is unique across the whole screen. Nested ids are
    // (docId, queryId)-scoped, so expanding the place_of_birth group on one card cannot
    // co-expand its twin on the other.
    @Test
    fun `Given Case 8, When getUiItems is called, Then nested item ids are distinct across same-document cards`() {
        coroutineRule.runTest {
            // Given
            val sdJwtPid = getMockedSdJwtFullPid()
            val selectionQuery1 = mockedSdJwtPresentationSelection(
                documentId = mockedSdJwtPidId,
                claimPaths = listOf(listOf("place_of_birth", "locality")),
                queryId = "query-1",
            )
            val selectionQuery2 = mockedSdJwtPresentationSelection(
                documentId = mockedSdJwtPidId,
                claimPaths = listOf(listOf("place_of_birth", "locality")),
                queryId = "query-2",
            )

            whenever(walletCorePresentationController.disclosedDocuments)
                .thenReturn(mutableListOf(selectionQuery1, selectionQuery2))
            whenever(walletCoreDocumentsController.getDocumentById(documentId = mockedSdJwtPidId))
                .thenReturn(sdJwtPid)
            whenever(walletCorePresentationController.verifierName).thenReturn(mockedVerifierName)
            whenever(walletCorePresentationController.verifierIsTrusted).thenReturn(true)
            mockSuccessHeaderStrings()
            mockTransformToUiItemsStrings(resourceProvider)
            whenever(uuidProvider.provideUuid()).thenReturn(mockedUuid)

            // When
            interactor.getUiItems().runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is PresentationSuccessInteractorGetUiItemsPartialState.Success)
                result as PresentationSuccessInteractorGetUiItemsPartialState.Success

                assertEquals(2, result.documentsUi.size)
                result.documentsUi.forEach { docUi ->
                    assertTrue(docUi.nestedItems.isNotEmpty())
                }
                val allItemIds = result.documentsUi.flatMap { collectItemIds(it) }
                assertEquals(allItemIds.size, allItemIds.toSet().size)
            }
        }
    }

    //endregion

    //region helper functions
    /** Collects the item id of [item] and of every item nested under it, depth-first. */
    private fun collectItemIds(item: ExpandableListItemUi): List<String> {
        return when (item) {
            is ExpandableListItemUi.SingleListItem -> listOf(item.header.itemId)

            is ExpandableListItemUi.NestedListItem ->
                listOf(item.header.itemId) + item.nestedItems.flatMap { collectItemIds(it) }
        }
    }

    private fun mockSuccessHeaderStrings() {
        whenever(resourceProvider.getString(R.string.document_success_header_description_when_error))
            .thenReturn(mockedErrorDescription)
        whenever(resourceProvider.getString(R.string.document_success_header_description))
            .thenReturn(mockedNormalDescription)
        whenever(resourceProvider.getString(R.string.document_success_relying_party_default_name))
            .thenReturn(mockedDefaultRelyingPartyName)
        whenever(resourceProvider.getString(R.string.document_success_collapsed_supporting_text))
            .thenReturn(mockedDocumentSupportingText)
    }
    //endregion

    //region mocked objects
    private val mockedInitiatorRoute = "mockedInitiatorRoute"
    private val mockedRedirectUriString = "https://example.com/redirect"
    private val mockedErrorDescription = "Mocked error description"
    private val mockedNormalDescription = "Mocked normal description"
    private val mockedDefaultRelyingPartyName = "Mocked default relying party name"
    private val mockedDocumentSupportingText = "Mocked document supporting text"
    //endregion
}
