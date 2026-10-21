package com.pause.app
import android.content.*
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        try { c.startForegroundService(Intent(c, GuardService::class.java)) } catch (e: Exception) { }
    }
}
