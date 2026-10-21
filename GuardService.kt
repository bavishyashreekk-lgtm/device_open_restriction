package com.pause.app
import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class GuardService : Service() {
    private lateinit var p: Prefs
    private val h = Handler(Looper.getMainLooper())
    private var openedAt = 0L
    private var warned = 0L
    private var lastW = 0L
    private var lastS = 0L
    private var lastB = 0L
    private var tts: TextToSpeech? = null

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> { p.lastOff = now(); h.removeCallbacks(tick); hideOverlay() }
                Intent.ACTION_USER_PRESENT -> onUnlock()
            }
        }
    }
    private val tick = object : Runnable {
        override fun run() {
            val t = now()
            if (warned == 0L && t - openedAt >= p.maxUse * 60000L) {
                warned = t
                note("Time to switch off", "You have used your phone for ${p.maxUse} minutes. Please put it down.")
            }
            if (warned > 0 && t >= warned + p.grace * 60000L) {
                p.lockoutUntil = t + p.lockLen * 60000L
                lock()
                return
            }
            if (t - lastW >= p.water * 60000L) { lastW = t; note("Water break", "Time to drink some water.") }
            if (t - lastS >= p.snack * 60000L) { lastS = t; note("Snack time", "Have a healthy snack.") }
            if (p.blink > 0 && t - lastB >= p.blink * 60000L) {
                lastB = t
                speak("Time to blink your eyes. Look away from the screen for a moment.")
            }
            h.postDelayed(this, 30000)
        }
    }

    private fun now(): Long = System.currentTimeMillis()
    private fun speak(m: String) { tts?.speak(m, TextToSpeech.QUEUE_FLUSH, null, "blink") }

    private fun inCall(): Boolean = try {
        (getSystemService(TELEPHONY_SERVICE) as TelephonyManager).callState != TelephonyManager.CALL_STATE_IDLE
    } catch (e: SecurityException) { false }

    private fun lock() {
        if (inCall()) return // never block a call
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))) {
            dpm.lockNow()
        } else {
            note("Pause cannot lock your phone", "Open Pause and tap 'Allow Device Admin'.")
        }
    }

    private var overlay: View? = null
    private val callWatch = object : Runnable {
        override fun run() {
            if (overlay != null) {
                if (inCall()) hideOverlay() else h.postDelayed(this, 1000)
            }
        }
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** Full-screen box that Home/Back cannot dismiss. It disappears during a phone call. */
    private fun showReasonOverlay() {
        if (overlay != null) return
        if (!Settings.canDrawOverlays(this)) { lock(); return }
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setBackgroundColor(Color.parseColor("#1B2A22"))
        box.setPadding(dp(24), dp(24), dp(24), dp(24))
        val title = TextView(this)
        title.text = "Too early.\nWhy do you need your phone now?\nReasons left today: " + p.reasonsLeft()
        title.textSize = 20f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        box.addView(title)
        val et = EditText(this)
        et.hint = "Write a real reason (12+ characters)"
        et.setHintTextColor(Color.LTGRAY)
        et.setTextColor(Color.WHITE)
        box.addView(et)
        val ok = Button(this)
        ok.text = "Submit reason"
        ok.setOnClickListener {
            if (et.text.trim().length < 12) {
                et.error = "Please give a real reason"
            } else {
                p.useReason()
                p.lastOff = 0L
                hideOverlay()
                startSession()
            }
        }
        box.addView(ok)
        val no = Button(this)
        no.text = "Never mind, lock it"
        no.setOnClickListener { hideOverlay(); lock() }
        box.addView(no)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE)
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(box, lp)
        overlay = box
        h.postDelayed(callWatch, 1000)
    }

    private fun hideOverlay() {
        val v = overlay ?: return
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v) } catch (e: Exception) { }
        overlay = null
    }

    private fun onUnlock() {
        if (inCall()) return
        val t = now()
        if (t < p.lockoutUntil) { lock(); return }
        if (p.lastOff > 0 && t - p.lastOff < p.wait * 60000L) { // unlocked too early
            if (p.reasonsLeft() > 0) showReasonOverlay() else lock()
            return
        }
        startSession()
    }

    private fun startSession() {
        openedAt = now(); warned = 0; lastW = openedAt; lastS = openedAt; lastB = openedAt
        show(PauseActivity.WELCOME)
        h.removeCallbacks(tick)
        h.postDelayed(tick, 30000)
    }

    private fun show(mode: String) {
        startActivity(Intent(this, PauseActivity::class.java).putExtra("mode", mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun note(title: String, text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alerts", "Alerts", NotificationManager.IMPORTANCE_HIGH))
        nm.notify((now() % 100000).toInt(), Notification.Builder(this, "alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).build())
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        p = Prefs(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("run", "Pause running", NotificationManager.IMPORTANCE_MIN))
        startForeground(1, Notification.Builder(this, "run").setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Pause is on").build())
        if (i?.action == "SESSION") startSession()
        else if (p.lastOff == 0L) p.lastOff = now() // start counting right away
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.getDefault() }
        }
        val f = IntentFilter()
        f.addAction(Intent.ACTION_SCREEN_OFF)
        f.addAction(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, RECEIVER_NOT_EXPORTED) else registerReceiver(rx, f)
    }

    override fun onDestroy() {
        hideOverlay()
        unregisterReceiver(rx)
        tts?.shutdown()
        super.onDestroy()
    }
    override fun onBind(i: Intent?): IBinder? = null
}
