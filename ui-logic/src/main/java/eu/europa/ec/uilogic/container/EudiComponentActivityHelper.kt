/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.uilogic.container

import android.content.Intent
import android.os.Bundle
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import java.security.MessageDigest

/**
 * The saved state the host activity passes on when it is created. A configuration change keeps the
 * screens. A process that has not yet started its navigation, such as one Android restarted after
 * ending it in the background, restores none: the wallet is locked then, and a restored screen would
 * start its work, such as fetching a request or advertising over Bluetooth, before the login check.
 */
internal fun getStateToRestore(
    savedInstanceState: Bundle?,
    navigationStartedInProcess: Boolean,
): Bundle? = savedInstanceState.takeIf { navigationStartedInProcess }

/**
 * Whether the host activity opens the intent it was created with. An activity recreated after a
 * configuration change, or after Android ended the process, is given the intent of its first start
 * again; that link or request was handled then, and opening it again would replay it.
 */
internal fun shouldOpenLaunchIntent(savedInstanceState: Bundle?): Boolean =
    savedInstanceState == null

/**
 * Whether an incoming link of [type] is dropped. A link that starts no wallet flow
 * ([DeepLinkType.EXTERNAL]) passes none of the manifest's filters, so it can only come from an app
 * that addressed the wallet directly; keeping it would let that app have the wallet open any link
 * once the user logs in.
 */
internal fun shouldDropIncomingLink(type: DeepLinkType): Boolean = type == DeepLinkType.EXTERNAL

/**
 * Whether declining a Digital Credential API request also finishes the activity. It does when the
 * request itself brought the wallet up: on a [coldBoot], or in a task restored after its process
 * ended, where the wallet was not [walletWasShown]. A wallet already open stays as it was.
 */
internal fun shouldFinishOnDeclinedRequest(coldBoot: Boolean, walletWasShown: Boolean): Boolean =
    coldBoot || !walletWasShown

/**
 * What the host activity records once a screen has used the link the task was started with: the task
 * and a SHA-256 fingerprint of the link, so that the record holds no link.
 */
internal fun createLaunchLinkRecord(taskId: Int, link: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(link.toByteArray(Charsets.UTF_8))
    return "$taskId:" + digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

/** The package of the system UI, which restarts an app's task after an update and from Recents. */
internal const val SYSTEM_UI_PACKAGE = "com.android.systemui"

/**
 * Whether a launch intent with [launchFlags] and [link], started by [launchedBy] in task [taskId], is
 * Android restarting the wallet's task with the link that first started it, rather than a link the
 * user has just opened.
 *
 * Android restarts a task without saved state after an app update, after a force-stop, and from
 * Recents once the user has left the wallet with Back. It gives the new activity the task's first
 * intent again, with [Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY] when it uses the Recents path, or
 * with [Intent.FLAG_ACTIVITY_CLEAR_TASK] when the system UI starts it, as the reference emulator does.
 * An app may send a link with CLEAR_TASK too, so that flag counts only when the system UI is the
 * caller Android recorded ([eu.europa.ec.uilogic.extension.launchingPackage]). A replay is recognised
 * only when, in addition,
 * [recorded], the [createLaunchLinkRecord] of the last launch link a screen used, names this task and
 * this link.
 */
internal fun isReplayedLaunchLink(
    launchFlags: Int,
    launchedBy: String?,
    taskId: Int,
    link: String,
    recorded: String,
): Boolean {
    val fromRecents = (launchFlags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
    val restartedBySystemUi = (launchFlags and Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0
            && launchedBy == SYSTEM_UI_PACKAGE
    return (fromRecents || restartedBySystemUi) && recorded == createLaunchLinkRecord(taskId, link)
}