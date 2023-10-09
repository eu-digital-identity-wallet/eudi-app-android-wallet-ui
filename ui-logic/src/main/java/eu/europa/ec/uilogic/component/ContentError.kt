package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.europa.ec.resourceslogic.R

@Composable
fun ContentError(
    errorTitle: String = stringResource(id = R.string.generic_error_message),
    errorSubTitle: String = stringResource(id = R.string.generic_error_retry),
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    ContentScreen(
        navigatableAction = onCancel?.let {
            ScreenNavigateAction.CANCELABLE
        } ?: ScreenNavigateAction.NONE,
        onBack = onCancel ?: {},
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(it),
        ) {
            ContentTitle(
                title = errorTitle,
                subtitle = errorSubTitle,
                subTitleMaxLines = 10
            )
            onRetry?.let { callback ->
                WrapPrimaryButton(
                    onClick = {
                        callback()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.generic_error_button_retry),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Preview composable of [ContentError].
 */
@Preview
@Composable
fun PreviewContentErrorScreen() {
    ContentError(
        onRetry = {},
        onCancel = {}
    )
}