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

package eu.europa.ec.commonfeature.util

object DocumentJsonKeys {
    const val FIRST_NAME = "given_name"
    const val LAST_NAME = "family_name"
    const val PORTRAIT = "portrait"
    const val SIGNATURE = "signature_usual_mark"
    const val EXPIRY_DATE = "expiry_date"
    const val USER_PSEUDONYM = "user_pseudonym"
    private const val GENDER = "gender"
    private const val SEX = "sex"

    val GENDER_KEYS: List<String> = listOf(GENDER, SEX)
    val BASE64_IMAGE_KEYS: List<String> = listOf(PORTRAIT, SIGNATURE)
}