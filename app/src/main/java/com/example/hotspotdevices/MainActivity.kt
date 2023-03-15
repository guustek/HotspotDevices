package com.example.hotspotdevices

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.hotspotdevices.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {

    private val networkManager: NetworkManager by lazy {
        NetworkManager(getSystemService(Context.WIFI_SERVICE) as WifiManager)
    }

    private var devices = mutableListOf<String>()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listView.adapter = object : ArrayAdapter<String>(this, R.layout.device_item_view, R.id.ipAddrTextView, devices) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.setOnClickListener {
                    val ipAddr = (convertView as TextView).text
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$ipAddr"))
                    startActivity(intent)
                }
                return view
            }
        }

        binding.button.setOnClickListener {
//            if (!networkManager.isHotspotEnabled()) {
//                Log.i("Access point", "Hotspot is disabled")
//                Toast.makeText(this, "Hotspot is disabled", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
            val hotspotNetworkInterfaces =
                networkManager.getHotspotNetworkInterfaces(NetworkManager.InterfacePrefix.WIFI_HOTSPOT)

            val networkPrefixes = hotspotNetworkInterfaces
                .map { networkManager.getNetworkAddress(it) }
                .toList()

            var result: List<String>
            Thread {
                result = runBlocking {
                    networkManager.scanNetworks(networkPrefixes).sorted()
                }
                devices.clear()
                devices.addAll(result)
                runOnUiThread {
                    (binding.listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                }
            }.start()
        }
    }
}