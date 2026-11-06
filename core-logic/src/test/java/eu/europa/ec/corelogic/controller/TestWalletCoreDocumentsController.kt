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
import eu.europa.ec.corelogic.worker.WalletWorkScheduler
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OfferResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.IssuerTrustDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import eu.europa.ec.storagelogic.model.IssuerTrust
import eu.europa.ec.storagelogic.model.IssuerTrustVerdict
import eu.europa.ec.storagelogic.model.RevocationStatus
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
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

            assertTrue(controller.isDocumentSuspended("suspended"))
            assertFalse(controller.isDocumentSuspended("revoked"))
            assertFalse(controller.isDocumentSuspended("valid"))
        }

    @Test
    fun `Given a stored document, When it is deleted, Then its own records go`() =
        coroutineRule.runTest {
            whenever(eudiWallet.deleteDocumentById("pid")).thenReturn(Outcome.success(null))

            val result = controller.deleteDocument("pid").first()

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
            whenever(eudiWallet.deleteDocumentById("pid")).thenReturn(Outcome.success(null))
            whenever(bookmarkDao.delete("pid")).doThrow(IllegalStateException("database closed"))

            val result = controller.deleteDocument("pid").first()

            assertEquals(DeleteDocumentPartialState.Success, result)
        }

    @Test
    fun `Given a re-issuance replaced the document, When it is cleaned up, Then the bookmark moves and the original records go`() =
        coroutineRule.runTest {
            whenever(eudiWallet.getDocumentById("pid")).thenReturn(null)
            whenever(bookmarkDao.retrieve("pid")).thenReturn(Bookmark("pid"))

            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.Success(documentIds = listOf("newPid")),
            )

            verify(bookmarkDao).store(Bookmark("newPid"))
            verify(bookmarkDao).delete("pid")
            verify(revokedDocumentDao).delete("pid")
            verify(issuerTrustDao).delete("pid")
            verify(walletWorkScheduler, never()).scheduleRevocationCheck()
        }

    @Test
    fun `Given the original was revoked during its re-issuance, When it is cleaned up, Then a revocation check is scheduled`() =
        coroutineRule.runTest {
            whenever(eudiWallet.getDocumentById("pid")).thenReturn(null)
            whenever(revokedDocumentDao.retrieve("pid"))
                .thenReturn(RevokedDocument("pid", RevocationStatus.INVALID))

            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.Success(documentIds = listOf("newPid")),
            )

            verify(walletWorkScheduler).scheduleRevocationCheck()
        }

    @Test
    fun `Given a deferred re-issuance, When it is cleaned up, Then the bookmark is copied and the original is kept until replaced`() =
        coroutineRule.runTest {
            whenever(bookmarkDao.retrieve("pid")).thenReturn(Bookmark("pid"))

            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.DeferredSuccess(deferredDocuments = mapOf("newPid" to pidType)),
            )

            verify(bookmarkDao).store(Bookmark("newPid"))
            verify(bookmarkDao, never()).delete(any())
            verify(revokedDocumentDao, never()).delete(any())
        }

    @Test
    fun `Given a failed re-issuance, When it is cleaned up, Then nothing changes`() =
        coroutineRule.runTest {
            implementation().cleanUpAfterReIssuance(
                originalId = "pid",
                state = IssueDocumentsPartialState.Failure(errorMessage = "error"),
            )

            verifyNoInteractions(bookmarkDao, revokedDocumentDao, issuerTrustDao)
        }

    @Test
    fun `Given records of a document Wallet Core no longer holds, When missing documents are cleaned up, Then only those records go`() =
        coroutineRule.runTest {
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

            implementation().deleteRecordsOfMissingDocuments()

            verify(issuerTrustDao).delete("oldPid")
            verify(bookmarkDao).delete("oldPid")
            verify(issuerTrustDao, never()).delete("otherPid")
            verify(failedReIssuedDocumentDao, never()).delete("otherPid")
            verify(walletWorkScheduler, never()).scheduleRevocationCheck()
        }

    @Test
    fun `Given a revoked document Wallet Core no longer holds, When missing documents are cleaned up, Then a revocation check is scheduled`() =
        coroutineRule.runTest {
            whenever(revokedDocumentDao.retrieveAll())
                .thenReturn(listOf(RevokedDocument("oldPid", RevocationStatus.INVALID)))
            whenever(issuerTrustDao.retrieveAll()).thenReturn(emptyList())
            whenever(statusRefreshFailureDao.retrieveAll()).thenReturn(emptyList())
            whenever(bookmarkDao.retrieveAll()).thenReturn(emptyList())
            whenever(failedReIssuedDocumentDao.retrieveAll()).thenReturn(emptyList())
            whenever(eudiWallet.getDocuments(anyOrNull())).thenReturn(listOf(otherPid))

            implementation().deleteRecordsOfMissingDocuments()

            verify(revokedDocumentDao).delete("oldPid")
            verify(walletWorkScheduler).scheduleRevocationCheck()
        }

    @Test
    fun `Given Wallet Core fails to delete the document, When it is deleted, Then nothing else is deleted`() =
        coroutineRule.runTest {
            whenever(eudiWallet.deleteDocumentById("pid"))
                .thenReturn(Outcome.failure(IllegalStateException("storage error")))
            whenever(resourceProvider.genericErrorMessage()).thenReturn("error")

            val result = controller.deleteDocument("pid").first()

            assertTrue(result is DeleteDocumentPartialState.Failure)
            verifyNoInteractions(
                revokedDocumentDao,
                issuerTrustDao,
                statusRefreshFailureDao,
                bookmarkDao,
                failedReIssuedDocumentDao,
            )
        }

    @Test
    fun `Given an offer from an issuer that is not configured, When it is resolved, Then it fails before any issuer is contacted`() =
        coroutineRule.runTest {
            givenTwoIssuers()

            val result =
                controller.resolveDocumentOffer(offerByValue("https://attacker.example")).first()

            assertEquals(ResolveDocumentOfferPartialState.Failure(NOT_CONFIGURED), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given a link that is not a usable offer, When it is resolved, Then it fails before any issuer is contacted`() =
        coroutineRule.runTest {
            givenTwoIssuers()

            val result =
                controller.resolveDocumentOffer("openid-credential-offer://?credential_offer=%7Bnot-json")
                    .first()

            assertEquals(ResolveDocumentOfferPartialState.Failure(UNREADABLE), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given an offer from a configured issuer, When it is resolved, Then that issuer's manager reads it`() =
        coroutineRule.runTest {
            givenTwoIssuers()
            val link = offerByValue(CONFIGURED_ISSUER)

            val job =
                coroutineRule.testScope.launch { controller.resolveDocumentOffer(link).collect {} }
            coroutineRule.testScope.runCurrent()

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

            val result = controller.resolveDocumentOffer(link).first()

            assertEquals(ResolveDocumentOfferPartialState.Failure(NOT_CONFIGURED), result)
        }

    @Test
    fun `Given a deferred document, When it is issued, Then the manager of its own issuer is used`() =
        coroutineRule.runTest {
            givenTwoIssuers()
            val metadata: IssuerMetadata =
                mock { on { credentialIssuerIdentifier } doReturn CONFIGURED_ISSUER }
            val deferred: DeferredDocument = mock {
                on { id } doReturn "deferred"
                on { issuerMetadata } doReturn metadata
            }
            whenever(eudiWallet.getDocumentById("deferred")).thenReturn(deferred)

            val job = coroutineRule.testScope.launch {
                controller.issueDeferredDocument("deferred").collect {}
            }
            coroutineRule.testScope.runCurrent()

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
            givenTwoIssuers()

            val result = controller.reIssueDocument(
                documentId = "pid",
                issuerId = "https://attacker.example",
                allowAuthorizationFallback = false,
            ).first()

            assertEquals(IssueDocumentsPartialState.Failure(errorMessage = DOCUMENT_ERROR), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given a revoked document, When it is re-issued, Then it is refused before the issuer is used`() =
        coroutineRule.runTest {
            givenTwoIssuers()
            whenever(revokedDocumentDao.retrieve("pid"))
                .thenReturn(RevokedDocument("pid", RevocationStatus.SUSPENDED))
            whenever(resourceProvider.getString(R.string.document_details_reissue_blocked_text))
                .thenReturn("blocked")

            val result = controller.reIssueDocument(
                documentId = "pid",
                issuerId = CONFIGURED_ISSUER,
                allowAuthorizationFallback = false,
            ).first()

            assertEquals(IssueDocumentsPartialState.Failure(errorMessage = "blocked"), result)
            verifyNoInteractions(firstIssuerManager, configuredIssuerManager)
        }

    @Test
    fun `Given the issuer issues only part of a request, When issueDocuments is called, Then the documents not issued are passed on`() =
        coroutineRule.runTest {
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

            val result = controller.issueDocuments(
                issuanceMethod = IssuanceMethod.OPENID4VCI,
                configIds = listOf("pid", "mdl"),
                issuerId = CONFIGURED_ISSUER,
            ).first()

            assertEquals(
                IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf("pid"),
                    nonIssuedDocuments = mapOf(MDL_TYPE to "mDL"),
                ),
                result
            )
        }

    @Test
    fun `Given a failed re-issuance record, When hasReIssuanceFailed is called, Then it is reported for that document only`() =
        coroutineRule.runTest {
            whenever(failedReIssuedDocumentDao.retrieve("pid")).thenReturn(FailedReIssuedDocument("pid"))

            assertTrue(controller.hasReIssuanceFailed("pid"))
            assertFalse(controller.hasReIssuanceFailed("otherPid"))
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
    }
}