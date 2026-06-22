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

package eu.europa.ec.commonfeature.util

import eu.europa.ec.businesslogic.extension.encodeToBase64String
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransformToUiItemsStrings
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedMdocClaim
import eu.europa.ec.testfeature.util.mockedMdocMdlNameSpace
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedSdJwtVcClaim
import eu.europa.ec.testfeature.util.mockedUuid
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class TestDocumentHelper {

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uuidProvider: UuidProvider

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        mockTransformToUiItemsStrings(resourceProvider)
        whenever(uuidProvider.provideUuid()).thenReturn(mockedUuid)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region extractValueFromDocumentOrEmpty

    // Case 1:
    // 1. The document holds an mdoc claim whose identifier matches the requested key.
    //
    // Case 1 Expected Result:
    // The claim's value, stringified.
    @Test
    fun `Given Case 1, When extractValueFromDocumentOrEmpty is called, Then Case 1 Expected Result is returned`() {
        // Given
        val document = mockedMdocDocument(
            mockedMdocClaim(dataElementName = "family_name", value = "ANDERSSON"),
        )

        // When
        val result = extractValueFromDocumentOrEmpty(
            document = document,
            key = "family_name",
        )

        // Then
        assertEquals("ANDERSSON", result)
    }

    // Case 2:
    // 1. The document holds no claim matching the requested key.
    //
    // Case 2 Expected Result:
    // The empty string.
    @Test
    fun `Given Case 2, When extractValueFromDocumentOrEmpty is called, Then Case 2 Expected Result is returned`() {
        // Given
        val document = mockedMdocDocument(
            mockedMdocClaim(dataElementName = "family_name", value = "ANDERSSON"),
        )

        // When
        val result = extractValueFromDocumentOrEmpty(
            document = document,
            key = "given_name",
        )

        // Then
        assertEquals("", result)
    }

    //endregion

    //region keyIsUserImage

    @Test
    fun `Given the user-image keys, When keyIsUserImage is called, Then only they classify as user images`() {
        // When / Then
        assertTrue(keyIsUserImage(key = "portrait"))
        assertTrue(keyIsUserImage(key = "picture"))
        assertFalse(keyIsUserImage(key = "family_name"))
    }

    //endregion

    //region keyIsSignature

    @Test
    fun `Given the signature key, When keyIsSignature is called, Then only it classifies as a signature`() {
        // When / Then
        assertTrue(keyIsSignature(key = "signature_usual_mark"))
        assertFalse(keyIsSignature(key = "portrait"))
    }

    //endregion

    //region getReadableNameFromIdentifier

    // Case 1:
    // 1. claimMetaData is null.
    //
    // Case 1 Expected Result:
    // The fallback string.
    @Test
    fun `Given Case 1, When getReadableNameFromIdentifier is called, Then Case 1 Expected Result is returned`() {
        // When
        val result = getReadableNameFromIdentifier(
            claimMetaData = null,
            userLocale = mockedDefaultLocale,
            fallback = "family_name",
        )

        // Then
        assertEquals("family_name", result)
    }

    // Case 2:
    // 1. claimMetaData carries a display whose locale language matches the user's.
    //
    // Case 2 Expected Result:
    // That display's name.
    @Test
    fun `Given Case 2, When getReadableNameFromIdentifier is called, Then Case 2 Expected Result is returned`() {
        // Given
        val claimMetaData = mockedClaimMetaData(
            mockedClaimDisplay(name = "Family name", locale = Locale.ENGLISH),
        )

        // When
        val result = getReadableNameFromIdentifier(
            claimMetaData = claimMetaData,
            userLocale = mockedDefaultLocale,
            fallback = "family_name",
        )

        // Then
        assertEquals("Family name", result)
    }

    // Case 3:
    // 1. claimMetaData carries only a display whose locale language does NOT match the user's.
    //
    // Case 3 Expected Result:
    // The FIRST display's name, not the fallback string.
    @Test
    fun `Given Case 3, When getReadableNameFromIdentifier is called, Then Case 3 Expected Result is returned`() {
        // Given
        val claimMetaData = mockedClaimMetaData(
            mockedClaimDisplay(name = "Επώνυμο", locale = Locale.forLanguageTag("el")),
        )

        // When
        val result = getReadableNameFromIdentifier(
            claimMetaData = claimMetaData,
            userLocale = mockedDefaultLocale,
            fallback = "family_name",
        )

        // Then
        assertEquals("Επώνυμο", result)
    }

    // Case 4:
    // 1. claimMetaData carries displays for SEVERAL locales including the user's, with the
    //    user's-language entry deliberately listed last.
    //
    // Case 4 Expected Result:
    // The user's-locale name — resolution picks by language, not by list position.
    @Test
    fun `Given Case 4, When getReadableNameFromIdentifier is called, Then Case 4 Expected Result is returned`() {
        // Given
        val claimMetaData = mockedTranslatedClaimMetaData(
            englishName = "Family name",
            "el" to "Επώνυμο",
            "sv" to "Efternamn",
        )

        // When
        val result = getReadableNameFromIdentifier(
            claimMetaData = claimMetaData,
            userLocale = mockedDefaultLocale,
            fallback = "family_name",
        )

        // Then
        assertEquals("Family name", result)
    }

    //endregion

    //region createKeyValue

    // Case 1:
    // 1. item is a plain (non-date) String.
    //
    // Case 1 Expected Result:
    // One Primitive: key = groupKey, displayTitle = the metadata-resolved name (fallback =
    // groupKey, metadata null), path = the disclosure path, value = the string as-is.
    @Test
    fun `Given Case 1, When createKeyValue is called, Then Case 1 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = "Some value",
            groupKey = "test_claim",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Primitive(
                    key = "test_claim",
                    displayTitle = "test_claim",
                    path = mockedMdocPidDisclosurePath,
                    isRequired = false,
                    value = "Some value",
                )
            ),
            allItems,
        )
    }

    // Case 2:
    // 1. item is a date String ("1985-03-30").
    //
    // Case 2 Expected Result:
    // One Primitive whose value is the formatted date ("30 Mar 1985").
    @Test
    fun `Given Case 2, When createKeyValue is called, Then Case 2 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = "1985-03-30",
            groupKey = "birth_date",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals("30 Mar 1985", (allItems.single() as ClaimDomain.Primitive).value)
    }

    // Case 3:
    // 1. item is a java.time.LocalDate.
    //
    // Case 3 Expected Result:
    // One Primitive whose value is the formatted date.
    @Test
    fun `Given Case 3, When createKeyValue is called, Then Case 3 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = LocalDate.of(1985, 3, 30),
            groupKey = "birth_date",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals("30 Mar 1985", (allItems.single() as ClaimDomain.Primitive).value)
    }

    // Case 4:
    // 1. item is a Boolean (true, then false on a second invocation).
    //
    // Case 4 Expected Result:
    // The readable boolean strings ("yes" / "no" as stubbed).
    @Test
    fun `Given Case 4, When createKeyValue is called, Then Case 4 Expected Result is returned`() {
        // Given
        val trueItems = mutableListOf<ClaimDomain>()
        val falseItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = true,
            groupKey = "age_over_18",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = trueItems,
        )
        createKeyValue(
            item = false,
            groupKey = "age_over_65",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = falseItems,
        )

        // Then
        assertEquals("yes", (trueItems.single() as ClaimDomain.Primitive).value)
        assertEquals("no", (falseItems.single() as ClaimDomain.Primitive).value)
    }

    // Case 5:
    // 1. groupKey is a gender key ("sex"); item is a known gender code ("1"), then an
    //    unmapped code ("5") on a second invocation.
    //
    // Case 5 Expected Result:
    // The mapped readable value ("Male") for the known code; the raw code for the unmapped
    // one.
    @Test
    fun `Given Case 5, When createKeyValue is called, Then Case 5 Expected Result is returned`() {
        // Given
        val mappedItems = mutableListOf<ClaimDomain>()
        val unmappedItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = "1",
            groupKey = "sex",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = mappedItems,
        )
        createKeyValue(
            item = "5",
            groupKey = "sex",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = unmappedItems,
        )

        // Then
        assertEquals("Male", (mappedItems.single() as ClaimDomain.Primitive).value)
        assertEquals("5", (unmappedItems.single() as ClaimDomain.Primitive).value)
    }

    // Case 6:
    // 1. groupKey is the user-pseudonym key; item is a Base64 string.
    //
    // Case 6 Expected Result:
    // The decoded value ("John Doe").
    @Test
    fun `Given Case 6, When createKeyValue is called, Then Case 6 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = "Sm9obiBEb2U",
            groupKey = "user_pseudonym",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals("John Doe", (allItems.single() as ClaimDomain.Primitive).value)
    }

    // Case 7:
    // 1. item is a ByteArray (e.g. a portrait).
    //
    // Case 7 Expected Result:
    // One Primitive whose value is the Base64 form of the bytes.
    @Test
    fun `Given Case 7, When createKeyValue is called, Then Case 7 Expected Result is returned`() {
        // Given
        val bytes = "test".toByteArray()
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = bytes,
            groupKey = "portrait",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            bytes.encodeToBase64String(),
            (allItems.single() as ClaimDomain.Primitive).value,
        )
    }

    // Case 8:
    // 1. item is a Map whose children are all named primitives (no nested collections),
    //    e.g. place_of_birth { locality, country }.
    //
    // Case 8 Expected Result:
    // ONE Group keyed by groupKey wrapping one Primitive per map entry, each keyed and
    // titled by its own child key.
    @Test
    fun `Given Case 8, When createKeyValue is called, Then Case 8 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = mapOf(
                "locality" to "KATRINEHOLM",
                "country" to "SE",
            ),
            groupKey = "place_of_birth",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "place_of_birth",
                    displayTitle = "place_of_birth",
                    path = mockedMdocPidUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "locality",
                            displayTitle = "locality",
                            path = mockedMdocPidDisclosurePath,
                            isRequired = false,
                            value = "KATRINEHOLM",
                        ),
                        ClaimDomain.Primitive(
                            key = "country",
                            displayTitle = "country",
                            path = mockedMdocPidDisclosurePath,
                            isRequired = false,
                            value = "SE",
                        ),
                    ),
                )
            ),
            allItems,
        )
    }

    // Case 9:
    // 1. item is a Map with one named primitive AND one nested collection ("codes").
    //
    // Case 9 Expected Result:
    // NO wrapper group: the named primitive and the collection's own Group are added flat.
    @Test
    fun `Given Case 9, When createKeyValue is called, Then Case 9 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = mapOf(
                "vehicle_category_code" to "A",
                "codes" to listOf("S01"),
            ),
            groupKey = "driving_privileges",
            disclosurePath = mockedMdocMdlDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf(
                ClaimDomain.Primitive(
                    key = "vehicle_category_code",
                    displayTitle = "vehicle_category_code",
                    path = mockedMdocMdlDisclosurePath,
                    isRequired = false,
                    value = "A",
                ),
                ClaimDomain.Group(
                    key = "codes",
                    displayTitle = "codes",
                    path = mockedMdlUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "codes",
                            displayTitle = "codes",
                            path = mockedMdocMdlDisclosurePath,
                            isRequired = false,
                            value = "S01",
                        ),
                    ),
                ),
            ),
            allItems,
        )
    }

    // Case 10:
    // 1. item is a record-level Collection of primitives.
    //
    // Case 10 Expected Result:
    // ONE Group keyed by groupKey wrapping one Primitive per element (no numbered
    // sub-groups — each entry produces a single child).
    @Test
    fun `Given Case 10, When createKeyValue is called, Then Case 10 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = listOf("SE", "DK"),
            groupKey = "nationality",
            disclosurePath = mockedMdocPidDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "nationality",
                    displayTitle = "nationality",
                    path = mockedMdocPidUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "nationality",
                            displayTitle = "nationality",
                            path = mockedMdocPidDisclosurePath,
                            isRequired = false,
                            value = "SE",
                        ),
                        ClaimDomain.Primitive(
                            key = "nationality",
                            displayTitle = "nationality",
                            path = mockedMdocPidDisclosurePath,
                            isRequired = false,
                            value = "DK",
                        ),
                    ),
                )
            ),
            allItems,
        )
    }

    // Case 11:
    // 1. item is a record-level Collection of TWO maps, each holding a named primitive plus
    //    a nested collection — the mDL driving_privileges shape.
    //
    // Case 11 Expected Result:
    // One outer Group keyed by groupKey, containing one NUMBERED sub-group per entry
    // ("driving_privileges-1" / "-2", titled "driving_privileges 1" / "… 2"), each holding
    // that entry's flat children (the map-in-list expansion of Case 9).
    @Test
    fun `Given Case 11, When createKeyValue is called, Then Case 11 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = listOf(
                mapOf(
                    "vehicle_category_code" to "A",
                    "codes" to listOf("S01"),
                ),
                mapOf(
                    "vehicle_category_code" to "B",
                    "codes" to listOf("S02"),
                ),
            ),
            groupKey = "driving_privileges",
            disclosurePath = mockedMdocMdlDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "driving_privileges",
                    displayTitle = "driving_privileges",
                    path = mockedMdlUiGroupPath,
                    items = listOf(
                        ClaimDomain.Group(
                            key = "driving_privileges-1",
                            displayTitle = "driving_privileges 1",
                            path = mockedMdlUiGroupPath,
                            items = listOf(
                                ClaimDomain.Primitive(
                                    key = "vehicle_category_code",
                                    displayTitle = "vehicle_category_code",
                                    path = mockedMdocMdlDisclosurePath,
                                    isRequired = false,
                                    value = "A",
                                ),
                                ClaimDomain.Group(
                                    key = "codes",
                                    displayTitle = "codes",
                                    path = mockedMdlUiGroupPath,
                                    items = listOf(
                                        ClaimDomain.Primitive(
                                            key = "codes",
                                            displayTitle = "codes",
                                            path = mockedMdocMdlDisclosurePath,
                                            isRequired = false,
                                            value = "S01",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                        ClaimDomain.Group(
                            key = "driving_privileges-2",
                            displayTitle = "driving_privileges 2",
                            path = mockedMdlUiGroupPath,
                            items = listOf(
                                ClaimDomain.Primitive(
                                    key = "vehicle_category_code",
                                    displayTitle = "vehicle_category_code",
                                    path = mockedMdocMdlDisclosurePath,
                                    isRequired = false,
                                    value = "B",
                                ),
                                ClaimDomain.Group(
                                    key = "codes",
                                    displayTitle = "codes",
                                    path = mockedMdlUiGroupPath,
                                    items = listOf(
                                        ClaimDomain.Primitive(
                                            key = "codes",
                                            displayTitle = "codes",
                                            path = mockedMdocMdlDisclosurePath,
                                            isRequired = false,
                                            value = "S02",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
            ),
            allItems,
        )
    }

    // Case 12:
    // 1. The same map-with-collection entry as Case 11, but as a SINGLE-element list.
    //
    // Case 12 Expected Result:
    // No numbered sub-group (numbering requires a multi-element list): one outer Group
    // wrapping the entry's flat children directly.
    @Test
    fun `Given Case 12, When createKeyValue is called, Then Case 12 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = listOf(
                mapOf(
                    "vehicle_category_code" to "A",
                    "codes" to listOf("S01"),
                ),
            ),
            groupKey = "driving_privileges",
            disclosurePath = mockedMdocMdlDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        val outerGroup = allItems.single() as ClaimDomain.Group
        assertEquals("driving_privileges", outerGroup.key)
        assertEquals(
            listOf("vehicle_category_code", "codes"),
            outerGroup.items.map { it.key },
        )
    }

    // Case 13:
    // 1. An SD-JWT-typed disclosure path; item is a plain String.
    //
    // Case 13 Expected Result:
    // One Primitive carrying the SD-JWT-typed path unchanged.
    @Test
    fun `Given Case 13, When createKeyValue is called, Then Case 13 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = "Some value",
            groupKey = "test_claim",
            disclosurePath = mockedSdJwtDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Primitive(
                    key = "test_claim",
                    displayTitle = "test_claim",
                    path = mockedSdJwtDisclosurePath,
                    isRequired = false,
                    value = "Some value",
                )
            ),
            allItems,
        )
    }

    // Case 14:
    // 1. An SD-JWT-typed disclosure path; item is a record-level collection of primitives.
    //
    // Case 14 Expected Result:
    // The same wrapper-Group shape as the mdoc Case 10, but every path — including the
    // UI-group path — is SD-JWT-typed.
    @Test
    fun `Given Case 14, When createKeyValue is called, Then Case 14 Expected Result is returned`() {
        // Given
        val allItems = mutableListOf<ClaimDomain>()

        // When
        createKeyValue(
            item = listOf("SE", "DK"),
            groupKey = "nationalities",
            disclosurePath = mockedSdJwtDisclosurePath,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            claimMetaData = null,
            allItems = allItems,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "nationalities",
                    displayTitle = "nationalities",
                    path = mockedUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "nationalities",
                            displayTitle = "nationalities",
                            path = mockedSdJwtDisclosurePath,
                            isRequired = false,
                            value = "SE",
                        ),
                        ClaimDomain.Primitive(
                            key = "nationalities",
                            displayTitle = "nationalities",
                            path = mockedSdJwtDisclosurePath,
                            isRequired = false,
                            value = "DK",
                        ),
                    ),
                )
            ),
            allItems,
        )
    }

    //endregion

    //region documentHasExpired

    // Case 1:
    // 1. The current date is after the expiration date.
    //
    // Case 1 Expected Result:
    // true.
    @Test
    fun `Given Case 1, When documentHasExpired is called, Then Case 1 Expected Result is returned`() {
        // When
        val result = documentHasExpired(
            documentExpirationDate = Instant.parse("2024-01-01T00:00:00Z"),
            currentDate = LocalDate.of(2024, 1, 2),
            zoneId = ZoneId.of("UTC"),
        )

        // Then
        assertTrue(result)
    }

    // Case 2:
    // 1. The current date is before the expiration date.
    //
    // Case 2 Expected Result:
    // false.
    @Test
    fun `Given Case 2, When documentHasExpired is called, Then Case 2 Expected Result is returned`() {
        // When
        val result = documentHasExpired(
            documentExpirationDate = Instant.parse("2024-01-01T00:00:00Z"),
            currentDate = LocalDate.of(2023, 12, 31),
            zoneId = ZoneId.of("UTC"),
        )

        // Then
        assertFalse(result)
    }

    // Case 3:
    // 1. The current date IS the expiration date.
    //
    // Case 3 Expected Result:
    // false — a document expires strictly after its expiration day, not on it.
    @Test
    fun `Given Case 3, When documentHasExpired is called, Then Case 3 Expected Result is returned`() {
        // When
        val result = documentHasExpired(
            documentExpirationDate = Instant.parse("2024-01-01T12:00:00Z"),
            currentDate = LocalDate.of(2024, 1, 1),
            zoneId = ZoneId.of("UTC"),
        )

        // Then
        assertFalse(result)
    }

    //endregion

    //region transformPathsToDomainClaims

    // Case 1:
    // 1. Two flat mdoc claims, supplied in NON-alphabetical order, both requested by path.
    //
    // Case 1 Expected Result:
    // One Primitive per claim, sorted alphabetically by display title, each carrying its
    // own disclosure path and stringified value.
    @Test
    fun `Given Case 1, When transformPathsToDomainClaims is called, Then Case 1 Expected Result is returned`() {
        // Given
        val givenNamePath = mdocPath(dataElementName = "given_name")
        val familyNamePath = mdocPath(dataElementName = "family_name")

        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(givenNamePath, familyNamePath),
            claims = listOf(
                mockedMdocClaim(dataElementName = "given_name", value = "JAN"),
                mockedMdocClaim(dataElementName = "family_name", value = "ANDERSSON"),
            ),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Primitive(
                    key = "family_name",
                    displayTitle = "family_name",
                    path = familyNamePath,
                    isRequired = false,
                    value = "ANDERSSON",
                ),
                ClaimDomain.Primitive(
                    key = "given_name",
                    displayTitle = "given_name",
                    path = givenNamePath,
                    isRequired = false,
                    value = "JAN",
                ),
            ),
            result,
        )
    }

    // Case 2:
    // 1. A nested SD-JWT claim (place_of_birth -> locality), requested by its full path.
    //
    // Case 2 Expected Result:
    // A Group for the parent segment (path = the one-segment prefix) wrapping the leaf
    // Primitive (path = the full disclosure path).
    @Test
    fun `Given Case 2, When transformPathsToDomainClaims is called, Then Case 2 Expected Result is returned`() {
        // Given
        val disclosurePath = ClaimPathDomain.ofPlainKeys(
            names = listOf("place_of_birth", "locality"),
            type = ClaimType.SdJwtVc,
        )
        val placeOfBirth = mockedSdJwtVcClaim(
            pathElement = ClaimPathElement.Claim("place_of_birth"),
            children = listOf(
                mockedSdJwtVcClaim(
                    pathElement = ClaimPathElement.Claim("locality"),
                    value = "KATRINEHOLM",
                ),
            ),
        )

        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(disclosurePath),
            claims = listOf(placeOfBirth),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "place_of_birth",
                    displayTitle = "place_of_birth",
                    path = ClaimPathDomain.ofPlainKeys(
                        names = listOf("place_of_birth"),
                        type = ClaimType.SdJwtVc,
                    ),
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "locality",
                            displayTitle = "locality",
                            path = disclosurePath,
                            isRequired = false,
                            value = "KATRINEHOLM",
                        ),
                    ),
                )
            ),
            result,
        )
    }

    // Case 3:
    // 1. A requested path that matches no stored claim.
    //
    // Case 3 Expected Result:
    // An empty list — nothing is invented for an unmatched path.
    @Test
    fun `Given Case 3, When transformPathsToDomainClaims is called, Then Case 3 Expected Result is returned`() {
        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(mdocPath(dataElementName = "given_name")),
            claims = listOf(
                mockedMdocClaim(dataElementName = "family_name", value = "ANDERSSON"),
            ),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(emptyList<ClaimDomain>(), result)
    }

    // Case 4:
    // 1. A matched claim whose stored value is null.
    //
    // Case 4 Expected Result:
    // An empty list — a null value contributes no node.
    @Test
    fun `Given Case 4, When transformPathsToDomainClaims is called, Then Case 4 Expected Result is returned`() {
        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(mdocPath(dataElementName = "family_name")),
            claims = listOf(
                mockedMdocClaim(dataElementName = "family_name", value = null),
            ),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(emptyList<ClaimDomain>(), result)
    }

    // Case 5:
    // 1. A THREE-level nested SD-JWT claim (address -> region -> municipality), requested
    //    by its full path.
    // 2. Every level carries issuer-metadata translations — the "address" level with
    //    MULTIPLE locales (Greek + the user's English, English last).
    //
    // Case 5 Expected Result:
    // A Group chain mirroring the nesting — each Group's path is the corresponding path
    // PREFIX, each display title is the level's user-locale translation — ending in the
    // leaf Primitive (full path, translated title, raw value).
    @Test
    fun `Given Case 5, When transformPathsToDomainClaims is called, Then Case 5 Expected Result is returned`() {
        // Given
        val disclosurePath = ClaimPathDomain.ofPlainKeys(
            names = listOf("address", "region", "municipality"),
            type = ClaimType.SdJwtVc,
        )
        val address = mockedSdJwtVcClaim(
            pathElement = ClaimPathElement.Claim("address"),
            issuerMetadata = mockedTranslatedClaimMetaData(
                englishName = "Address",
                "el" to "Διεύθυνση",
            ),
            children = listOf(
                mockedSdJwtVcClaim(
                    pathElement = ClaimPathElement.Claim("region"),
                    issuerMetadata = mockedTranslatedClaimMetaData(englishName = "Region"),
                    children = listOf(
                        mockedSdJwtVcClaim(
                            pathElement = ClaimPathElement.Claim("municipality"),
                            issuerMetadata = mockedTranslatedClaimMetaData(englishName = "Municipality"),
                            value = "KATRINEHOLM",
                        ),
                    ),
                ),
            ),
        )

        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(disclosurePath),
            claims = listOf(address),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "address",
                    displayTitle = "Address",
                    path = ClaimPathDomain.ofPlainKeys(
                        names = listOf("address"),
                        type = ClaimType.SdJwtVc,
                    ),
                    items = listOf(
                        ClaimDomain.Group(
                            key = "region",
                            displayTitle = "Region",
                            path = ClaimPathDomain.ofPlainKeys(
                                names = listOf("address", "region"),
                                type = ClaimType.SdJwtVc,
                            ),
                            items = listOf(
                                ClaimDomain.Primitive(
                                    key = "municipality",
                                    displayTitle = "Municipality",
                                    path = disclosurePath,
                                    isRequired = false,
                                    value = "KATRINEHOLM",
                                ),
                            ),
                        ),
                    ),
                )
            ),
            result,
        )
    }

    // Case 6:
    // 1. An SD-JWT ARRAY claim (nationalities) whose children carry ArrayElement path
    //    elements, requested per element via typed Index paths.
    // 2. The parent claim carries multi-locale translations; the array-element children
    //    have NULL metadata.
    //
    // Case 6 Expected Result:
    // ONE Group for the array envelope (translated title; both paths fold into it) holding
    // one Primitive per element — keyed/titled by the index's string form ("0"/"1"), each
    // carrying its full typed Index path and element value.
    @Test
    fun `Given Case 6, When transformPathsToDomainClaims is called, Then Case 6 Expected Result is returned`() {
        // Given
        val firstElementPath = ClaimPathDomain(
            segments = listOf(ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)),
            type = ClaimType.SdJwtVc,
        )
        val secondElementPath = ClaimPathDomain(
            segments = listOf(ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(1)),
            type = ClaimType.SdJwtVc,
        )
        val nationalities = mockedSdJwtVcClaim(
            pathElement = ClaimPathElement.Claim("nationalities"),
            issuerMetadata = mockedTranslatedClaimMetaData(
                englishName = "Nationalities",
                "el" to "Υπηκοότητες",
            ),
            children = listOf(
                mockedSdJwtVcClaim(pathElement = ClaimPathElement.ArrayElement(0), value = "SE"),
                mockedSdJwtVcClaim(pathElement = ClaimPathElement.ArrayElement(1), value = "DK"),
            ),
        )

        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(firstElementPath, secondElementPath),
            claims = listOf(nationalities),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "nationalities",
                    displayTitle = "Nationalities",
                    path = ClaimPathDomain.ofPlainKeys(
                        names = listOf("nationalities"),
                        type = ClaimType.SdJwtVc,
                    ),
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "0",
                            displayTitle = "0",
                            path = firstElementPath,
                            isRequired = false,
                            value = "SE",
                        ),
                        ClaimDomain.Primitive(
                            key = "1",
                            displayTitle = "1",
                            path = secondElementPath,
                            isRequired = false,
                            value = "DK",
                        ),
                    ),
                )
            ),
            result,
        )
    }

    // Case 7:
    // 1. An mdoc claim whose stored VALUE is a structured map with all-named children
    //    (place_of_birth { locality, country }).
    // 2. The claim carries multi-locale translations.
    //
    // Case 7 Expected Result:
    // One wrapper Group titled with the user-locale translation,
    // mdoc-typed UI-group path, children sorted alphabetically.
    @Test
    fun `Given Case 7, When transformPathsToDomainClaims is called, Then Case 7 Expected Result is returned`() {
        // Given
        val disclosurePath = mdocPath(dataElementName = "place_of_birth")

        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(disclosurePath),
            claims = listOf(
                mockedMdocClaim(
                    dataElementName = "place_of_birth",
                    value = mapOf(
                        "locality" to "KATRINEHOLM",
                        "country" to "SE",
                    ),
                    issuerMetadata = mockedTranslatedClaimMetaData(
                        englishName = "Place of birth",
                        "el" to "Τόπος γέννησης",
                        "sv" to "Födelseort",
                    ),
                ),
            ),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "place_of_birth",
                    displayTitle = "Place of birth",
                    path = mockedMdocPidUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "country",
                            displayTitle = "country",
                            path = disclosurePath,
                            isRequired = false,
                            value = "SE",
                        ),
                        ClaimDomain.Primitive(
                            key = "locality",
                            displayTitle = "locality",
                            path = disclosurePath,
                            isRequired = false,
                            value = "KATRINEHOLM",
                        ),
                    ),
                )
            ),
            result,
        )
    }

    // Case 8:
    // 1. An mdoc claim shaped like a real stored mDL `driving_privileges` element: an ARRAY of
    //    two entry maps, each holding a `codes` ARRAY OF MAPS (one + three named fields),
    //    two date strings, and a primitive — array > map > array > map.
    // 2. The claim carries issuer metadata with an English display ("Driving Privileges").
    //
    // Case 8 Expected Result:
    // One outer Group with the TRANSLATED title; one NUMBERED sub-group per entry; each
    // codes element wraps as its own sibling "codes" Group; date strings render formatted;
    // every level sorted by title.
    @Test
    fun `Given Case 8, When transformPathsToDomainClaims is called, Then Case 8 Expected Result is returned`() {
        // Given
        val disclosurePath = mockedMdocMdlDisclosurePath

        // When
        val result = transformPathsToDomainClaims(
            paths = listOf(disclosurePath),
            claims = listOf(
                mockedMdocClaim(
                    dataElementName = "driving_privileges",
                    nameSpace = mockedMdocMdlNameSpace,
                    value = listOf(
                        mapOf(
                            "codes" to listOf(
                                mapOf("code" to "78"),
                                mapOf("code" to "S02", "sign" to "<=", "value" to "5"),
                            ),
                            "issue_date" to "2000-01-01",
                            "expiry_date" to "2040-12-31",
                            "vehicle_category_code" to "B",
                        ),
                        mapOf(
                            "codes" to listOf(
                                mapOf("code" to "S03", "sign" to "<=", "value" to "125"),
                            ),
                            "issue_date" to "2000-01-01",
                            "expiry_date" to "2040-12-31",
                            "vehicle_category_code" to "A",
                        ),
                    ),
                    issuerMetadata = mockedTranslatedClaimMetaData(
                        englishName = "Driving Privileges",
                    ),
                ),
            ),
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        // Then
        val expectedFirstEntryGroup = ClaimDomain.Group(
            key = "driving_privileges-1",
            displayTitle = "Driving Privileges 1",
            path = mockedMdlUiGroupPath,
            items = listOf(
                ClaimDomain.Group(
                    key = "codes",
                    displayTitle = "codes",
                    path = mockedMdlUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "code",
                            displayTitle = "code",
                            path = disclosurePath,
                            isRequired = false,
                            value = "78",
                        ),
                    ),
                ),
                ClaimDomain.Group(
                    key = "codes",
                    displayTitle = "codes",
                    path = mockedMdlUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "code",
                            displayTitle = "code",
                            path = disclosurePath,
                            isRequired = false,
                            value = "S02",
                        ),
                        ClaimDomain.Primitive(
                            key = "sign",
                            displayTitle = "sign",
                            path = disclosurePath,
                            isRequired = false,
                            value = "<=",
                        ),
                        ClaimDomain.Primitive(
                            key = "value",
                            displayTitle = "value",
                            path = disclosurePath,
                            isRequired = false,
                            value = "5",
                        ),
                    ),
                ),
                ClaimDomain.Primitive(
                    key = "expiry_date",
                    displayTitle = "expiry_date",
                    path = disclosurePath,
                    isRequired = false,
                    value = "31 Dec 2040",
                ),
                ClaimDomain.Primitive(
                    key = "issue_date",
                    displayTitle = "issue_date",
                    path = disclosurePath,
                    isRequired = false,
                    value = "1 Jan 2000",
                ),
                ClaimDomain.Primitive(
                    key = "vehicle_category_code",
                    displayTitle = "vehicle_category_code",
                    path = disclosurePath,
                    isRequired = false,
                    value = "B",
                ),
            ),
        )
        val expectedSecondEntryGroup = ClaimDomain.Group(
            key = "driving_privileges-2",
            displayTitle = "Driving Privileges 2",
            path = mockedMdlUiGroupPath,
            items = listOf(
                ClaimDomain.Group(
                    key = "codes",
                    displayTitle = "codes",
                    path = mockedMdlUiGroupPath,
                    items = listOf(
                        ClaimDomain.Primitive(
                            key = "code",
                            displayTitle = "code",
                            path = disclosurePath,
                            isRequired = false,
                            value = "S03",
                        ),
                        ClaimDomain.Primitive(
                            key = "sign",
                            displayTitle = "sign",
                            path = disclosurePath,
                            isRequired = false,
                            value = "<=",
                        ),
                        ClaimDomain.Primitive(
                            key = "value",
                            displayTitle = "value",
                            path = disclosurePath,
                            isRequired = false,
                            value = "125",
                        ),
                    ),
                ),
                ClaimDomain.Primitive(
                    key = "expiry_date",
                    displayTitle = "expiry_date",
                    path = disclosurePath,
                    isRequired = false,
                    value = "31 Dec 2040",
                ),
                ClaimDomain.Primitive(
                    key = "issue_date",
                    displayTitle = "issue_date",
                    path = disclosurePath,
                    isRequired = false,
                    value = "1 Jan 2000",
                ),
                ClaimDomain.Primitive(
                    key = "vehicle_category_code",
                    displayTitle = "vehicle_category_code",
                    path = disclosurePath,
                    isRequired = false,
                    value = "A",
                ),
            ),
        )
        assertEquals(
            listOf<ClaimDomain>(
                ClaimDomain.Group(
                    key = "driving_privileges",
                    displayTitle = "Driving Privileges",
                    path = mockedMdlUiGroupPath,
                    items = listOf(
                        expectedFirstEntryGroup,
                        expectedSecondEntryGroup,
                    ),
                )
            ),
            result,
        )
    }

    //endregion

    //region helper functions

    private val mockedMdocPidDisclosurePath: ClaimPathDomain = ClaimPathDomain.ofPlainKeys(
        names = listOf("test_claim"),
        type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace),
    )

    private val mockedMdocMdlDisclosurePath: ClaimPathDomain = ClaimPathDomain.ofPlainKeys(
        names = listOf("driving_privileges"),
        type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace),
    )

    private val mockedSdJwtDisclosurePath: ClaimPathDomain = ClaimPathDomain.ofPlainKeys(
        names = listOf("test_claim"),
        type = ClaimType.SdJwtVc,
    )

    /** The path of every UI-only group built under an SD-JWT disclosure path. */
    private val mockedUiGroupPath: ClaimPathDomain = ClaimPathDomain.forUiGroup(
        groupId = mockedUuid,
        type = ClaimType.SdJwtVc,
    )

    /** [mockedUiGroupPath]'s twin for groups built under an mdoc disclosure path. */
    private val mockedMdocPidUiGroupPath: ClaimPathDomain = ClaimPathDomain.forUiGroup(
        groupId = mockedUuid,
        type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace),
    )

    /** [mockedMdocPidUiGroupPath]'s mDL-namespaced twin. */
    private val mockedMdlUiGroupPath: ClaimPathDomain = ClaimPathDomain.forUiGroup(
        groupId = mockedUuid,
        type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace),
    )

    private fun mdocPath(dataElementName: String): ClaimPathDomain =
        ClaimPathDomain.ofPlainKeys(
            names = listOf(dataElementName),
            type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace),
        )

    private fun mockedMdocDocument(vararg claims: MsoMdocClaim): IssuedDocument {
        val data = mock<MsoMdocData>()
        whenever(data.claims).thenReturn(claims.toList())

        val document = mock<IssuedDocument>()
        whenever(document.data).thenReturn(data)
        return document
    }

    private fun mockedClaimDisplay(name: String, locale: Locale): IssuerMetadata.Claim.Display {
        val display = mock<IssuerMetadata.Claim.Display>()
        whenever(display.name).thenReturn(name)
        whenever(display.locale).thenReturn(locale)
        return display
    }

    private fun mockedClaimMetaData(vararg displays: IssuerMetadata.Claim.Display): IssuerMetadata.Claim {
        val claimMetaData = mock<IssuerMetadata.Claim>()
        whenever(claimMetaData.display).thenReturn(displays.toList())
        return claimMetaData
    }

    /**
     * The shared translation helper: claim metadata whose display list carries a name for the
     * user's locale (English — what the stubbed [ResourceProvider.getLocale] returns) plus any
     * extra `languageTag to name` locales. The English entry is deliberately LAST, so a passing
     * test proves resolution picks by language, not by list position.
     */
    private fun mockedTranslatedClaimMetaData(
        englishName: String,
        vararg otherLocaleNames: Pair<String, String>,
    ): IssuerMetadata.Claim {
        val otherDisplays = otherLocaleNames.map { (languageTag, name) ->
            mockedClaimDisplay(name = name, locale = Locale.forLanguageTag(languageTag))
        }
        val displays = otherDisplays + mockedClaimDisplay(
            name = englishName,
            locale = Locale.ENGLISH,
        )
        return mockedClaimMetaData(*displays.toTypedArray())
    }

    //endregion
}