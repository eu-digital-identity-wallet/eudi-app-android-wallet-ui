/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.corelogic.config

import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager

/**
 * Configuration class that associates an [OpenId4VciManager.Config] with a specific display order in the ``AddDocument`` Screen.
 *
 * This class facilitates the management of multiple Verifiable Credential Issuance (VCI)
 * configurations by assigning an order, ensuring they are displayed
 * in a predetermined priority.
 *
 * @property config The [OpenId4VciManager.Config] instance containing the Issuer configuration.
 * @property order An integer defining the priority of this configuration.
 */
data class VciConfig(
    val config: OpenId4VciManager.Config,
    val order: Int,
)