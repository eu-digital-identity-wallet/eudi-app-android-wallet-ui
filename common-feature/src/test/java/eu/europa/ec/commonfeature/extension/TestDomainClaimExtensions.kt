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

package eu.europa.ec.commonfeature.extension

import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.testfeature.util.mockedUserImageClaimKeys
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestDomainClaimExtensions {

    private fun primitive(key: String) = ClaimDomain.Primitive(
        key = key,
        displayTitle = key,
        path = ClaimPathDomain.ofPlainKeys(names = listOf(key), type = ClaimType.SdJwtVc),
        value = "imageBase64",
        isRequired = false,
    )

    private fun ClaimDomain.header(userImageClaimKeys: Set<String>) =
        (toExpandableListItems(
            docId = "docId",
            queryId = null,
            userImageClaimKeys = userImageClaimKeys,
        ) as ExpandableListItemUi.SingleListItem).header

    @Test
    fun `Given the default keys, When portrait is rendered, Then it is shown as the user image`() {
        val header =
            primitive(key = "portrait").header(userImageClaimKeys = mockedUserImageClaimKeys)

        assertEquals(
            ListItemLeadingContentDataUi.UserImage(userBase64Image = "imageBase64"),
            header.leadingContentData,
        )
        assertEquals(ListItemMainContentDataUi.Text(text = ""), header.mainContentData)
    }

    @Test
    fun `Given a configured key, When that claim is rendered, Then it is shown as the user image`() {
        val header = primitive(key = "face_image").header(userImageClaimKeys = setOf("face_image"))

        assertEquals(
            ListItemLeadingContentDataUi.UserImage(userBase64Image = "imageBase64"),
            header.leadingContentData,
        )
    }

    @Test
    fun `Given portrait is not configured, When portrait is rendered, Then it is shown as text`() {
        val header = primitive(key = "portrait").header(userImageClaimKeys = setOf("face_image"))

        assertNull(header.leadingContentData)
        assertEquals(ListItemMainContentDataUi.Text(text = "imageBase64"), header.mainContentData)
    }
}