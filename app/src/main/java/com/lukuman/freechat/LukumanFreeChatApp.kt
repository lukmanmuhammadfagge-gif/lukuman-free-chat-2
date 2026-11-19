package com.lukuman.freechat

import android.app.Application
import com.google.firebase.FirebaseApp

class LukumanFreeChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
