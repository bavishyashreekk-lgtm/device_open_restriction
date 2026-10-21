package com.pause.app
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

/** Welcome screen: a different animated animal on a colourful background each time. */
class PauseActivity : Activity() {
    companion object { const val REASON = "reason"; const val WELCOME = "welcome" }

    private class Animal(val emoji: String, val name: String, val top: String, val bottom: String)
    private val animals = listOf(
        Animal("🦊", "Fox", "#FF9A5A", "#FFE0C2"), Animal("🐼", "Panda", "#5FC9A0", "#DFF5E9"),
        Animal("🦉", "Owl", "#8E7CC3", "#E2DCF4"), Animal("🐢", "Turtle", "#3FAE86", "#CDEFDD"),
        Animal("🐙", "Octopus", "#E86A92", "#FFD8E3"), Animal("🦒", "Giraffe", "#F2B84B", "#FFF0C2"),
        Animal("🐧", "Penguin", "#4A90D9", "#D6E9FB"), Animal("🦦", "Otter", "#A47C5B", "#EFE0D2"),
        Animal("🐘", "Elephant", "#7C93A8", "#DDE6ED"), Animal("🦜", "Parrot", "#E94F4F", "#FFD9D2"))
    private val tasks = listOf("Read a book for 10 minutes", "Fill your water bottle", "Stretch your shoulders",
        "Step outside for fresh air", "Tidy your desk", "Write one thing you are grateful for",
        "Do 10 slow deep breaths", "Walk around for two minutes")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun loop(a: ObjectAnimator, ms: Long) {
        a.duration = ms
        a.repeatCount = ValueAnimator.INFINITE
        a.repeatMode = ValueAnimator.REVERSE
        a.interpolator = AccelerateDecelerateInterpolator()
        a.start()
    }

    /** Each animal moves differently: bounce, wiggle, pulse, sway. */
    private fun animateAnimal(v: View, motion: Int) {
        when (motion) {
            0 -> loop(ObjectAnimator.ofFloat(v, "translationY", 0f, -dp(40).toFloat()), 700)
            1 -> loop(ObjectAnimator.ofFloat(v, "rotation", -14f, 14f), 600)
            2 -> { loop(ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.25f), 800); loop(ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.25f), 800) }
            else -> loop(ObjectAnimator.ofFloat(v, "translationX", -dp(36).toFloat(), dp(36).toFloat()), 1100)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = Prefs(this)
        var i = Random.nextInt(animals.size)
        while (i == p.lastAnimal) i = Random.nextInt(animals.size)
        p.lastAnimal = i
        val an = animals[i]

        val root = FrameLayout(this)
        root.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor(an.top), Color.parseColor(an.bottom)))

        // floating sparkles drifting upward behind the card
        val bits = listOf("✨", "⭐", "💧", "🍃")
        root.post {
            val w = root.width
            val h = root.height
            for (k in 0 until 14) {
                val t = TextView(this)
                t.text = bits[k % bits.size]
                t.textSize = (16 + Random.nextInt(14)).toFloat()
                t.alpha = 0.7f
                val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                lp.leftMargin = Random.nextInt(maxOf(w - dp(40), 1))
                root.addView(t, 0, lp)
                val a = ObjectAnimator.ofFloat(t, "translationY", h.toFloat(), -dp(80).toFloat())
                a.duration = (5000 + Random.nextInt(5000)).toLong()
                a.startDelay = Random.nextInt(4000).toLong()
                a.repeatCount = ValueAnimator.INFINITE
                a.interpolator = LinearInterpolator()
                a.start()
            }
        }

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(dp(24), dp(32), dp(24), dp(24))
        val bg = GradientDrawable()
        bg.setColor(Color.argb(215, 255, 255, 255))
        bg.cornerRadius = dp(32).toFloat()
        card.background = bg

        val animal = TextView(this)
        animal.text = an.emoji
        animal.textSize = 110f
        animal.gravity = Gravity.CENTER
        card.addView(animal)

        val title = TextView(this)
        title.text = an.name + " says hi!"
        title.textSize = 26f
        title.setTextColor(Color.parseColor("#1D2B22"))
        title.gravity = Gravity.CENTER
        card.addView(title)

        val task = TextView(this)
        task.text = "Try this instead:\n" + tasks.random()
        task.textSize = 18f
        task.setTextColor(Color.parseColor("#1D2B22"))
        task.gravity = Gravity.CENTER
        task.setPadding(0, dp(16), 0, dp(16))
        card.addView(task)

        val ok = Button(this)
        ok.text = "OK, I will!"
        ok.setTextColor(Color.WHITE)
        val bb = GradientDrawable()
        bb.setColor(Color.parseColor(an.top))
        bb.cornerRadius = dp(24).toFloat()
        ok.background = bb
        ok.setOnClickListener { finish() }
        card.addView(ok)

        val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        lp.leftMargin = dp(24)
        lp.rightMargin = dp(24)
        root.addView(card, lp)
        setContentView(root)

        // card slides up with a bounce, then the animal starts moving
        card.translationY = dp(400).toFloat()
        card.alpha = 0f
        card.animate().translationY(0f).alpha(1f).setDuration(700).setInterpolator(OvershootInterpolator()).start()
        animateAnimal(animal, i % 4)
    }
}
