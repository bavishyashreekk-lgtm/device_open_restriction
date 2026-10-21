package com.pause.app
import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = Prefs(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(40, 60, 40, 40)
        val sv = ScrollView(this)
        sv.addView(col)
        setContentView(sv)

        fun tv(t: String, sp: Float = 16f) {
            val v = TextView(this)
            v.text = t; v.textSize = sp; v.setPadding(0, 12, 0, 4)
            col.addView(v)
        }
        fun btn(t: String, f: () -> Unit) {
            val v = Button(this)
            v.text = t
            v.setOnClickListener { f() }
            col.addView(v)
        }

        val left = p.lockoutUntil - System.currentTimeMillis()
        if (left > 0) { // the app itself is locked during a lockout
            tv("🌙 Pause is locked", 26f)
            tv("You kept using your phone after the warning. This app opens again in ${left / 60000 + 1} minutes.")
            return
        }

        tv("Pause settings (minutes)", 26f)
        val labels = listOf("Stay locked for (min 15)", "Warn after using", "Lock app if used this long after warning",
            "Lockout length", "Water reminder every", "Snack reminder every",
            "Spoken blink reminder every (0 = off)", "Early-unlock reasons per day (max 3)")
        val values = listOf(p.wait, p.maxUse, p.grace, p.lockLen, p.water, p.snack, p.blink, p.maxReasons)
        val edits = ArrayList<EditText>()
        for (k in labels.indices) {
            tv(labels[k])
            val e = EditText(this)
            e.setText(values[k].toString())
            e.inputType = InputType.TYPE_CLASS_NUMBER
            col.addView(e)
            edits.add(e)
        }

        tv("Permissions (the lock only works if you allow these)", 18f)
        btn("1. Allow Device Admin (lets Pause lock the screen)") {
            val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this, AdminReceiver::class.java))
            startActivity(i)
        }
        btn("2. Allow display over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btn("3. Allow notifications and call state") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_PHONE_STATE), 1)
        }
        btn("Save and start") {
            fun n(k: Int): Int = edits[k].text.toString().toIntOrNull() ?: values[k]
            p.wait = n(0); p.maxUse = n(1); p.grace = n(2); p.lockLen = n(3)
            p.water = n(4); p.snack = n(5); p.blink = n(6); p.maxReasons = n(7)
            startForegroundService(Intent(this, GuardService::class.java))
            Toast.makeText(this, "Pause is on", Toast.LENGTH_SHORT).show()
        }
    }
}
