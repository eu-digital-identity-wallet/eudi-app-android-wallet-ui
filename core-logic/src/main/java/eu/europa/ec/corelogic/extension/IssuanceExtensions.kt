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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Binds each Core key-unlock request to the [observer] before the tracked operation forwards it.
 * When collection ends, pending requests are cancelled. A later request is cancelled on arrival.
 * A prompt result and observer cancellation can race. Only the first can resume or cancel Core.
 */
internal fun Flow<IssueDocumentsPartialState>.withAuthenticationObserver(
    observer: Job,
): Flow<IssueDocumentsPartialState> = map { state ->
    if (state !is IssueDocumentsPartialState.UserAuthRequired) return@map state

    val resultHandler = state.resultHandler
    val pendingResponse = Job(parent = observer)
    pendingResponse.invokeOnCompletion { cause ->
        if (cause is CancellationException) resultHandler.onAuthenticationError()
    }
    state.copy(
        resultHandler = resultHandler.copy(
            onAuthenticationSuccess = {
                if (pendingResponse.complete()) resultHandler.onAuthenticationSuccess()
            },
            onAuthenticationError = { pendingResponse.cancel() },
        )
    )
}