package com.flowlock.app
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AccessibilityLockService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.flowlock.app" || pkg == "com.android.systemui" || pkg == "android") return

        val seriesList = SeriesStorage.loadSeries(this)
        val progress = SeriesStorage.loadProgress(this)

        for (series in seriesList) {
            if (!SeriesStorage.isSeriesActive(series)) continue
            val isBlocked = series.blockedApps.any { it.packageName == pkg }
            if (isBlocked) {
                val blockedApp = series.blockedApps.first { it.packageName == pkg }
                val intent = Intent(this, BlockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("series_name", series.name)
                    putExtra("blocked_app", blockedApp.appName)
                    putExtra("end_time", series.endTime)
                    val allowedNames = series.allowedApps.map { it.appName }.toTypedArray()
                    val allowedMins = series.allowedApps.map { it.targetMinutes }.toIntArray()
                    val usedMins = series.allowedApps.map { ((progress[it.packageName] ?: 0L) / 60000).toInt() }.toIntArray()
                    putExtra("allowed_names", allowedNames)
                    putExtra("allowed_mins", allowedMins)
                    putExtra("used_mins", usedMins)
                }
                startActivity(intent)
                return
            }
        }
    }

    override fun onInterrupt() {}
}
