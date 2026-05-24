package com.thecalcurate.android.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Mirrors iOS networkMonitor.swift's ConnectivityQuality enum:
 *  - NONE  = no internet (iOS .noConnection → red banner)
 *  - METERED = constrained connection like cellular/Data Saver (iOS .bad → yellow banner)
 *  - HEALTHY = unmetered + connected (iOS .okay/.great → no banner)
 */
enum class NetworkQuality { NONE, METERED, HEALTHY }

class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _quality = MutableLiveData<NetworkQuality>(currentQuality())
    val quality: LiveData<NetworkQuality> get() = _quality

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { update() }
        override fun onLost(network: Network) { update() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { update() }
        override fun onUnavailable() { update() }
    }

    fun start() {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, callback)
    }

    fun stop() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (e: IllegalArgumentException) {
            /* not registered */
        }
    }

    private fun update() {
        _quality.postValue(currentQuality())
    }

    private fun currentQuality(): NetworkQuality {
        val net = cm.activeNetwork ?: return NetworkQuality.NONE
        val caps = cm.getNetworkCapabilities(net) ?: return NetworkQuality.NONE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return NetworkQuality.NONE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return NetworkQuality.NONE
        // NET_CAPABILITY_NOT_METERED absent → metered (cellular, hotspot, Data Saver) → iOS "bad"
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            NetworkQuality.HEALTHY
        } else {
            NetworkQuality.METERED
        }
    }
}
