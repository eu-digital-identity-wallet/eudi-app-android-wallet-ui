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

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TestContextExtensions {

    private val context: ComponentActivity =
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    @Test
    fun `Given a web link, When it is opened in the browser, Then a browsable view intent with a lower-case scheme is started`() {
        context.openInBrowser(Uri.parse("HTTPS://Verifier.Example/done?state=1"))

        val started = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(Uri.parse("https://Verifier.Example/done?state=1"), started.data)
        assertTrue(started.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `Given a link that only the wallet itself handles, When it is opened in the browser, Then nothing is started`() {
        val walletActivity = ComponentName(context.packageName, "eu.europa.ec.WalletActivity")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(walletActivity)
            addIntentFilterForActivity(
                walletActivity,
                IntentFilter(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addDataScheme("https")
                    addDataAuthority("wallet.example", null)
                }
            )
        }

        context.openInBrowser(Uri.parse("https://wallet.example/done"))

        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun `Given an https link that only the wallet handles, When it is opened as an external address, Then nothing is started`() {
        val walletActivity = ComponentName(context.packageName, "eu.europa.ec.WalletActivity")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(walletActivity)
            addIntentFilterForActivity(
                walletActivity,
                IntentFilter(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addDataScheme("https")
                    addDataAuthority("wallet.example", null)
                }
            )
        }

        context.openExternalUri(Uri.parse("https://wallet.example/privacy"))

        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun `Given a mail address, When it is opened as an external address, Then a view intent for it is started`() {
        context.openExternalUri(Uri.parse("mailto:dpo@verifier.example"))

        val started = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(Uri.parse("mailto:dpo@verifier.example"), started.data)
    }

    @Test
    fun `Given an http contact, When it is opened as an external address, Then it is opened as a browser link`() {
        context.openExternalUri(Uri.parse("http://verifier.example/contact"))

        val started = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertTrue(started.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `Given an address with another scheme, When it is opened as an external address, Then nothing is started`() {
        context.openExternalUri(Uri.parse("intent://scan/#Intent;scheme=eudi-wallet;end"))
        context.openExternalUri(Uri.parse("file:///sdcard/policy.html"))

        assertNull(shadowOf(context).nextStartedActivity)
    }
}