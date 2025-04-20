package com.example.mediaplayerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            this.action = action
        }
        context.startService(serviceIntent)
    }
}
