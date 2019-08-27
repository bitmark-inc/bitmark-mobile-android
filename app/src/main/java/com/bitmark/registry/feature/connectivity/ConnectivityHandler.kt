package com.bitmark.registry.feature.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-27
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ConnectivityHandler @Inject constructor(private val context: Context) :
    BroadcastReceiver() {

    private var networkStateChangeListeners =
        mutableListOf<NetworkStateChangeListener>()

    fun addNetworkStateChangeListener(listener: NetworkStateChangeListener) {
        if (networkStateChangeListeners.contains(listener)) return
        networkStateChangeListeners.add(listener)
    }

    fun removeNetworkStateChangeListener(listener: NetworkStateChangeListener) {
        networkStateChangeListeners.remove(listener)
    }

    fun register() {
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(this, intentFilter)
    }

    fun unregister() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val cm =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val connected: Boolean =
            activeNetwork?.isConnectedOrConnecting == true
        networkStateChangeListeners.forEach { listener ->
            listener.onChange(
                connected
            )
        }
    }

    interface NetworkStateChangeListener {
        fun onChange(connected: Boolean)
    }
}