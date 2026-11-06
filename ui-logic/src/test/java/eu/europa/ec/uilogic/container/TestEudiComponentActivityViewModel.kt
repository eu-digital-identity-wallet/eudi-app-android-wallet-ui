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

import android.content.Intent
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.provider.UuidProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestEudiComponentActivityViewModel {

    private val sessionController: SessionController = mock()

    private val uuidProvider: UuidProvider = mock {
        on { provideUuid() } doReturn "session"
    }

    private val prefKeys: PrefKeys = mock()

    private val viewModel = EudiComponentActivityViewModel(
        prefKeys = prefKeys,
        dcapiSettingProvider = mock(),
        sessionController = sessionController,
        uuidProvider = uuidProvider,
    )

    private val link: Intent = mock()

    @Test
    fun `Given the activity is created, When it starts, Then it sets the session id`() {
        viewModel.onCreate()

        runBlocking { verify(prefKeys).setSessionId("session") }
    }

    @Test
    fun `Given a link opened and used, When the system UI restarts the task with it, Then it is not opened again`() {
        storePrefsInMemory()
        whenever(sessionController.isUnlocked).thenReturn(true)
        val tap = linkIntent(CHROME_TAP_FLAGS)

        assertTrue(opens(tap, launchedBy = "com.android.chrome"))
        viewModel.cacheIntent(tap)
        assertSame(tap, viewModel.getCachedIntent())

        assertFalse(opens(linkIntent(RESTART_FLAGS), launchedBy = SYSTEM_UI_PACKAGE))
        assertTrue(
            opens(
                linkIntent(RESTART_FLAGS),
                launchedBy = SYSTEM_UI_PACKAGE,
                taskId = TASK + 1
            )
        )
        assertTrue(opens(linkIntent(CHROME_TAP_FLAGS), launchedBy = "com.android.chrome"))
        assertTrue(opens(linkIntent(RESTART_FLAGS), launchedBy = "com.example.relyingparty"))
    }

    @Test
    fun `Given a link opened but never used, When the task restarts with it, Then it opens again`() {
        storePrefsInMemory()
        whenever(sessionController.isUnlocked).thenReturn(false)
        val tap = linkIntent(CHROME_TAP_FLAGS)

        assertTrue(opens(tap, launchedBy = "com.android.chrome"))
        viewModel.cacheIntent(tap)
        assertNull(viewModel.getCachedIntent())

        assertTrue(opens(linkIntent(RESTART_FLAGS), launchedBy = SYSTEM_UI_PACKAGE))
        runBlocking { verify(prefKeys, never()).setOpenedLaunchLink(any()) }
    }

    @Test
    fun `Given a launch link replaced by a newer link before a screen used it, When the task restarts with it, Then it opens again`() {
        storePrefsInMemory()
        whenever(sessionController.isUnlocked).thenReturn(true)

        assertTrue(opens(linkIntent(CHROME_TAP_FLAGS), launchedBy = "com.android.chrome"))
        viewModel.cacheIntent(linkIntent(CHROME_TAP_FLAGS, link = NEWER_LINK))
        viewModel.getCachedIntent()

        assertTrue(opens(linkIntent(RESTART_FLAGS), launchedBy = SYSTEM_UI_PACKAGE))
        runBlocking { verify(prefKeys, never()).setOpenedLaunchLink(any()) }
    }

    @Test
    fun `Given an activity that is not its task's root or has no link, When it starts, Then it opens and records nothing`() {
        storePrefsInMemory()
        whenever(sessionController.isUnlocked).thenReturn(true)
        val tap = linkIntent(CHROME_TAP_FLAGS)

        assertTrue(
            viewModel.shouldOpenLaunchLink(
                tap,
                TASK,
                isTaskRoot = false,
                launchedBy = SYSTEM_UI_PACKAGE
            )
        )
        assertTrue(
            viewModel.shouldOpenLaunchLink(
                mock(),
                TASK,
                isTaskRoot = true,
                launchedBy = null
            )
        )
        viewModel.cacheIntent(tap)
        viewModel.getCachedIntent()

        runBlocking { verify(prefKeys, never()).setOpenedLaunchLink(any()) }
    }

    private var storedLaunchLink = ""

    private fun storePrefsInMemory() = runBlocking {
        whenever(prefKeys.getOpenedLaunchLink()).thenAnswer { storedLaunchLink }
        whenever(prefKeys.setOpenedLaunchLink(any())).thenAnswer { call ->
            storedLaunchLink = call.getArgument(0)
            Unit
        }
    }

    private fun linkIntent(launchFlags: Int, link: String = LINK): Intent = mock {
        on { dataString } doReturn link
        on { flags } doReturn launchFlags
    }

    private fun opens(intent: Intent, launchedBy: String, taskId: Int = TASK): Boolean =
        viewModel.shouldOpenLaunchLink(intent, taskId, isTaskRoot = true, launchedBy = launchedBy)

    private companion object {
        const val LINK = "haip-vp://?request_uri=https%3A%2F%2Fverifier.example%2Fr%2F1"
        const val NEWER_LINK = "haip-vp://?request_uri=https%3A%2F%2Fverifier.example%2Fr%2F2"
        const val TASK = 132
        const val CHROME_TAP_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        const val RESTART_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    @Test
    fun `Given a locked wallet with a kept link, When it is read or cleared, Then it is withheld and kept until login`() {
        whenever(sessionController.isUnlocked).thenReturn(true)
        viewModel.cacheIntent(link)
        whenever(sessionController.isUnlocked).thenReturn(false)

        assertNull(viewModel.getCachedIntent())
        viewModel.cacheIntent(null)

        whenever(sessionController.isUnlocked).thenReturn(true)
        assertSame(link, viewModel.getCachedIntent())
    }

    @Test
    fun `Given an unlocked wallet with a kept link, When it is cleared, Then it is gone`() {
        whenever(sessionController.isUnlocked).thenReturn(true)
        viewModel.cacheIntent(link)

        viewModel.cacheIntent(null)

        assertNull(viewModel.getCachedIntent())
    }

    @Test
    fun `Given a locked wallet with a kept link, When a factory reset forgets it, Then it is gone after login too`() {
        whenever(sessionController.isUnlocked).thenReturn(false)
        viewModel.cacheIntent(link)

        viewModel.clearCachedIntent()

        whenever(sessionController.isUnlocked).thenReturn(true)
        assertNull(viewModel.getCachedIntent())
    }
}