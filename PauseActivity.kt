package com.pause.app
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.view.Gravity
import android.os.Bundle
import android.widget.*

class PauseActivity : Activity() {
    companion object { const val REASON = "reason"; const val WELCOME = "welcome" }
    private val animals = listOf("🦊" to "Fox", "🐼" to "Panda", "🦉" to "Owl", "🐢" to "Turtle", "🐙" to "Octopus",
        "🦒" to "Giraffe", "🐧" to "Penguin", "🦦" to "Otter", "🐘" to "Elephant", "🦜" to "Parrot")
    private val tasks = listOf("Read a book for 10 minutes", "Fill your water bottle", "Stretch your shoulders",
        "Step outside for fresh air", "Tidy your desk", "Write one thing you are grateful for")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = Prefs(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(48, 48, 48, 48)
        fun tv(t: String, sp: Float) {
            val v = TextView(this)
            v.text = t; v.textSize = sp; v.gravity = Gravity.CENTER
            root.addView(v)
        }
        if (intent.getStringExtra("mode") == REASON) {
            tv("🔒", 72f)
            tv("Too early. Why do you need your phone now?\nReasons left today: ${p.reasonsLeft()}", 18f)
            val et = EditText(this)
            et.hint = "Write a real reason (12+ characters)"
            root.addView(et)
            val submit = Button(this)
            submit.text = "Submit reason"
            submit.setOnClickListener {
                if (et.text.trim().length < 12) {
                    et.error = "Please give a real reason"
                } else {
                    p.useReason()
                    p.lastOff = 0L
                    startForegroundService(Intent(this, GuardService::class.java).setAction("SESSION"))
                    finish()
                }
            }
            root.addView(submit)
            val cancel = Button(this)
            cancel.text = "Never mind, lock it"
            cancel.setOnClickListener { lockAndClose() }
            root.addView(cancel)
        } else {
            var i = animals.indices.random()
            while (i == p.lastAnimal) i = animals.indices.random()
            p.lastAnimal = i
            tv(animals[i].first, 110f)
            tv(animals[i].second + " says hi", 22f)
            tv("Try this instead: " + tasks.random(), 18f)
            val ok = Button(this)
            ok.text = "OK"
            ok.setOnClickListener { finish() }
            root.addView(ok)
        }
        setContentView(root)
    }

    private fun lockAndClose() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))) dpm.lockNow()
        finish()
    }

    @Deprecated("Back means lock in reason mode")
    override fun onBackPressed() {
        if (intent.getStringExtra("mode") == REASON) lockAndClose() else finish()
    }
}
