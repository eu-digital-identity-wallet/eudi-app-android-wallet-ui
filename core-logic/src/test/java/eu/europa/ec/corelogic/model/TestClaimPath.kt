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

package eu.europa.ec.corelogic.model

import eu.europa.ec.corelogic.extension.toClaimPath
import eu.europa.ec.corelogic.model.ClaimPathDomain.Companion.toClaimPathDomain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim

class TestClaimPath {

    private fun sdJwt(path: List<String>) = JsonRequestedClaim(
        vctValues = listOf("vct"),
        claimPath = JsonArray(path.map { JsonPrimitive(it) }),
    )

    /** Like [sdJwt] but takes raw [JsonElement]s, so wildcard and array-index segments keep their type. */
    private fun sdJwtRaw(path: List<JsonElement>) = JsonRequestedClaim(
        vctValues = listOf("vct"),
        claimPath = JsonArray(path),
    )

    private fun mdoc(namespace: String, dataElementName: String) = MdocRequestedClaim(
        docType = "doctype",
        namespaceName = namespace,
        dataElementName = dataElementName,
        intentToRetain = false,
    )

    // ── Conversion: RequestedClaim → ClaimPathDomain ─────────────────────────────

    @Test
    fun `mdoc claim converts to a single Key segment carrying the namespace in the type`() {
        val path = mdoc("org.iso.18013.5.1", "family_name").toClaimPath()

        assertEquals(listOf(ClaimPathSegment.Key("family_name")), path.segments)
        assertEquals(ClaimType.MsoMdoc("org.iso.18013.5.1"), path.type)
    }

    @Test
    fun `sd-jwt string path converts to Key segments`() {
        val path = sdJwt(
            listOf("place_of_birth", "country")
        ).toClaimPath()

        assertEquals(
            listOf(
                ClaimPathSegment.Key("place_of_birth"),
                ClaimPathSegment.Key("country")
            ),
            path.segments,
        )
        assertEquals(ClaimType.SdJwtVc, path.type)
    }

