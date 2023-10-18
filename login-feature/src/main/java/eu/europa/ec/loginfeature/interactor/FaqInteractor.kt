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

package eu.europa.ec.loginfeature.interactor

import eu.europa.ec.loginfeature.model.FaqUiModel

interface FaqInteractor {
    fun initializeData(): List<FaqUiModel>
}

class FaqInteractorImpl : FaqInteractor {

    override fun initializeData(): List<FaqUiModel> = listOf(
        FaqUiModel(
            title = "Question A goes Here",
            description = "Lorem ipsum dolor sit amet," +
                    " consectetur adipiscing elit,"
        ),
        FaqUiModel(
            title = "Question B goes Here",
            description = "Duis aute irure dolor in reprehenderit in" +
                    " voluptate velit esse cillum dolore eu fugiat nulla pariatur."
        ),
        FaqUiModel(
            title = "Question C goes Here",
            description = "Excepteur sint occaecat cupidatat non proident, " +
                    "sunt in culpa qui officia deserunt mollit anim id est laborum."
        ),
        FaqUiModel(
            title = "Question D goes Here",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                    "sed  magn laboris nisi ut aliquip ex ea commodo consequat."
        ),
        FaqUiModel(
            title = "Question E goes Here",
            description = "Duis aute irure dolor in reprehenderit" +
                    " in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
        ),
        FaqUiModel(
            title = "Question F goes Here",
            description = "Excepteur sint occaecat cupidatat non proident, " +
                    "sunt in culpa qui officia deserunt mollit anim id est laborum."
        ),
        FaqUiModel(
            title = "Question A goes Here",
            description = "Lorem ipsum dolor sit amet," +
                    " consectetur adipiscing elit,"
        ),
        FaqUiModel(
            title = "Question B goes Here",
            description = "Duis aute irure dolor in reprehenderit in" +
                    " voluptate velit esse cillum dolore eu fugiat nulla pariatur."
        ),
        FaqUiModel(
            title = "Question C goes Here",
            description = "Excepteur sint occaecat cupidatat non proident, " +
                    "sunt in culpa qui officia deserunt mollit anim id est laborum."
        ),
        FaqUiModel(
            title = "Question D goes Here",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                    "sed  magn laboris nisi ut aliquip ex ea commodo consequat."
        ),
        FaqUiModel(
            title = "Question E goes Here",
            description = "Duis aute irure dolor in reprehenderit" +
                    " in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
        ),
        FaqUiModel(
            title = "Question F goes Here",
            description = "Excepteur sint occaecat cupidatat non proident, " +
                    "sunt in culpa qui officia deserunt mollit anim id est laborum."
        )
    )
}