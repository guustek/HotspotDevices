package com.example.hotspotdevices

import java.lang.reflect.Method
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.stream.IntStream
import kotlin.system.measureTimeMillis
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*

class NetworkManager(private val wifiManager: WifiManager) {

    object InterfacePrefix {
        val WIFI = setOf("wlan")
        val WIFI_HOTSPOT = setOf("wlan", "ap", "swlan")
    }

    companion object {
        private const val PING_TIMEOUT = 1000
    }

    fun isHotspotEnabled(): Boolean {
        val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
        method.isAccessible = true
        return method.invoke(wifiManager) as Boolean
    }

    fun getHotspotNetworkInterfaces(interfacePrefixes: Collection<String>): Collection<NetworkInterface> =
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { net -> interfacePrefixes.any { net.name.startsWith(it) } }
            .toList()

    fun getNetworkAddress(networkInterface: NetworkInterface): String {
        return networkInterface.interfaceAddresses.asSequence()
            .filter { it.broadcast != null }
            .mapNotNull { it.address.hostAddress }
            .map { it.substring(0, it.lastIndexOf(".")) }
            .first()
    }

    suspend fun scanNetworks(networkPrefixes: List<String>): List<String> = coroutineScope {
        val jobs = mutableListOf<Job>()
        val result = mutableListOf<String>()
        val executionTime = measureTimeMillis {
            networkPrefixes.forEach { networkPrefix ->
                Log.d("Network scan", "Started scanning $networkPrefix.* network")
                IntStream.range(1, 254).forEach { i ->
                    jobs.add(
                        launch {
                            withContext(Dispatchers.IO) {
                                val ipAddr = "${networkPrefix}.${i}"
                                val inetAddr = InetAddress.getByName(ipAddr)
                                if (inetAddr.isReachable(PING_TIMEOUT)) {
                                    result.add(ipAddr)
                                }
                            }
                        })
                }
            }
            jobs.joinAll()
        }
        Log.d("Network scan", "Found ${result.size} devices in $executionTime ms")
        result
    }
}