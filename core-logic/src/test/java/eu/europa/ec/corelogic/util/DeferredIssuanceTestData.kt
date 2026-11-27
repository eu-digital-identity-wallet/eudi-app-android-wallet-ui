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

import eu.europa.ec.corelogic.config.DeferredIssuanceConfig
import eu.europa.ec.corelogic.model.DeferredDocumentDataDomain
import eu.europa.ec.corelogic.model.WalletDocumentOperationStateDomain
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import kotlin.time.Duration.Companion.seconds

val mockedDeferredConfig = DeferredIssuanceConfig(
    initialPollingDelay = 5.seconds,
    failureRetryDelay = 30.seconds,
    maxConcurrentDeferredRequests = null,
)
val mockedDeferredSchedule = DeferredDocumentSchedule(
    identifier = "pending-document",
    nextAttemptAt = 0,
    lastIntervalMillis = null,
    originalDocumentId = null,
)
val mockedDocumentOperationState = WalletDocumentOperationStateDomain(
    isForeground = true,
    isStopped = false,
    isRemovingAllDocuments = false,
    failedDeferredDocumentIds = emptySet(),
    activeDocumentIds = emptySet(),
    removingDocumentIds = emptySet(),
    removedDocumentIds = emptySet(),
    pendingOperations = 0,
    walletGeneration = 0,
)
val mockedDeferredDocumentData = DeferredDocumentDataDomain(
    documentId = mockedDeferredSchedule.identifier,
    formatType = "org.iso.18013.5.1.mDL",
    docName = "Driving licence",
)