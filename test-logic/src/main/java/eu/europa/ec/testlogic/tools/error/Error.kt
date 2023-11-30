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

package eu.europa.ec.testlogic.tools.error

const val plainErrorMessage = "error message"
const val plainFailedMessage = "failed message"
const val mockedGenericErrorMessage = "resourceProvider's genericErrorMessage"
const val code400 = 400
const val exceptionError = "fromJson(...) must not be null"
const val errorResponseJson = "{\"message\": \"$plainErrorMessage\"}"

//val errorResponseBody: ResponseBody get() = errorResponseJson.toResponseBody()

//val errorResponse: ResponseError
//    get() = Gson().fromJson(
//        errorResponseJson,
//        ResponseError::class.java
//    )
//
//val exceptionResponseBody: ResponseBody = "".toResponseBody()

val mockedException = RuntimeException("Exception to test interactor.")