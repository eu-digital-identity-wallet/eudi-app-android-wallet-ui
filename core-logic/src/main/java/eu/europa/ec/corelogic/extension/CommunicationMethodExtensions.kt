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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.CommunicationMethodDomain

private const val COMMUNICATION_METHOD_WEBSITE = "website"
private const val COMMUNICATION_METHOD_EMAIL = "email"
private const val COMMUNICATION_METHOD_PHONE = "phone"

internal fun CommunicationMethodDomain.toStoredCommunicationMethod(): String = when (this) {
    CommunicationMethodDomain.Website -> COMMUNICATION_METHOD_WEBSITE
    CommunicationMethodDomain.Email -> COMMUNICATION_METHOD_EMAIL
    CommunicationMethodDomain.Phone -> COMMUNICATION_METHOD_PHONE
}

internal fun String?.toCommunicationMethodDomainOrNull(): CommunicationMethodDomain? = when (this) {
    COMMUNICATION_METHOD_WEBSITE -> CommunicationMethodDomain.Website
    COMMUNICATION_METHOD_EMAIL -> CommunicationMethodDomain.Email
    COMMUNICATION_METHOD_PHONE -> CommunicationMethodDomain.Phone
    else -> null
}