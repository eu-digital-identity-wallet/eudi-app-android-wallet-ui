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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.corelogic.controller.WalletResetStatus
import eu.europa.ec.corelogic.di.getOrNullKoinScope
import eu.europa.ec.corelogic.provider.DCAPISettingProvider
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.uilogic.config.ConfigUILogic
import eu.europa.ec.uilogic.extension.exposeTestTagsAsResourceId
import eu.europa.ec.uilogic.extension.launchingPackage
import eu.europa.ec.uilogic.extension.reportAccessibilityInteraction
import eu.europa.ec.uilogic.extension.setScreenCapturePrevented
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.RouterHost
import eu.europa.ec.uilogic.navigation.helper.DeepLinkAction
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.IntentType
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.navigation.helper.hasIntentAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The wallet's host activity. Besides hosting the navigation graph, it drives the session lock:
 * when the app returns to the foreground an expired session is sent to login, and an idle check
 * runs while the activity is visible. The app counts as in the background only when the whole
 * process is. Another activity of the app, such as the RQES SDK's signing screens, is tracked
 * apart: the wallet cannot see interaction there, so returning from it after the protected-flow
 * timeout requires login. Both are recorded by [registerSessionLockObservers], registered
 * once for the process.
 */
open class EudiComponentActivity : FragmentActivity() {

    private val routerHost: RouterHost by inject()
    private val configUILogic: ConfigUILogic by inject()
    private val sessionController: SessionController by inject()
    private val walletResetStatus: WalletResetStatus by inject()
    private val viewModel: EudiComponentActivityViewModel by viewModel()

    private var idleLockJob: Job? = null

    private var opensItsLaunchIntent: Boolean = true

