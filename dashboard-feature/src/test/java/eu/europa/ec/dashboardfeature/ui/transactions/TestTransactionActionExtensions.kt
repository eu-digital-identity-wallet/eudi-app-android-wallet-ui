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

package eu.europa.ec.dashboardfeature.ui.transactions

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TestTransactionActionExtensions {

    private val context: ComponentActivity =
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    @Test
    fun `Given a web address, When a transaction action opens it, Then it is opened as a browser link`() {
        listOf(
            "https://verifier.example/deletion",
            "http://verifier.example/deletion",
        ).forEach { url ->
            assertTrue(url, context.openTransactionAction(url))

            val started = shadowOf(context).nextStartedActivity
            assertEquals(url, Intent.ACTION_VIEW, started.action)
            assertEquals(url, Uri.parse(url), started.data)
            assertTrue(url, started.hasCategory(Intent.CATEGORY_BROWSABLE))
        }
    }

    @Test
    fun `Given a web address that only the wallet itself handles, When a transaction action opens it, Then nothing is started`() {
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

        assertFalse(context.openTransactionAction("https://wallet.example/deletion"))
        assertNull(shadowOf(context).nextStartedActivity)

        assertTrue(context.openTransactionAction("https://verifier.example/deletion"))
        assertNotNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun `Given a mail address, When a transaction action opens it, Then a mail app is asked to write to it`() {
        assertTrue(context.openTransactionAction("mailto:dpo@verifier.example"))

        val started = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_SENDTO, started.action)
        assertEquals(Uri.parse("mailto:dpo@verifier.example"), started.data)
    }

    @Test
    fun `Given a phone number, When a transaction action opens it, Then the dialler is opened with it`() {
        assertTrue(context.openTransactionAction("tel:+352123456"))

        val started = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_DIAL, started.action)
        assertEquals(Uri.parse("tel:+352123456"), started.data)
    }

    @Test
    fun `Given an address with another scheme, When a transaction action opens it, Then nothing is started`() {
        listOf(
            "intent://scan/#Intent;scheme=eudi-wallet;end",
            "eudi-wallet://dashboard",
            "file:///sdcard/policy.html",
            "javascript:alert(1)",
        ).forEach { url ->
            assertFalse(url, context.openTransactionAction(url))
        }
        assertNull(shadowOf(context).nextStartedActivity)
    }
}
