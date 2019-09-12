package com.bitmark.registry

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.bitmark.registry.logging.Tracer


/**
 * @author Hieu Pham
 * @since 2019-08-11
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AppLifecycleHandler : Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "AppLifecycleHandler"
    }

    private var runningActivityCount = 0

    private var runningActivity: Activity? = null

    private var isConfigChanged = false

    private var appStateChangedListeners =
        mutableListOf<AppStateChangedListener>()

    fun getRunningActivity() = runningActivity

    fun addAppStateChangedListener(listener: AppStateChangedListener) {
        if (appStateChangedListeners.contains(listener)) return
        appStateChangedListeners.add(listener)
    }

    fun removeAppStateChangedListener(listener: AppStateChangedListener) {
        appStateChangedListeners.remove(listener)
    }

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }

    override fun onActivityStarted(activity: Activity?) {
        if (++runningActivityCount == 1 && !isConfigChanged) {
            Tracer.INFO.log(TAG, "on foreground")
            runningActivity = activity
            appStateChangedListeners.forEach { l -> l.onForeground() }
        }
    }

    override fun onActivityDestroyed(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity?,
        outState: Bundle?
    ) {
    }

    override fun onActivityStopped(activity: Activity?) {
        isConfigChanged = activity?.isChangingConfigurations ?: false
        if (--runningActivityCount == 0 && !isConfigChanged) {
            Tracer.INFO.log(TAG, "on background")
            runningActivity = null
            appStateChangedListeners.forEach { l -> l.onBackground() }
        }
    }

    override fun onActivityCreated(
        activity: Activity?,
        savedInstanceState: Bundle?
    ) {
    }

    interface AppStateChangedListener {
        fun onForeground() {}

        fun onBackground() {}
    }
}