    private companion object {

        /** How often [runWhenNavigationReady] looks for the navigation controller, and how many times. */
        val NAVIGATION_READY_POLL_INTERVAL = 100.milliseconds
        const val NAVIGATION_READY_ATTEMPTS = 50

        /** The shortest wait between two idle checks. */
        val MIN_IDLE_CHECK_INTERVAL = 1.seconds

        /** Whether a host activity has started its navigation in this process. */
        @Volatile
        var navigationStartedInProcess: Boolean = false
    }

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            checkSessionOnForeground()
        }
    }

    /**
     * Restores the saved screens only as [getStateToRestore] allows, and opens the launch intent only
     * as [shouldOpenLaunchIntent] allows and unless it replays the link that first started this task
     * (see [EudiComponentActivityViewModel.shouldOpenLaunchLink]). Screen-reader actions in the
     * activity's window count as interaction for the idle lock ([reportAccessibilityInteraction]).
     * The window is kept out of screen capture when
     * [ConfigUILogic.shouldPreventScreenCapture] asks.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(getStateToRestore(savedInstanceState, navigationStartedInProcess))
        setScreenCapturePrevented(configUILogic.shouldPreventScreenCapture)
        opensItsLaunchIntent =
            shouldOpenLaunchIntent(savedInstanceState) && viewModel.shouldOpenLaunchLink(
                intent = intent,
                taskId = taskId,
                isTaskRoot = isTaskRoot,
                launchedBy = launchingPackage(),
            )
        viewModel.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
        window.decorView.reportAccessibilityInteraction { onUserInteraction() }
        forgetKeptLinkOnReset()
    }

    /** A factory reset forgets the link kept for later, so that it does not open after onboarding. */
    private fun forgetKeptLinkOnReset() {
        lifecycleScope.launch {
            walletResetStatus.isResetting.collect { resetting ->
                if (resetting) viewModel.clearCachedIntent()
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
        super.onDestroy()
    }

    /**
     * Keeps [intent] until a screen opens it after login. While the wallet is locked a kept link
     * is not cleared, so that it survives until the user has logged in.
     */
    internal fun cacheIntent(intent: Intent?) {
        viewModel.cacheIntent(intent)
    }

    /**
     * The kept link, or null while the wallet is locked: a screen restored before login, such as
     * the dashboard after the activity is recreated, must not open it.
     */
    internal fun getCachedIntent(): Intent? = viewModel.getCachedIntent()

    @Composable
    protected fun Content(
        intent: Intent?,
        builder: NavGraphBuilder.(NavController) -> Unit,
    ) {
        ThemeManager.instance.Theme {
            Surface(
                modifier = Modifier
                    .exposeTestTagsAsResourceId()
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                routerHost.StartFlow {
                    builder(it)
                }
                LaunchedEffect(Unit) {
                    navigationStartedInProcess = true
                    viewModel.onFlowStart()
                    routerHost.requireLoginIfLocked()
                    if (opensItsLaunchIntent) {
                        handleDeepLink(intent, coldBoot = true)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        checkSessionAfterCover()
    }

    /**
     * Sends a locked wallet to login when it returns from another activity of the app after the
     * protected-flow timeout. Runs before a new intent is handled, and again on resume.
     */
    private fun checkSessionAfterCover() {
        if (sessionController.onWalletScreenShown() && viewModel.hasFlowStarted()) {
            routerHost.requireLoginIfLocked()
        }
    }

    override fun onStart() {
        super.onStart()
        sessionController.onUserInteraction()
        scheduleIdleLock()
    }

    override fun onStop() {
        super.onStop()
        idleLockJob?.cancel()
        sessionController.recordProtectedFlow(routerHost.isInProtectedFlow())
    }

    /**
     * Applies the background timeout when the app returns to the foreground, before anything can
     * navigate, and sends a locked wallet to login. A recreated activity has no navigation state
     * until its first composition; its login check then runs from [Content].
     */
    private fun checkSessionOnForeground() {
        sessionController.onForeground()
        if (routerHost.isNavigationReady() && viewModel.hasFlowStarted()) {
            routerHost.requireLoginIfLocked()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionController.onUserInteraction()
    }

    private fun scheduleIdleLock() {
        idleLockJob?.cancel()
        idleLockJob = lifecycleScope.launch {
            while (true) {
                delay(sessionController.timeUntilIdleLock().coerceAtLeast(MIN_IDLE_CHECK_INTERVAL))
                if (viewModel.hasFlowStarted() && sessionController.lockIfIdle()) {
                    routerHost.requireLoginIfLocked()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkSessionOnForeground()
        checkSessionAfterCover()
        if (viewModel.hasFlowStarted()) {
            handleDeepLink(intent)
        } else {
            runPendingDeepLink(intent)
        }
    }

    private fun runPendingDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            var count = 0
            while (!viewModel.hasFlowStarted() && count <= 10) {
                count++
                delay(500.milliseconds)
            }
            if (count <= 10) {
                handleDeepLink(intent, walletWasShown = false)
            }
        }
    }

    /**
     * Runs [block] once the navigation controller exists. An activity recreated while the app was
     * in the background, for example after a configuration change, gets it only at its first
     * composition, which can come after the intent that resumes an issuance. Gives up after about
     * five seconds.
     */
    private fun runWhenNavigationReady(block: () -> Unit) {
        if (routerHost.isNavigationReady()) {
            block()
            return
        }
        lifecycleScope.launch {
            var attempts = 0
            while (!routerHost.isNavigationReady() && attempts < NAVIGATION_READY_ATTEMPTS) {
                attempts++
                delay(NAVIGATION_READY_POLL_INTERVAL)
            }
            if (routerHost.isNavigationReady()) {
                block()
            }
        }
    }

    /**
     * Opens or keeps the link or Digital Credential API request in [intent]. One that arrives during
     * a factory reset is dropped: the documents it would use are being deleted, and opening it would
     * take the user away from the reset screen.
     *
     * A link that starts no wallet flow ([DeepLinkType.EXTERNAL]) is dropped too. The manifest admits
     * none, so it can only come from an app that addressed the wallet directly, and keeping it would
     * let that app have the wallet open any link once the user logs in.
     *
     * A Digital Credential API request is declined while the setting is off. When the request itself
     * brought the wallet up ([coldBoot], or a task restored after its process ended, which
     * [walletWasShown] false marks), the activity also finishes, so the wallet does not stay in front.
     */
    private fun handleDeepLink(
        intent: Intent?,
        coldBoot: Boolean = false,
        walletWasShown: Boolean = true,
    ) {
        if (walletResetStatus.isResetting.value) {
            this.intent = Intent()
            return
        }
        val deepLink = hasDeepLink(intent?.data)
        if (deepLink != null && shouldDropIncomingLink(deepLink.type)) {
            this.intent = Intent()
            return
        }
        deepLink?.let {
            if (it.type == DeepLinkType.ISSUANCE && !coldBoot) {
                val link = it.link
                runWhenNavigationReady {
                    handleDeepLinkAction(
                        routerHost.getNavController(),
                        link
                    )
                }
            } else if (
                it.type == DeepLinkType.CREDENTIAL_OFFER
                && !routerHost.userIsLoggedInWithDocuments()
                && routerHost.userIsLoggedInWithNoDocuments()
            ) {
                cacheIntent(intent)
                routerHost.popToIssuanceOnboardingScreen()
            } else if (it.type == DeepLinkType.OPENID4VP
                && routerHost.userIsLoggedInWithDocuments()
                && (routerHost.isScreenOnBackStackOrForeground(IssuanceScreens.AddDocument)
                        || routerHost.isScreenOnBackStackOrForeground(IssuanceScreens.DocumentOffer)
                        || routerHost.isScreenOnBackStackOrForeground(DashboardScreens.DocumentDetails))
            ) {
                handleDeepLinkAction(
                    routerHost.getNavController(),
                    DeepLinkAction(it.link, DeepLinkType.DYNAMIC_PRESENTATION)
                )
            } else if (it.type != DeepLinkType.ISSUANCE) {
                cacheIntent(intent)
                if (routerHost.userIsLoggedInWithDocuments()) {
                    routerHost.popToDashboardScreen()
                }
            }
            this.intent = Intent()
        } ?: hasIntentAction(intent)?.let {
            when (it.type) {
                IntentType.DC_API -> {
                    if (!viewModel.isDCAPIEnabled()) {
                        this.intent = Intent()
                        if (shouldFinishOnDeclinedRequest(coldBoot, walletWasShown)) finish()
                        return
                    }
                    cacheIntent(it.intent)
                    if (routerHost.userIsLoggedInWithDocuments()) {
                        routerHost.popToDashboardScreen()
                    }
                }
            }
            this.intent = Intent()
        }
    }
}

internal class EudiComponentActivityViewModel(
    private val prefKeys: PrefKeys,
    private val dcapiSettingProvider: DCAPISettingProvider,
    private val sessionController: SessionController,
    uuidProvider: UuidProvider
) : ViewModel() {

    private val sessionId: String = uuidProvider.provideUuid()

    private var flowStarted: Boolean = false
    private var pendingIntent: Intent? = null

    /** The launch intent the activity opened, and its record, until a screen uses it. */
    private var launchLinkToRecord: Pair<Intent, String>? = null

    override fun onCleared() {
        getOrNullKoinScope(sessionId)?.close()
    }

    /**
     * Sets the session id. The background checks start later, from the dashboard, once the user has
     * logged in.
     */
    fun onCreate() {
        setSessionId()
    }

    fun onResume() {
        setSessionId()
    }

    fun onFlowStart() {
        flowStarted = true
    }

    /**
     * Keeps [intent] until a screen opens it after login. While the wallet is locked a kept link
     * is not cleared, so that it survives until the user has logged in.
     */
    fun cacheIntent(intent: Intent?) {
        if (intent == null && !sessionController.isUnlocked) return
        pendingIntent = intent
    }

    /**
     * The kept link, or null while the wallet is locked: a screen restored before login, such as
     * the dashboard after the activity is recreated, must not open it.
     */
    fun getCachedIntent(): Intent? {
        if (!sessionController.isUnlocked) return null
        val intent = pendingIntent
        launchLinkToRecord?.let { (launch, record) ->
            if (intent === launch) {
                launchLinkToRecord = null
                runBlocking(Dispatchers.IO) { prefKeys.setOpenedLaunchLink(record) }
            }
        }
        return intent
    }

    /** Forgets the kept link whether or not the wallet is locked, as a factory reset does. */
    fun clearCachedIntent() {
        pendingIntent = null
    }

    /**
     * Whether the activity opens the link of its launch [intent], started by [launchedBy] in task
     * [taskId]. It does not when that is Android replaying the link that first started the task (see
     * [isReplayedLaunchLink]). Only the task's root activity is checked, since only its intent is
     * replayed. When the link opens, its record is written once a screen uses it after login
     * ([getCachedIntent]), so that a link never used is not taken for used. Nor is a launch link
     * that a newer link replaced before a screen used it.
     */
    fun shouldOpenLaunchLink(
        intent: Intent?,
        taskId: Int,
        isTaskRoot: Boolean,
        launchedBy: String?,
    ): Boolean {
        val link = intent?.dataString ?: return true
        if (!isTaskRoot) return true
        val recorded = runBlocking(Dispatchers.IO) { prefKeys.getOpenedLaunchLink() }
        if (isReplayedLaunchLink(intent.flags, launchedBy, taskId, link, recorded)) return false
        launchLinkToRecord = intent to createLaunchLinkRecord(taskId, link)
        return true
    }

    /**
     * Whether the user allows the Digital Credential API. A request that arrives while it is off is
     * declined. When the request started the activity, the activity finishes without a result, as
     * for a cancelled request; when the wallet was already open, the request is dropped and the
     * wallet stays as it was.
     */
    fun isDCAPIEnabled(): Boolean = runBlocking(Dispatchers.IO) { dcapiSettingProvider.isEnabled() }

    fun hasFlowStarted(): Boolean = flowStarted

    private fun setSessionId() {
        runBlocking(Dispatchers.IO) {
            prefKeys.setSessionId(sessionId)
        }
    }
}