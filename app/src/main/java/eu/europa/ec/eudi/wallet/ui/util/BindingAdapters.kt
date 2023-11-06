/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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