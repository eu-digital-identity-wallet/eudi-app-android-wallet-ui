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

package eu.europa.ec.networklogic.config

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TestNetworkConfig {

    private val config = NetworkConfigImpl()

    @Test
    fun `Given the reference configuration, When the limits are read, Then they are the documented defaults`() {
        assertEquals(15.seconds, config.connectTimeout)
        assertEquals(30.seconds, config.socketTimeout)
        assertEquals(60.seconds, config.requestTimeout)
        assertEquals(10L * 1024 * 1024, config.maxResponseBodySize)
    }
}
