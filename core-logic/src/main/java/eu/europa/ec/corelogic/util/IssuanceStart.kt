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

package eu.europa.ec.corelogic.util

import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Marks, for one tracked issuance, whether Wallet Core has reported [IssueEvent.Started]. It
 * reaches the issuance callback through the coroutine context of the collected flow.
 */
internal class IssuanceStart : AbstractCoroutineContextElement(IssuanceStart) {
    companion object Key : CoroutineContext.Key<IssuanceStart>

    private val started = AtomicBoolean(false)

    val hasStarted: Boolean get() = started.get()

    fun markStarted() = started.set(true)
}
