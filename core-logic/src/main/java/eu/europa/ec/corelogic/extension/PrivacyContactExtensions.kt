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
import eu.europa.ec.corelogic.model.PrivacyContactDomain
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder

private const val HTTP_PREFIX = "http:"
private const val HTTPS_PREFIX = "https:"
private const val MAILTO_PREFIX = "mailto:"
private const val TEL_PREFIX = "tel:"

private val emailAddressPattern = Regex(
    // No leading, trailing or repeated dots before '@'.
    "^[A-Za-z0-9!$'*+/_=^-]+(?:\\.[A-Za-z0-9!$'*+/_=^-]+)*@" +
            // Require a dotted domain whose parts do not start or end with '-'.
            "[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?" +
            "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$"
)

// Allow short support numbers without assuming a country or number length.
private val phoneNumberPattern = Regex("^\\+?[0-9 ()-]+$")

/** Identifies the contact method; the value alone does not tell us what the contact is for. */
fun String.toPrivacyContactOrNull(): PrivacyContactDomain? {
    // Check before trimming so line breaks and tabs are rejected too.
    if (any { character -> character.isISOControl() }) {
        return null
    }

    val value = trim()
    val hasWebsitePrefix = value.startsWith(HTTPS_PREFIX, ignoreCase = true) ||
            value.startsWith(HTTP_PREFIX, ignoreCase = true)

    return when {
        hasWebsitePrefix -> value.toWebsiteContactOrNull()

        value.startsWith(MAILTO_PREFIX, ignoreCase = true) ->
            value.toMailtoContactOrNull()

        value.startsWith(TEL_PREFIX, ignoreCase = true) ->
            value.substring(TEL_PREFIX.length).toPhoneContactOrNull()

        else -> value.toEmailContactOrNull() ?: value.toPhoneContactOrNull()
    }
}

fun List<String>.toPrivacyContacts(): List<PrivacyContactDomain> =
    mapNotNull { contact -> contact.toPrivacyContactOrNull() }
        .distinctBy { contact -> contact.url }

/** Deletion requests prefer a website, followed by email and phone. */
fun List<PrivacyContactDomain>.preferredDataDeletionContact(): PrivacyContactDomain? =
    firstOrNull { contact -> contact.method == CommunicationMethodDomain.Website }
        ?: firstOrNull { contact -> contact.method == CommunicationMethodDomain.Email }
        ?: firstOrNull { contact -> contact.method == CommunicationMethodDomain.Phone }

private fun String.toWebsiteContactOrNull(): PrivacyContactDomain? {
    val uri = toUriOrNull() ?: return null

    if (uri.host.isNullOrBlank()) {
        return null
    }

    // Contact URLs must not contain a username or password.
    if (uri.userInfo != null) {
        return null
    }

    // URI uses -1 when no port is specified.
    if (uri.port != -1 && uri.port !in 1..65535) {
        return null
    }

    // An IPv6 interface name follows '%' and must keep its case.
    val hostPrefix = uri.rawAuthority.substringBefore('%')
    val schemeAndHost = "${uri.scheme}://$hostPrefix"

    // Change only this prefix, leaving encoded paths, queries and fragments intact.
    val normalizedUrl = uri.toASCIIString().replaceFirst(
        oldValue = schemeAndHost,
        newValue = schemeAndHost.lowercase(),
    )

    return PrivacyContactDomain(
        method = CommunicationMethodDomain.Website,
        url = normalizedUrl,
        displayValue = this,
    )
}

private fun String.toMailtoContactOrNull(): PrivacyContactDomain? {
    val uri = toUriOrNull() ?: return null

    // isOpaque excludes a leading slash after 'mailto:'. Fragments are not part of an address.
    if (!uri.isOpaque || uri.rawFragment != null) {
        return null
    }

    // URI decodes percent escapes once and leaves '+' unchanged.
    val emailAddress = uri.schemeSpecificPart
    return emailAddress.toEmailContactOrNull()
}

private fun String.toEmailContactOrNull(): PrivacyContactDomain? {
    if (!emailAddressPattern.matches(this)) {
        return null
    }

    val mailbox = substringBefore('@')
    val domain = substringAfter('@').lowercase()

    // Escape the mailbox for the URI, keeping '+' readable in tagged addresses.
    val encodedMailbox = URLEncoder.encode(mailbox, Charsets.UTF_8.name())
        .replace("%2B", "+")

    return PrivacyContactDomain(
        method = CommunicationMethodDomain.Email,
        url = "$MAILTO_PREFIX$encodedMailbox@$domain",
        displayValue = this,
    )
}

private fun String.toPhoneContactOrNull(): PrivacyContactDomain? {
    if (!phoneNumberPattern.matches(this)) {
        return null
    }

    // Remove formatting while keeping leading zeros and the optional '+'.
    val digits = filter { character -> character.isDigit() }
    if (digits.isEmpty()) {
        return null
    }

    val phoneNumber = if (startsWith('+')) "+$digits" else digits

    return PrivacyContactDomain(
        method = CommunicationMethodDomain.Phone,
        url = "$TEL_PREFIX$phoneNumber",
        displayValue = this,
    )
}

private fun String.toUriOrNull(): URI? = try {
    URI(this)
} catch (_: URISyntaxException) {
    null
}