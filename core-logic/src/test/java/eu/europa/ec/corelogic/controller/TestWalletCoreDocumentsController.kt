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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.corelogic.config.VciConfig
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.util.mockedDeferredConfig
import eu.europa.ec.corelogic.util.mockedDocumentOperationState
import eu.europa.ec.corelogic.worker.WalletWorkScheduler
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OfferResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.trust.IssuerNotTrustedException
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.DeferredDocumentScheduleDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.IssuerTrustDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import eu.europa.ec.storagelogic.model.DeferredDocumentScheduleStatus
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import eu.europa.ec.storagelogic.model.IssuerTrust
import eu.europa.ec.storagelogic.model.IssuerTrustVerdict
import eu.europa.ec.storagelogic.model.RevocationStatus
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.net.URL
import java.net.URLEncoder
import java.security.cert.CertificateExpiredException

class TestWalletCoreDocumentsController {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCoreConfig: WalletCoreConfig

    @Mock
    private lateinit var bookmarkDao: BookmarkDao

    @Mock
    private lateinit var revokedDocumentDao: RevokedDocumentDao

    @Mock
    private lateinit var failedReIssuedDocumentDao: FailedReIssuedDocumentDao

    @Mock
    private lateinit var issuerTrustDao: IssuerTrustDao

    @Mock
    private lateinit var statusRefreshFailureDao: StatusRefreshFailureDao

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var walletWorkScheduler: WalletWorkScheduler

    @Mock
    private lateinit var eudiWallet: EudiWallet

    @Mock
    private lateinit var deferredScheduleDao: DeferredDocumentScheduleDao

    @Mock
    private lateinit var walletDocumentOperationController: WalletDocumentOperationController

    private lateinit var controller: WalletCoreDocumentsController

    private val firstIssuerManager: OpenId4VciManager = mock()

    private val configuredIssuerManager: OpenId4VciManager = mock()

    private lateinit var closeable: AutoCloseable

    private val pidType = "eu.europa.ec.eudi.pid.1"
    private val pid: IssuedDocument = mock {
        on { id } doReturn "pid"
        on { format } doReturn MsoMdocFormat(pidType)
    }
    private val otherPid: IssuedDocument = mock {
        on { id } doReturn "otherPid"
        on { format } doReturn MsoMdocFormat(pidType)
    }

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        runBlocking {
            whenever(prefKeys.getSessionId()).thenReturn("session")
        }
        whenever(walletCoreConfig.deferredIssuanceConfig).thenReturn(mockedDeferredConfig)
        whenever(walletDocumentOperationController.state).thenReturn(
            MutableStateFlow(
                mockedDocumentOperationState
            )
        )
        whenever(
            walletDocumentOperationController.trackedOperationFlow<IssueDocumentsPartialState>(
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenAnswer { invocation ->
                invocation.getArgument<() -> Flow<IssueDocumentsPartialState>>(
                    3
                ).invoke()
            }
        runBlocking {
            whenever(
                walletDocumentOperationController.withRemoval<DeleteDocumentPartialState>(
                    any(),
                    any()
                )
            )
                .doSuspendableAnswer { invocation ->
                    invocation.getArgument<suspend () -> DeleteDocumentPartialState>(
                        1
                    ).invoke()
                }
            whenever(walletDocumentOperationController.withDocumentLock<Unit>(any(), any()))
                .doSuspendableAnswer { invocation ->
                    invocation.getArgument<suspend () -> Unit>(1).invoke()
                }
        }
        whenever(eudiWallet.getDocumentById("pid")).thenReturn(pid)
        controller = WalletCoreDocumentsControllerImpl(
            resourceProvider = resourceProvider,
            walletCoreConfig = walletCoreConfig,
            bookmarkDao = bookmarkDao,
            revokedDocumentDao = revokedDocumentDao,
            failedReIssuedDocumentDao = failedReIssuedDocumentDao,
            issuerTrustDao = issuerTrustDao,
            statusRefreshFailureDao = statusRefreshFailureDao,
            prefKeys = prefKeys,
            walletWorkScheduler = walletWorkScheduler,
            deferredScheduleDao = deferredScheduleDao,
            walletDocumentOperationController = walletDocumentOperationController,
            dispatcher = coroutineRule.testDispatcher,
            walletCore = eudiWallet,
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    private fun implementation() = controller as WalletCoreDocumentsControllerImpl

    @Test
    fun `Given blocked documents, When asked whether each is suspended, Then only the suspended one is`() =
        coroutineRule.runTest {
            // Given
            whenever(revokedDocumentDao.retrieve("suspended"))
                .thenReturn(
                    RevokedDocument(
                        identifier = "suspended",
                        status = RevocationStatus.SUSPENDED
                    )
                )
            whenever(revokedDocumentDao.retrieve("revoked"))
                .thenReturn(
                    RevokedDocument(
                        identifier = "revoked",
                        status = RevocationStatus.INVALID
                    )
                )

            // Then
            assertTrue(controller.isDocumentSuspended("suspended"))
            assertFalse(controller.isDocumentSuspended("revoked"))
            assertFalse(controller.isDocumentSuspended("valid"))
        }

    @Test
    fun `Given a stored document, When it is deleted, Then its own records go`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.deleteDocumentById("pid")).thenReturn(Outcome.success(null))

            // When
            val result = controller.deleteDocument("pid").first()

            // Then
            assertEquals(DeleteDocumentPartialState.Success, result)
            verify(revokedDocumentDao).delete("pid")
            verify(issuerTrustDao).delete("pid")
            verify(statusRefreshFailureDao).delete("pid")
            verify(bookmarkDao).delete("pid")
            verify(failedReIssuedDocumentDao).delete("pid")
        }

    @Test
    fun `Given a storage error after Wallet Core deleted the document, When it is deleted, Then the deletion is still reported`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.deleteDocumentById("pid")).thenReturn(Outcome.success(null))
            whenever(bookmarkDao.delete("pid")).doThrow(IllegalStateException("database closed"))

