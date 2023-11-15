/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

<<<<<<<< HEAD:assembly-logic/src/main/java/eu/europa/ec/assemblylogic/service/NfcEngagementServiceImpl.kt
package eu.europa.ec.assemblylogic.service

import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.wallet.EudiWallet

class NfcEngagementServiceImpl : NfcEngagementService() {
    override val transferManager: TransferManager
        get() = EudiWallet.transferManager
========
package eu.europa.ec.proximityfeature.ui.request

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.ui.request.RequestScreen

@Composable
fun ProximityRequestScreen(
    navController: NavController,
    viewModel: ProximityRequestViewModel
) {
    RequestScreen(
        navController = navController,
        viewModel = viewModel
    )
>>>>>>>> develop:proximity-feature/src/main/java/eu/europa/ec/proximityfeature/ui/request/ProximityRequestScreen.kt
}