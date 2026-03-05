package com.flowlock.app
import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class UsageTrackerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val CH = "flowlock_channel"
    private var lastResetDate = ""

    private val ticker = object : Runnable {
        override fun run() {
            checkDailyReset()
            updateProgress()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, buildNotif())
        handler.post(ticker)
    }

    private fun checkDailyReset() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastResetDate != today) {
            lastResetDate = today
            SeriesStorage.resetProgress(this)
        }
    }

    private fun updateProgress() {
        val um = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val stats = um.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, now)
        val progress = SeriesStorage.loadProgress(this).toMutableMap()
        for (s in stats) if (s.totalTimeInForeground > 0) progress[s.packageName] = s.totalTimeInForeground
        SeriesStorage.saveProgress(this, progress)
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CH)
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
        .setContentTitle("FlowLock Active").setContentText("Monitoring series...").build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(CH, "FlowLock", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
