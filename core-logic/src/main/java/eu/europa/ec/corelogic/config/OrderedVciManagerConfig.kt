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
 * Configuration data class that associates an [OpenId4VciManager.Config] with an order.
 *
 * This is used to maintain a specific order for different VCI (Verifiable Credential Issuance)
 * manager configurations, which can be useful when multiple issuer configurations need to be
 * processed or displayed in a predetermined sequence.
 *
 * @property config The configuration for an [OpenId4VciManager] instance.
 * @property order An integer representing the order or priority of this configuration.
 * Lower numbers indicate higher priority.
 */
data class OrderedVciManagerConfig(
    val config: OpenId4VciManager.Config,
    val order: Int,
)