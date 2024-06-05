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
import eu.europa.ec.euidi.config.LibraryModule
import eu.europa.ec.euidi.config.LibraryPluginConfig
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
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {

            val config =
                extensions.create<LibraryPluginConfig>("moduleConfig", LibraryModule.Unspecified)

            val walletScheme = "eudi-wallet"
            val walletHost = "*"

            val eudiOpenId4VpScheme = "eudi-openid4vp"
            val eudiOpenid4VpHost = "*"

            val mdocOpenId4VpScheme = "mdoc-openid4vp"
            val mdocOpenid4VpHost = "*"

            val openId4VpScheme = "openid4vp"
            val openid4VpHost = "*"

            val credentialOfferScheme = "openid-credential-offer"
            val credentialOfferHost = "*"

            val openId4VciAuthorizationScheme = "eudi-issuance"
            val openId4VciAuthorizationHost = "authorization"

            val storedVersion = getProperty<String>(
                "VERSION_NAME",
                "version.properties"
            ).orEmpty()

            with(pluginManager) {
                apply("com.android.library")
                apply("eudi.android.library.kover")
                apply("eudi.android.lint")
                apply("eudi.android.koin")
                apply("org.jetbrains.kotlin.android")
                apply("kotlinx-serialization")
                apply("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                with(defaultConfig) {

                    targetSdk = 34

                    addConfigField("DEEPLINK", "$walletScheme://")
                    addConfigField("EUDI_OPENID4VP_SCHEME", eudiOpenId4VpScheme)
                    addConfigField("MDOC_OPENID4VP_SCHEME", mdocOpenId4VpScheme)
                    addConfigField("OPENID4VP_SCHEME", openId4VpScheme)
                    addConfigField("CREDENTIAL_OFFER_SCHEME", credentialOfferScheme)
                    addConfigField("ISSUE_AUTHORIZATION_SCHEME", openId4VciAuthorizationScheme)
                    addConfigField("ISSUE_AUTHORIZATION_HOST", openId4VciAuthorizationHost)
                    addConfigField(
                        "ISSUE_AUTHORIZATION_DEEPLINK",
                        "$openId4VciAuthorizationScheme://$openId4VciAuthorizationHost"
                    )

                    // Manifest placeholders for Wallet deepLink
                    manifestPlaceholders["deepLinkScheme"] = walletScheme
                    manifestPlaceholders["deepLinkHost"] = walletHost

                    // Manifest placeholders used for OpenId4VP
                    manifestPlaceholders["eudiOpenid4vpScheme"] = eudiOpenId4VpScheme
                    manifestPlaceholders["eudiOpenid4vpHost"] = eudiOpenid4VpHost
                    manifestPlaceholders["mdocOpenid4vpScheme"] = mdocOpenId4VpScheme
                    manifestPlaceholders["mdocOpenid4vpHost"] = mdocOpenid4VpHost
                    manifestPlaceholders["openid4vpScheme"] = openId4VpScheme
                    manifestPlaceholders["openid4vpHost"] = openid4VpHost

                    // Manifest placeholders used for OpenId4VCI
                    manifestPlaceholders["credentialOfferHost"] = credentialOfferHost
                    manifestPlaceholders["credentialOfferScheme"] = credentialOfferScheme

                    // Manifest placeholders used for OpenId4VCI Authorization
                    manifestPlaceholders["openId4VciAuthorizationScheme"] =
                        openId4VciAuthorizationScheme
                    manifestPlaceholders["openId4VciAuthorizationHost"] =
                        openId4VciAuthorizationHost
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
            afterEvaluate {
                if (!config.module.isLogicModule && !config.module.isFeatureCommon) {
                    dependencies {
                        add("implementation", project(LibraryModule.CommonFeature.path))
                    }
                }
            }
        }
    }
}
