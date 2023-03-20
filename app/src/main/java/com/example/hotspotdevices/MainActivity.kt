package com.example.hotspotdevices

import kotlin.system.measureTimeMillis
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hotspotdevices.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {

    companion object {
        private const val SCAN_RESULTS_BUNDLE_KEY = "DEVICE_LIST"
    }

    private val networkManager: NetworkManager by lazy {
        NetworkManager(getSystemService(Context.WIFI_SERVICE) as WifiManager)
    }

    private val preferences: SharedPreferences by lazy {
        getPreferences(Context.MODE_PRIVATE)
    }

    private val devices = arrayListOf<ScanResult>()

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            @Suppress("Deprecation", "kotlin:S1874")
            val bundledResults = it.getParcelableArrayList<ScanResult>(SCAN_RESULTS_BUNDLE_KEY)
            if (bundledResults != null) {
                devices.addAll(bundledResults)
            }
        }
        preferences.edit {
            networkManager.getWirelessInterfaces().forEach {
                if (!preferences.contains(it.name)) {
                    putBoolean(it.name, true)
                }
            }
            apply()
        }

        with(binding.listView) {
            adapter = ScanResultsAdapter(devices)
            layoutManager = LinearLayoutManager(context)
        }

        binding.button.setOnClickListener {
            val wirelessInterfaces = networkManager.getWirelessInterfaces()
                .filter { preferences.getBoolean(it.name, false) }
                .toList()

            var result: List<ScanResult>
            binding.progressBar.visibility = View.VISIBLE

            Thread {
                val executionTime = measureTimeMillis {
                    result = runBlocking {
                        networkManager.scanNetworks(wirelessInterfaces)
                    }
                    devices.clear()
                    devices.addAll(result)
                }
                runOnUiThread {
                    @Suppress("NotifyDataSetChanged")
                    (binding.listView.adapter as ScanResultsAdapter).notifyDataSetChanged()

                    binding.progressBar.visibility = View.GONE
                    val msg = "Found ${result.size} devices in $executionTime ms"
                    Log.d("Network scan", msg)
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                }
            }.start()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu!!.clear()
        val wirelessInterfaces = networkManager.getWirelessInterfaces()
        preferences.edit {
            preferences.all.keys.forEach { key ->
                if (wirelessInterfaces.none { it.name == key }) {
                    remove(key)
                }
            }
            apply()
        }
        wirelessInterfaces.forEachIndexed { index, net ->
            val menuItem = menu.add(
                R.id.netInterfaces,
                Menu.NONE,
                index,
                "${net.name} : ${networkManager.getInterfaceAddress(net)}"
            )
            menuItem.apply {
                isCheckable = true
                if(!preferences.contains(net.name)){
                    preferences.edit {
                        putBoolean(net.name,true)
                        apply()
                    }
                }
                isChecked = preferences.getBoolean(net.name, true)
                setOnMenuItemClickListener {
                    isChecked = !isChecked
                    preferences.edit {
                        putBoolean(net.name, isChecked)
                        apply()
                    }
                    true
                }
            }
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SCAN_RESULTS_BUNDLE_KEY, devices)
    }
}