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

package eu.europa.ec.eudi.web.lightIssuing

import androidx.appcompat.app.AppCompatActivity
import java.util.Base64

class EudiPidIssuerActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        EudiPidIssuer.bytes = try {
            intent?.data?.getQueryParameter("mdoc")
                ?.let { Base64.getUrlDecoder().decode(it) }
                ?.let {
                    try {
                        Base64.getUrlDecoder().decode(it)
                    } catch (_: Exception) {
                        Base64.getDecoder().decode(it)
                    }
                }
        } catch (_: Exception) {
            null
        }
        finish()
    }
}