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

package eu.europa.ec.uilogic.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import eu.europa.ec.resourceslogic.R
import kotlinx.serialization.Serializable

/**
 * Data class wrapping an [AppIconKey].
 *
 * Holding *only* an [AppIconKey] means the type system, not a runtime check, enforces
 * that every icon passed through the UI layer is registered in the central enum.
 * Callers cannot construct an `IconDataUi` from a raw `R.drawable.example` or a Material
 * `Icons.Filled.X`; they must add an entry to [AppIconKey] first.
 *
 * Serialization writes just the key (e.g. `{"iconKey":"WalletSecured"}`); the
 * destination reconstructs the full data via the enum entry.
 */
@Stable
@Serializable
data class IconDataUi(val iconKey: AppIconKey) {

    @get:DrawableRes
    val resourceId: Int? get() = iconKey.resourceId

    @get:StringRes
    val contentDescriptionId: Int get() = iconKey.contentDescriptionId

    val imageVector: ImageVector? get() = iconKey.imageVector
}

/**
 * Stable identity AND data for every icon in the app.
 *
 * Each entry carries either a drawable `resourceId`, an `imageVector`, or both — and
 * a `contentDescriptionId`. The enum is the single source of truth for icon assets:
 * callers reference icons by their key, never by raw resource id or `ImageVector`.
 *
 * Serialization writes the enum's *name* (e.g. `"WalletSecured"`) on the wire and
 * resolves back to the same entry on the other side. `ImageVector` and `resourceId`
 * therefore survive a navigation round-trip without ever being serialized themselves.
 *
 * **Adding a new icon:** add one entry here. That's it — the [IconDataUi] wrapper
 * picks it up automatically, [AppIcons] just needs a one-line alias.
 *
 * @throws IllegalArgumentException at class init if any entry has both `resourceId`
 *   and `imageVector` null.
 */
