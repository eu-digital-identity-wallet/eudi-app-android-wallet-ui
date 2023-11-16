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

package eu.europa.ec.eudi.wallet.ui.util

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter

object BindingAdapters {

    /**
     * A Binding Adapter that is called whenever the value of the attribute `app:engagementView`
     * changes. Receives a view with the QR Code for the device engagement.
     */
    @BindingAdapter("app:engagementView")
    @JvmStatic
    fun engagementView(view: LinearLayout, viewEngagement: View?) {
        viewEngagement?.let {
            (viewEngagement.parent as? ViewGroup)?.removeView(viewEngagement)
            view.addView(it)
        }
    }

    /**
     * A Binding Adapter that is called whenever the value of the attribute `app:enableDisable`
     * changes. It enables or disables a view.
     */
    @BindingAdapter("app:enableDisable")
    @JvmStatic
    fun setEnableDisable(view: View, enable: Boolean) {
        view.isEnabled = enable
    }

    /**
     * A Binding Adapter that is called whenever the value of the attribute `app:visibleGone`
     * changes. It changes the visibility of a view.
     */
    @BindingAdapter("app:visibleGone")
    @JvmStatic
    fun setVisibleGone(view: View, enable: Boolean) {
        view.isVisible = enable
    }
}