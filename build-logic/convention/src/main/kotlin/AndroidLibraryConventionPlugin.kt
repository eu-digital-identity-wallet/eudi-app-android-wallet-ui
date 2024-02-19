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

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.google.android.libraries.mapsplatform.secrets_gradle_plugin.SecretsPluginExtension
import eu.europa.ec.euidi.addConfigField
import eu.europa.ec.euidi.configureFlavors
import eu.europa.ec.euidi.configureGradleManagedDevices
import eu.europa.ec.euidi.configureKotlinAndroid
import eu.europa.ec.euidi.configurePrintApksTask
import eu.europa.ec.euidi.disableUnnecessaryAndroidTests
import eu.europa.ec.euidi.getProperty
import eu.europa.ec.euidi.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {

            val walletScheme = "eudi-wallet"
            val walletHost = "*"

            val openId4VpScheme = "eudi-openid4vp"
            val openid4VpHost = "*"

            val openId4VciScheme = "eudi-openid4ci"
            val openid4VciHost = "authorize"
            val openid4VciPath = ""

            val storedVersion = getProperty<String>(
                "VERSION_NAME",
                "version.properties"
            ).orEmpty()

            with(pluginManager) {
                apply("com.android.library")
                apply("eudi.android.library.jacoco")
                apply("eudi.android.lint")
                apply("eudi.android.koin")
                apply("org.jetbrains.kotlin.android")
                apply("jacoco")
                apply("kotlinx-serialization")
                apply("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                with(defaultConfig) {

                    targetSdk = 34

                    addConfigField("DEEPLINK", "$walletScheme://")
                    addConfigField("OPENID4VP_SCHEME", openId4VpScheme)

                    // Manifest placeholders for Wallet deepLink
                    manifestPlaceholders["deepLinkScheme"] = walletScheme
                    manifestPlaceholders["deepLinkHost"] = walletHost

                    // Manifest placeholders used for OpenId4VP
                    manifestPlaceholders["openid4vpScheme"] = openId4VpScheme
                    manifestPlaceholders["openid4vpHost"] = openid4VpHost

                    // Manifest placeholders used by the Core's VCI
                    manifestPlaceholders["openid4vciAuthorizeHost"] = openid4VciHost
                    manifestPlaceholders["openid4vciAuthorizePath"] = openid4VciPath
                    manifestPlaceholders["openid4vciAuthorizeScheme"] = openId4VciScheme
                }
                configureFlavors(this, storedVersion)
                configureGradleManagedDevices(this)
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                configurePrintApksTask(this)
                disableUnnecessaryAndroidTests(target)
            }
            extensions.configure<SecretsPluginExtension> {
                defaultPropertiesFileName = "secrets.defaults.properties"
                ignoreList.add("sdk.*")
            }
            dependencies {
                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-guava").get())
                add("implementation", libs.findLibrary("kotlinx.serialization.json").get())
            }
        }
    }
}
