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

package eu.europa.ec.uilogic.extension

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestActivityExtensions {

    @Test
    fun `Given a sender that set a referrer extra, When the launching package is read, Then it is the one Android recorded`() {
        val recorded = activityStartedWith(linkIntent()).launchingPackage()

        val withReferrer = linkIntent().putExtra(Intent.EXTRA_REFERRER, Uri.parse(PAGE))
        val withReferrerName = linkIntent().putExtra(Intent.EXTRA_REFERRER_NAME, PAGE)

        assertEquals("verifier.example", activityStartedWith(withReferrer).referrer?.host)
        assertEquals(recorded, activityStartedWith(withReferrer).launchingPackage())
        assertEquals(recorded, activityStartedWith(withReferrerName).launchingPackage())
        assertNotEquals("verifier.example", recorded)
    }

    @Test
    fun `Given a launch intent, When the launching package is read, Then the activity keeps that same intent`() {
        val activity =
            activityStartedWith(linkIntent().putExtra(Intent.EXTRA_REFERRER, Uri.parse(PAGE)))
        val launch = activity.intent

        activity.launchingPackage()

        assertSame(launch, activity.intent)
    }

    @Test
    fun `Given screen capture prevented, When it is set, Then the window is secure`() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        activity.setScreenCapturePrevented(true)

        assertTrue(activity.isWindowSecure())
    }

    @Test
    fun `Given a secure window, When screen capture is allowed, Then the window is no longer secure`() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        activity.setScreenCapturePrevented(true)

        activity.setScreenCapturePrevented(false)

        assertFalse(activity.isWindowSecure())
    }

    private fun Activity.isWindowSecure(): Boolean =
        window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    private fun activityStartedWith(intent: Intent): Activity =
        Robolectric.buildActivity(Activity::class.java, intent).get()

    private fun linkIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(LINK))

    private companion object {
        const val LINK = "haip-vp://?request_uri=https%3A%2F%2Fverifier.example%2Fr%2F1"
        const val PAGE = "https://verifier.example/invoke-wallet"
    }
}