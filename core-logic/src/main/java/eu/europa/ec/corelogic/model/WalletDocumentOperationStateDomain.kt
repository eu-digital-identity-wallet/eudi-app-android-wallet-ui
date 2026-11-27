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

package eu.europa.ec.corelogic.model

/**
 * Process-local document operations and polling indications. Room owns the retry deadlines.
 *
 * @property isForeground Whether the wallet activity is resumed; unlock is checked separately.
 * @property isStopped Blocks new ordinary operations during drain; tracked operations continue.
 * @property isRemovingAllDocuments Blocks new work until bulk removal and its writes finish.
 * @property failedDeferredDocumentIds Retryable documents whose latest poll failed in this
 * process.
 * @property activeDocumentIds Deferred attempts holding a concurrency slot, including result
 * writes.
 * @property removingDocumentIds IDs waiting for their removal lock or being removed; new pings
 * stop.
 * @property removedDocumentIds Confirmed removals kept until reset/process exit to reject late
 * registration.
 * @property pendingOperations Count used by reset, including non-polling operations and metadata
 * writes.
 * @property walletGeneration Increases after storage is cleared; polling needs a new Dashboard
 * activation.
 */
data class WalletDocumentOperationStateDomain(
    val isForeground: Boolean,
    val isStopped: Boolean,
    val isRemovingAllDocuments: Boolean,
    val failedDeferredDocumentIds: Set<String>,
    val activeDocumentIds: Set<String>,
    val removingDocumentIds: Set<String>,
    val removedDocumentIds: Set<String>,
    val pendingOperations: Int,
    val walletGeneration: Long,
)