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

package eu.europa.ec.uilogic.component.snackbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import eu.europa.ec.resourceslogic.theme.values.onSuccess
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.snackbar.Snackbar.Builder
import eu.europa.ec.uilogic.component.snackbar.Snackbar.SnackbarType
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.Z_SNACKBAR
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ## SUMMARY
 * ### 1. Introduction
 * Helper class that is used to easily initialize, build and show a
 * [androidx.compose.material3.Snackbar] in your ***Jetpack compose*** project. Please, make sure to
 * read all the instructions in order to make this tool work correctly.
 *
 * ### 2. Points of interest
 * You should notice some points here. Since we are developing at ***Jetpack compose***, there are
 * some variations on the usage of [androidx.compose.material3.Snackbar] with classic Android Kotlin
 * framework. For example, a [Snackbar] must have a valid placeholder to be placed correctly
 * inside your layout. For that, you should place [Snackbar.PlaceHolder] inside your layout. That
 * is the place where a [Snackbar] will be placed.
 *
 * ### 3. Styling
 * This helper class contains a variety of types defined in [SnackbarType]. Each type will style
 * the [Snackbar] with different colors. For example, [SnackbarType.Warning] will show an orange
 * [Snackbar]. Make sure to check all styles.
 *
 * ## INITIALIZATION - BUILDING - USAGE
 * ### 1. Initialize
 * As you have already noticed, [Snackbar] class has a private constructor that will not allow you
 * to directly initialize an instance. Instead, you can construct an instance by calling the
 * [Builder] inner class.
 *
 * ### 2. Building
 * As said in section 1, you can use the [Builder] inner class to initialize and build an instance
 * of [Snackbar]. [Builder] class contains helper functions like [Builder.withMessage] that will
 * initialize an instance. When ready, you can call [Builder.build] to build the [Snackbar]
 * instance.
 *
 * Notice here that all [Builder] initialize functions are returning that [Builder] instance to help
 * you initialize a [Snackbar] easier.
 *
 * ### 3. Usage
 * When you have built a [Snackbar] instance, then you are ready to show it on your layout. You can
 * directly call [Snackbar.show] to show the [Snackbar]. Directly calling [Snackbar.show] will
 * result of an un-styled [Snackbar] or in the worst case, yo a [RuntimeException]. The reason
 * is that you need to call [Snackbar.PlaceHolder] first to initialize [SnackbarHostState]. That
 * state is responsible for placing the [Snackbar] in your layout while also overriding the style.
 *
 * [SnackbarHostState] is initialized from you. You can place that state inside a [Row] or a
 * [Column] or wherever you want. Some ***Jetpack Compose*** elements have a placeholder for that
 * state. Consider using that placeholders to correctly showing your [Snackbar].
 *
 * For example, a [Scaffold] does have a state placeholder. Using that will result of a [Snackbar]
 * showing above the [BottomAppBar] or [FloatingActionButton].
 *
 * ## EXAMPLE
 * ## 0. Deployment
 * You first need to place the [Snackbar.PlaceHolder] in the place that you want to show your
 * snackbar.
 * ```
Snackbar.PlaceHolder(snackbarHostState = snackbarState)
 * ```
 *
 * ## 1. In plain layout
 * ```
val scope = rememberCoroutineScope()
val snackbarHostState = remember { SnackbarHostState() }
val snackbar = Snackbar.Builder()
.withMessage(data.message)
.ofType(Snackbar.SnackbarType.Warning)
.withDuration(Snackbar.SnackbarDurationType.Sticky)
.withAction(data.actionLabel, data.onAction)
.build()
scope.launch {
snackbar.show()
}
 * ```
 *
 * ## 2. In [Scaffold]
 * ```
val scope = rememberCoroutineScope()
val snackbarHostState = remember { SnackbarHostState() }
Scaffold(
...
snackbarHost = { snackbarState ->
state.snackbarData?.let { data ->
val snackbar = Snackbar.Builder()
.withMessage(data.message)
.ofType(Snackbar.SnackbarType.Warning)
.withDuration(Snackbar.SnackbarDurationType.Sticky)
.withAction(data.actionLabel, data.onAction)
.build()
scope.launch {
snackbar.show()
}
}
}
...
)
 * ```
 */
class Snackbar private constructor(private val data: SnackbarValue) {

