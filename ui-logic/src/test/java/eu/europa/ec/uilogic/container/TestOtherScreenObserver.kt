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

package eu.europa.ec.uilogic.container

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class TestOtherScreenObserver {

    private val sessionController: SessionController = mock()

    private val observer = OtherScreenObserver(sessionController)

    @Test
    fun `Given another activity of the app, When it comes to the front, Then the wallet counts as covered`() {
        observer.onActivityResumed(mock())

        verify(sessionController).onCoveredByOtherScreen()
    }

    @Test
    fun `Given the wallet activity, When it comes to the front, Then it does not count as covered`() {
        observer.onActivityResumed(mock<EudiComponentActivity>())

        verify(sessionController, never()).onCoveredByOtherScreen()
    }
}