@Serializable
enum class AppIconKey(
    @field:DrawableRes val resourceId: Int? = null,
    @field:StringRes val contentDescriptionId: Int,
    val imageVector: ImageVector? = null,
) {
    ArrowBack(
        contentDescriptionId = R.string.content_description_arrow_back_icon,
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
    ),
    Close(
        contentDescriptionId = R.string.content_description_close_icon,
        imageVector = Icons.Filled.Close,
    ),
    VerticalMore(
        resourceId = R.drawable.ic_more,
        contentDescriptionId = R.string.content_description_more_vert_icon,
    ),
    Warning(
        resourceId = R.drawable.ic_warning,
        contentDescriptionId = R.string.content_description_warning_icon,
    ),
    Error(
        resourceId = R.drawable.ic_error,
        contentDescriptionId = R.string.content_description_error_icon,
    ),
    ErrorFilled(
        contentDescriptionId = R.string.content_description_error_icon,
        imageVector = Icons.Default.Info,
    ),
    Delete(
        resourceId = R.drawable.ic_delete,
        contentDescriptionId = R.string.content_description_delete_icon,
    ),
    TouchId(
        resourceId = R.drawable.ic_touch_id,
        contentDescriptionId = R.string.content_description_touch_id_icon,
    ),
    QR(
        resourceId = R.drawable.ic_qr,
        contentDescriptionId = R.string.content_description_qr_icon,
    ),
    NFC(
        resourceId = R.drawable.ic_nfc,
        contentDescriptionId = R.string.content_description_nfc_icon,
    ),
    User(
        resourceId = R.drawable.ic_user,
        contentDescriptionId = R.string.content_description_user_icon,
    ),
    Id(
        resourceId = R.drawable.ic_id,
        contentDescriptionId = R.string.content_description_id_icon,
    ),
    IdStroke(
        resourceId = R.drawable.ic_id_stroke,
        contentDescriptionId = R.string.content_description_id_stroke_icon,
    ),
    LogoIcon(
        resourceId = R.drawable.ic_logo_icon,
        contentDescriptionId = R.string.content_description_logo_icon,
    ),
    LogoIconAndText(
        resourceId = R.drawable.ic_logo_icon_and_text,
        contentDescriptionId = R.string.content_description_logo_icon_and_text,
    ),
    KeyboardArrowDown(
        contentDescriptionId = R.string.content_description_arrow_down_icon,
        imageVector = Icons.Default.KeyboardArrowDown,
    ),
    KeyboardArrowUp(
        contentDescriptionId = R.string.content_description_arrow_up_icon,
        imageVector = Icons.Default.KeyboardArrowUp,
    ),
    Visibility(
        resourceId = R.drawable.ic_visibility_on,
        contentDescriptionId = R.string.content_description_visibility_icon,
    ),
    VisibilityOff(
        resourceId = R.drawable.ic_visibility_off,
        contentDescriptionId = R.string.content_description_visibility_off_icon,
    ),
    Add(
        resourceId = R.drawable.ic_add,
        contentDescriptionId = R.string.content_description_add_icon,
    ),
    Edit(
        resourceId = R.drawable.ic_edit,
        contentDescriptionId = R.string.content_description_edit_icon,
    ),
    Sign(
        resourceId = R.drawable.ic_sign_document,
        contentDescriptionId = R.string.content_description_edit_icon,
    ),
    QrScanner(
        resourceId = R.drawable.ic_qr_scanner,
        contentDescriptionId = R.string.content_description_qr_scanner_icon,
    ),
    Verified(
        resourceId = R.drawable.ic_verified,
        contentDescriptionId = R.string.content_description_verified_icon,
    ),
    Message(
        resourceId = R.drawable.ic_message,
        contentDescriptionId = R.string.content_description_message_icon,
    ),
    ClockTimer(
        resourceId = R.drawable.ic_clock_timer,
        contentDescriptionId = R.string.content_description_clock_timer_icon,
    ),
    OpenNew(
        resourceId = R.drawable.ic_open_new,
        contentDescriptionId = R.string.content_description_open_new_icon,
    ),
    KeyboardArrowRight(
        contentDescriptionId = R.string.content_description_arrow_right_icon,
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    ),
    HandleBar(
        resourceId = R.drawable.ic_handle_bar,
        contentDescriptionId = R.string.content_description_handle_bar_icon,
    ),
    Search(
        resourceId = R.drawable.ic_search,
        contentDescriptionId = R.string.content_description_search_icon,
    ),
    PresentDocumentInPerson(
        resourceId = R.drawable.ic_present_document_same_device,
        contentDescriptionId = R.string.content_description_present_document_same_device_icon,
    ),
    PresentDocumentOnline(
        resourceId = R.drawable.ic_present_document_cross_device,
        contentDescriptionId = R.string.content_description_present_document_cross_device_icon,
    ),
    AddDocumentFromList(
        resourceId = R.drawable.ic_add_document_from_list,
        contentDescriptionId = R.string.content_description_add_document_from_list_icon,
    ),
    AddDocumentFromQr(
        resourceId = R.drawable.ic_add_document_from_qr,
        contentDescriptionId = R.string.content_description_add_document_from_qr_icon,
    ),
    Bookmark(
        resourceId = R.drawable.ic_bookmark,
        contentDescriptionId = R.string.content_description_bookmark_icon,
    ),
    BookmarkFilled(
        resourceId = R.drawable.ic_bookmark_filled,
        contentDescriptionId = R.string.content_description_bookmark_filled_icon,
    ),
    Certified(
        resourceId = R.drawable.ic_certified,
        contentDescriptionId = R.string.content_description_certified_icon,
    ),
    Success(
        resourceId = R.drawable.ic_success,
        contentDescriptionId = R.string.content_description_success_icon,
    ),
    Documents(
        resourceId = R.drawable.ic_documents,
        contentDescriptionId = R.string.content_description_documents_icon,
    ),
    Download(
        resourceId = R.drawable.ic_download,
        contentDescriptionId = R.string.content_description_download_icon,
    ),
    Filters(
        resourceId = R.drawable.ic_filters,
        contentDescriptionId = R.string.content_description_filters_icon,
    ),
    Home(
        resourceId = R.drawable.ic_home,
        contentDescriptionId = R.string.content_description_home_icon,
    ),
    Menu(
        resourceId = R.drawable.ic_menu,
        contentDescriptionId = R.string.content_description_menu_icon,
    ),
    Contract(
        resourceId = R.drawable.ic_contract,
        contentDescriptionId = R.string.content_description_signature_icon,
    ),
    InProgress(
        resourceId = R.drawable.ic_in_progress,
        contentDescriptionId = R.string.content_description_in_progress_icon,
    ),
    Notifications(
        resourceId = R.drawable.ic_notifications,
        contentDescriptionId = R.string.content_description_notifications_icon,
    ),
    Transactions(
        resourceId = R.drawable.ic_transactions,
        contentDescriptionId = R.string.content_description_transactions_icon,
    ),
    WalletActivated(
        resourceId = R.drawable.ic_wallet_activated,
        contentDescriptionId = R.string.content_description_wallet_activated_icon,
    ),
    WalletSecured(
        resourceId = R.drawable.ic_wallet_secured,
        contentDescriptionId = R.string.content_description_wallet_secured_icon,
    ),
    Info(
        resourceId = R.drawable.ic_info,
        contentDescriptionId = R.string.content_description_info_icon,
    ),
    IdCards(
        resourceId = R.drawable.ic_authenticate_id_cards,
        contentDescriptionId = R.string.content_description_issuer_icon,
    ),
    SignDocumentFromDevice(
        resourceId = R.drawable.ic_sign_document_from_device,
        contentDescriptionId = R.string.content_description_add_document_from_list_icon,
    ),
    SignDocumentFromQr(
        resourceId = R.drawable.ic_sign_document_from_qr,
        contentDescriptionId = R.string.content_description_add_document_from_qr_icon,
    ),
    ChangePin(
        resourceId = R.drawable.ic_change_pin,
        contentDescriptionId = R.string.content_description_change_pin_icon,
    ),
    Check(
        resourceId = R.drawable.ic_check,
        contentDescriptionId = R.string.content_description_check_icon,
    ),
    OpenInBrowser(
        resourceId = R.drawable.ic_open_in_browser,
        contentDescriptionId = R.string.content_description_open_in_browser_icon,
    ),
    DateRange(
        contentDescriptionId = R.string.content_description_date_range_icon,
        imageVector = Icons.Default.DateRange,
    ),
    Settings(
        resourceId = R.drawable.ic_settings,
        contentDescriptionId = R.string.content_description_settings_icon,
    ),
    BatchIssuanceCounter(
        resourceId = R.drawable.ic_batch_issuance_counter,
        contentDescriptionId = R.string.content_description_batch_issuance_counter_icon,
    )
    ;

    init {
        require(resourceId != null || imageVector != null) {
            "AppIconKey.$name must have a non-null resourceId or imageVector."
        }
    }
}

