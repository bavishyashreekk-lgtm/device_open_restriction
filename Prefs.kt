package com.pause.app
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings (minutes) and counters. Wait time can never go below 15 minutes. */
class Prefs(c: Context) {
    private val p = c.getSharedPreferences("pause", 0)
    private fun getI(k: String, d: Int): Int = p.getInt(k, d)
    private fun getL(k: String): Long = p.getLong(k, 0L)
    private fun setI(k: String, v: Int) { p.edit().putInt(k, v).apply() }
    private fun setL(k: String, v: Long) { p.edit().putLong(k, v).apply() }

    var wait: Int
        get() = getI("wait", 15).coerceAtLeast(15)
        set(v) { setI("wait", v.coerceAtLeast(15)) }
    var maxUse: Int
        get() = getI("maxUse", 20)
        set(v) { setI("maxUse", v.coerceAtLeast(1)) }
    var grace: Int
        get() = getI("grace", 5)
        set(v) { setI("grace", v.coerceAtLeast(1)) }
    var lockLen: Int
        get() = getI("lockLen", 60)
        set(v) { setI("lockLen", v.coerceAtLeast(1)) }
    var water: Int
        get() = getI("water", 30)
        set(v) { setI("water", v.coerceAtLeast(1)) }
    var snack: Int
        get() = getI("snack", 45)
        set(v) { setI("snack", v.coerceAtLeast(1)) }
    var blink: Int
        get() = getI("blink", 20)
        set(v) { setI("blink", v.coerceAtLeast(0)) }
    var maxReasons: Int
        get() = getI("maxReasons", 3)
        set(v) { setI("maxReasons", v.coerceIn(0, 3)) }
    var lastOff: Long
        get() = getL("lastOff")
        set(v) { setL("lastOff", v) }
    var lockoutUntil: Long
        get() = getL("lockoutUntil")
        set(v) { setL("lockoutUntil", v) }
    var lastAnimal: Int
        get() = getI("lastAnimal", -1)
        set(v) { setI("lastAnimal", v) }

    private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    fun reasonsLeft(): Int {
        if (p.getString("day", "") != today()) p.edit().putString("day", today()).putInt("used", 0).apply()
        return maxReasons - getI("used", 0)
    }
    fun useReason() {
        reasonsLeft()
        setI("used", getI("used", 0) + 1)
    }
}
