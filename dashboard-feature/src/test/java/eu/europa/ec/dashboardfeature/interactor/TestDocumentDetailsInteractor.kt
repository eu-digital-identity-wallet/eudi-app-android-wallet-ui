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

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.DeleteAllDocumentsPartialState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.IssuerTrustVerdictDomain
import eu.europa.ec.corelogic.model.UntrustedIssuerReasonDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.model.DocumentCredentialsInfoUi
import eu.europa.ec.dashboardfeature.util.mockedBasicMdlDomain
import eu.europa.ec.dashboardfeature.util.mockedBasicPidDomain
import eu.europa.ec.dashboardfeature.util.mockedBookmarkId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker
import eu.europa.ec.testfeature.util.copy
import eu.europa.ec.testfeature.util.createMockedNamespaceData
import eu.europa.ec.testfeature.util.getMockedMdlWithBasicFields
import eu.europa.ec.testfeature.util.getMockedOldestPidWithBasicFields
import eu.europa.ec.testfeature.util.getMockedPidWithBasicFields
import eu.europa.ec.testfeature.util.getMockedSdJwtPidWithBasicFields
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedDocumentAvailableCredentials
import eu.europa.ec.testfeature.util.mockedDocumentTotalCredentials
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedFormattedExpirationDate
import eu.europa.ec.testfeature.util.mockedFormattedIssuanceDate
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdlId
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.IssuerDetailsCardDataUi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI

class TestDocumentDetailsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uuidProvider: UuidProvider

    @Mock
    private lateinit var configLogic: ConfigLogic

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var deviceAuthenticationInteractor: DeviceAuthenticationInteractor

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var sessionController: SessionController

    private lateinit var interactor: DocumentDetailsInteractor

    private val mockedReIssueIssuerId = "mockedReIssueIssuerId"
    private val mockedReIssueUri = "mockedReIssueUri"
    private val mockedReIssueBlockedMessage = "mockedReIssueBlockedMessage"

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DocumentDetailsInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            deviceAuthenticationInteractor = deviceAuthenticationInteractor,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            configLogic = configLogic,
            prefKeys = prefKeys,
            sessionController = sessionController,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
        whenever(configLogic.forcePidActivation).thenReturn(true)
        runBlocking {
            mockIsDocumentRevoked(isRevoked = false)
            mockGetIssuerTrustVerdictCall(response = IssuerTrustVerdictDomain.NOT_TRUSTED)
            mockHasStatusRefreshFailed(hasFailed = false)
            mockHasReIssuanceFailed(hasFailed = false)
        }
    }

    @After
    fun after() {
        closeable.close()
    }

    /**
     * Case 1:
     * 1. walletCoreDocumentsController.getDocumentById() returns a PID document.
     * 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
     * 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.
     * 4. prefKeys.getShowBatchIssuanceCounter() returns true.
     *
     * Case 1 Expected Result:
     * DocumentDetailsInteractorPartialState.Success state, with a PID document item and
     * documentIsBookmarked is false and document credentials info is returned.
     */
    @Test
    fun `Given Case 1, When getDocumentDetails is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi(
                issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
            )

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given the last status refresh failed, When getDocumentDetails is called, Then the issuer card opens and says so`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)
            mockHasStatusRefreshFailed(hasFailed = true)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                val success = awaitItem() as DocumentDetailsInteractorPartialState.Success

                assertEquals(
                    getMockedIssuerDetailsCardDataUi(
                        issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
                        isExpanded = true,
                        hasStatusRefreshFailed = true
                    ),
                    success.issuerDetails
                )
            }
        }
    }

    @Test
    fun `Given the last automatic re-issuance failed, When getDocumentDetails is called, Then the issuer card opens and says so`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)
            mockHasStatusRefreshFailed(hasFailed = false)
            mockHasReIssuanceFailed(hasFailed = true)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = false,
            ).runFlowTest {
                // Then
                val success = awaitItem() as DocumentDetailsInteractorPartialState.Success

                assertEquals(
                    getMockedIssuerDetailsCardDataUi(
                        issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
                        isExpanded = true,
                        hasReIssuanceFailed = true,
                    ),
                    success.issuerDetails
                )
            }
        }
    }

    @Test
    fun `Given a collapsed card and a failed status refresh, When getDocumentDetails is called again, Then the card opens`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)
            mockHasStatusRefreshFailed(hasFailed = true)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = false,
            ).runFlowTest {
                // Then
                val success = awaitItem() as DocumentDetailsInteractorPartialState.Success

                assertEquals(true, success.issuerDetails.isExpanded)
            }
        }
    }

    @Test
    fun `Given the issuer was trusted at issuance, When getDocumentDetails is called, Then the issuer is shown as verified`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)
            mockGetIssuerTrustVerdictCall(response = IssuerTrustVerdictDomain.TRUSTED)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = getMockedIssuerDetailsCardDataUi(
                            issuerTrustVerdict = IssuerTrustVerdictDomain.TRUSTED,
                        ),
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                            resourceProvider = resourceProvider,
                        ),
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 2:
     * 1. walletCoreDocumentsController.getDocumentById() returns a PID document.
     * 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
     * 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns true.
     * 4. prefKeys.getShowBatchIssuanceCounter() returns true.
     *
     * Case 2 Expected Result:
     * DocumentDetailsInteractorPartialState.Success state, with
     * a PID document item and document credentials info is returned.
     */
    @Test
    fun `Given Case 2, When getDocumentDetails is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = true
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi(
                issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
            )

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 3:
     * 1. walletCoreDocumentsController.getDocumentById() returns a PID document.
     * 2. walletCoreDocumentsController.isDocumentBookmarked() returns true.
     * 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.
     * 4. prefKeys.getShowBatchIssuanceCounter() returns true.
     *
     * Case 3 Expected Result:
     * DocumentDetailsInteractorPartialState.Success state, with a PID document item and
     * documentIsBookmarked is true and document credentials info is returned.
     */
    @Test
    fun `Given Case 3, When getDocumentDetails is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi(
                issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
            )

            mockRetrieveBookmarkCall(response = true)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = true,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 4:
     * 1. walletCoreDocumentsController.getDocumentById() returns an mDL document.
     * 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
     * 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.
     * 4. prefKeys.getShowBatchIssuanceCounter() returns true.
     *
     * Case 4 Expected Result:
     * DocumentDetailsInteractorPartialState.Success state, with an mDL document item and
     * documentIsBookmarked is false and document credentials info is returned.
     */
    @Test
    fun `Given Case 4, When getDocumentDetails is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockGetDocumentByIdCall(response = mockedMdlWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi(
                issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
            )

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedMdlId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicMdlDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 5:
     * 1. walletCoreDocumentsController.getDocumentById() returns null.
     *
     * Case 5 Expected Result:
     * DocumentDetailsInteractorPartialState.Failed state,
     * with the generic error message.
     */
    @Test
    fun `Given Case 5, When getDocumentDetails is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetDocumentByIdCall(response = null)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 6:
     * 1. walletCoreDocumentsController.getDocumentById() returns a PID document, with:
     * no expiration date,
     * no image, and
     * no user name.
     * 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
     * 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.
     * 4. prefKeys.getShowBatchIssuanceCounter() returns true.
     *
     * Case 6 Expected Result:
     * DocumentDetailsInteractorPartialState.Success state, with a PID document item, with:
     * documentIsBookmarked is false and document credentials info is returned.
     */
    @Test
    fun `Given Case 6, When getDocumentDetails is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()

            mockGetDocumentByIdCall(
                response = mockedPidWithBasicFields.copy(
                    data = MsoMdocData(
                        format = MsoMdocFormat(mockedMdocPidNameSpace),
                        issuerMetadata = null,
                        nameSpacedData = createMockedNamespaceData(
                            mockedMdocPidNameSpace, mapOf(
                                "no_data_item" to byteArrayOf(0)
                            )
                        )
                    )
                )
            )

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi(
                issuerTrustVerdict = IssuerTrustVerdictDomain.NOT_TRUSTED,
            )

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = DocumentDetailsDomain(
                            docName = mockedPidDocName,
                            docId = mockedPidId,
                            issuerId = "",
                            documentConfigId = "",
                            documentIdentifier = DocumentIdentifier.MdocPid,
                            documentClaims = listOf(
                                ClaimDomain.Primitive(
                                    key = "no_data_item",
                                    value = "0",
                                    displayTitle = "no_data_item",
                                    path = ClaimPathDomain.ofPlainKeys(
                                        names = listOf("no_data_item"),
                                        type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
                                    ),
                                    isRequired = false,
                                ),
                            ),
                            documentIssuanceDate = mockedFormattedIssuanceDate,
                            documentExpirationDate = mockedFormattedExpirationDate,
                        ),
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 7:
     * 1. walletCoreDocumentsController.getDocumentById() throws an exception with a message.
     *
     * Case 7 Expected Result:
     * DocumentDetailsInteractorPartialState.Failed state,
     * with the exception's localized message.
     */
    @Test
    fun `Given Case 7, When getDocumentDetails is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 8:
     * 1. walletCoreDocumentsController.getDocumentById() throws an exception with no message.
     *
     * Case 8 Expected Result:
     * DocumentDetailsInteractorPartialState.Failed state,
     * with the generic error message.
     */
    @Test
    fun `Given Case 8, When getDocumentDetails is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 9:
     * Same as Case 1 but with wasIssuerDetailsExpanded = true → IssuerDetailsCardDataUi.isExpanded
     * is true (covers the non-null branch of `wasIssuerDetailsExpanded ?: false`).
     * prefKeys.getShowBatchIssuanceCounter() returns true.
     */
    @Test
    fun `Given Case 9, When getDocumentDetails is called with wasIssuerDetailsExpanded=true, Then issuerDetails#isExpanded is true`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(resourceProvider = resourceProvider)
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = true,
            ).runFlowTest {
                // Then
                val result = awaitItem()
                kotlin.test.assertTrue(result is DocumentDetailsInteractorPartialState.Success)
                assertEquals(true, result.issuerDetails.isExpanded)
            }
        }
    }

    /**
     * Case 10:
     * prefKeys.getShowBatchIssuanceCounter() returns true.
     * isDocumentRevoked returns true → IssuerDetailsCardDataUi.documentState is Revoked.
     */
    @Test
    fun `Given Case 10, When getDocumentDetails is called for a revoked document, Then DocumentState is Revoked`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(resourceProvider = resourceProvider)
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = true)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                val result = awaitItem()
                kotlin.test.assertTrue(result is DocumentDetailsInteractorPartialState.Success)
                assertEquals(
                    IssuerDetailsCardDataUi.DocumentState.Revoked,
                    result.issuerDetails.documentState
                )
            }
        }
    }

    /**
     * Case 11:
     * prefKeys.getShowBatchIssuanceCounter() returns false.
     *
     * Case 11 Expected Result:
     * DocumentDetailsInteractorPartialState.Success state, with document credentials info hidden.
     */
    @Test
    fun `Given a suspended document, When getDocumentDetails is called, Then the issuer card says it is suspended`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentDetailsStrings(resourceProvider = resourceProvider)
            mockGetDocumentByIdCall(response = getMockedPidWithBasicFields())
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = true, isSuspended = true)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                val result = awaitItem()
                kotlin.test.assertTrue(result is DocumentDetailsInteractorPartialState.Success)
                assertEquals(
                    IssuerDetailsCardDataUi.DocumentState.Suspended,
                    result.issuerDetails.documentState
                )
            }
        }
    }

    @Test
    fun `Given show batch issuance counter preference is false, When getDocumentDetails is called, Then credentials info is hidden`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = false)
            mockGetDocumentDetailsStrings(resourceProvider = resourceProvider)
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is DocumentDetailsInteractorPartialState.Success)
                result as DocumentDetailsInteractorPartialState.Success
                assertEquals(null, result.documentCredentialsInfoUi)
            }
        }
    }

    // Case 1:
    // 1. PID activation is required and the selected document is the only Issued PID.
    //
    // Case 1 Expected Result:
    // Request full-wallet deletion, which includes any Pending replacements.
    @Test
    fun `Given Case 1, When deleteDocument is called for the single PID, Then AllDocumentsDeleted is returned`() =
        coroutineRule.runTest {
            // Given
            val original = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = original)
            mockGetAllDocumentsWithTypeCall(response = listOf(original))
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)

            // When
            interactor.deleteDocument(documentId = original.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteAllDocuments()
            verify(walletCoreDocumentsController, never()).deleteDocument(any())
        }

    // Case 2:
    // 1. PID activation is required and the selected PID is the main one among several PIDs.
    //
    // Case 2 Expected Result:
    // Request full-wallet deletion and return AllDocumentsDeleted.
    @Test
    fun `Given Case 2, When deleteDocument is called for the main PID with multiple PIDs, Then AllDocumentsDeleted is returned`() =
        coroutineRule.runTest {
            // Given
            val mainPid = getMockedOldestPidWithBasicFields()
            val otherPid = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mainPid)
            mockGetAllDocumentsWithTypeCall(response = listOf(mainPid, otherPid))
            mockGetMainPidDocument(response = mainPid)
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mainPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteAllDocuments()
            verify(walletCoreDocumentsController, never()).deleteDocument(any())
        }

    // Case 3:
    // 1. PID activation is optional and the selected document is a PID.
    //
    // Case 3 Expected Result:
    // Delete only the selected ID, leaving any replacement intact.
    @Test
    fun `Given Case 3, When deleteDocument is called with forcePidActivation false, Then SingleDocumentDeleted is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(configLogic.forcePidActivation).thenReturn(false)
            mockGetDocumentByIdCall(response = getMockedPidWithBasicFields())
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mockedPidId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteDocument(mockedPidId)
            verify(walletCoreDocumentsController, never()).deleteAllDocuments()
        }

    // Case 4:
    // 1. PID activation is required and the selected PID is not the main PID.
    //
    // Case 4 Expected Result:
    // Delete only the selected ID.
    @Test
    fun `Given Case 4, When deleteDocument is called, Then it returns SingleDocumentDeleted`() =
        coroutineRule.runTest {
            // Given
            val mainPid = getMockedOldestPidWithBasicFields()
            val otherPid = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = otherPid)
            mockGetAllDocumentsWithTypeCall(response = listOf(mainPid, otherPid))
            mockGetMainPidDocument(response = mainPid)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = otherPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteDocument(otherPid.id)
            verify(walletCoreDocumentsController, never()).deleteAllDocuments()
        }

    // Case 5:
    // 1. PID activation is required and the selected document is an mDL.
    //
    // Case 5 Expected Result:
    // Delete only the selected ID, leaving any replacement intact.
    @Test
    fun `Given Case 5, When deleteDocument is called for a non-PID document, Then SingleDocumentDeleted is returned`() =
        coroutineRule.runTest {
            // Given
            mockGetDocumentByIdCall(response = getMockedMdlWithBasicFields())
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mockedMdlId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteDocument(mockedMdlId)
            verify(walletCoreDocumentsController, never()).deleteAllDocuments()
        }

    // Case 6:
    // 1. Removing the only Issued PID requests full-wallet deletion, which fails.
    //
    // Case 6 Expected Result:
    // Forward the failure without reporting AllDocumentsDeleted.
    @Test
    fun `Given Case 6, When deleteDocument is called, Then it returns Failure with failure's error message`() =
        coroutineRule.runTest {
            // Given
            val original = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = original)
            mockGetAllDocumentsWithTypeCall(response = listOf(original))
            mockDeleteAllDocumentsCall(
                response = DeleteAllDocumentsPartialState.Failure(mockedPlainFailureMessage)
            )

            // When
            interactor.deleteDocument(documentId = original.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        mockedPlainFailureMessage
                    ),
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController, never()).deleteDocument(any())
        }

    /**
     * Case 7:
     *
     * 1. A documentId and document is mDL.
     * 2. walletCoreDocumentsController.deleteDocument() throws an exception with a message.
     */
    @Test
    fun `Given Case 7, When deleteDocument is called, Then it returns Failure with the exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockedMdlId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.deleteDocument(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 8:
     *
     * 1. A documentId and document is mDL.
     * 2. walletCoreDocumentsController.deleteDocument() throws an exception with no message.
     */
    @Test
    fun `Given Case 8, When deleteDocument is called, Then it returns Failure with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockedMdlId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.deleteDocument(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 9:
    // 1. Deleting an mDL fails.
    //
    // Case 9 Expected Result:
    // Forward the single-document deletion failure.
    @Test
    fun `Given Case 9, When deleteDocument is called, Then it returns Failure with failure's error message`() =
        coroutineRule.runTest {
            // Given
            mockGetDocumentByIdCall(response = getMockedMdlWithBasicFields())
            mockDeleteDocumentCall(
                response = DeleteDocumentPartialState.Failure(mockedPlainFailureMessage)
            )

            // When
            interactor.deleteDocument(documentId = mockedMdlId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        mockedPlainFailureMessage
                    ),
                    awaitItem(),
                )
            }
        }

    // Case 10:
    // 1. PID activation is required and the only Issued PID uses the SD-JWT format.
    //
    // Case 10 Expected Result:
    // Apply the same full-wallet deletion rule as for an mdoc PID.
    @Test
    fun `Given Case 10, When deleteDocument is called for an SdJwt PID, Then AllDocumentsDeleted is returned`() =
        coroutineRule.runTest {
            // Given
            val original = getMockedSdJwtPidWithBasicFields()
            mockGetDocumentByIdCall(response = original)
            mockGetAllDocumentsWithTypeCall(response = listOf(original))
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)

            // When
            interactor.deleteDocument(documentId = original.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteAllDocuments()
            verify(walletCoreDocumentsController, never()).deleteDocument(any())
        }

    /**
     * Case 11:
     * 1. PID activation is required and the wallet holds two issued PIDs.
     * 2. The wallet cannot identify the main PID.
     *
     * Case 11 Expected Result:
     * Remove deletes only the selected PID.
     */
    @Test
    fun `Given two PIDs and no main PID, When deleteDocument is called, Then only the selected PID is deleted`() =
        coroutineRule.runTest {
            // Given
            val selectedPid = getMockedPidWithBasicFields()
            val otherPid = getMockedOldestPidWithBasicFields()
            mockGetDocumentByIdCall(response = selectedPid)
            mockGetAllDocumentsWithTypeCall(response = listOf(selectedPid, otherPid))
            mockGetMainPidDocument(response = null)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = selectedPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem(),
                )
            }
            verify(walletCoreDocumentsController).deleteDocument(selectedPid.id)
            verify(walletCoreDocumentsController, never()).deleteDocument(otherPid.id)
            verify(walletCoreDocumentsController, never()).deleteAllDocuments()
        }

    /**
     * Case 1:
     * 1. A valid bookmarkId is provided.
     * 2. walletCoreDocumentsController.storeBookmark() succeeds.
     * Expected result:
     * DocumentDetailsInteractorStoreBookmarkPartialState.Success state is returned with the
     * bookmarkId.
     */
    @Test
    fun `Given Case 1, When storeBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Given
            mockStoreBookmarkCall(bookmarkId = mockedBookmarkId)

            // When
            interactor.storeBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorStoreBookmarkPartialState.Success(
                        bookmarkId = mockedBookmarkId
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 2:
     * 1. A valid bookmarkId is provided.
     * 2. When walletCoreDocumentsController.storeBookmark() is called, an exception is thrown.
     * Expected result:
     * DocumentDetailsInteractorStoreBookmarkPartialState.Failure state is returned.
     */
    @Test
    fun `Given Case 2, When storeBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Given
            mockStoreBookmarkCall(
                bookmarkId = mockedBookmarkId,
                throwable = mockedExceptionWithMessage
            )

            // When
            interactor.storeBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorStoreBookmarkPartialState.Failure,
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 1:
     * 1. A valid bookmarkId is provided.
     * 2. walletCoreDocumentsController.deleteBookmark() succeeds.
     * Expected result:
     * DocumentDetailsInteractorDeleteBookmarkPartialState.Success state is returned.
     */
    @Test
    fun `Given Case 1, When deleteBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDeleteBookmarkCall(bookmarkId = mockedBookmarkId)

            // When
            interactor.deleteBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteBookmarkPartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    /**
     * Case 2:
     * 1. A valid bookmarkId is provided for the bookmark to be deleted.
     * 2. When walletCoreDocumentsController.deleteBookmark() is called, an exception is thrown.
     * Expected result:
     * DocumentDetailsInteractorDeleteBookmarkPartialState.Failure state is returned.
     */
    @Test
    fun `Given Case 2, When deleteBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDeleteBookmarkCall(
                bookmarkId = mockedBookmarkId,
                throwable = mockedExceptionWithMessage
            )

            // When
            interactor.deleteBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteBookmarkPartialState.Failure,
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller emits DeferredSuccess, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.DeferredSuccess(
                    deferredDocuments = emptyMap(),
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller emits Failure, When reIssueDocument is called, Then Failure with the same message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.Failure(errorMessage = mockedPlainFailureMessage)
                    .toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Failure(
                        errorMessage = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller emits IssuerNotTrusted for the access certificate, When reIssueDocument is called, Then IssuerNotTrusted with that reason is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.IssuerNotTrusted(
                    reason = UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.IssuerNotTrusted(
                        reason = UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller emits Success, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.Success(
                    documentIds = listOf(mockedPidId),
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    /**
     * A Deferred result completes the manual submission while the replacement keeps polling.
     */
    @Test
    fun `Given a Deferred replacement, When the user reissues the original again, Then each submission succeeds independently`() =
        coroutineRule.runTest {
            // Given
            val replacements = listOf("R1", "R2", "R3").iterator()
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenAnswer {
                IssueDocumentsPartialState.DeferredSuccess(
                    deferredDocuments = mapOf(replacements.next() to mockedMdocPidNameSpace)
                ).toFlow()
            }

            // When
            repeat(3) {
                interactor.reIssueDocument(mockedPidId, mockedReIssueIssuerId).runFlowTest {
                    // Then
                    assertEquals(DocumentDetailsInteractorIssuancePartialState.Success, awaitItem())
                }
            }
            verify(walletCoreDocumentsController, times(3))
                .reIssueDocument(mockedPidId, mockedReIssueIssuerId, true)
        }

    @Test
    fun `Given a re-issuance that succeeds, When reIssueDocument is called, Then it runs as an external flow that ends with the outcome`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.Success(
                    documentIds = listOf(mockedPidId),
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                awaitItem()
            }

            val order = inOrder(sessionController)
            order.verify(sessionController).beginExternalFlow()
            order.verify(sessionController, atLeastOnce()).endExternalFlow()
        }
    }

    @Test
    fun `Given a re-issuance that fails, When reIssueDocument is called, Then the external flow ends with the failure`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.Failure(
                    errorMessage = mockedGenericErrorMessage,
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                awaitItem()
            }

            val order = inOrder(sessionController)
            order.verify(sessionController).beginExternalFlow()
            order.verify(sessionController, atLeastOnce()).endExternalFlow()
        }
    }

    @Test
    fun `Given a revoked document, When reIssueDocument is called, Then it is refused and nothing is re-issued`() {
        coroutineRule.runTest {
            // Given
            mockIsDocumentRevoked(isRevoked = true)
            whenever(resourceProvider.getString(R.string.document_details_reissue_blocked_text))
                .thenReturn(mockedReIssueBlockedMessage)

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Blocked(
                        errorMessage = mockedReIssueBlockedMessage
                    ),
                    awaitItem()
                )
            }

            verify(walletCoreDocumentsController, never()).reIssueDocument(
                any(),
                any(),
                any(),
                any()
            )
            verify(sessionController, never()).beginExternalFlow()
        }
    }

    @Test
    fun `Given a suspended document, When reIssueDocument is called, Then it is refused and nothing is re-issued`() {
        coroutineRule.runTest {
            // Given
            mockIsDocumentRevoked(isRevoked = true, isSuspended = true)
            whenever(resourceProvider.getString(R.string.document_details_reissue_blocked_text))
                .thenReturn(mockedReIssueBlockedMessage)

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Blocked(
                        errorMessage = mockedReIssueBlockedMessage
                    ),
                    awaitItem()
                )
            }

            verify(walletCoreDocumentsController, never()).reIssueDocument(
                any(),
                any(),
                any(),
                any()
            )
            verify(sessionController, never()).beginExternalFlow()
        }
    }

    @Test
    fun `Given the revocation records cannot be read, When reIssueDocument is called, Then it is refused and nothing is re-issued`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.isDocumentRevoked(mockedPidId))
                .thenThrow(RuntimeException(mockedPlainFailureMessage))

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Failure(
                        errorMessage = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }

            verify(walletCoreDocumentsController, never()).reIssueDocument(
                any(),
                any(),
                any(),
                any()
            )
            verify(sessionController, never()).beginExternalFlow()
        }
    }

    @Test
    fun `Given controller emits PartialSuccess, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf(mockedPidId),
                    nonIssuedDocuments = emptyMap(),
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    /**
     * Single-document re-issue cannot mix an issued and a trust-blocked document; this locks in the
     * defensive mapping to Success.
     */
    @Test
    fun `Given controller emits PartialSuccessWithUntrustedIssuer, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer(
                    issuedDocumentIds = listOf(mockedPidId),
                    untrustedDocuments = emptyMap(),
                    nonIssuedDocuments = emptyMap(),
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller emits UserAuthRequired, When reIssueDocument is called, Then UserAuthRequired is emitted`() {
        coroutineRule.runTest {
            // Given
            val crypto = BiometricCrypto(cryptoObject = null)
            val resultHandler = DeviceAuthenticationResult()
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.UserAuthRequired(
                    crypto = crypto,
                    resultHandler = resultHandler,
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.UserAuthRequired(
                        crypto = crypto,
                        resultHandler = resultHandler,
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller throws with message, When reIssueDocument is called, Then Failure with localized message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenThrow(mockedExceptionWithMessage)

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given controller throws without message, When reIssueDocument is called, Then Failure with generic message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    /**
     * The pre-flight refuses before anything starts, so the original document is intact
     * when this arrives; the reason travels through unchanged.
     */
    @Test
    fun `Given controller emits IssuerNotTrusted for the registration certificate, When reIssueDocument is called, Then IssuerNotTrusted with that reason is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.IssuerNotTrusted(
                    reason = UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(
                documentId = mockedPidId,
                issuerId = mockedReIssueIssuerId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorIssuancePartialState.IssuerNotTrusted(
                        reason = UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given biometrics availability is CanAuthenticate, When handleUserAuth is called, Then authenticateWithBiometrics is invoked`() {
        // Given
        val context = mockContext
        val crypto = BiometricCrypto(cryptoObject = null)
        val resultHandler = DeviceAuthenticationResult()
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(crypto))
            .thenReturn(BiometricsAvailability.CanAuthenticate)

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler,
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context,
                crypto,
                mockedNotifyOnAuthenticationFailure,
                resultHandler,
            )
    }

    @Test
    fun `Given biometrics are not enrolled, When handleUserAuth is called, Then the request ends before enrollment opens`() {
        // Given
        val crypto = BiometricCrypto(cryptoObject = null)
        val onError = mock<() -> Unit>()
        val resultHandler = DeviceAuthenticationResult(onAuthenticationError = onError)
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(crypto))
            .thenReturn(BiometricsAvailability.NonEnrolled)

        // When
        interactor.handleUserAuth(
            context = mockContext,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler,
        )

        // Then
        inOrder(onError, deviceAuthenticationInteractor) {
            verify(onError).invoke()
            verify(deviceAuthenticationInteractor).launchBiometricSystemScreen(crypto)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `Given biometrics availability is Failure, When handleUserAuth is called, Then resultHandler#onAuthenticationError is invoked`() {
        // Given
        val onError = mock<() -> Unit>()
        val resultHandler = DeviceAuthenticationResult(onAuthenticationError = onError)
        val crypto = BiometricCrypto(cryptoObject = null)
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(crypto))
            .thenReturn(BiometricsAvailability.Failure(errorMessage = mockedPlainFailureMessage))

        // When
        interactor.handleUserAuth(
            context = mockContext,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler,
        )

        // Then
        verify(onError).invoke()
    }

    @Test
    fun `Given an authorization uri, When resumeOpenId4VciWithAuthorization is called, Then it delegates to walletCoreDocumentsController`() {
        // When
        interactor.resumeOpenId4VciWithAuthorization(mockedReIssueUri)

        // Then
        verify(walletCoreDocumentsController, times(1))
            .resumeOpenId4VciWithAuthorization(mockedReIssueUri)
    }

    private fun mockGetAllDocumentsWithTypeCall(response: List<IssuedDocument>) {
        whenever(walletCoreDocumentsController.getAllDocumentsByType(documentIdentifiers = any()))
            .thenReturn(response)
    }

    private fun mockGetDocumentByIdCall(response: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getDocumentById(ArgumentMatchers.anyString()))
            .thenReturn(response)
    }

    private fun mockGetMainPidDocument(response: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(response)
    }

    private fun mockDeleteAllDocumentsCall(response: DeleteAllDocumentsPartialState) {
        whenever(walletCoreDocumentsController.deleteAllDocuments())
            .thenReturn(response.toFlow())
    }

    private fun mockDeleteDocumentCall(response: DeleteDocumentPartialState) {
        whenever(walletCoreDocumentsController.deleteDocument(ArgumentMatchers.anyString()))
            .thenReturn(response.toFlow())
    }

    private suspend fun mockStoreBookmarkCall(bookmarkId: String, throwable: Throwable? = null) {
        whenever(walletCoreDocumentsController.storeBookmark(bookmarkId))
            .thenAnswer {
                throwable?.let { throw throwable }
                Unit
            }
    }

    private suspend fun mockDeleteBookmarkCall(bookmarkId: String, throwable: Throwable? = null) {
        whenever(walletCoreDocumentsController.deleteBookmark(bookmarkId))
            .thenAnswer {
                throwable?.let { throw throwable }
                Unit
            }
    }

    private suspend fun mockRetrieveBookmarkCall(response: Boolean) {
        whenever(walletCoreDocumentsController.isDocumentBookmarked(ArgumentMatchers.anyString()))
            .thenReturn(response)
    }

    private suspend fun mockIsDocumentRevoked(isRevoked: Boolean, isSuspended: Boolean = false) {
        whenever(walletCoreDocumentsController.isDocumentRevoked(any())).thenReturn(isRevoked)
        whenever(walletCoreDocumentsController.isDocumentSuspended(any())).thenReturn(isSuspended)
    }

    private suspend fun mockGetIssuerTrustVerdictCall(response: IssuerTrustVerdictDomain?) {
        whenever(walletCoreDocumentsController.getIssuerTrustVerdict(any())).thenReturn(response)
    }

    private suspend fun mockHasStatusRefreshFailed(hasFailed: Boolean) {
        whenever(walletCoreDocumentsController.hasStatusRefreshFailed(any())).thenReturn(hasFailed)
    }

    private suspend fun mockHasReIssuanceFailed(hasFailed: Boolean) {
        whenever(walletCoreDocumentsController.hasReIssuanceFailed(any())).thenReturn(hasFailed)
    }

    private suspend fun mockIsDocumentLowOnCredentialsCall(response: Boolean) {
        whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(any()))
            .thenReturn(response)
    }

    private fun mockShowBatchIssuanceCounterPreference(response: Boolean) {
        whenever(suspend { prefKeys.getShowBatchIssuanceCounter() }).thenReturn(response)
    }

    private fun getMockedDocumentCredentialsInfoUi(
        resourceProvider: ResourceProvider,
        availableCredentials: Int = mockedDocumentAvailableCredentials,
        totalCredentials: Int = mockedDocumentTotalCredentials,
    ): DocumentCredentialsInfoUi {
        return DocumentCredentialsInfoUi(
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials,
            title = resourceProvider.getString(
                R.string.document_details_document_credentials_info_text,
                availableCredentials,
                totalCredentials
            ),
        )
    }

    private fun getMockedIssuerDetailsCardDataUi(
        issuerTrustVerdict: IssuerTrustVerdictDomain?,
        issuerName: String? = null,
        issuerLogo: URI? = null,
        documentState: IssuerDetailsCardDataUi.DocumentState = IssuerDetailsCardDataUi.DocumentState.Issued(
            issuanceDate = mockedFormattedIssuanceDate,
            expirationDate = mockedFormattedExpirationDate
        ),
        isExpanded: Boolean = false,
        hasStatusRefreshFailed: Boolean = false,
        hasReIssuanceFailed: Boolean = false,
    ): IssuerDetailsCardDataUi {
        return IssuerDetailsCardDataUi(
            issuerName = issuerName,
            issuerLogo = issuerLogo,
            documentState = documentState,
            isExpanded = isExpanded,
            issuerTrustVerdict = issuerTrustVerdict,
            hasStatusRefreshFailed = hasStatusRefreshFailed,
            hasReIssuanceFailed = hasReIssuanceFailed,
        )
    }

    private fun mockGetDocumentDetailsStrings(
        resourceProvider: ResourceProvider,
        availableCredentials: Int = mockedDocumentAvailableCredentials,
        totalCredentials: Int = mockedDocumentTotalCredentials
    ) {
        StringResourceProviderMocker.mockGetDocumentDetailsStrings(
            resourceProvider = resourceProvider,
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials,
        )
    }
}