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

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument

/**
 * Whether the verifier requested this document via a zero-knowledge predicate (i.e. it should be
 * proven without disclosing the underlying values).
 *
 * The signalling field [RequestedDocument.zkRequestSystemSpecs] is `internal` to the data-transfer
 * library and therefore not part of its public API, so we read it reflectively. We only need to
 * know whether any ZK spec is present, so this never touches the multipaz spec type.
 *
 * NOTE: This is a proof-of-concept addition. The real solution needs forking of
 * `eu.europa.ec.eudi:eudi-lib-android-iso18013-data-transfer` which was considered too much for
 * this iteration.
 */
fun RequestedDocument.isZeroKnowledgeRequest(): Boolean = runCatching {
    val field = this::class.java.getDeclaredField("zkRequestSystemSpecs").apply {
        isAccessible = true
    }
    (field.get(this) as? List<*>)?.isNotEmpty() == true
}.getOrDefault(false)
