package com.example.hotspotdevices

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hotspotdevices.databinding.ScanResultItemViewBinding

class ScanResultsAdapter(private val data: List<ScanResult>) :
    RecyclerView.Adapter<ScanResultsAdapter.ScanResultHolder>() {

    private lateinit var binding: ScanResultItemViewBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultHolder {
        binding =
            ScanResultItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanResultHolder(binding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ScanResultHolder, position: Int) {
        holder.bind(data[position])
    }

    class ScanResultHolder(private val binding: ScanResultItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var item: ScanResult

        fun bind(currentItem: ScanResult) {
            item = currentItem
            binding.deviceNameTextView.text = item.deviceName
            binding.ipAddrTextView.text =
                itemView.context.getString(R.string.ip_display, item.ipAddress)
            binding.macAddrTextView.text =
                itemView.context.getString(R.string.mac_display, item.macAddress)

            itemView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${item.ipAddress}"))
                itemView.context.startActivity(intent)
            }
        }
    }
}