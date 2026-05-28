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

package eu.europa.ec.uilogic.serializer

/**
 * Marker interface for any model that can be round-tripped through a navigation argument
 * via [UiSerializer]. Implementations must additionally be annotated with
 * `@kotlinx.serialization.Serializable`.
 */
interface UiSerializable

/**
 * Carries the navigation-argument key used to embed a [UiSerializable] payload in a
 * route string. Each [UiSerializable] type declares one of these as its companion so
 * call sites can do `mapOf(MyConfig.serializedKeyName to uiSerializer.toBase64(...))`.
 */
interface UiSerializableParser {
    val serializedKeyName: String
}