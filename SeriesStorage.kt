package com.flowlock.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AllowedApp(val packageName: String, val appName: String, val targetMinutes: Int)
data class BlockedApp(val packageName: String, val appName: String)

data class AppSeries(
    val id: String,
    val name: String,
    val activeDays: List<String>,
    val startTime: String,
    val endTime: String,
    val allowedApps: List<AllowedApp>,
    val blockedApps: List<BlockedApp>,
    var isActive: Boolean = true
)

object SeriesStorage {
    private const val PREF = "flowlock_prefs"
    private val DAYS = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    fun saveSeries(context: Context, list: List<AppSeries>) {
        val arr = JSONArray()
        for (s in list) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("startTime", s.startTime)
            obj.put("endTime", s.endTime)
            obj.put("isActive", s.isActive)
            val days = JSONArray(); s.activeDays.forEach { days.put(it) }
            obj.put("activeDays", days)
            val allowed = JSONArray()
            s.allowedApps.forEach { a -> val o = JSONObject(); o.put("pkg", a.packageName); o.put("name", a.appName); o.put("mins", a.targetMinutes); allowed.put(o) }
            obj.put("allowedApps", allowed)
            val blocked = JSONArray()
            s.blockedApps.forEach { b -> val o = JSONObject(); o.put("pkg", b.packageName); o.put("name", b.appName); blocked.put(o) }
            obj.put("blockedApps", blocked)
            arr.put(obj)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("series", arr.toString()).apply()
    }

    fun loadSeries(context: Context): MutableList<AppSeries> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("series", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<AppSeries>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val daysArr = obj.getJSONArray("activeDays")
            val days = mutableListOf<String>()
            for (j in 0 until daysArr.length()) days.add(daysArr.getString(j))
            val allowedArr = obj.getJSONArray("allowedApps")
            val allowed = mutableListOf<AllowedApp>()
            for (j in 0 until allowedArr.length()) { val o = allowedArr.getJSONObject(j); allowed.add(AllowedApp(o.getString("pkg"), o.getString("name"), o.getInt("mins"))) }
            val blockedArr = obj.getJSONArray("blockedApps")
            val blocked = mutableListOf<BlockedApp>()
            for (j in 0 until blockedArr.length()) { val o = blockedArr.getJSONObject(j); blocked.add(BlockedApp(o.getString("pkg"), o.getString("name"))) }
            list.add(AppSeries(obj.getString("id"), obj.getString("name"), days, obj.getString("startTime"), obj.getString("endTime"), allowed, blocked, obj.optBoolean("isActive", true)))
        }
        return list
    }

    fun saveProgress(context: Context, progress: Map<String, Long>) {
        val obj = JSONObject()
        for ((k, v) in progress) obj.put(k, v)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("progress", obj.toString()).apply()
    }

    fun loadProgress(context: Context): MutableMap<String, Long> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("progress", "{}") ?: "{}"
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Long>()
        for (k in obj.keys()) map[k] = obj.getLong(k)
        return map
    }

    fun resetProgress(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("progress").apply()
    }

    fun isSeriesActive(series: AppSeries): Boolean {
        if (!series.isActive) return false
        val cal = java.util.Calendar.getInstance()
        val today = DAYS[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        if (today !in series.activeDays) return false
        val nowH = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val nowM = cal.get(java.util.Calendar.MINUTE)
        val nowMins = nowH * 60 + nowM
        val (sh, sm) = series.startTime.split(":").map { it.toInt() }
        val (eh, em) = series.endTime.split(":").map { it.toInt() }
        val startMins = sh * 60 + sm
        val endMins = eh * 60 + em
        return nowMins in startMins until endMins
    }
}
