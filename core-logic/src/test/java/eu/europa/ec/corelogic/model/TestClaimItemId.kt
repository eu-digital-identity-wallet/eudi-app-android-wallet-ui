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
    fun `the same identity always produces the same id`() {
        val claim = sd("place_of_birth", "country")

        assertEquals(
            claim.itemId("doc-1", "pid"),
            claim.itemId("doc-1", "pid")
        )
    }

    // ── multiplicity: 2 PIDs, same claim ─────────────────────────────────────────

    @Test
    fun `same claim on two different documents yields different ids`() {
        val familyName = sd("family_name")

        assertNotEquals(
            familyName.itemId("pid-1", "pid"),
            familyName.itemId("pid-2", "pid"),
        )
    }

    // ── multi-query, same document ────────────────────────

    @Test
    fun `same claim on the same document but a different query yields different ids`() {
        val familyName = md("org.iso.18013.5.1", "family_name")

        assertNotEquals(
            familyName.itemId("doc-1", "query-a"),
            familyName.itemId("doc-1", "query-b"),
        )
    }

    @Test
    fun `a null query id is distinct from a present query id`() {
        val familyName = sd("family_name")

        assertNotEquals(
            familyName.itemId("doc-1", queryId = null),
            familyName.itemId("doc-1", queryId = "q"),
        )
    }

    @Test
    fun `a null query id is distinct from an empty-string query id`() {
        val familyName = sd("family_name")

        assertNotEquals(
            familyName.itemId("doc-1", queryId = null),
            familyName.itemId("doc-1", queryId = ""),
        )
    }

    // ── format / namespace discriminates ─────────────────────────────────────────

    @Test
    fun `the same path string under mdoc and sd-jwt yields different ids`() {
        assertNotEquals(
            sd("family_name").itemId("doc-1", null),
            md("ns", "family_name").itemId("doc-1", null),
        )
    }

    @Test
    fun `different mdoc namespaces yield different ids`() {
        assertNotEquals(
            md("ns-1", "family_name").itemId("doc-1", null),
            md("ns-2", "family_name").itemId("doc-1", null),
        )
    }

    // ── distinct paths never collide ─────────────────────────────────────────────

    @Test
    fun `distinct claim paths yield distinct ids`() {
        val ids = listOf(
            sd("a"),
            sd("b"),
            sd("a", "b"),
            sd("a", "c"),
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0)),
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(1)),
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements),
        ).map { it.itemId("doc-1", "q") }

        assertEquals(
            "all distinct paths must encode to distinct ids",
            ids.size,
            ids.toSet().size
        )
    }

    // ── adversarial claim names ───────────

    @Test
    fun `an object key named 0 does not collide with array index 0`() {
        val keyZero = path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Key("0"))
        val indexZero =
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Index(0))

        assertNotEquals(
            keyZero.itemId("doc-1", null),
            indexZero.itemId("doc-1", null)
        )
    }

    @Test
    fun `an object key named null does not collide with the wildcard`() {
        val keyNull =
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.Key("null"))
        val wildcard =
            path(sdJwt, ClaimPathSegment.Key("nationalities"), ClaimPathSegment.AllElements)

        assertNotEquals(
            keyNull.itemId("doc-1", null),
            wildcard.itemId("doc-1", null)
        )
    }

    @Test
    fun `a comma inside a claim name does not collide with a split path`() {
        assertNotEquals(
            sd("a,b").itemId("doc-1", null),
            sd("a", "b").itemId("doc-1", null),
        )
    }

    @Test
    fun `doc ids differing only by an embedded separator stay unique`() {
        val claim = sd("x")
        val ids = listOf(
            "a",
            "a,b",
            "ab",
            "a,"
        ).map { claim.itemId(it, null) }

        assertEquals(
            ids.size,
            ids.toSet().size
        )
    }

    // ── document-card header ids ─────────────────────────────────────────────────

    @Test
    fun `a document header id is stable per query and differs across queries`() {
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
    fun `a null query id is distinct from a present query id on header ids too`() {
        assertNotEquals(
            headerItemId("doc-1", queryId = null),
            headerItemId("doc-1", queryId = "q"),
        )
    }

    @Test
    fun `a document header id never collides with a claim-row id`() {
        val headerId = headerItemId("doc-1", "q")

        // Header and claim are distinct ClaimItemId variants, so no claim path — sd-jwt
        // or mdoc — can ever encode to a header id.
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

    @Test
    fun `a query id equal to the doc id stays distinct from swapped and absent variants`() {
        val claim = sd("family_name")
        val sameDocAndQuery = claim.itemId(docId = "x", queryId = "x")

        // docId and queryId occupy fixed, separate slots, so equal values don't merge
        assertEquals(sameDocAndQuery, claim.itemId(docId = "x", queryId = "x"))
        assertNotEquals(sameDocAndQuery, claim.itemId(docId = "x", queryId = null))
        assertNotEquals(sameDocAndQuery, claim.itemId(docId = "x", queryId = "y"))
        assertNotEquals(sameDocAndQuery, claim.itemId(docId = "y", queryId = "x"))
    }

    @Test
    fun `a query id whose value equals a query marker is distinct from a null query id`() {
        val claim = sd("family_name")

        // "query-absent"/"query" are the encoding's own query markers; a real query id that
        // happens to equal one must never be read as "no query".
        assertNotEquals(
            claim.itemId(docId = "doc-1", queryId = null),
            claim.itemId(docId = "doc-1", queryId = "query-absent"),
        )
        assertNotEquals(
            claim.itemId(docId = "doc-1", queryId = null),
            claim.itemId(docId = "doc-1", queryId = "query"),
        )
    }

    @Test
    fun `data values equal to the encoding's own tokens never collide`() {
        // Every reserved token, used as a *data* value (claim name, docId, query id, namespace,
        // on both variants). Length-prefix encoding must stop any of them impersonating the
        // structure, so all of these logically-distinct ids must encode distinctly.
        val tokens = listOf(
            "header", "claim", "query", "query-absent",
            "sd-jwt-vc", "mso-mdoc", "key", "index", "wildcard",
        )

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

        assertEquals(
            "data values equal to structural tokens must not collide",
            ids.size,
            ids.toSet().size,
        )
    }

    @Test
    fun `values that mimic the length-prefix encoding stay unique`() {
        val claim = sd("x")

        // doc ids crafted to look like the `<length>:<value>` encoding itself.
        val crafted = listOf(":", "0:", "1", "11", "1:1", "5:x", "12:query-absent")
        val ids = crafted.map { claim.itemId(docId = it, queryId = null) }

        assertEquals(ids.size, ids.toSet().size)
    }
}