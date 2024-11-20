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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import eu.europa.ec.resourceslogic.R

/**
 * Data class to be used when we want to display an Icon.
 * @param resourceId The id of the icon. Can be null
 * @param contentDescriptionId The id of its content description.
 * @param imageVector The [ImageVector] of the icon, null by default.
 * @throws IllegalArgumentException If both [resourceId] AND [imageVector] are null.
 */
@Stable
data class IconData(
    @DrawableRes val resourceId: Int?,
    @StringRes val contentDescriptionId: Int,
    val imageVector: ImageVector? = null,
) {
    init {
        require(
            resourceId == null && imageVector != null
                    || resourceId != null && imageVector == null
                    || resourceId != null && imageVector != null
        ) {
            "An Icon should at least have a valid resourceId or a valid imageVector."
        }
    }
}

/**
 * A Singleton object responsible for providing access to all the app's Icons.
 */
object AppIcons {

    val ArrowBack: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_back_icon,
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
    )

    val Close: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_close_icon,
        imageVector = Icons.Filled.Close
    )

    val VerticalMore: IconData = IconData(
        resourceId = R.drawable.ic_more,
        contentDescriptionId = R.string.content_description_more_vert_icon,
        imageVector = null
    )

    val Warning: IconData = IconData(
        resourceId = R.drawable.ic_warning,
        contentDescriptionId = R.string.content_description_warning_icon,
        imageVector = null
    )

    val Error: IconData = IconData(
        resourceId = R.drawable.ic_error,
        contentDescriptionId = R.string.content_description_error_icon,
        imageVector = null
    )

    val ErrorFilled: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_error_icon,
        imageVector = Icons.Default.Info
    )

    val Delete: IconData = IconData(
        resourceId = R.drawable.ic_delete,
        contentDescriptionId = R.string.content_description_delete_icon,
        imageVector = null
    )

    val TouchId: IconData = IconData(
        resourceId = R.drawable.ic_touch_id,
        contentDescriptionId = R.string.content_description_touch_id_icon,
        imageVector = null
    )

    val QR: IconData = IconData(
        resourceId = R.drawable.ic_qr,
        contentDescriptionId = R.string.content_description_qr_icon,
        imageVector = null
    )

    val NFC: IconData = IconData(
        resourceId = R.drawable.ic_nfc,
        contentDescriptionId = R.string.content_description_nfc_icon,
        imageVector = null
    )

    val User: IconData = IconData(
        resourceId = R.drawable.ic_user,
        contentDescriptionId = R.string.content_description_user_icon,
        imageVector = null
    )

    val Id: IconData = IconData(
        resourceId = R.drawable.ic_id,
        contentDescriptionId = R.string.content_description_id_icon,
        imageVector = null
    )

    val IdStroke: IconData = IconData(
        resourceId = R.drawable.ic_id_stroke,
        contentDescriptionId = R.string.content_description_id_stroke_icon,
        imageVector = null
    )

    val Logo: IconData = IconData(
        resourceId = R.drawable.ic_logo,
        contentDescriptionId = R.string.content_description_logo_icon,
        imageVector = null
    )

    val KeyboardArrowDown: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_down_icon,
        imageVector = Icons.Default.KeyboardArrowDown
    )

    val KeyboardArrowUp: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_up_icon,
        imageVector = Icons.Default.KeyboardArrowUp
    )

    val Visibility: IconData = IconData(
        resourceId = R.drawable.ic_visibility_on,
        contentDescriptionId = R.string.content_description_visibility_icon,
        imageVector = null
    )

    val VisibilityOff: IconData = IconData(
        resourceId = R.drawable.ic_visibility_off,
        contentDescriptionId = R.string.content_description_visibility_off_icon,
        imageVector = null
    )

    val Add: IconData = IconData(
        resourceId = R.drawable.ic_add,
        contentDescriptionId = R.string.content_description_add_icon,
        imageVector = null
    )

    val Edit: IconData = IconData(
        resourceId = R.drawable.ic_edit,
        contentDescriptionId = R.string.content_description_edit_icon,
        imageVector = null
    )

    val Sign: IconData = IconData(
        resourceId = R.drawable.ic_sign_document,
        contentDescriptionId = R.string.content_description_edit_icon,
        imageVector = null
    )

    val QrScanner: IconData = IconData(
        resourceId = R.drawable.ic_qr_scanner,
        contentDescriptionId = R.string.content_description_qr_scanner_icon,
        imageVector = null
    )

    val Verified: IconData = IconData(
        resourceId = R.drawable.ic_verified,
        contentDescriptionId = R.string.content_description_verified_icon,
        imageVector = null
    )

    val Message: IconData = IconData(
        resourceId = R.drawable.ic_message,
        contentDescriptionId = R.string.content_description_message,
        imageVector = null
    )

    val ClockTimer: IconData = IconData(
        resourceId = R.drawable.ic_clock_timer,
        contentDescriptionId = R.string.content_description_clock_timer,
        imageVector = null
    )

    val OpenNew: IconData = IconData(
        resourceId = R.drawable.ic_open_new,
        contentDescriptionId = R.string.content_description_open_new,
        imageVector = null
    )

    val KeyboardArrowRight: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_right,
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
    )
}