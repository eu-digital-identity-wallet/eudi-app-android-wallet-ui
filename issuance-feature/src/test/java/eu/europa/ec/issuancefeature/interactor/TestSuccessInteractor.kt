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

package eu.europa.ec.issuancefeature.interactor

import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.commonfeature.util.TestsConstants.mockedDocUiNamePid
import eu.europa.ec.commonfeature.util.extractFullNameFromDocumentOrEmpty
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.mockDocumentTypeUiToUiNameCall
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedFullPid
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals

class TestSuccessInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: SuccessInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = SuccessInteractorImpl(
            resourceProvider = resourceProvider,
            walletCoreDocumentsController = walletCoreDocumentsController
        )

        `when`(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    // region fetchDocumentById


    @Test
    fun `Given invalid document id, When fetchDocumentById is called, Then Failure Result is returned`() {
        coroutineRule.runTest {
            // Given
            `when`(walletCoreDocumentsController.getDocumentById(""))
                .thenReturn(null)

            // When
            interactor.fetchDocumentById("").runFlowTest {
                assertEquals(
                    SuccessFetchDocumentByIdPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `When walletCoreDocumentsController,getAllDocuments() throws an exception with a message, Then fetchDocumentById Failure Result is returned`() {
        coroutineRule.runTest {
            // Given
            `when`(walletCoreDocumentsController.getDocumentById(""))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.fetchDocumentById("").runFlowTest {
                assertEquals(
                    SuccessFetchDocumentByIdPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given valid document id, When fetchDocumentById is called, Then Success Result is returned with valid data`() {
        coroutineRule.runTest {
            // Given
            `when`(walletCoreDocumentsController.getDocumentById("test_id"))
                .thenReturn(mockedFullPid)
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.fetchDocumentById("test_id").runFlowTest {
                assertEquals(
                    SuccessFetchDocumentByIdPartialState.Success(
                        document = mockedFullPid,
                        documentName = mockedDocUiNamePid,
                        fullName = extractFullNameFromDocumentOrEmpty(mockedFullPid)
                    ),
                    awaitItem()
                )
            }
        }
    }

    // endregion
}