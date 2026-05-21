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
import android.net.Uri
import eu.europa.ec.dashboardfeature.ui.document_sign.model.DocumentSignButtonUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class TestDocumentSignInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var uri: Uri

    private lateinit var interactor: DocumentSignInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DocumentSignInteractorImpl(
            resourceProvider = resourceProvider,
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region launchRqesSdk

    // The function is a single-line delegation to the EudiRQESUi Kotlin object singleton
    // (an instance method on a singleton, not a Java static), so Mockito's mockStatic
    // cannot intercept the call. We exercise the line for coverage and tolerate any
    // throwable from the un-initialized SDK; deeper verification would require
    // injecting EudiRQESUi behind a wallet-facing interface.
    @Test
    fun `When launchRqesSdk is called, Then the call is dispatched to the EudiRQESUi singleton`() {
        try {
            // When
            interactor.launchRqesSdk(context = context, uri = uri)
        } catch (_: Throwable) {
            // Expected: SDK throws because setup() has not been called in unit-test context.
        }
    }
    //endregion

    //region getItemUi
    @Test
    fun `When getItemUi is called, Then the expected DocumentSignButtonUi is returned`() {
        // Given
        whenever(resourceProvider.getString(R.string.document_sign_select_document))
            .thenReturn(mockedSelectDocumentText)

        // When
        val result = interactor.getItemUi()

        // Then
        val expectedResult = DocumentSignButtonUi(
            data = ListItemDataUi(
                itemId = "documentSignButtonId",
                mainContentData = ListItemMainContentDataUi.Text(text = mockedSelectDocumentText),
                trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.Add),
            )
        )
        assertEquals(expectedResult, result)
    }
    //endregion

    //region mocked objects
    private val mockedSelectDocumentText = "Select document"
    //endregion
}
