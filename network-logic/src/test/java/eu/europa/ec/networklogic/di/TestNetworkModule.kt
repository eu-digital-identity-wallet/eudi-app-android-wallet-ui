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

package eu.europa.ec.networklogic.di

import eu.europa.ec.businesslogic.config.AppBuildType
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.networklogic.config.NetworkConfigImpl
import eu.europa.ec.networklogic.util.ResponseSizeLimit
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TestNetworkModule {

    @Test
    fun `Given the reference configuration, When the wallet's client is built, Then it has the timeouts and the size limit`() {
        val configLogic: ConfigLogic = mock {
            on { appBuildType } doReturn AppBuildType.RELEASE
        }

        provideHttpClient(provideJson(), configLogic, NetworkConfigImpl()).use { client ->
            assertNotNull(client.pluginOrNull(HttpTimeout))
            assertNotNull(client.pluginOrNull(ResponseSizeLimit))
        }
    }
}
