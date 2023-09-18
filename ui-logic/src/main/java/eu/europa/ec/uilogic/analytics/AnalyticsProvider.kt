package eu.europa.ec.uilogic.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

interface AnalyticsProvider {
    fun logScreen(name: String)
    fun logEvent(event: String, arguments: Map<String, String>)
}

object FirebaseAnalyticsProvider : AnalyticsProvider {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    override fun logScreen(name: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, name)
        }
    }

    override fun logEvent(event: String, arguments: Map<String, String>) {
        val bundle = Bundle()
        arguments.map {
            bundle.putString(it.key, it.value)
        }
        firebaseAnalytics.logEvent(event, bundle)
    }
}