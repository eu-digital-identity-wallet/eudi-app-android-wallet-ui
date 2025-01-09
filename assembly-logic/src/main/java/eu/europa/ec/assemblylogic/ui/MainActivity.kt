/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.assemblylogic.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import eu.europa.ec.commonfeature.router.featureCommonGraph
import eu.europa.ec.dashboardfeature.router.featureDashboardGraph
import eu.europa.ec.issuancefeature.router.featureIssuanceGraph
import eu.europa.ec.presentationfeature.router.presentationGraph
import eu.europa.ec.proximityfeature.router.featureProximityGraph
import eu.europa.ec.startupfeature.router.featureStartupGraph
import eu.europa.ec.uilogic.container.EudiComponentActivity

class MainActivity : EudiComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content(intent) {
                featureStartupGraph(it)
                featureCommonGraph(it)
                featureDashboardGraph(it)
                presentationGraph(it)
                featureProximityGraph(it)
                featureIssuanceGraph(it)
            }
        }
    }
}