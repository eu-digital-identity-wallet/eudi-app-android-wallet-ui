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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TestClaimItemId {

    private val sdJwt = ClaimType.SdJwtVc
    private fun mdoc(namespace: String) = ClaimType.MsoMdoc(namespace)

    /** SD-JWT claim path of plain object keys. */
    private fun sd(vararg names: String) = ClaimPathDomain.ofPlainKeys(names.toList(), sdJwt)

    /** mso_mdoc claim — a single data element under a namespace. */
    private fun md(namespace: String, element: String) =
        ClaimPathDomain.ofPlainKeys(listOf(element), mdoc(namespace))

    private fun path(type: ClaimType, vararg segments: ClaimPathSegment) =
        ClaimPathDomain(segments = segments.toList(), type = type)

    /** The row id [ClaimItemId.Claim] produces for this claim path. */
    private fun ClaimPathDomain.itemId(docId: String, queryId: String?): String =
        ClaimItemId.Claim(docId = docId, queryId = queryId, path = this).encode()

    /** The header id [ClaimItemId.DocumentHeader] produces for a document card. */
    private fun headerItemId(docId: String, queryId: String?): String =
        ClaimItemId.DocumentHeader(docId = docId, queryId = queryId).encode()

    // ── stability ────────────────────────────────────────────────────────────────

    @Test
    fun `Given the same claim, document and query, When the id is encoded twice, Then both ids are equal`() {
        // Given
        val claim = sd("place_of_birth", "country")

        // Then
        assertEquals(
            claim.itemId("doc-1", "pid"),
            claim.itemId("doc-1", "pid")
        )
    }

    // ── multiplicity: 2 PIDs, same claim ─────────────────────────────────────────

    @Test
    fun `Given the same claim on two documents, When the ids are encoded, Then they differ`() {
        // Given
        val familyName = sd("family_name")

        // Then
        assertNotEquals(
            familyName.itemId("pid-1", "pid"),
            familyName.itemId("pid-2", "pid"),
        )
    }

    // ── multi-query, same document ────────────────────────

    @Test
    fun `Given the same claim and document under two queries, When the ids are encoded, Then they differ`() {
        // Given
        val familyName = md("org.iso.18013.5.1", "family_name")

        // Then
        assertNotEquals(
            familyName.itemId("doc-1", "query-a"),
            familyName.itemId("doc-1", "query-b"),
        )
    }

    @Test
    fun `Given a null and a present query id, When the ids are encoded, Then they differ`() {
        // Given
        val familyName = sd("family_name")

        // Then
        assertNotEquals(
            familyName.itemId("doc-1", queryId = null),
            familyName.itemId("doc-1", queryId = "q"),
        )
    }

    @Test
    fun `Given a null and an empty-string query id, When the ids are encoded, Then they differ`() {
        // Given
        val familyName = sd("family_name")

        // Then
        assertNotEquals(
            familyName.itemId("doc-1", queryId = null),
            familyName.itemId("doc-1", queryId = ""),
        )
    }

    // ── format / namespace discriminates ─────────────────────────────────────────

    @Test
    fun `Given the same path string under mdoc and sd-jwt, When the ids are encoded, Then they differ`() {
        // Then
        assertNotEquals(
            sd("family_name").itemId("doc-1", null),
            md("ns", "family_name").itemId("doc-1", null),
        )
    }

    @Test
    fun `Given two mdoc namespaces, When the ids are encoded, Then they differ`() {
        // Then
        assertNotEquals(
            md("ns-1", "family_name").itemId("doc-1", null),
            md("ns-2", "family_name").itemId("doc-1", null),
        )
    }

    // ── distinct paths never collide ─────────────────────────────────────────────

    @Test
    fun `Given distinct claim paths, When the ids are encoded, Then all ids are distinct`() {
        // When
        val ids = listOf(
            sd("a"),
            sd("b"),
            sd("a", "b"),
            sd("a", "c"),
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)),
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(1)),
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements),
        ).map { it.itemId("doc-1", "q") }

        // Then
        assertEquals(
            "all distinct paths must encode to distinct ids",
            ids.size,
            ids.toSet().size
        )
    }

    // ── adversarial claim names ───────────

    @Test
    fun `Given an object key named 0 and array index 0, When the ids are encoded, Then they differ`() {
        // Given
        val keyZero = path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Key("0"))
        val indexZero =
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0))

        // Then
        assertNotEquals(
            keyZero.itemId("doc-1", null),
            indexZero.itemId("doc-1", null)
        )
    }

    @Test
    fun `Given an object key named null and the wildcard, When the ids are encoded, Then they differ`() {
        // Given
        val keyNull =
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Key("null"))
        val wildcard =
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements)

        // Then
        assertNotEquals(
            keyNull.itemId("doc-1", null),
            wildcard.itemId("doc-1", null)
        )
    }

    @Test
    fun `Given a comma inside a claim name and a split path, When the ids are encoded, Then they differ`() {
        // Then
        assertNotEquals(
            sd("a,b").itemId("doc-1", null),
            sd("a", "b").itemId("doc-1", null),
        )
    }

    @Test
    fun `Given doc ids that differ only by an embedded separator, When the ids are encoded, Then all ids are distinct`() {
        // Given
        val claim = sd("x")

        // When
        val ids = listOf(
            "a",
            "a,b",
            "ab",
            "a,"
        ).map { claim.itemId(it, null) }

        // Then
        assertEquals(
            ids.size,
            ids.toSet().size
        )
    }

    // ── document-card header ids ─────────────────────────────────────────────────

    @Test
    fun `Given a document header id, When it is encoded per query, Then it is stable per query and differs across queries`() {
        // Then
        assertEquals(
            headerItemId("doc-1", "query-a"),
            headerItemId("doc-1", "query-a"),
        )
        assertNotEquals(
            headerItemId("doc-1", "query-a"),
            headerItemId("doc-1", "query-b"),
        )
    }

    @Test
    fun `Given a null and a present query id on header ids, When the ids are encoded, Then they differ`() {
        // Then
        assertNotEquals(
            headerItemId("doc-1", queryId = null),
            headerItemId("doc-1", queryId = "q"),
        )
    }

    /**
     * Header and claim are distinct ClaimItemId variants, so no claim path — sd-jwt or mdoc — can
     * ever encode to a header id.
     */
    @Test
    fun `Given a document header id and claim-row ids, When they are encoded, Then they never collide`() {
        // Given
        val headerId = headerItemId("doc-1", "q")

        // Then
        assertNotEquals(
            headerId,
            sd("d").itemId("doc-1", "q")
        )
        assertNotEquals(
            headerId,
            md("d", "d").itemId("doc-1", "q")
        )
    }

    // ── values can't impersonate the structure ───────────────────────────────────

    /**
     * docId and queryId occupy fixed, separate slots, so equal values don't merge
     */
    @Test
    fun `Given a query id equal to the doc id, When the ids are encoded, Then it differs from swapped and absent variants`() {
        // Given
        val claim = sd("family_name")
        val sameDocAndQuery = claim.itemId(docId = "x", queryId = "x")

        // Then
        assertEquals(sameDocAndQuery, claim.itemId(docId = "x", queryId = "x"))
        assertNotEquals(sameDocAndQuery, claim.itemId(docId = "x", queryId = null))
        assertNotEquals(sameDocAndQuery, claim.itemId(docId = "x", queryId = "y"))
        assertNotEquals(sameDocAndQuery, claim.itemId(docId = "y", queryId = "x"))
    }

    /**
     * "query-absent"/"query" are the encoding's own query markers; a real query id that happens to
     * equal one must never be read as "no query".
     */
    @Test
    fun `Given a query id equal to a query marker, When the ids are encoded, Then it differs from a null query id`() {
        // Given
        val claim = sd("family_name")

        // Then
        assertNotEquals(
            claim.itemId(docId = "doc-1", queryId = null),
            claim.itemId(docId = "doc-1", queryId = "query-absent"),
        )
        assertNotEquals(
            claim.itemId(docId = "doc-1", queryId = null),
            claim.itemId(docId = "doc-1", queryId = "query"),
        )
    }

    /**
     * Every reserved token, used as a *data* value (claim name, docId, query id, namespace, on both
     * variants). Length-prefix encoding must stop any of them impersonating the structure, so all
     * of these logically-distinct ids must encode distinctly.
     */
    @Test
    fun `Given data values equal to the encoding's own tokens, When the ids are encoded, Then no ids collide`() {
        // Given
        val tokens = listOf(
            "header", "claim", "query", "query-absent",
            "sd-jwt-vc", "mso-mdoc", "key", "index", "wildcard",
        )

        // When
        val ids = buildList {
            tokens.forEach { token ->
                add(sd(token).itemId(docId = "doc", queryId = null))
                add(sd("family_name").itemId(docId = token, queryId = null))
                add(sd("family_name").itemId(docId = "doc", queryId = token))
                add(md(token, "family_name").itemId(docId = "doc", queryId = null))
                add(headerItemId(docId = token, queryId = null))
                add(headerItemId(docId = "doc", queryId = token))
            }
        }

        // Then
        assertEquals(
            "data values equal to structural tokens must not collide",
            ids.size,
            ids.toSet().size,
        )
    }

    /**
     * doc ids crafted to look like the `<length>:<value>` encoding itself.
     */
    @Test
    fun `Given values that mimic the length-prefix encoding, When the ids are encoded, Then all ids are distinct`() {
        // Given
        val claim = sd("x")

        val crafted = listOf(":", "0:", "1", "11", "1:1", "5:x", "12:query-absent")

        // When
        val ids = crafted.map { claim.itemId(docId = it, queryId = null) }

        // Then
        assertEquals(ids.size, ids.toSet().size)
    }
}