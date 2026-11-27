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

package eu.europa.ec.corelogic.worker

import eu.europa.ec.corelogic.config.DeferredIssuanceConfig
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import kotlin.time.Duration

private const val ISSUER_INTERVAL_MARGIN_MILLIS = 1_000L

/** Pure schedule transitions; callers own persistence and Core requests. */
internal class DeferredRetryPolicy(private val config: DeferredIssuanceConfig) {

    /**
     * Returns a schedule to persist before the request, keeping a deadline if the process dies
     * before the response is saved.
     */
    fun prepareForAttempt(schedule: DeferredDocumentSchedule, now: Long): DeferredDocumentSchedule =
        schedule.copy(nextAttemptAt = now + pollingDelay(schedule))

    /**
     * A fresh issuer interval may be rounded down to whole seconds. Add one second to the
     * deadline and keep the reported interval for fallbacks. If this response has no valid
     * interval, reuse the last interval or the configured initial delay.
     */
    fun notReady(
        schedule: DeferredDocumentSchedule,
        interval: Duration?,
        now: Long,
    ): DeferredDocumentSchedule {
        val issuerIntervalMillis = interval
            ?.takeIf { candidate -> candidate.isFinite() && candidate.inWholeMilliseconds > 0 }
            ?.inWholeMilliseconds
            ?: return schedule.copy(nextAttemptAt = now + pollingDelay(schedule))

        return schedule.copy(
            nextAttemptAt = now + issuerIntervalMillis + ISSUER_INTERVAL_MARGIN_MILLIS,
            lastIntervalMillis = issuerIntervalMillis,
        )
    }

    /** The previous wait elapsed before this attempt. An ordinary failure starts the configured retry delay. */
    fun failed(schedule: DeferredDocumentSchedule, now: Long): DeferredDocumentSchedule =
        schedule.copy(nextAttemptAt = now + config.failureRetryDelay.inWholeMilliseconds)

    private fun pollingDelay(schedule: DeferredDocumentSchedule): Long =
        schedule.lastIntervalMillis ?: config.initialPollingDelay.inWholeMilliseconds
}