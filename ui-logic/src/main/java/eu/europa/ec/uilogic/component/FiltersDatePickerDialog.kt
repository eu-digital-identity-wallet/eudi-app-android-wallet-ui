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

package eu.europa.ec.uilogic.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.europa.ec.resourceslogic.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DatePickerDialogType {
    SelectStartDate, SelectEndDate
}

data class DatePickerDialogConfig(
    val type: DatePickerDialogType,
    val lowerLimit: LocalDate? = LocalDate.MIN,
    val upperLimit: LocalDate? = LocalDate.MAX,
    val selectedUtcDateMillis: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersDatePickerDialog(
    datePickerDialogConfig: DatePickerDialogConfig,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val customSelectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val date = Instant.ofEpochMilli(utcTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val min = datePickerDialogConfig.lowerLimit ?: LocalDate.MIN
            val max = datePickerDialogConfig.upperLimit ?: LocalDate.MAX
            return !date.isBefore(min) && !date.isAfter(max)
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = datePickerDialogConfig.selectedUtcDateMillis,
        selectableDates = customSelectableDates
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.generic_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}