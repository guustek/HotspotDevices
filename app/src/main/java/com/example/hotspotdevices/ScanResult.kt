package com.example.hotspotdevices

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScanResult(val deviceName: String, val ipAddress: String, val macAddress: String) :
    Parcelable {}