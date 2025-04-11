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

package eu.europa.ec.businesslogic.extension

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.annotation.CheckResult

@CheckResult
inline fun <reified T : Parcelable> Intent?.getParcelableArrayListExtra(
    action: String
): List<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this?.getParcelableArrayListExtra(
            action,
            T::class.java
        )
    } else {
        @Suppress("DEPRECATION")
        this?.getParcelableArrayListExtra(action)
    }?.filterNotNull()
}