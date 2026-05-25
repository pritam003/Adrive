package com.example.adrive

import android.app.Application
import com.example.adrive.data.network.ApiClient

class AdriveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}

