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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.corelogic.di.getOrNullKoinScope
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.uilogic.extension.exposeTestTagsAsResourceId
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.annotation.KoinViewModel

open class EudiComponentActivity : FragmentActivity() {

    private val routerHost: RouterHost by inject()
    private val viewModel: EudiComponentActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onCreate()
    }

    internal fun cacheIntent(intent: Intent?) {
        viewModel.cacheIntent(intent)
    }

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
                    viewModel.onFlowStart()
                    handleDeepLink(intent, coldBoot = true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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
                delay(500)
            }
            if (count <= 10) {
                handleDeepLink(intent)
            }
        }
    }

    private fun handleDeepLink(intent: Intent?, coldBoot: Boolean = false) {
        hasDeepLink(intent?.data)?.let {
            if (it.type == DeepLinkType.ISSUANCE && !coldBoot) {
                handleDeepLinkAction(
                    routerHost.getNavController(),
                    it.link
                )
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
            setIntent(Intent())
        } ?: hasIntentAction(intent)?.let {
            when (it.type) {
                IntentType.DC_API -> {
                    cacheIntent(it.intent)
                    if (routerHost.userIsLoggedInWithDocuments()) {
                        routerHost.popToDashboardScreen()
                    }
                }
            }
            setIntent(Intent())
        }
    }
}

@KoinViewModel
internal class EudiComponentActivityViewModel(
    private val prefKeys: PrefKeys,
    uuidProvider: UuidProvider
) : ViewModel() {

    private val sessionId: String = uuidProvider.provideUuid()

    private var flowStarted: Boolean = false
    private var pendingIntent: Intent? = null

    override fun onCleared() {
        getOrNullKoinScope(sessionId)?.close()
        super.onCleared()
    }

    fun onCreate() {
        setSessionId()
    }

    fun onResume() {
        setSessionId()
    }

    fun onFlowStart() {
        flowStarted = true
    }

    fun cacheIntent(intent: Intent?) {
        pendingIntent = intent
    }

    fun getCachedIntent(): Intent? = pendingIntent

    fun hasFlowStarted(): Boolean = flowStarted

    private fun setSessionId() {
        runBlocking(Dispatchers.IO) {
            prefKeys.setSessionId(sessionId)
        }
    }
}