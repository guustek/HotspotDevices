package com.example.hotspotdevices

import java.lang.reflect.Method
import java.net.InetAddress
import java.net.NetworkInterface
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*

class NetworkManager(private val wifiManager: WifiManager) {

    object InterfaceType {
        val WIFI = setOf("wlan")
        val WIRELESS = setOf("wlan", "ap", "swlan")
    }

    companion object {
        private const val PING_TIMEOUT = 1000
    }

    fun isHotspotEnabled(): Boolean {
        val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
        method.isAccessible = true
        return method.invoke(wifiManager) as Boolean
    }

    fun getInterfaces(prefixes: Set<String>): Collection<NetworkInterface> =
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { net: NetworkInterface ->
                if (prefixes.isEmpty()) {
                    return@filter true
                }
                prefixes.any { net.name.startsWith(it) }
            }
            .toList()

    suspend fun scanNetworks(networkInterface: Collection<NetworkInterface>): List<ScanResult> =
        coroutineScope {
            val jobs = mutableListOf<Job>()
            val result = mutableListOf<ScanResult>()
            networkInterface.forEach {
                val address = getInterfaceAddress(it)
                val networkPrefix = address.substring(0, address.lastIndexOf("."))
                Log.d("Network scan", "Started scanning $address.* network")
                for (i in 1..254) {
                    val ipAddr = "${networkPrefix}.${i}"
                    if (ipAddr == address) {
                        continue
                    }
                    jobs.add(launch {
                        withContext(Dispatchers.IO) {
                            val inetAddr = InetAddress.getByName(ipAddr)
                            if (inetAddr.isReachable(PING_TIMEOUT)) {
                                result.add(ScanResult("NAME", ipAddr, "MAC"))
                            }
                        }
                    })
                }
            }
            jobs.joinAll()
            result.sortedBy { it.ipAddress }
        }

    fun getInterfaceAddress(networkInterface: NetworkInterface): String {
        return networkInterface.interfaceAddresses
            .filter { it.broadcast != null }
            .firstNotNullOf { it.address.hostAddress }
    }
}