    companion object {

        // ╔═══════════════════════════════════════════════════════════════════════════════════════╗
        // ║ VARIABLES                                                                             ║
        // ╚═══════════════════════════════════════════════════════════════════════════════════════╝
        // ┌───────────────────────────────────────────────────────────────────────────────────────┐
        //   → PRIVATE VARIABLES
        // └───────────────────────────────────────────────────────────────────────────────────────┘

        /**
         * State of the SnackbarHost.
         */
        private var snackbarHostState: SnackbarHostState? = null

        /**
         * Map containing all snackbar data values with their ids.
         */
        private val snackbarValues: MutableMap<String, SnackbarValue> = mutableMapOf()

        /**
         * Flag that defines if a [Snackbar] is currently being shown. You can observe this values
         * to get updates of visibility events.
         */
        private val isShowingData: MutableLiveData<Boolean> = MutableLiveData(false)

        // ┌───────────────────────────────────────────────────────────────────────────────────────┐
        //   → PUBLIC VARIABLES
        // └───────────────────────────────────────────────────────────────────────────────────────┘

        /**
         * Flag that defines if a [Snackbar] is currently being shown. You can observe this values
         * to get updates of visibility events.
         */
        val isShowing: LiveData<Boolean>
            get() = isShowingData

        /**
         * Snackbar size in pixes. Will be 0 if no [Snackbar] is shown yet. First [Snackbar] will
         * also calculate this height.
         */
        var snackbarHeight = 0

        // ╔═══════════════════════════════════════════════════════════════════════════════════════╗
        // ║ FUNCTIONS                                                                             ║
        // ╚═══════════════════════════════════════════════════════════════════════════════════════╝
        // ┌───────────────────────────────────────────────────────────────────────────────────────┐
        //   → PUBLIC FUNCTIONS
        // └───────────────────────────────────────────────────────────────────────────────────────┘

        /**
         * Composable function that is used to place the [SnackbarHost] in your layout. You should
         * call this function either in your layout body or in your [Scaffold] inside `snackbarHost`
         * callback so snackbar will be placed correctly abode your bottom bar and fab.
         *
         * Not calling this operation will still make this class work ***but*** snackbar will not be
         * styled correctly.
         *
         * For more information using [Snackbar], consider reading [Snackbar] docs.
         *
         * @param snackbarHostState [SnackbarHostState] used to initialize [SnackbarHost] and place
         * [Snackbar].
         */
        @Composable
        fun PlaceHolder(snackbarHostState: SnackbarHostState) {
            // Check is host state is the same as previous.
            if (Companion.snackbarHostState != snackbarHostState) dismissAll()

            // Store host state.
            Companion.snackbarHostState = snackbarHostState

            // Get current data.
            snackbarHostState.currentSnackbarData?.visuals?.message?.let { currentId ->
                snackbarValues[currentId]?.let { data ->
                    // Deploy placeholder.
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                            .onGloballyPositioned { layoutCoordinates ->
                                snackbarHeight = layoutCoordinates.size.height
                            }
                            .zIndex(Z_SNACKBAR),
                        snackbar = {
                            ConstraintLayout {
                                val snackBar = createRef()
                                Card(
                                    shape = RectangleShape,
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = data.type.colorBackground(),
                                        contentColor = data.type.colorOnBackground()
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .constrainAs(snackBar) {
                                            start.linkTo(parent.start)
                                            end.linkTo(parent.end)
                                            bottom.linkTo(data.anchor ?: parent.bottom)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(
                                            vertical = SIZE_SMALL.dp,
                                            horizontal = SIZE_MEDIUM.dp
                                        ),
                                    ) {
                                        if (data.hasAction()) SnackbarSimpleAction(data)
                                        else SnackbarSimple(data)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        /**
         * Dismisses current [Snackbar] and also, clears the [Snackbar] queue. Dismiss callback of
         * current snackbar will still be invoked.
         */
        fun dismissAll() {
            // Clear snackbar array.
            snackbarValues.clear()

            // Update current snackbar.
            snackbarHostState?.currentSnackbarData?.dismiss()

            // Also update visibility flag.
            isShowingData.postValue(false)
        }

        // ┌───────────────────────────────────────────────────────────────────────────────────────┐
        //   → PRIVATE FUNCTIONS
        // └───────────────────────────────────────────────────────────────────────────────────────┘

        /**
         * Function that composes a snackbar with a message and an action button.
         *
         * @see SnackbarSimple()
         */
        @Composable
        private fun SnackbarSimpleAction(data: SnackbarValue) {
            return Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = data.message,
                    color = data.type.colorOnBackground(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = data.onAction ?: {},
                ) {
                    Text(
                        text = data.actionLabel ?: "",
                        color = data.type.colorOnBackground().copy(alpha = 0.7f),
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }

        /**
         * Function that composes a snackbar with a message.
         *
         * @see SnackbarSimpleAction()
         */
        @Composable
        private fun SnackbarSimple(data: SnackbarValue) {
            return Text(
                modifier = Modifier.padding(vertical = SIZE_SMALL.dp),
                text = data.message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ FUNCTIONS                                                                                 ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → PUBLIC FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * Shows or queues to be shown a [Snackbar].
     *
     * This operation guarantees to show at most one snackbar at a time. If this function is
     * called while another snackbar is already visible, it will be suspended until this snack
     * bar is shown and subsequently addressed. If the caller is cancelled, the snackbar will be
     * removed from display and/or the queue to be displayed.
     *
     * @param owner [LifecycleOwner] used to launch show suspend function.
     */
    fun show(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            // Initialize an id.
            val id = UUID.randomUUID().toString()

            // Add data to map.
            snackbarValues[id] = data

            // Update showing flag.
            isShowingData.postValue(true)

            // Show snackbar and get result.
            val result = snackbarHostState?.showSnackbar(
                message = id,
                duration = data.duration.asSnackbarDuration()
            )

            // Remove data from list.
            snackbarValues.remove(id)

            // Update showing flag.
            if (snackbarValues.isEmpty()) isShowingData.postValue(false)

            // Check result and perform action if action was performed.
            if (result == SnackbarResult.ActionPerformed) data.onAction?.invoke()

            // Notify on dismiss listener.
            data.onDismiss?.invoke(snackbarValues.isNotEmpty())
        }
    }

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ BUILDER CLASS                                                                             ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝

    /**
     * ### SUMMARY
     * [Snackbar] helping class that is used to easily initialize and build a snackbar.
     *
     * ### OPERATIONS
     * You must start by initializing an instance of this [Builder]. By calling the assisting
     * functions, you can set the data you want for the snackbar being built. Notice that some of
     * the data are required (message etc.).
     *
     * All snackbar data can be initialized here. You can set the message for example by calling
     * [Builder.withMessage]. All functions return the same instance of [Builder] to easily help
     * you construct a [Snackbar].
     *
     * ### BUILDING
     * When you are done setting the snackbar values, you can call [Builder.build] in order to
     * construct final [Snackbar].
     */
    class Builder {

        /**
         * [SnackbarValue] instance used to initialize builder and later on, [Snackbar].
         */
        private var data: SnackbarValue = SnackbarValue()

        /**
         * (Optional) Initializes this [Builder] with the given [SnackbarValue]. You can
         * still use other [Builder] functions to change specific or all values. Notice
         * that calling this function will override any previous set values.
         *
         * @param data [SnackbarValue] data used to initialize this [Builder].
         */
        fun ofData(data: SnackbarValue): Builder {
            this.data = data
            return this
        }

        /**
         * (Optional) Sets the type of the snackbar to build. Type defines the style of the
         * snackbar (info, warning etc.).
         *
         * @param type Type of snackbar to build.
         *
         * @return This [Builder] instance for chaining.
         */
        fun ofType(type: SnackbarType): Builder {
            data = data.copy(
                type = type
            )
            return this
        }

        /**
         * Sets the message of this snackbar. Setting an empty string here will result a void
         * snackbar (will not show).
         *
         * @param message Message shown in the snackbar. In case message is too big, message will be
         * clipped to a maximum of 2 lines.
         *
         * @return This [Builder] instance for chaining.
         */
        fun withMessage(message: String): Builder {
            data = data.copy(
                message = message
            )
            return this
        }

        /**
         * (Optional) Sets the duration of the snackbar to build. Duration defines how much time
         * the snackbar will be visible.
         *
         * @param duration Duration of snackbar to build.
         *
         * @return This [Builder] instance for chaining.
         */
        fun withDuration(duration: SnackbarDurationType): Builder {
            data = data.copy(
                duration = duration
            )
            return this
        }

        /**
         * (Optional) Sets given [anchor] reference as deployment reference of the [Snackbar]. When
         * shown, [Snackbar] will be aligned at the top of this [anchor]. If not set, parent bottom
         * border will be used as anchor.
         *
         * @param anchor [ConstraintLayoutBaseScope.HorizontalAnchor] user as bottom anchor of the
         * [Snackbar].
         *
         * @return This [Builder] instance for chaining.
         */
        fun withAnchor(anchor: ConstraintLayoutBaseScope.HorizontalAnchor?): Builder {
            data = data.copy(
                anchor = anchor
            )
            return this
        }

        /**
         * (Optional) Initializes the action button of this snackbar. Both [actionLabel] and
         * [onAction] must be valid in order to show the action button.
         *
         * @param actionLabel Defines the label of the action button of the snackbar.
         * @param onAction    Defines the action to perform when user clicks the action
         * button of the snackbar.
         *
         * @return This [Builder] instance for chaining.
         */
        fun withAction(actionLabel: String?, onAction: (() -> Unit)?): Builder {
            data = data.copy(
                actionLabel = actionLabel,
                onAction = onAction
            )
            return this
        }

        /**
         * (Optional) Initializes the action to perform when a snackbar is dismissed. Listener will
         * be notified when snackbar is dismissed either by timeout or by action.
         *
         * @param onDismiss The action to perform when this specific snackbar is
         * dismissed. Snackbar can be dismissed by timeout or by action. A [Boolean] flag is
         * returned to notified listener to inform if there are more snackbar s in the queue.
         */
        fun onDismiss(onDismiss: ((Boolean) -> Unit)?): Builder {
            data = data.copy(
                onDismiss = onDismiss
            )
            return this
        }

        /**
         * Constructs the [Snackbar] based on the settings of the [Builder] class.
         */
        fun build(): Snackbar {
            // Data is valid. Construct snackbar.
            return Snackbar(data = data)
        }
    }

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ DATA CLASS                                                                                ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝

    /**
     * Data class that contains the data used to initialize a [Snackbar].
     *
     * @param message     Message shown in the snackbar. In case message is too big, message will be
     * clipped to a maximum of 2 lines.
     * @param type        (Optional) Type of snackbar (default is [SnackbarType.Success].
     * @param duration    (Optional) Duration of snackbar (default is [SnackbarDurationType.Short]).
     * @param anchor      (Optional) [ConstrainedLayoutReference] used as bottom anchor. [Snackbar]
     * will be anchored to top of this reference.
     * @param actionLabel (Optional) Defines the label of the action button of the snackbar.
     * @param onAction    (Optional) Defines the action to perform when user clicks the action
     * button of the snackbar.
     * @param onDismiss   (Optional) Defines the action to perform when this specific snackbar is
     * dismissed. Snackbar can be dismissed by timeout or by action. A [Boolean] flag is returned
     * to notified listener to inform if there are more snackbar s in the queue.
     */
    data class SnackbarValue(
        val message: String = "",
        val type: SnackbarType = SnackbarType.Success,
        val duration: SnackbarDurationType = SnackbarDurationType.Short,
        val anchor: ConstraintLayoutBaseScope.HorizontalAnchor? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
        val onDismiss: ((Boolean) -> Unit)? = null
    ) {
        /**
         * Defines if the data set has a valid action to be used for [Snackbar]. If this returns
         * `false`, not action button will be shown in the [Snackbar] even if you have initialized
         * one value.
         *
         * @return `true` if action is valid and must be shown in the [Snackbar]. `false`
         * otherwise.
         */
        fun hasAction(): Boolean {
            return actionLabel.isNullOrEmpty().not() && onAction != null
        }
    }

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ TYPE CLASS                                                                                ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝

    /**
     * Sealed class that contains all the available [Snackbar] types.
     */
    sealed class SnackbarType {

        /**
         * Used to show a success snackbar.
         */
        data object Success : SnackbarType()

        /**
         * Used to show an error snackbar.
         */
        data object Error : SnackbarType()

        /**
         * Returns a [Color] based on this [SnackbarType] that must be used as the snackbar
         * background.
         *
         * @return Corresponding [Color] for snackbar background.
         */
        @Composable
        fun colorBackground(): Color {
            return when (this) {
                is Success -> MaterialTheme.colorScheme.success
                is Error -> MaterialTheme.colorScheme.error
            }
        }

        /**
         * Returns a [Color] based on this [SnackbarType] that must be used as the snackbar
         * content color.
         *
         * @return Corresponding [Color] for snackbar content.
         */
        @Composable
        fun colorOnBackground(): Color {
            return when (this) {
                is Success -> MaterialTheme.colorScheme.onSuccess
                is Error -> MaterialTheme.colorScheme.onError
            }
        }

    }

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ SUB-TYPE CLASS                                                                            ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝

    /**
     * Sealed class that contains all the available [Snackbar] duration types.
     */
    sealed class SnackbarDurationType {
        /**
         * Defines that this snackbar should be shown for a short period of time. User os settings
         * do not apply.
         */
        data object Short : SnackbarDurationType()

        /**
         * Defines that this snackbar should be shown for a long period of time. User os settings
         * apply.
         */
        data object Long : SnackbarDurationType()

        /**
         * Defines that this snackbar should not be hidden automatically unless user clicks the
         * action button or it is hidden programmatically.
         */
        data object Sticky : SnackbarDurationType()

        /**
         * Returns the corresponding [SnackbarDuration] based on this type.
         *
         * @return [SnackbarDuration] corresponding to this type.
         */
        fun asSnackbarDuration(): SnackbarDuration {
            return when (this) {
                is Short -> SnackbarDuration.Short
                is Long -> SnackbarDuration.Long
                is Sticky -> SnackbarDuration.Indefinite
            }
        }
    }
}