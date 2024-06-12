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

import org.gradle.api.Project
import java.util.Properties

@Suppress("UNCHECKED_CAST")
fun <T> Project.getProperty(key: String, fileName: String = "local.properties"): T? {
    return try {
        val properties = Properties().apply {
            load(rootProject.file(fileName).reader())
        }
        properties[key] as? T
    } catch (_: Exception) {
        null
    }
}