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

package eu.europa.ec.assemblylogic.di

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import org.junit.Test
import org.koin.android.test.verify.androidVerify
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.ParameterTypeInjection

/**
 * Checks, without running any definition, that every public constructor of a definition's type has
 * a definition for each of its parameters. It cannot see inside definition lambdas, so a definition
 * built by a provider function is only checked for the type that function returns;
 * [TestKoinGraphResolution] covers those.
 */
@OptIn(KoinExperimentalAPI::class)
class TestKoinGraphVerification {

    @Test
    fun `every constructor parameter of the wallet modules has a definition`() {
        module { includes(eudiWalletModules) }.androidVerify(
            // Constructor parameters of library types that provider functions build themselves.
            extraTypes = listOf(
                HttpClientEngine::class,
                HttpClientConfig::class,
            ),
            injections = injectedParameterSamples.map { (type, samples) ->
                ParameterTypeInjection(type, samples.map { it::class })
            },
        )
    }
}