    @Test
    fun `sd-jwt raw path converts wildcard and index elements to typed segments`() {
        val wildcard = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonNull)
        ).toClaimPath()

        val indexed = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(0))
        ).toClaimPath()

        assertEquals(
            listOf(
                ClaimPathSegment.Key("nationalities"),
                ClaimPathSegment.AllElements
            ),
            wildcard.segments,
        )
        assertEquals(
            listOf(
                ClaimPathSegment.Key("nationalities"),
                ClaimPathSegment.Index(0)
            ),
            indexed.segments,
        )
    }

    @Test
    fun `an empty sd-jwt claim path fails loudly at conversion`() {
        // DCQL requires at least one path element; an empty path would otherwise match every
        // stored claim of its type.
        val result = runCatching { sdJwtRaw(emptyList()).toClaimPath() }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    // ── matches(): exact / ancestor / descendant ─────────────────────────────────

    @Test
    fun `matches is true for an exact path`() {
        val requested = sdJwt(listOf("address", "street")).toClaimPath()
        val available = sdJwt(listOf("address", "street")).toClaimPath()

        assertTrue(requested.matches(available))
    }

    @Test
    fun `matches is bidirectional across the shared prefix - ancestor and descendant both match`() {
        val parent = sdJwt(listOf("address")).toClaimPath()
        val child = sdJwt(listOf("address", "country")).toClaimPath()

        assertTrue(parent.matches(child))
        assertTrue(child.matches(parent))
    }

    @Test
    fun `matches is false when the shared prefix diverges`() {
        val requested = sdJwt(listOf("address", "formatted")).toClaimPath()

        val sibling = sdJwt(listOf("address", "formattedNo")).toClaimPath()
        val differentRoot = sdJwt(listOf("addressA", "street")).toClaimPath()

        assertFalse(requested.matches(sibling))
        assertFalse(requested.matches(differentRoot))
    }

    @Test
    fun `matches is false across different claim types`() {
        val mdocPath = mdoc("address", "city").toClaimPath()
        val sdJwtPath = sdJwt(listOf("address", "city")).toClaimPath()

        assertFalse(mdocPath.matches(sdJwtPath))
        assertFalse(sdJwtPath.matches(mdocPath))
    }

    @Test
    fun `an empty path matches nothing`() {
        val empty = ClaimPathDomain(segments = emptyList(), type = ClaimType.SdJwtVc)
        val stored = sdJwt(listOf("family_name")).toClaimPath()

        assertFalse(empty.matches(stored))
        assertFalse(stored.matches(empty))
        assertFalse(empty.matches(empty))
    }

    // ── matches(): wildcard and array index ──────────────────────────────────────

    @Test
    fun `AllElements wildcard matches any concrete index but not a key`() {
        val wildcard = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonNull)
        ).toClaimPath()

        val index0 = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(0))
        ).toClaimPath()
        val index1 = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(1))
        ).toClaimPath()
        val key = sdJwt(listOf("nationalities", "primary")).toClaimPath()

        assertTrue(wildcard.matches(index0))
        assertTrue(wildcard.matches(index1))
        assertFalse(wildcard.matches(key))
    }

    @Test
    fun `wildcard inside a path matches the same trailing key under any index`() {
        val requested = sdJwtRaw(
            listOf(JsonPrimitive("addresses"), JsonNull, JsonPrimitive("city")),
        ).toClaimPath()

        val cityOfFirst = sdJwtRaw(
            listOf(JsonPrimitive("addresses"), JsonPrimitive(0), JsonPrimitive("city")),
        ).toClaimPath()
        val streetOfFirst = sdJwtRaw(
            listOf(JsonPrimitive("addresses"), JsonPrimitive(0), JsonPrimitive("street")),
        ).toClaimPath()

        assertTrue(requested.matches(cityOfFirst))
        assertFalse(requested.matches(streetOfFirst))
    }

    @Test
    fun `concrete index matches the same index and the array ancestor but not a sibling index`() {
        val index0 = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(0))
        ).toClaimPath()

        val sameIndex = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(0))
        ).toClaimPath()
        val ancestor = sdJwt(
            listOf("nationalities")
        ).toClaimPath()
        val siblingIndex = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(1))
        ).toClaimPath()

        assertTrue(index0.matches(sameIndex))
        assertTrue(index0.matches(ancestor))
        assertFalse(index0.matches(siblingIndex))
    }

    // matching is one-directional: a wildcard matches a concrete index only on the requested
    // (left) side; a concrete requested index does not match a stored wildcard.
    @Test
    fun `a concrete index does not match a stored wildcard`() {
        val index0 = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonPrimitive(0))
        ).toClaimPath()
        val wildcard = sdJwtRaw(
            listOf(JsonPrimitive("nationalities"), JsonNull)
        ).toClaimPath()

        assertFalse(index0.matches(wildcard))
    }

    // ── mdoc matching ────────────────────────────────────────────────────────────

    @Test
    fun `mdoc matches only on the same namespace and element`() {
        val requested = mdoc("ns", "family_name").toClaimPath()

        assertTrue(requested.matches(mdoc("ns", "family_name").toClaimPath()))
        assertFalse(requested.matches(mdoc("ns", "given_name").toClaimPath()))
        assertFalse(requested.matches(mdoc("other_ns", "family_name").toClaimPath()))
    }

    // ── factories ────────────────────────────────────────────────────────────────

    @Test
    fun `forUiGroup builds a single-key path of the given type`() {
        val group = ClaimPathDomain.forUiGroup(groupId = "uuid-123", type = ClaimType.SdJwtVc)

        assertEquals(listOf(ClaimPathSegment.Key("uuid-123")), group.segments)
        assertEquals(ClaimType.SdJwtVc, group.type)
    }

    @Test
    fun `toClaimPathDomain wraps a segment list with the given type`() {
        val segments = listOf(
            ClaimPathSegment.Key("a"),
            ClaimPathSegment.Index(0)
        )

        val path = segments.toClaimPathDomain(ClaimType.SdJwtVc)

        assertEquals(segments, path.segments)
        assertEquals(ClaimType.SdJwtVc, path.type)
    }
}