/**
 * Convenience constants — `AppIcons.Example` is exactly `IconDataUi(AppIconKey.Example)`.
 */
object AppIcons {
    val ArrowBack: IconDataUi = IconDataUi(AppIconKey.ArrowBack)
    val Close: IconDataUi = IconDataUi(AppIconKey.Close)
    val VerticalMore: IconDataUi = IconDataUi(AppIconKey.VerticalMore)
    val Warning: IconDataUi = IconDataUi(AppIconKey.Warning)
    val Error: IconDataUi = IconDataUi(AppIconKey.Error)
    val ErrorFilled: IconDataUi = IconDataUi(AppIconKey.ErrorFilled)
    val Delete: IconDataUi = IconDataUi(AppIconKey.Delete)
    val TouchId: IconDataUi = IconDataUi(AppIconKey.TouchId)
    val QR: IconDataUi = IconDataUi(AppIconKey.QR)
    val NFC: IconDataUi = IconDataUi(AppIconKey.NFC)
    val User: IconDataUi = IconDataUi(AppIconKey.User)
    val Id: IconDataUi = IconDataUi(AppIconKey.Id)
    val IdStroke: IconDataUi = IconDataUi(AppIconKey.IdStroke)
    val LogoIcon: IconDataUi = IconDataUi(AppIconKey.LogoIcon)
    val LogoIconAndText: IconDataUi = IconDataUi(AppIconKey.LogoIconAndText)
    val KeyboardArrowDown: IconDataUi = IconDataUi(AppIconKey.KeyboardArrowDown)
    val KeyboardArrowUp: IconDataUi = IconDataUi(AppIconKey.KeyboardArrowUp)
    val Visibility: IconDataUi = IconDataUi(AppIconKey.Visibility)
    val VisibilityOff: IconDataUi = IconDataUi(AppIconKey.VisibilityOff)
    val Add: IconDataUi = IconDataUi(AppIconKey.Add)
    val Edit: IconDataUi = IconDataUi(AppIconKey.Edit)
    val Sign: IconDataUi = IconDataUi(AppIconKey.Sign)
    val QrScanner: IconDataUi = IconDataUi(AppIconKey.QrScanner)
    val Verified: IconDataUi = IconDataUi(AppIconKey.Verified)
    val Message: IconDataUi = IconDataUi(AppIconKey.Message)
    val ClockTimer: IconDataUi = IconDataUi(AppIconKey.ClockTimer)
    val OpenNew: IconDataUi = IconDataUi(AppIconKey.OpenNew)
    val KeyboardArrowRight: IconDataUi = IconDataUi(AppIconKey.KeyboardArrowRight)
    val HandleBar: IconDataUi = IconDataUi(AppIconKey.HandleBar)
    val Search: IconDataUi = IconDataUi(AppIconKey.Search)
    val PresentDocumentInPerson: IconDataUi = IconDataUi(AppIconKey.PresentDocumentInPerson)
    val PresentDocumentOnline: IconDataUi = IconDataUi(AppIconKey.PresentDocumentOnline)
    val AddDocumentFromList: IconDataUi = IconDataUi(AppIconKey.AddDocumentFromList)
    val AddDocumentFromQr: IconDataUi = IconDataUi(AppIconKey.AddDocumentFromQr)
    val Bookmark: IconDataUi = IconDataUi(AppIconKey.Bookmark)
    val BookmarkFilled: IconDataUi = IconDataUi(AppIconKey.BookmarkFilled)
    val Certified: IconDataUi = IconDataUi(AppIconKey.Certified)
    val Success: IconDataUi = IconDataUi(AppIconKey.Success)
    val Documents: IconDataUi = IconDataUi(AppIconKey.Documents)
    val Download: IconDataUi = IconDataUi(AppIconKey.Download)
    val Filters: IconDataUi = IconDataUi(AppIconKey.Filters)
    val Home: IconDataUi = IconDataUi(AppIconKey.Home)
    val Menu: IconDataUi = IconDataUi(AppIconKey.Menu)
    val Contract: IconDataUi = IconDataUi(AppIconKey.Contract)
    val InProgress: IconDataUi = IconDataUi(AppIconKey.InProgress)
    val Notifications: IconDataUi = IconDataUi(AppIconKey.Notifications)
    val Transactions: IconDataUi = IconDataUi(AppIconKey.Transactions)
    val WalletActivated: IconDataUi = IconDataUi(AppIconKey.WalletActivated)
    val WalletSecured: IconDataUi = IconDataUi(AppIconKey.WalletSecured)
    val Info: IconDataUi = IconDataUi(AppIconKey.Info)
    val IdCards: IconDataUi = IconDataUi(AppIconKey.IdCards)
    val SignDocumentFromDevice: IconDataUi = IconDataUi(AppIconKey.SignDocumentFromDevice)
    val SignDocumentFromQr: IconDataUi = IconDataUi(AppIconKey.SignDocumentFromQr)
    val ChangePin: IconDataUi = IconDataUi(AppIconKey.ChangePin)
    val Check: IconDataUi = IconDataUi(AppIconKey.Check)
    val OpenInBrowser: IconDataUi = IconDataUi(AppIconKey.OpenInBrowser)
    val DateRange: IconDataUi = IconDataUi(AppIconKey.DateRange)
    val Settings: IconDataUi = IconDataUi(AppIconKey.Settings)
    val BatchIssuanceCounter: IconDataUi = IconDataUi(AppIconKey.BatchIssuanceCounter)
}