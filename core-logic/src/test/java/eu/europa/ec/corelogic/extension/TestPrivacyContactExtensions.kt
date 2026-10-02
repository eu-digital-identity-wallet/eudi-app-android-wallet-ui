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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestPrivacyContactExtensions {

    //region toPrivacyContactOrNull

    // Case 1:
    // 1. A website has an uppercase scheme and encoded path/query content.
    //
    // Case 1 Expected Result:
    // The scheme is normalized without decoding or changing the destination.
    @Test
    fun `Given Case 1, When toPrivacyContactOrNull is called, Then Case 1 Expected Result is returned`() {
        // Given
        val value = " HTTPS://rp.example/a%2Fb?token=a%2Bb&next=%2Fform#details "

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(
            PrivacyContactDomain(
                method = CommunicationMethodDomain.Website,
                url = "https://rp.example/a%2Fb?token=a%2Bb&next=%2Fform#details",
                displayValue = value.trim(),
            ),
            result,
        )
    }

    // Case 2:
    // 1. An HTTP support URL includes an explicit port.
    //
    // Case 2 Expected Result:
    // The supported scheme and port remain unchanged.
    @Test
    fun `Given Case 2, When toPrivacyContactOrNull is called, Then Case 2 Expected Result is returned`() {
        // Given
        val value = "http://rp.example:8080/support"

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(CommunicationMethodDomain.Website, result?.method)
        assertEquals(value, result?.url)
    }

    // Case 3:
    // 1. A website path contains a non-ASCII character.
    //
    // Case 3 Expected Result:
    // The target is ASCII-encoded while the display text remains readable.
    @Test
    fun `Given Case 3, When toPrivacyContactOrNull is called, Then Case 3 Expected Result is returned`() {
        // Given
        val value = "https://rp.example/café"

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals("https://rp.example/caf%C3%A9", result?.url)
        assertEquals(value, result?.displayValue)
    }

    // Case 4:
    // 1. Website candidates lack a valid host, contain credentials or use unsupported targets.
    //
    // Case 4 Expected Result:
    // None becomes a contact channel.
    @Test
    fun `Given Case 4, When toPrivacyContactOrNull is called, Then Case 4 Expected Result is returned`() {
        // Given
        val values = listOf(
            "https:///support",
            "https://user:password@rp.example/support",
            "https://rp.example:65536/support",
            "https://rp.example:0/support",
            "https://rp.example/a b",
            "https://rp.example/%zz",
            "https:rp.example",
            "www.rp.example",
            "ftp://rp.example/support",
            "javascript:alert(1)",
            "content://rp.example/support",
            "file:///support",
            "intent://rp.example/support",
        )

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertTrue(results.all { contact -> contact == null })
    }

    // Case 5:
    // 1. A mailto contact contains a mixed-case domain and a tagged mailbox.
    //
    // Case 5 Expected Result:
    // Mailbox case and tag are preserved, the domain and scheme are normalized.
    @Test
    fun `Given Case 5, When toPrivacyContactOrNull is called, Then Case 5 Expected Result is returned`() {
        // Given
        val value = "MAILTO:Support+privacy@RP.EXAMPLE"

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(
            PrivacyContactDomain(
                method = CommunicationMethodDomain.Email,
                url = "mailto:Support+privacy@rp.example",
                displayValue = "Support+privacy@RP.EXAMPLE",
            ),
            result,
        )
    }

    // Case 6:
    // 1. A contact is a bare email address.
    //
    // Case 6 Expected Result:
    // It becomes a mailto target.
    @Test
    fun `Given Case 6, When toPrivacyContactOrNull is called, Then Case 6 Expected Result is returned`() {
        // Given
        val value = mockedEmailAddress

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(
            mockedEmail,
            result,
        )
    }

    // Case 7:
    // 1. Email candidates include query overrides, fragments, multiple recipients or malformed mailboxes.
    //
    // Case 7 Expected Result:
    // They are rejected rather than changing the generated draft.
    @Test
    fun `Given Case 7, When toPrivacyContactOrNull is called, Then Case 7 Expected Result is returned`() {
        // Given
        val values = listOf(
            "mailto:support@rp.example?bcc=other@example.org",
            "mailto:support@rp.example?subject=override",
            "mailto:support@rp.example#fragment",
            "mailto:support%0D%0ABcc:other@example.org",
            "mailto:support@rp.example,other@example.org",
            "mailto:support@rp.example;other@example.org",
            "Support <support@rp.example>",
            ".support@rp.example",
            "support..privacy@rp.example",
            "support.@rp.example",
            "support@-rp.example",
            "support@rp-.example",
            "support@rp",
            "support@@rp.example",
            "mailto:",
        )

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertTrue(results.all { contact -> contact == null })
    }

    // Case 8:
    // 1. A telephone contact contains visual separators.
    //
    // Case 8 Expected Result:
    // Dialing retains the country prefix and digits; display retains the readable formatting.
    @Test
    fun `Given Case 8, When toPrivacyContactOrNull is called, Then Case 8 Expected Result is returned`() {
        // Given
        val value = " TEL:" + mockedPhoneNumber + " "

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(mockedPhone, result)
    }

    // Case 9:
    // 1. Bare local phone numbers include leading zeros and a short support number.
    //
    // Case 9 Expected Result:
    // Digits are preserved without imposing a country or number-length rule.
    @Test
    fun `Given Case 9, When toPrivacyContactOrNull is called, Then Case 9 Expected Result is returned`() {
        // Given
        val values = listOf("020 1234 5678", "1234")

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertEquals(listOf("tel:02012345678", "tel:1234"), results.map { contact -> contact?.url })
        assertTrue(results.all { contact -> contact?.method == CommunicationMethodDomain.Phone })
    }

    // Case 10:
    // 1. Telephone candidates contain extensions, dialing commands or unsupported characters.
    //
    // Case 10 Expected Result:
    // They are rejected without stripping meaningful parts of the destination.
    @Test
    fun `Given Case 10, When toPrivacyContactOrNull is called, Then Case 10 Expected Result is returned`() {
        // Given
        val values = listOf(
            "tel:+302106475600;ext=123",
            "tel:+302106475600,123",
            "tel:+302106475600#123",
            "tel:*123#",
            "tel:++302106475600",
            "tel:30+2106475600",
            "tel:+30-CALL-ME",
            "tel:+٣٠٢١٠٦٤٧٥٦٠٠",
            "tel:()--",
            "tel:",
        )

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertTrue(results.all { contact -> contact == null })
    }

    // Case 11:
    // 1. Contacts contain control characters, including surrounding line breaks.
    //
    // Case 11 Expected Result:
    // Trimming does not turn them into accepted targets.
    @Test
    fun `Given Case 11, When toPrivacyContactOrNull is called, Then Case 11 Expected Result is returned`() {
        // Given
        val values = listOf(
            "\n" + mockedWebsiteUrl,
            mockedWebsiteUrl + "\r",
            "mailto:support@rp.example\nBcc:other@example.org",
            "tel:+30\t2106475600",
        )

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertTrue(results.all { contact -> contact == null })
    }

    // Case 12:
    // 1. Values contain no communication target.
    //
    // Case 12 Expected Result:
    // Blank text, country names and labels are omitted.
    @Test
    fun `Given Case 12, When toPrivacyContactOrNull is called, Then Case 12 Expected Result is returned`() {
        // Given
        val values = listOf("", "  ", "GR", "Greece", "Contact support")

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertTrue(results.all { contact -> contact == null })
    }

    // Case 13:
    // 1. A mailbox contains supported special characters.
    //
    // Case 13 Expected Result:
    // The email target is escaped correctly and can be read back without changing the address.
    @Test
    fun `Given Case 13, When toPrivacyContactOrNull is called, Then Case 13 Expected Result is returned`() {
        // Given
        val value = "Support!$'*+/_=^-privacy@rp.example"
        val expected = PrivacyContactDomain(
            method = CommunicationMethodDomain.Email,
            url = "mailto:Support%21%24%27*+%2F_%3D%5E-privacy@rp.example",
            displayValue = value,
        )

        // When
        val contact = value.toPrivacyContactOrNull()
        val decodedContact = contact?.url?.toPrivacyContactOrNull()

        // Then
        assertEquals(expected, contact)
        assertEquals(expected, decodedContact)
    }

    // Case 14:
    // 1. An email target mixes encoded characters, a literal plus and an uppercase domain.
    //
    // Case 14 Expected Result:
    // Both forms of plus remain part of the mailbox and the display address is decoded once.
    @Test
    fun `Given Case 14, When toPrivacyContactOrNull is called, Then Case 14 Expected Result is returned`() {
        // Given
        val value = "MAILTO:Support+privacy%2frequests%3dcopy%5Etag%2Bextra@RP.EXAMPLE"

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(
            PrivacyContactDomain(
                method = CommunicationMethodDomain.Email,
                url = "mailto:Support+privacy%2Frequests%3Dcopy%5Etag+extra@rp.example",
                displayValue = "Support+privacy/requests=copy^tag+extra@RP.EXAMPLE",
            ),
            result,
        )
    }

    // Case 15:
    // 1. Email targets contain malformed escapes, encoded unsupported content or extra URI parts.
    //
    // Case 15 Expected Result:
    // Decoding does not allow headers, extra recipients, control characters or a second decoding pass.
    @Test
    fun `Given Case 15, When toPrivacyContactOrNull is called, Then Case 15 Expected Result is returned`() {
        // Given
        val values = listOf(
            "mailto:support%2@rp.example",
            "mailto:support%GG@rp.example",
            "mailto:support%0D%0A@rp.example",
            "mailto:support%00@rp.example",
            "mailto:support%20privacy@rp.example",
            "mailto:support%3Fsubject=override@rp.example",
            "mailto:support%23privacy@rp.example",
            "mailto:support@rp.example%2Cother@example.org",
            "mailto:support@rp.example%3Bother@example.org",
            "mailto:support%252Bprivacy@rp.example",
            "mailto:support%2Bprivacy@rp.example?subject=override",
            "mailto:support%2Bprivacy@rp.example#fragment",
            "mailto:support%2Bprivacy@rp.example#",
            "mailto://support@rp.example",
            "mailto:///support@rp.example",
        )

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertTrue(results.all { contact -> contact == null })
    }

    // Case 16:
    // 1. A website has uppercase scheme and host, a port and encoded path/query content.
    //
    // Case 16 Expected Result:
    // Only scheme and host casing change; the destination and display text are preserved.
    @Test
    fun `Given Case 16, When toPrivacyContactOrNull is called, Then Case 16 Expected Result is returned`() {
        // Given
        val value = "HTTPS://RP.EXAMPLE:8443/Support/café/%2f?Token=a%2Bb&next=%2FForm#Details"

        // When
        val result = value.toPrivacyContactOrNull()

        // Then
        assertEquals(
            PrivacyContactDomain(
                method = CommunicationMethodDomain.Website,
                url = "https://rp.example:8443/Support/caf%C3%A9/%2f?Token=a%2Bb&next=%2FForm#Details",
                displayValue = value,
            ),
            result,
        )
    }

    // Case 17:
    // 1. IPv6 websites include an address with a named network interface.
    //
    // Case 17 Expected Result:
    // Host normalization preserves the interface name, port and path.
    @Test
    fun `Given Case 17, When toPrivacyContactOrNull is called, Then Case 17 Expected Result is returned`() {
        // Given
        val values = listOf(
            "HTTPS://[2001:DB8::AB]:8443/Support",
            "HTTPS://[FE80::AB%25Eth0]:8443/Support",
        )

        // When
        val results = values.map { value -> value.toPrivacyContactOrNull() }

        // Then
        assertEquals(
            listOf(
                "https://[2001:db8::ab]:8443/Support",
                "https://[fe80::ab%25Eth0]:8443/Support",
            ),
            results.map { contact -> contact?.url },
        )
        assertEquals(values, results.map { contact -> contact?.displayValue })
    }

    //endregion

    //region toPrivacyContacts

    // Case 1:
    // 1. Contacts contain invalid values and equivalent normalized targets.
    //
    // Case 1 Expected Result:
    // Valid targets keep their first occurrence and input order.
    @Test
    fun `Given Case 1, When toPrivacyContacts is called, Then Case 1 Expected Result is returned`() {
        // Given
        val values = listOf(
            "GR",
            mockedEmailAddress,
            "mailto:support@RP.EXAMPLE",
            mockedPhoneNumber,
            "tel:+302106475600",
            mockedWebsiteUrl,
            "HTTPS://rp.example/support",
        )

        // When
        val result = values.toPrivacyContacts()

        // Then
        assertEquals(listOf(mockedEmail, mockedPhone, mockedWebsite), result)
    }

    // Case 2:
    // 1. Two valid URLs differ in query contents.
    //
    // Case 2 Expected Result:
    // They remain separate destinations.
    @Test
    fun `Given Case 2, When toPrivacyContacts is called, Then Case 2 Expected Result is returned`() {
        // Given
        val values = listOf(mockedWebsiteUrl + "?form=one", mockedWebsiteUrl + "?form=two")

        // When
        val result = values.toPrivacyContacts()

        // Then
        assertEquals(values, result.map { contact -> contact.url })
    }

    // Case 3:
    // 1. Equivalent websites and email addresses use different casing and encoding.
    //
    // Case 3 Expected Result:
    // Each destination appears once, keeping the first contact's display text and list position.
    @Test
    fun `Given Case 3, When toPrivacyContacts is called, Then Case 3 Expected Result is returned`() {
        // Given
        val values = listOf(
            "HTTPS://RP.EXAMPLE/Support?Token=A%2BB",
            "https://rp.example/Support?Token=A%2BB",
            "mailto:support%2Bprivacy@RP.EXAMPLE",
            "support+privacy@rp.example",
        )

        // When
        val result = values.toPrivacyContacts()

        // Then
        assertEquals(
            listOf(
                PrivacyContactDomain(
                    method = CommunicationMethodDomain.Website,
                    url = "https://rp.example/Support?Token=A%2BB",
                    displayValue = values.first(),
                ),
                PrivacyContactDomain(
                    method = CommunicationMethodDomain.Email,
                    url = "mailto:support+privacy@rp.example",
                    displayValue = "support+privacy@RP.EXAMPLE",
                ),
            ),
            result,
        )
    }

    // Case 4:
    // 1. Websites differ only in path or query casing.
    //
    // Case 4 Expected Result:
    // All destinations remain separate.
    @Test
    fun `Given Case 4, When toPrivacyContacts is called, Then Case 4 Expected Result is returned`() {
        // Given
        val values = listOf(
            "https://rp.example/Support?token=A",
            "https://rp.example/support?token=A",
            "https://rp.example/support?token=a",
        )

        // When
        val result = values.toPrivacyContacts()

        // Then
        assertEquals(values, result.map { contact -> contact.url })
    }

    //endregion

    //region preferredDataDeletionContact

    // Case 1:
    // 1. Phone and email precede two support websites.
    //
    // Case 1 Expected Result:
    // The first website wins regardless of input method order.
    @Test
    fun `Given Case 1, When preferredDataDeletionContact is called, Then Case 1 Expected Result is returned`() {
        // Given
        val contacts = listOf(
            mockedPhone,
            mockedEmail,
            mockedWebsite,
            mockedWebsite.copy(url = "https://other.example/support"),
        )

        // When
        val result = contacts.preferredDataDeletionContact()

        // Then
        assertEquals(mockedWebsite, result)
    }

    // Case 2:
    // 1. There is no support website and phone precedes email.
    //
    // Case 2 Expected Result:
    // Email is selected.
    @Test
    fun `Given Case 2, When preferredDataDeletionContact is called, Then Case 2 Expected Result is returned`() {
        // Given
        val contacts = listOf(mockedPhone, mockedEmail)

        // When
        val result = contacts.preferredDataDeletionContact()

        // Then
        assertEquals(mockedEmail, result)
    }

    // Case 3:
    // 1. Only a phone contact is available.
    //
    // Case 3 Expected Result:
    // Phone is selected.
    @Test
    fun `Given Case 3, When preferredDataDeletionContact is called, Then Case 3 Expected Result is returned`() {
        // Given
        val contacts = listOf(mockedPhone)

        // When
        val result = contacts.preferredDataDeletionContact()

        // Then
        assertEquals(mockedPhone, result)
    }

    // Case 4:
    // 1. There are no valid support contacts.
    //
    // Case 4 Expected Result:
    // No communication method is fabricated.
    @Test
    fun `Given Case 4, When preferredDataDeletionContact is called, Then Case 4 Expected Result is returned`() {
        // Given
        val contacts = emptyList<PrivacyContactDomain>()

        // When
        val result = contacts.preferredDataDeletionContact()

        // Then
        assertNull(result)
    }

    // Case 5:
    // 1. A malformed website precedes a usable email address.
    //
    // Case 5 Expected Result:
    // The unusable website does not prevent email selection.
    @Test
    fun `Given Case 5, When preferredDataDeletionContact is called, Then Case 5 Expected Result is returned`() {
        // Given
        val contacts = listOf("https:///support", mockedEmailAddress)
            .toPrivacyContacts()

        // When
        val result = contacts.preferredDataDeletionContact()

        // Then
        assertEquals(mockedEmail, result)
    }

    //endregion

    //region mocked objects
    private val mockedWebsiteUrl = "https://rp.example/support"
    private val mockedEmailAddress = "support@rp.example"
    private val mockedPhoneNumber = "+30 (210) 647-5600"
    private val mockedWebsite = PrivacyContactDomain(
        method = CommunicationMethodDomain.Website,
        url = mockedWebsiteUrl,
        displayValue = mockedWebsiteUrl,
    )
    private val mockedEmail = PrivacyContactDomain(
        method = CommunicationMethodDomain.Email,
        url = "mailto:" + mockedEmailAddress,
        displayValue = mockedEmailAddress,
    )
    private val mockedPhone = PrivacyContactDomain(
        method = CommunicationMethodDomain.Phone,
        url = "tel:+302106475600",
        displayValue = mockedPhoneNumber,
    )
    //endregion
}