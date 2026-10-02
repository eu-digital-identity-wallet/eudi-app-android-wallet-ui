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

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal fun Context.openTransactionAction(url: String): Boolean = runCatching {
    val uri = url.toUri()
    val action = when (uri.scheme?.lowercase()) {
        "mailto" -> Intent.ACTION_SENDTO
        "tel" -> Intent.ACTION_DIAL
        "http", "https" -> Intent.ACTION_VIEW
        else -> return false
    }
    startActivity(Intent(action, uri))
    true
}.getOrDefault(false)