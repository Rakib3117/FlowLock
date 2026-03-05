package com.flowlock.app
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.*

class BlockScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val seriesName = intent.getStringExtra("series_name") ?: "Active Series"
        val blockedApp = intent.getStringExtra("blocked_app") ?: "This App"
        val endTime = intent.getStringExtra("end_time") ?: ""
        val allowedNames = intent.getStringArrayExtra("allowed_names") ?: arrayOf()
        val allowedMins = intent.getIntArrayExtra("allowed_mins") ?: intArrayOf()
        val usedMins = intent.getIntArrayExtra("used_mins") ?: intArrayOf()

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#070714"))
            setPadding(48, 80, 48, 48)
        }
        scroll.addView(root)

        // Blocked message
        root.addView(TextView(this).apply {
            text = "BLOCKED"
            textSize = 13f
            setTextColor(Color.parseColor("#ef4444"))
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
        })
        root.addView(Space(this).apply { minimumHeight = 8 })
        root.addView(TextView(this).apply {
            text = blockedApp
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(Space(this).apply { minimumHeight = 4 })
        root.addView(TextView(this).apply {
            text = "Not available during $seriesName"
            textSize = 13f
            setTextColor(Color.parseColor("#6b6b8a"))
            gravity = Gravity.CENTER
        })
        if (endTime.isNotEmpty()) {
            root.addView(Space(this).apply { minimumHeight = 4 })
            root.addView(TextView(this).apply {
                text = "Series ends at $endTime"
                textSize = 12f
                setTextColor(Color.parseColor("#4b4b6a"))
                gravity = Gravity.CENTER
            })
        }

        root.addView(Space(this).apply { minimumHeight = 40 })

        // Allowed apps progress
        if (allowedNames.isNotEmpty()) {
            root.addView(TextView(this).apply {
                text = "YOUR PROGRESS"
                textSize = 11f
                setTextColor(Color.parseColor("#6b6b8a"))
                letterSpacing = 0.2f
            })
            root.addView(Space(this).apply { minimumHeight = 16 })

            for (i in allowedNames.indices) {
                val used = if (i < usedMins.size) usedMins[i] else 0
                val target = if (i < allowedMins.size) allowedMins[i] else 1
                val pct = (used.toFloat() / target).coerceIn(0f, 1f)
                val done = used >= target

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#0f0f1e"))
                    setPadding(28, 20, 28, 20)
                }
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(TextView(this).apply {
                    text = allowedNames[i]
                    textSize = 14f
                    setTextColor(if (done) Color.parseColor("#22c55e") else Color.WHITE)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "$used / $target min"
                    textSize = 12f
                    setTextColor(if (done) Color.parseColor("#22c55e") else Color.parseColor("#a0a0c0"))
                })
                card.addView(row)
                card.addView(Space(this).apply { minimumHeight = 10 })
                card.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    progress = (pct * 100).toInt()
                    minimumHeight = 12
                    progressDrawable.setColorFilter(
                        if (done) Color.parseColor("#22c55e") else Color.parseColor("#6366f1"),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                })
                root.addView(card)
                root.addView(Space(this).apply { minimumHeight = 12 })
            }
        }

        root.addView(Space(this).apply { minimumHeight = 32 })
        root.addView(Button(this).apply {
            text = "Go Back"
            setTextColor(Color.parseColor("#a0a0c0"))
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            textSize = 14f
            setPadding(0, 24, 0, 24)
            setOnClickListener { goHome() }
        })
        setContentView(scroll)
    }

    private fun goHome() {
        startActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    override fun onBackPressed() = goHome()
}
