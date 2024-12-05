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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.wrap.WrapImage

data class AppIconAndTextData(
    val appIcon: IconData,
    val appText: IconData,
)

@Composable
fun AppIconAndText(
    modifier: Modifier = Modifier,
    appIconAndTextData: AppIconAndTextData
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WrapImage(iconData = appIconAndTextData.appIcon)
        WrapImage(iconData = appIconAndTextData.appText)
    }
}

@ThemeModePreviews
@Composable
private fun AppIconAndTextPreview() {
    PreviewTheme {
        AppIconAndText(
            appIconAndTextData = AppIconAndTextData(
                appIcon = AppIcons.Logo,
                appText = AppIcons.Logo,
            )
        )
    }
}