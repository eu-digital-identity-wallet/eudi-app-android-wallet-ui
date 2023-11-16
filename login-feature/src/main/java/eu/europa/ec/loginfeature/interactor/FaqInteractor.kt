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