            // When
            val result = controller.deleteDocument("pid").first()

            // Then
            assertEquals(DeleteDocumentPartialState.Success, result)
        }

    @Test
    fun `Given a re-issuance replaced the document, When it is cleaned up, Then the bookmark moves and the original records go`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.getDocumentById("pid")).thenReturn(null)
            whenever(bookmarkDao.retrieve("pid")).thenReturn(Bookmark("pid"))

            // When
            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.Success(documentIds = listOf("newPid")),
            )

            // Then
            verify(bookmarkDao).store(Bookmark("newPid"))
            verify(bookmarkDao).delete("pid")
            verify(revokedDocumentDao).delete("pid")
            verify(issuerTrustDao).delete("pid")
            verify(walletWorkScheduler, never()).scheduleRevocationCheck()
        }

    @Test
    fun `Given the original was revoked during its re-issuance, When it is cleaned up, Then a revocation check is scheduled`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.getDocumentById("pid")).thenReturn(null)
            whenever(revokedDocumentDao.retrieve("pid"))
                .thenReturn(RevokedDocument("pid", RevocationStatus.INVALID))

            // When
            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.Success(documentIds = listOf("newPid")),
            )

            // Then
            verify(walletWorkScheduler).scheduleRevocationCheck()
        }

    @Test
    fun `Given a deferred re-issuance, When it is cleaned up, Then the bookmark is copied and the original is kept until replaced`() =
        coroutineRule.runTest {
            // Given
            whenever(bookmarkDao.retrieve("pid")).thenReturn(Bookmark("pid"))

            // When
            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.DeferredSuccess(deferredDocuments = mapOf("newPid" to pidType)),
            )

            // Then
            verify(bookmarkDao).store(Bookmark("newPid"))
            verify(bookmarkDao, never()).delete(any())
            verify(revokedDocumentDao, never()).delete(any())
        }

    @Test
    fun `Given a failed re-issuance, When it is cleaned up, Then nothing changes`() =
        coroutineRule.runTest {
            // When
            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.Failure(errorMessage = "error"),
            )

            // Then
            verifyNoInteractions(bookmarkDao, revokedDocumentDao, issuerTrustDao)
        }

    @Test
    fun `Given records of a document Wallet Core no longer holds, When missing documents are cleaned up, Then only those records go`() =
        coroutineRule.runTest {
            // Given
            whenever(revokedDocumentDao.retrieveAll()).thenReturn(emptyList())
            whenever(issuerTrustDao.retrieveAll()).thenReturn(
                listOf(
                    IssuerTrust(identifier = "oldPid", verdict = IssuerTrustVerdict.TRUSTED),
                    IssuerTrust(identifier = "otherPid", verdict = IssuerTrustVerdict.TRUSTED),
                )
            )
            whenever(statusRefreshFailureDao.retrieveAll()).thenReturn(emptyList())
            whenever(bookmarkDao.retrieveAll()).thenReturn(listOf(Bookmark("oldPid")))
            whenever(failedReIssuedDocumentDao.retrieveAll()).thenReturn(
                listOf(
                    FailedReIssuedDocument("otherPid")
                )
            )
            whenever(eudiWallet.getDocuments(anyOrNull())).thenReturn(listOf(otherPid))

            // When
            implementation().deleteLocalMetadataForMissingDocuments()

            // Then
            verify(issuerTrustDao).delete("oldPid")
            verify(bookmarkDao).delete("oldPid")
            verify(issuerTrustDao, never()).delete("otherPid")
            verify(failedReIssuedDocumentDao, never()).delete("otherPid")
            verify(walletWorkScheduler, never()).scheduleRevocationCheck()
        }

    @Test
    fun `Given a revoked document Wallet Core no longer holds, When missing documents are cleaned up, Then a revocation check is scheduled`() =
        coroutineRule.runTest {
            // Given
            whenever(revokedDocumentDao.retrieveAll())
                .thenReturn(listOf(RevokedDocument("oldPid", RevocationStatus.INVALID)))
            whenever(issuerTrustDao.retrieveAll()).thenReturn(emptyList())
            whenever(statusRefreshFailureDao.retrieveAll()).thenReturn(emptyList())
            whenever(bookmarkDao.retrieveAll()).thenReturn(emptyList())
            whenever(failedReIssuedDocumentDao.retrieveAll()).thenReturn(emptyList())
            whenever(eudiWallet.getDocuments(anyOrNull())).thenReturn(listOf(otherPid))

            // When
            implementation().deleteLocalMetadataForMissingDocuments()

            // Then
            verify(revokedDocumentDao).delete("oldPid")
            verify(walletWorkScheduler).scheduleRevocationCheck()
        }

    @Test
    fun `Given Wallet Core fails to delete the document, When it is deleted, Then nothing else is deleted`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.deleteDocumentById("pid"))
                .thenReturn(Outcome.failure(IllegalStateException("storage error")))
            whenever(resourceProvider.genericErrorMessage()).thenReturn("error")

            // When
            val result = controller.deleteDocument("pid").first()

            // Then
            assertTrue(result is DeleteDocumentPartialState.Failure)
            verifyNoInteractions(
                revokedDocumentDao,
                issuerTrustDao,
                statusRefreshFailureDao,
                bookmarkDao,
                failedReIssuedDocumentDao,
                deferredScheduleDao,
            )
        }

    @Test
    fun `Given an offer from an issuer that is not configured, When it is resolved, Then it fails before any issuer is contacted`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()

            // When
            val result =
                controller.resolveDocumentOffer(offerByValue("https://attacker.example")).first()

            // Then
            assertEquals(ResolveDocumentOfferPartialState.Failure(NOT_CONFIGURED), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given a link that is not a usable offer, When it is resolved, Then it fails before any issuer is contacted`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()

            // When
            val result =
                controller.resolveDocumentOffer("openid-credential-offer://?credential_offer=%7Bnot-json")
                    .first()

            // Then
            assertEquals(ResolveDocumentOfferPartialState.Failure(UNREADABLE), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given an offer from a configured issuer, When it is resolved, Then that issuer's manager reads it`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            val link = offerByValue(CONFIGURED_ISSUER)

            // When
            val job =
                coroutineRule.testScope.launch { controller.resolveDocumentOffer(link).collect {} }
            coroutineRule.testScope.runCurrent()

            // Then
            verify(configuredIssuerManager, timeout(5_000)).resolveDocumentOffer(
                eq(link),
                anyOrNull(),
                any()
            )
            verifyNoInteractions(firstIssuerManager)
            job.cancel()
        }

    @Test
    fun `Given an offer that resolves to an issuer that is not configured, When it is resolved, Then it is refused`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            val link = offerByValue(CONFIGURED_ISSUER)
            val resolvedOffer: CredentialOffer = mock()
            whenever(resolvedOffer.credentialIssuerIdentifier).thenAnswer { URL("https://other.example") }
            val offer: Offer = mock {
                on { credentialOffer } doReturn resolvedOffer
            }
            whenever(configuredIssuerManager.resolveDocumentOffer(eq(link), anyOrNull(), any()))
                .thenAnswer { invocation ->
                    invocation.getArgument<OpenId4VciManager.OnResolvedOffer>(2)
                        .onResult(OfferResult.Success(offer))
                }

            // When
            val result = controller.resolveDocumentOffer(link).first()

            // Then
            assertEquals(ResolveDocumentOfferPartialState.Failure(NOT_CONFIGURED), result)
        }

    @Test
    fun `Given a deferred document, When it is issued, Then the manager of its own issuer is used`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            val documentId = "deferred"
            val metadata: IssuerMetadata =
                mock { on { credentialIssuerIdentifier } doReturn CONFIGURED_ISSUER }
            val deferred: DeferredDocument = mock {
                on { id } doReturn documentId
                on { issuerMetadata } doReturn metadata
            }
            whenever(eudiWallet.getDocumentById(documentId)).thenReturn(deferred)
            whenever(walletDocumentOperationController.startCorePolling(eq(documentId), any()))
                .thenAnswer { invocation ->
                    invocation.getArgument<() -> Unit>(1).invoke()
                    true
                }

            // When
            val job = coroutineRule.testScope.launch {
                controller.issueDeferredDocument(documentId).collect {}
            }
            coroutineRule.testScope.runCurrent()

            // Then
            verify(configuredIssuerManager, timeout(5_000)).issueDeferredDocument(
                eq(deferred),
                anyOrNull(),
                any()
            )
            verifyNoInteractions(firstIssuerManager)
            job.cancel()
        }

    @Test
    fun `Given a document from an issuer that is not configured, When it is re-issued, Then it fails without using another issuer`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()

            // When
            val result = controller.reIssueDocument(
                documentId = "pid",
                issuerId = "https://attacker.example",
                allowAuthorizationFallback = false,
            ).first()

            // Then
            assertEquals(IssueDocumentsPartialState.Failure(errorMessage = DOCUMENT_ERROR), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given a revoked document, When it is re-issued, Then it is refused before the issuer is used`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            whenever(revokedDocumentDao.retrieve("pid"))
                .thenReturn(RevokedDocument("pid", RevocationStatus.SUSPENDED))
            whenever(resourceProvider.getString(R.string.document_details_reissue_blocked_text))
                .thenReturn("blocked")

            // When
            val result = controller.reIssueDocument(
                documentId = "pid",
                issuerId = CONFIGURED_ISSUER,
                allowAuthorizationFallback = false,
            ).first()

            // Then
            assertEquals(IssueDocumentsPartialState.Failure(errorMessage = "blocked"), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given the issuer issues only part of a request, When issueDocuments is called, Then the documents not issued are passed on`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            val notIssued: UnsignedDocument = mock {
                on { name } doReturn "mDL"
                on { format } doReturn MsoMdocFormat(MDL_TYPE)
            }
            doAnswer { invocation ->
                val onIssueEvent = invocation.getArgument<OpenId4VciManager.OnIssueEvent>(4)
                onIssueEvent(IssueEvent.Started(total = 2))
                onIssueEvent(
                    IssueEvent.DocumentFailed(
                        notIssued,
                        IllegalStateException("not issued")
                    )
                )
                onIssueEvent(IssueEvent.Finished(issuedDocuments = listOf("pid")))
            }.whenever(configuredIssuerManager).issueDocumentByConfigurationIdentifiers(
                eq(CONFIGURED_ISSUER),
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )

            // When
            val result = controller.issueDocuments(
                issuanceMethod = IssuanceMethod.OPENID4VCI,
                configIds = listOf("pid", "mdl"),
                issuerId = CONFIGURED_ISSUER,
            ).first()

            // Then
            assertEquals(
                IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf("pid"),
                    nonIssuedDocuments = mapOf(MDL_TYPE to "mDL"),
                ),
                result
            )
        }

    /**
     * 1. An issuance waits for its browser sign-in, then Core reports Started.
     *
     * Expected result:
     * The tracked operation can be abandoned while it waits, and no longer once Core has started.
     */
    @Test
    fun `Given an issuance, When Core reports Started, Then it can no longer be abandoned`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            var isAbandonable: (() -> Boolean)? = null
            var abandonableBeforeStart: Boolean? = null
            whenever(
                walletDocumentOperationController.trackedOperationFlow<IssueDocumentsPartialState>(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer { invocation ->
                isAbandonable = invocation.getArgument(2)
                invocation.getArgument<() -> Flow<IssueDocumentsPartialState>>(3).invoke()
            }
            doAnswer { invocation ->
                abandonableBeforeStart = isAbandonable?.invoke()
                val onIssueEvent = invocation.getArgument<OpenId4VciManager.OnIssueEvent>(4)
                onIssueEvent(IssueEvent.Started(total = 1))
                onIssueEvent(IssueEvent.Finished(issuedDocuments = listOf("pid")))
            }.whenever(configuredIssuerManager).issueDocumentByConfigurationIdentifiers(
                eq(CONFIGURED_ISSUER),
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )

            // When
            controller.issueDocuments(
                issuanceMethod = IssuanceMethod.OPENID4VCI,
                configIds = listOf("pid"),
                issuerId = CONFIGURED_ISSUER,
            ).first()

            // Then
            assertEquals(true, abandonableBeforeStart)
            assertEquals(false, isAbandonable?.invoke())
        }

    @Test
    fun `Given a failed re-issuance record, When hasReIssuanceFailed is called, Then it is reported for that document only`() =
        coroutineRule.runTest {
            // Given
            whenever(failedReIssuedDocumentDao.retrieve("pid")).thenReturn(FailedReIssuedDocument("pid"))

            // Then
            assertTrue(controller.hasReIssuanceFailed("pid"))
            assertFalse(controller.hasReIssuanceFailed("otherPid"))
        }

    /**
     * 1. Core reports one document as deferred during an issuance.
     *
     * Expected result:
     * An active schedule is stored for it, due after the configured initial polling delay.
     */
    @Test
    fun `Given Core defers a document, When it is issued, Then its schedule is stored`() =
        coroutineRule.runTest {
            // Given
            givenTwoIssuers()
            val deferred = mockedDeferredDocument()
            doAnswer { invocation ->
                val onIssueEvent = invocation.getArgument<OpenId4VciManager.OnIssueEvent>(4)
                onIssueEvent(IssueEvent.Started(total = 1))
                onIssueEvent(IssueEvent.DocumentDeferred(deferred))
                onIssueEvent(IssueEvent.Finished(issuedDocuments = listOf(DEFERRED_ID)))
            }.whenever(configuredIssuerManager).issueDocumentByConfigurationIdentifiers(
                eq(CONFIGURED_ISSUER),
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )
            val delay = mockedDeferredConfig.initialPollingDelay.inWholeMilliseconds
            val earliest = System.currentTimeMillis() + delay

            // When
            val result = controller.issueDocuments(
                issuanceMethod = IssuanceMethod.OPENID4VCI,
                configIds = listOf("mdl"),
                issuerId = CONFIGURED_ISSUER,
            ).first()

            // Then
            val latest = System.currentTimeMillis() + delay
            assertEquals(
                IssueDocumentsPartialState.DeferredSuccess(
                    deferredDocuments = mapOf(DEFERRED_ID to MDL_TYPE)
                ),
                result,
            )
            val schedule = argumentCaptor<DeferredDocumentSchedule>().apply {
                verify(deferredScheduleDao).store(capture())
            }.firstValue
            assertEquals(DEFERRED_ID, schedule.identifier)
            assertEquals(DeferredDocumentScheduleStatus.ACTIVE, schedule.status)
            assertNull(schedule.lastIntervalMillis)
            assertNull(schedule.originalDocumentId)
            assertTrue(schedule.nextAttemptAt in earliest..latest)
            verify(deferredScheduleDao, never()).attachOriginalIfUnknown(any(), any())
        }

    @Test
    fun `Given a deferred document, When it is deleted, Then its schedule is deleted`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.deleteDocumentById(DEFERRED_ID)).thenReturn(Outcome.success(null))

            // When
            val result = controller.deleteDocument(DEFERRED_ID).first()

            // Then
            assertEquals(DeleteDocumentPartialState.Success, result)
            verify(walletDocumentOperationController).recordRemoval(DEFERRED_ID)
            verify(deferredScheduleDao).delete(DEFERRED_ID)
        }

    /** Wallet Core has already deleted the document, so the failure is reported, not undone. */
    @Test
    fun `Given the schedule cannot be deleted, When the document is deleted, Then it fails`() =
        coroutineRule.runTest {
            // Given
            whenever(eudiWallet.deleteDocumentById(DEFERRED_ID)).thenReturn(Outcome.success(null))
            whenever(deferredScheduleDao.delete(DEFERRED_ID))
                .doThrow(IllegalStateException("database closed"))

            // When
            val result = controller.deleteDocument(DEFERRED_ID).first()

            // Then
            assertEquals(DeleteDocumentPartialState.Failure("database closed"), result)
        }

    @Test
    fun `Given an unknown transaction, When a deferred request fails, Then it is Expired`() =
        coroutineRule.runTest {
            // Given
            givenDeferredFailure(IllegalStateException("invalid_transaction_id"))

            // When
            val result = controller.issueDeferredDocument(DEFERRED_ID).first()

            // Then
            assertEquals(IssueDeferredDocumentPartialState.Expired(DEFERRED_ID), result)
        }

    /** A trust check that produced no result is asked again. */
    @Test
    fun `Given a refusal without a cause, When a deferred request fails, Then it is Failed`() =
        coroutineRule.runTest {
            // Given
            givenDeferredFailure(IssuerNotTrustedException(message = "no trust result"))

            // When
            val result = controller.issueDeferredDocument(DEFERRED_ID).first()

            // Then
            assertEquals(IssueDeferredDocumentPartialState.Failed, result)
        }

    @Test
    fun `Given an untrusted chain, When a deferred request fails, Then it is IssuerNotTrusted`() =
        coroutineRule.runTest {
            // Given
            givenDeferredFailure(
                IssuerNotTrustedException(
                    message = "untrusted",
                    cause = RuntimeException("untrusted chain"),
                )
            )

            // When
            val result = controller.issueDeferredDocument(DEFERRED_ID).first()

            // Then
            assertEquals(IssueDeferredDocumentPartialState.IssuerNotTrusted(DEFERRED_ID), result)
        }

    /** A chain refused for its validity period may be a wrong device clock; it is asked again. */
    @Test
    fun `Given an expired certificate, When a deferred request fails, Then it is Failed`() =
        coroutineRule.runTest {
            // Given
            givenDeferredFailure(
                IssuerNotTrustedException(
                    message = "untrusted",
                    cause = RuntimeException("chain", CertificateExpiredException("expired")),
                )
            )

            // When
            val result = controller.issueDeferredDocument(DEFERRED_ID).first()

            // Then
            assertEquals(IssueDeferredDocumentPartialState.Failed, result)
        }

    @Test
    fun `Given an invalid token, When a deferred request fails, Then it is Failed`() =
        coroutineRule.runTest {
            // Given
            givenDeferredFailure(IllegalStateException("invalid_token"))

            // When
            val result = controller.issueDeferredDocument(DEFERRED_ID).first()

            // Then
            assertEquals(IssueDeferredDocumentPartialState.Failed, result)
        }

    @Test
    fun `Given no issued PID, When all documents are deleted, Then it fails and deletes nothing`() =
        coroutineRule.runTest {
            // Given
            val mdl: IssuedDocument = mock {
                on { id } doReturn "mdl"
                on { format } doReturn MsoMdocFormat(MDL_TYPE)
            }
            val deferred = mockedDeferredDocument()
            whenever(eudiWallet.getDocuments(anyOrNull())).thenReturn(listOf(mdl, deferred))
            whenever(resourceProvider.genericErrorMessage()).thenReturn("error")
            whenever(
                walletDocumentOperationController
                    .tryStartRemovingAllDocuments<DeleteAllDocumentsPartialState>(any())
            ).thenAnswer { invocation ->
                coroutineRule.testScope.async {
                    invocation.getArgument<suspend () -> DeleteAllDocumentsPartialState>(0)
                        .invoke()
                }
            }

            // When
            val result = controller.deleteAllDocuments().first()

            // Then
            assertEquals(DeleteAllDocumentsPartialState.Failure(errorMessage = "error"), result)
            verify(eudiWallet, never()).deleteDocumentById(any())
            verify(walletDocumentOperationController, never()).recordRemoval(any())
            verifyNoInteractions(
                deferredScheduleDao,
                revokedDocumentDao,
                issuerTrustDao,
                statusRefreshFailureDao,
                bookmarkDao,
                failedReIssuedDocumentDao,
            )
        }

    /**
     * Two configured issuers, the first of which the old fallback would have used for any unknown
     * issuer, each with its own mocked manager.
     */
    private fun givenTwoIssuers() {
        val firstConfig: OpenId4VciManager.Config = mock()
        val configuredConfig: OpenId4VciManager.Config = mock()
        whenever(walletCoreConfig.issuersConfig).thenReturn(
            listOf(
                VciConfig(issuerUrl = "https://first.example", config = firstConfig, order = 0),
                VciConfig(issuerUrl = CONFIGURED_ISSUER, config = configuredConfig, order = 1),
            )
        )
        whenever(eudiWallet.createOpenId4VciManager(eq(firstConfig), anyOrNull()))
            .thenReturn(firstIssuerManager)
        whenever(eudiWallet.createOpenId4VciManager(eq(configuredConfig), anyOrNull()))
            .thenReturn(configuredIssuerManager)
        whenever(resourceProvider.getString(R.string.issuance_document_offer_error_issuer_not_configured))
            .thenReturn(NOT_CONFIGURED)
        whenever(resourceProvider.getString(R.string.issuance_document_offer_error_no_document))
            .thenReturn(UNREADABLE)
        whenever(resourceProvider.getString(R.string.issuance_generic_error)).thenReturn(
            DOCUMENT_ERROR
        )
        whenever(resourceProvider.genericErrorMessage()).thenReturn("error")
    }

    private fun mockedDeferredDocument(): DeferredDocument {
        val metadata: IssuerMetadata =
            mock { on { credentialIssuerIdentifier } doReturn CONFIGURED_ISSUER }
        return mock {
            on { id } doReturn DEFERRED_ID
            on { name } doReturn "mDL"
            on { format } doReturn MsoMdocFormat(MDL_TYPE)
            on { issuerMetadata } doReturn metadata
        }
    }

    /** Core answers the deferred request for [DEFERRED_ID] with a failure caused by [cause]. */
    private fun givenDeferredFailure(cause: Throwable) {
        givenTwoIssuers()
        val deferred = mockedDeferredDocument()
        whenever(eudiWallet.getDocumentById(DEFERRED_ID)).thenReturn(deferred)
        whenever(walletDocumentOperationController.startCorePolling(eq(DEFERRED_ID), any()))
            .thenAnswer { invocation ->
                invocation.getArgument<() -> Unit>(1).invoke()
                true
            }
        doAnswer { invocation ->
            invocation.getArgument<OpenId4VciManager.OnDeferredIssueResult>(2)
                .onResult(DeferredIssueResult.DocumentFailed(deferred, cause))
        }.whenever(configuredIssuerManager).issueDeferredDocument(
            eq(deferred),
            anyOrNull(),
            any(),
        )
    }

    private fun offerByValue(issuer: String): String {
        val offer = """{"credential_issuer":"$issuer","credential_configuration_ids":["pid"]}"""
        return "openid-credential-offer://?credential_offer=${URLEncoder.encode(offer, "UTF-8")}"
    }

    private companion object {
        const val CONFIGURED_ISSUER = "https://issuer.example"
        const val NOT_CONFIGURED = "not configured"
        const val UNREADABLE = "unreadable"
        const val DOCUMENT_ERROR = "document error"
        const val MDL_TYPE = "org.iso.18013.5.1.mDL"
        const val DEFERRED_ID = "deferred"
    }
}