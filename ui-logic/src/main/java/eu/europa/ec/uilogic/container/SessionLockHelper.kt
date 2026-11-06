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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import eu.europa.ec.authenticationlogic.controller.session.SessionController

/**
 * Records, for the session lock, when the whole app goes to the background, and when another
 * screen of the app covers the wallet's own. Call it once, from `Application.onCreate`.
 * `ProcessLifecycleOwner` reports the stop about 700 ms after the last activity stops, by which
 * time that activity may already be destroyed, so the observer must not belong to an activity.
 */
fun registerSessionLockObservers(
    application: Application,
    sessionController: SessionController,
) {
    ProcessLifecycleOwner.get().lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                sessionController.onBackground()
            }
        }
    )
    application.registerActivityLifecycleCallbacks(OtherScreenObserver(sessionController))
}

/**
 * Tells [sessionController] when an activity other than the wallet's own, such as the RQES SDK's
 * signing screens, comes to the front. The wallet's activity reports its own return.
 */
internal class OtherScreenObserver(
    private val sessionController: SessionController,
) : Application.ActivityLifecycleCallbacks {

    override fun onActivityResumed(activity: Activity) {
        if (activity !is EudiComponentActivity) {
            sessionController.onCoveredByOtherScreen()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}