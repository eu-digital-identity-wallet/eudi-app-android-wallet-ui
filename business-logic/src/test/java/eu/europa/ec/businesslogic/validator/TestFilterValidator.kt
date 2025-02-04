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

package eu.europa.ec.businesslogic.validator

import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before

class TestFilterValidator {

    @get:org.junit.Rule
    val coroutineRule = CoroutineTestRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var filterValidator: FilterValidator

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)
        filterValidator = FilterValidatorImpl(testDispatcher)
    }

}