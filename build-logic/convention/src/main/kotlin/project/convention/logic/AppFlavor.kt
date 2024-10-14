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

package project.convention.logic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.api.Project

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

enum class AppFlavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
    val applicationNameSuffix: String? = null
) {
    Dev(FlavorDimension.contentType, applicationIdSuffix = ".dev"),
    Demo(FlavorDimension.contentType)
}

fun Project.configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: AppFlavor) -> Unit = {}
) {

    val version = getProperty<String>(
        "VERSION_NAME",
        "version.properties"
    ).orEmpty()

    commonExtension.apply {
        flavorDimensions += FlavorDimension.contentType.name
        productFlavors {
            AppFlavor.values().forEach {
                create(it.name.lowercase()) {
                    dimension = it.dimension.name
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        versionName = version
                        if (it.applicationIdSuffix != null) {
                            applicationIdSuffix = it.applicationIdSuffix
                        }
                    }
                    manifestPlaceholders["appNameSuffix"] = it.applicationNameSuffix.orEmpty()
                    addConfigField(
                        "APP_VERSION",
                        version
                    )
                    flavorConfigurationBlock(this, it)
                }
            }
        }
    }
}
