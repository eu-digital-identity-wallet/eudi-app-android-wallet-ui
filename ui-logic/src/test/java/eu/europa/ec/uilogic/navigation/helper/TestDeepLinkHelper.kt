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

package eu.europa.ec.uilogic.navigation.helper

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.navigation.NavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TestDeepLinkHelper {

    private val navController: NavController = mock {
        on { context } doReturn RuntimeEnvironment.getApplication()
    }

    @Test
    fun `Given no signing session, When an RQES sign-in callback arrives, Then it is ignored without a crash`() {
        handleDeepLinkAction(
            navController = navController,
            uri = Uri.parse("rqes://oauth/callback?code=forged-code"),
        )
    }

    @Test
    fun `Given a link of no known flow in one of the wallet's own schemes, When it is handled, Then nothing is started`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val activityNavController: NavController = mock {
            on { context } doReturn activity
        }

        handleDeepLinkAction(
            navController = activityNavController,
            action = DeepLinkAction(
                link = Uri.parse("eu.europa.ec.euidi://AUTHORIZATION-UNKNOWN?code=1"),
                type = DeepLinkType.EXTERNAL,
            ),
        )

        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `Given an https link of no known flow, When it is handled, Then it is opened as a browser link`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val activityNavController: NavController = mock {
            on { context } doReturn activity
        }

        handleDeepLinkAction(
            navController = activityNavController,
            action = DeepLinkAction(
                link = Uri.parse("https://verifier.example/done"),
                type = DeepLinkType.EXTERNAL,
            ),
        )

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(setOf(Intent.CATEGORY_BROWSABLE), started.categories)
    }

    /**
     * The path comes from the link itself, so a callback opened by another app with another path,
     * a trailing slash or none does not resume a signing.
     */
    @Test
    fun `Given rqes links with several paths, When they are read, Then only the callback's path starts the signing flow`() {
        assertEquals(
            DeepLinkType.RQES,
            hasDeepLink(Uri.parse("rqes://oauth/callback?code=1"))?.type
        )
        listOf(
            "rqes://oauth/callback/?code=1",
            "rqes://oauth?code=1",
            "rqes://oauth/other"
        ).forEach {
            assertEquals(it, DeepLinkType.EXTERNAL, hasDeepLink(Uri.parse(it))?.type)
        }
    }
}