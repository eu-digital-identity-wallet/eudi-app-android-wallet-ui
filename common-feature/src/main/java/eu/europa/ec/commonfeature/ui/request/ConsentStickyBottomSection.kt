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

package eu.europa.ec.commonfeature.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.commonfeature.ui.request.model.RegistrationWarningUi
import eu.europa.ec.commonfeature.ui.request.model.RegistrationWarningVariantUi
import eu.europa.ec.uilogic.component.AcknowledgeWarningCard
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.extension.paddingFrom

/**
 * The registration-warning slot of [ConsentStickyBottomSection]: the warning to render plus the
 * hosting screen's texts and acknowledge callback. Null when the screen never shows a warning.
 */
data class ConsentWarningSection(
    val registrationWarning: RegistrationWarningUi?,
    val notVerifiedWarningText: String,
    val overaskedWarningText: String,
    val acknowledgeText: String,
    val onAcknowledgeChange: (Boolean) -> Unit,
)

/**
 * The consent screens' sticky bottom: a Cancel | confirm button pair with the registration
 * warning, when present, docked above the buttons.
 */
@Composable
fun ConsentStickyBottomSection(
    modifier: Modifier,
    paddingValues: PaddingValues,
    buttonsTestTag: String,
    warningSection: ConsentWarningSection?,
    primaryButtonText: String,
    cancelButtonText: String,
    primaryButtonEnabled: Boolean,
    onPrimaryButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .paddingFrom(
                    pv = paddingValues,
                    top = false,
                    bottom = false,
                )
        ) {
            warningSection?.registrationWarning?.let { safeRegistrationWarning ->
                AcknowledgeWarningCard(
                    modifier = Modifier.fillMaxWidth(),
                    warningText = when (safeRegistrationWarning.variant) {
                        RegistrationWarningVariantUi.NOT_VERIFIED -> warningSection.notVerifiedWarningText
                        RegistrationWarningVariantUi.OVERASKED -> warningSection.overaskedWarningText
                    },
                    acknowledgeText = warningSection.acknowledgeText,
                    isAcknowledged = safeRegistrationWarning.riskAccepted,
                    onAcknowledgeChange = warningSection.onAcknowledgeChange,
                )
            }

            WrapStickyBottomContent(
                modifier = Modifier
                    .applyTestTag(buttonsTestTag)
                    .fillMaxWidth()
                    .padding(top = SPACING_LARGE.dp),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.TwoButtons(
                        primaryButtonConfig = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            enabled = primaryButtonEnabled,
                            onClick = onPrimaryButtonClick,
                        ),
                        secondaryButtonConfig = ButtonConfig(
                            type = ButtonType.SECONDARY,
                            enabled = true,
                            onClick = onCancelButtonClick,
                        ),
                        secondaryButtonContent = {
                            Text(text = cancelButtonText)
                        },
                    ),
                    showDivider = false,
                )
            ) {
                WrapIcon(
                    iconData = AppIcons.Check,
                )
                HSpacer.Small()
                Text(text = primaryButtonText)
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun ConsentStickyBottomSectionWithWarningPreview() {
    PreviewTheme {
        ConsentStickyBottomSection(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            buttonsTestTag = "buttons",
            warningSection = ConsentWarningSection(
                registrationWarning = RegistrationWarningUi(
                    variant = RegistrationWarningVariantUi.NOT_VERIFIED,
                    riskAccepted = false,
                ),
                notVerifiedWarningText = "Warning: The information registered about the entity " +
                        "could not be obtained. Review carefully before sharing your data.",
                overaskedWarningText = "Warning: Some of the requested data are not registered " +
                        "with this interacting party. Review carefully before sharing your data.",
                acknowledgeText = "I understand the risks and agree to share my data",
                onAcknowledgeChange = {},
            ),
            primaryButtonText = "Share",
            cancelButtonText = "Cancel",
            primaryButtonEnabled = false,
            onPrimaryButtonClick = {},
            onCancelButtonClick = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun ConsentStickyBottomSectionWithoutWarningPreview() {
    PreviewTheme {
        ConsentStickyBottomSection(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            buttonsTestTag = "buttons",
            warningSection = null,
            primaryButtonText = "Share",
            cancelButtonText = "Cancel",
            primaryButtonEnabled = true,
            onPrimaryButtonClick = {},
            onCancelButtonClick = {},
        )
    }
}