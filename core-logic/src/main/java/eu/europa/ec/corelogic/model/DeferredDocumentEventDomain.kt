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

enum class DeferredDocumentRemovalReasonDomain {
    ISSUER_NOT_TRUSTED,
}

/** Per-document outcomes published after persistence; consumers refresh from current storage. */
sealed interface DeferredDocumentEventDomain {
    val documentId: String

    data class Issued(
        override val documentId: String
    ) : DeferredDocumentEventDomain

    /** The visible Pending/Failed state can change while polling continues. */
    data class StatusChanged(
        override val documentId: String
    ) : DeferredDocumentEventDomain

    data class Removed(
        override val documentId: String,
        val reason: DeferredDocumentRemovalReasonDomain,
    ) : DeferredDocumentEventDomain
}