/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.businesslogic.model

import com.google.gson.annotations.SerializedName

/**
 * Data class used to represent the Biometric information saved in shared preferences and are needed
 * to validate the biometric authentication.
 *
 * @param randomString          The random String that is used for biometric validation.
 * @param encryptedString       The random String encrypted with biometric cipher and in base64 form.
 * @param ivString              The iv used in biometric cipher in base64 form.
 */
data class BiometricData(
    @SerializedName("random") val randomString: String,
    @SerializedName("encrypted") val encryptedString: String,
    @SerializedName("iv") val ivString: String,
)