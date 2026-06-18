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

package eu.europa.ec.zkplogic

import android.content.Context
import org.multipaz.mdoc.zkp.ZkSystemRepository

/**
 * Builds the [ZkSystemRepository] the wallet enables via `EudiWalletConfig.configureZkp(...)`.
 *
 * Mirrors `eu.europa.ec.eudi.wallet.zkp.LongfellowZkSystemRepository`. The [context] is currently
 * unused (the SDK bundles its native `.so` inside the AAR) but kept for parity and for future
 * circuit/asset loading.
 */
class StwoZkSystemRepository(
    @Suppress("unused") private val context: Context,
) {
    fun build(): ZkSystemRepository = ZkSystemRepository().add(StwoZkSystem())
}
