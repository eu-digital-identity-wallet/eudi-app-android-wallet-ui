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

package eu.europa.ec.businesslogic.controller.storage

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestPrefKeys {

    private val prefsController: PrefsController = mock()

    private val prefKeys = PrefKeysImpl(prefsController = prefsController)

    @Test
    fun `Given a factory reset, When the preferences are cleared, Then only the session id and the database key are kept`() =
        runBlocking {
            prefKeys.clearAllExceptSessionIdAndDbKey()

            verify(prefsController).clearExcept(keys = setOf("SessionId", "dbKey"))
        }

    @Test
    fun `Given an opened launch link, When it is recorded and read, Then the same preference holds it`() =
        runBlocking {
            whenever(prefsController.getString("OpenedLaunchLink", "")).thenReturn("12:abc")

            prefKeys.setOpenedLaunchLink("12:abc")

            verify(prefsController).setString("OpenedLaunchLink", "12:abc")
            assertEquals("12:abc", prefKeys.getOpenedLaunchLink())
        }
}