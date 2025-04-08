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

package eu.europa.ec.storagelogic.model

import eu.europa.ec.storagelogic.model.type.StoredObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmRevokedDocument : RealmObject {
    @PrimaryKey
    var identifier: String = ""
}

data class RevokedDocument(
    val identifier: String
) : StoredObject

internal fun RevokedDocument.toRealm() = RealmRevokedDocument().apply {
    identifier = this@toRealm.identifier
}

internal fun RealmRevokedDocument?.toRevokedDocument() = this?.let {
    RevokedDocument(
        it.identifier
    )
}

internal fun List<RealmRevokedDocument>.toRevokedDocuments() = this.map {
    RevokedDocument(
        it.identifier
    )
}