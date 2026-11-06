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
import android.os.Bundle
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class TestEudiComponentActivityHelper {

    private val saved: Bundle = mock()

    @Test
    fun `Given a process that has not started its navigation, When the activity is recreated, Then no screens are restored`() {
        assertNull(getStateToRestore(saved, navigationStartedInProcess = false))
    }

    @Test
    fun `Given a configuration change, When the activity is recreated, Then its screens are restored`() {
        assertSame(saved, getStateToRestore(saved, navigationStartedInProcess = true))
    }

    @Test
    fun `Given a recreated activity, When it starts, Then its launch intent is not opened again`() {
        assertFalse(shouldOpenLaunchIntent(saved))
        assertTrue(shouldOpenLaunchIntent(null))
    }

    @Test
    fun `Given the link that started this task, When Android restarts the task with it, Then it is a replay`() {
        val recorded = createLaunchLinkRecord(taskId = 132, link = LINK)

        assertTrue(
            replayed(
                UPDATE_RESTART_FLAGS,
                SYSTEM_UI_PACKAGE,
                taskId = 132,
                recorded = recorded
            )
        )
        assertTrue(
            replayed(
                RECENTS_RESTART_FLAGS,
                SYSTEM_UI_PACKAGE,
                taskId = 132,
                recorded = recorded
            )
        )
        assertTrue(
            replayed(
                FROM_HISTORY_FLAGS,
                LAUNCHER_PACKAGE,
                taskId = 132,
                recorded = recorded
            )
        )
    }

    @Test
    fun `Given a link the user opens, When Chrome or adb sends it, Then it is never a replay`() {
        val recorded = createLaunchLinkRecord(taskId = 132, link = LINK)

        assertFalse(replayed(CHROME_TAP_FLAGS, CHROME_PACKAGE, taskId = 132, recorded = recorded))
        assertFalse(
            replayed(
                Intent.FLAG_ACTIVITY_NEW_TASK,
                SHELL_PACKAGE,
                taskId = 132,
                recorded = recorded
            )
        )
    }

    @Test
    fun `Given an app that sends the same link with CLEAR_TASK, When it arrives in the task, Then it is not a replay`() {
        val recorded = createLaunchLinkRecord(taskId = 132, link = LINK)

        assertFalse(
            replayed(
                RECENTS_RESTART_FLAGS,
                OTHER_APP_PACKAGE,
                taskId = 132,
                recorded = recorded
            )
        )
        assertFalse(
            replayed(
                UPDATE_RESTART_FLAGS,
                launchedBy = null,
                taskId = 132,
                recorded = recorded
            )
        )
    }

    @Test
    fun `Given the system UI starts the activity without a restart flag, When the link matches, Then it is not a replay`() {
        val recorded = createLaunchLinkRecord(taskId = 132, link = LINK)

        assertFalse(
            replayed(
                Intent.FLAG_ACTIVITY_NEW_TASK,
                SYSTEM_UI_PACKAGE,
                taskId = 132,
                recorded = recorded
            )
        )
    }

    @Test
    fun `Given a restart, When the task or the link differs from the record, Then it is not a replay`() {
        val recorded = createLaunchLinkRecord(taskId = 132, link = LINK)

        assertFalse(
            replayed(
                RECENTS_RESTART_FLAGS,
                SYSTEM_UI_PACKAGE,
                taskId = 140,
                recorded = recorded
            )
        )
        assertFalse(
            replayed(
                RECENTS_RESTART_FLAGS,
                SYSTEM_UI_PACKAGE,
                taskId = 132,
                link = "$LINK&x=1",
                recorded = recorded
            )
        )
        assertFalse(replayed(RECENTS_RESTART_FLAGS, SYSTEM_UI_PACKAGE, taskId = 132, recorded = ""))
    }

    private fun replayed(
        launchFlags: Int,
        launchedBy: String?,
        taskId: Int,
        link: String = LINK,
        recorded: String,
    ): Boolean = isReplayedLaunchLink(launchFlags, launchedBy, taskId, link, recorded)

    @Test
    fun `Given a launch link, When its record is built, Then it names the task and holds no part of the link`() {
        val record = createLaunchLinkRecord(taskId = 132, link = LINK)

        assertTrue(record.matches(Regex("132:[0-9a-f]{64}")))
        assertFalse(record.contains("request_uri"))
        assertEquals(record, createLaunchLinkRecord(taskId = 132, link = LINK))
    }

    @Test
    fun `Given incoming links, When one starts no wallet flow, Then only that one is dropped`() {
        val dropped = DeepLinkType.entries.filter(::shouldDropIncomingLink)

        assertEquals(listOf(DeepLinkType.EXTERNAL), dropped)
    }

    @Test
    fun `Given a declined request, When it brought the wallet up, Then the activity finishes, and otherwise not`() {
        assertTrue(shouldFinishOnDeclinedRequest(coldBoot = true, walletWasShown = true))
        assertTrue(shouldFinishOnDeclinedRequest(coldBoot = false, walletWasShown = false))
        assertFalse(shouldFinishOnDeclinedRequest(coldBoot = false, walletWasShown = true))
    }

    private companion object {
        const val LINK =
            "haip-vp://?client_id=x509_hash%3Aabc&request_uri=https%3A%2F%2Fverifier.example%2Fr%2F1"

        /** The flags of an intent Chrome sends when the user taps a wallet link, as seen on the emulator. */
        const val CHROME_TAP_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        /** The flags of the task's first intent when Android restarts it after an app update. */
        const val UPDATE_RESTART_FLAGS = Intent.FLAG_ACTIVITY_CLEAR_TASK

        /** The flags of the task's first intent when Recents restarts it without saved state. */
        const val RECENTS_RESTART_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        /** The flags AOSP adds when a launcher restarts a task through the Recents path. */
        const val FROM_HISTORY_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY

        const val CHROME_PACKAGE = "com.android.chrome"
        const val SHELL_PACKAGE = "com.android.shell"
        const val LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher"
        const val OTHER_APP_PACKAGE = "com.example.relyingparty"
    }
}