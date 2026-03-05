package com.flowlock.app
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var seriesList: LinearLayout
    private lateinit var statusTv: TextView
    private val DAYS = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#070714"))
            setPadding(24, 48, 24, 48)
        }
        scroll.addView(root)

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(this).apply {
            text = "FlowLock"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(Button(this).apply {
            text = "\u22ee"
            textSize = 20f
            setTextColor(Color.parseColor("#a0a0c0"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showDevInfo() }
        })
        root.addView(headerRow)

        root.addView(TextView(this).apply {
            text = "Time-Based Series Lock"
            textSize = 13f
            setTextColor(Color.parseColor("#6b6b8a"))
            gravity = Gravity.CENTER
        })
        root.addView(Space(this).apply { minimumHeight = 28 })

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0f0f1e"))
            setPadding(32, 24, 32, 24)
        }
        statusTv = TextView(this).apply { textSize = 13f; setTextColor(Color.parseColor("#a0a0c0")) }
        statusCard.addView(statusTv)
        statusCard.addView(Space(this).apply { minimumHeight = 12 })
        statusCard.addView(Button(this).apply {
            text = "Grant Permissions"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6366f1"))
            setOnClickListener { requestPerms() }
        })
        root.addView(statusCard)
        root.addView(Space(this).apply { minimumHeight = 20 })

        root.addView(Button(this).apply {
            text = "+ New Series"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6366f1"))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 32, 0, 32)
            setOnClickListener { addSeriesDialog() }
        })
        root.addView(Space(this).apply { minimumHeight = 28 })

        root.addView(TextView(this).apply {
            text = "YOUR SERIES"
            textSize = 11f
            setTextColor(Color.parseColor("#6b6b8a"))
            letterSpacing = 0.2f
        })
        root.addView(Space(this).apply { minimumHeight = 12 })

        seriesList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(seriesList)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshList()
        startService(Intent(this, UsageTrackerService::class.java))
    }

    private fun updateStatus() {
        val a = isAccessOn()
        val u = isUsageOn()
        val aStr = if (a) "ON" else "OFF"
        val uStr = if (u) "ON" else "OFF"
        val stateStr = if (a && u) "ACTIVE" else "Permissions needed"
        statusTv.text = "Accessibility: " + aStr + "\nUsage Stats: " + uStr + "\n\n" + stateStr
    }

    private fun isAccessOn(): Boolean {
        val svc = packageName + "/" + AccessibilityLockService::class.java.canonicalName
        val en = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return TextUtils.SimpleStringSplitter(':').apply { setString(en) }.any { it.equals(svc, true) }
    }

    private fun isUsageOn(): Boolean {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val ops = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, info.applicationInfo.uid, packageName) == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    private fun requestPerms() {
        when {
            !isUsageOn() -> { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); Toast.makeText(this, "Enable FlowLock in Usage Access", Toast.LENGTH_LONG).show() }
            !isAccessOn() -> { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); Toast.makeText(this, "Enable FlowLock in Accessibility", Toast.LENGTH_LONG).show() }
            else -> Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList() {
        seriesList.removeAllViews()
        val list = SeriesStorage.loadSeries(this)
        val progress = SeriesStorage.loadProgress(this)
        if (list.isEmpty()) {
            seriesList.addView(TextView(this).apply {
                text = "No series yet.\nTap + New Series!"
                textSize = 14f
                setTextColor(Color.parseColor("#6b6b8a"))
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            })
            return
        }
        for (s in list) {
            seriesList.addView(buildCard(s, progress))
            seriesList.addView(Space(this).apply { minimumHeight = 16 })
        }
    }

    private fun buildCard(series: AppSeries, progress: Map<String, Long>): LinearLayout {
        val isNowActive = SeriesStorage.isSeriesActive(series)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0f0f1e"))
            setPadding(28, 24, 28, 24)
        }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply {
            text = series.name
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = if (isNowActive) "LIVE" else series.startTime + "-" + series.endTime
            textSize = 11f
            setTextColor(if (isNowActive) Color.parseColor("#22c55e") else Color.parseColor("#6b6b8a"))
            setPadding(12, 6, 12, 6)
            if (isNowActive) setBackgroundColor(Color.parseColor("#22c55e22"))
        })
        card.addView(row)

        val daysStr = series.activeDays.joinToString(" ")
        card.addView(TextView(this).apply {
            text = daysStr
            textSize = 12f
            setTextColor(Color.parseColor("#6366f1"))
            setPadding(0, 6, 0, 12)
        })

        if (series.allowedApps.isNotEmpty()) {
            card.addView(TextView(this).apply { text = "ALLOWED"; textSize = 10f; setTextColor(Color.parseColor("#22c55e66")) })
            card.addView(Space(this).apply { minimumHeight = 6 })
            for (a in series.allowedApps) {
                val usedMs = progress[a.packageName] ?: 0L
                val usedM = (usedMs / 60000).toInt()
                val pct = (usedM.toFloat() / a.targetMinutes).coerceIn(0f, 1f)
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 4, 0, 4) }
                r.addView(TextView(this).apply { text = a.appName; textSize = 13f; setTextColor(Color.parseColor("#a0a0c0")); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                r.addView(TextView(this).apply { text = "$usedM/${a.targetMinutes}m"; textSize = 11f; setTextColor(Color.parseColor("#6b6b8a")) })
                card.addView(r)
                val progressVal = (pct * 100).toInt()
                card.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100; setProgress(progressVal); minimumHeight = 8
                    progressDrawable.setColorFilter(Color.parseColor("#6366f1"), android.graphics.PorterDuff.Mode.SRC_IN)
                })
                card.addView(Space(this).apply { minimumHeight = 4 })
            }
        }

        if (series.blockedApps.isNotEmpty()) {
            card.addView(Space(this).apply { minimumHeight = 8 })
            card.addView(TextView(this).apply { text = "BLOCKED"; textSize = 10f; setTextColor(Color.parseColor("#ef444466")) })
            card.addView(Space(this).apply { minimumHeight = 6 })
            val blockedStr = series.blockedApps.joinToString(", ") { it.appName }
            card.addView(TextView(this).apply { text = blockedStr; textSize = 12f; setTextColor(Color.parseColor("#6b6b8a")) })
        }

        card.addView(Space(this).apply { minimumHeight = 16 })
        val br = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        br.addView(Button(this).apply {
            text = if (series.isActive) "Pause" else "Resume"
            setTextColor(Color.parseColor("#a0a0c0")); setBackgroundColor(Color.parseColor("#1a1a2e")); textSize = 12f
            setOnClickListener {
                val list = SeriesStorage.loadSeries(this@MainActivity).toMutableList()
                val idx = list.indexOfFirst { it.id == series.id }
                if (idx >= 0) { list[idx] = list[idx].copy(isActive = !list[idx].isActive); SeriesStorage.saveSeries(this@MainActivity, list); refreshList() }
            }
        })
        br.addView(Space(this).apply { minimumWidth = 8 })
        br.addView(Button(this).apply {
            text = "Delete"; setTextColor(Color.parseColor("#ef4444")); setBackgroundColor(Color.parseColor("#1a1a2e")); textSize = 12f
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle("Delete series?")
                    .setPositiveButton("Delete") { _, _ ->
                        val list = SeriesStorage.loadSeries(this@MainActivity).toMutableList()
                        list.removeAll { it.id == series.id }
                        SeriesStorage.saveSeries(this@MainActivity, list)
                        refreshList()
                    }.setNegativeButton("Cancel", null).show()
            }
        })
        card.addView(br)
        return card
    }

    private fun addSeriesDialog() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }
        val appNames = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val appPkgs = apps.map { it.packageName }

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 16) }
        val nameInput = EditText(this).apply { hint = "Series name e.g. Study Mode"; textSize = 15f }
        layout.addView(TextView(this).apply { text = "Series Name"; textSize = 12f; setTextColor(Color.GRAY) })
        layout.addView(nameInput)
        layout.addView(Space(this).apply { minimumHeight = 16 })

        layout.addView(TextView(this).apply { text = "Active Days"; textSize = 12f; setTextColor(Color.GRAY) })
        val dayChecks = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val checkBoxes = DAYS.map { day ->
            CheckBox(this).apply { text = day.take(2); setTextColor(Color.parseColor("#a0a0c0")); textSize = 11f }
        }
        checkBoxes.forEach { dayChecks.addView(it) }
        layout.addView(dayChecks)
        layout.addView(Space(this).apply { minimumHeight = 16 })

        var startH = 17; var startM = 0; var endH = 19; var endM = 0
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val startBtn = Button(this).apply { text = "17:00"; textSize = 13f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1a1a2e")) }
        val endBtn = Button(this).apply { text = "19:00"; textSize = 13f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1a1a2e")) }
        startBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> startH = h; startM = m; startBtn.text = String.format("%02d:%02d", h, m) }, startH, startM, true).show()
        }
        endBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> endH = h; endM = m; endBtn.text = String.format("%02d:%02d", h, m) }, endH, endM, true).show()
        }
        timeRow.addView(TextView(this).apply { text = "From "; setTextColor(Color.parseColor("#a0a0c0")); textSize = 13f })
        timeRow.addView(startBtn)
        timeRow.addView(TextView(this).apply { text = " to "; setTextColor(Color.parseColor("#a0a0c0")); textSize = 13f })
        timeRow.addView(endBtn)
        layout.addView(timeRow)
        layout.addView(Space(this).apply { minimumHeight = 16 })

        layout.addView(TextView(this).apply { text = "Allowed Apps (with target minutes)"; textSize = 12f; setTextColor(Color.parseColor("#22c55e")) })
        val allowedContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(allowedContainer)
        fun addAllowed() {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 6, 0, 6) }
            val sp = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, appNames).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val mi = EditText(this).apply { hint = "min"; setText("10"); textSize = 12f; inputType = android.text.InputType.TYPE_CLASS_NUMBER; layoutParams = LinearLayout.LayoutParams(110, ViewGroup.LayoutParams.WRAP_CONTENT) }
            r.addView(sp); r.addView(Space(this).apply { minimumWidth = 8 }); r.addView(mi)
            allowedContainer.addView(r)
        }
        addAllowed()
        layout.addView(Button(this).apply { text = "+ Allowed App"; textSize = 12f; setTextColor(Color.parseColor("#22c55e")); setBackgroundColor(Color.parseColor("#22c55e22")); setOnClickListener { addAllowed() } })
        layout.addView(Space(this).apply { minimumHeight = 12 })

        layout.addView(TextView(this).apply { text = "Blocked Apps"; textSize = 12f; setTextColor(Color.parseColor("#ef4444")) })
        val blockedContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(blockedContainer)
        fun addBlocked() {
            val sp = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, appNames).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            blockedContainer.addView(sp)
            blockedContainer.addView(Space(this).apply { minimumHeight = 6 })
        }
        addBlocked()
        layout.addView(Button(this).apply { text = "+ Blocked App"; textSize = 12f; setTextColor(Color.parseColor("#ef4444")); setBackgroundColor(Color.parseColor("#ef444422")); setOnClickListener { addBlocked() } })

        val scrollDialog = ScrollView(this)
        scrollDialog.addView(layout)

        AlertDialog.Builder(this).setTitle("Create Series").setView(scrollDialog)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim().ifEmpty { "Series " + System.currentTimeMillis() }
                val selectedDays = DAYS.filterIndexed { i, _ -> checkBoxes[i].isChecked }
                if (selectedDays.isEmpty()) { Toast.makeText(this, "Select at least one day!", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

                val allowed = mutableListOf<AllowedApp>()
                for (i in 0 until allowedContainer.childCount) {
                    val r = allowedContainer.getChildAt(i) as? LinearLayout ?: continue
                    val sp = r.getChildAt(0) as? Spinner ?: continue
                    val mi = r.getChildAt(2) as? EditText ?: continue
                    allowed.add(AllowedApp(appPkgs[sp.selectedItemPosition], appNames[sp.selectedItemPosition], mi.text.toString().toIntOrNull() ?: 10))
                }

                val blocked = mutableListOf<BlockedApp>()
                var bIdx = 0
                while (bIdx < blockedContainer.childCount) {
                    val view = blockedContainer.getChildAt(bIdx)
                    if (view is Spinner) blocked.add(BlockedApp(appPkgs[view.selectedItemPosition], appNames[view.selectedItemPosition]))
                    bIdx++
                }

                val allowedPkgs = allowed.map { it.packageName }.toSet()
                val blockedPkgs = blocked.map { it.packageName }.toSet()
                if (allowedPkgs.intersect(blockedPkgs).isNotEmpty()) {
                    Toast.makeText(this, "Conflict! Same app in allowed & blocked!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val startTime = String.format("%02d:%02d", startH, startM)
                val endTime = String.format("%02d:%02d", endH, endM)
                val list = SeriesStorage.loadSeries(this).toMutableList()
                list.add(AppSeries(System.currentTimeMillis().toString(), name, selectedDays, startTime, endTime, allowed, blocked))
                SeriesStorage.saveSeries(this, list)
                refreshList()
                Toast.makeText(this, "Series saved!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showDevInfo() {
        AlertDialog.Builder(this)
            .setTitle("About FlowLock")
            .setMessage(
                "Developer: Kazi Rakib Hasan\n\n" +
                "Email: kazirakibhasan2008@gmail.com\n\n" +
                "Version: 1.0\n\n" +
                "For feedback, suggestions, or inquiries, " +
                "feel free to reach out to the developer."
            )
            .setPositiveButton("Close", null)
            .show()
    }
}
