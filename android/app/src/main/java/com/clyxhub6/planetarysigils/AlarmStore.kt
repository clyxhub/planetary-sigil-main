package com.clyxhub6.planetarysigils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the set of scheduled planetary-hour alarms/reminders so they can be
 * re-registered after a process death, an app restart, a device reboot, or a
 * timezone change. This is the single source of truth: scheduling always goes
 * through this store, so re-scheduling is idempotent and never duplicates.
 */
object AlarmStore {

    private const val PREFS = "planetary_alarms"
    private const val KEY_ITEMS = "items"
    private const val KEY_SOUND_URI = "alarm_sound_uri"
    const val KEY_REMIND_FIRED = "remind_fired" // marker prevents re-firing an already-delivered reminder after reboot

    data class Alarm(
        val timestamp: Long,      // absolute alarm time (epoch millis, local wall-clock intended)
        val planet: String,
        val leadMinutes: Int = 0  // > 0 means a reminder is scheduled leadMinutes before `timestamp`
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("timestamp", timestamp)
            .put("planet", planet)
            .put("leadMinutes", leadMinutes)

        companion object {
            fun fromJson(o: JSONObject): Alarm = Alarm(
                timestamp = o.optLong("timestamp", 0L),
                planet = o.optString("planet", "Unknown"),
                leadMinutes = o.optInt("leadMinutes", 0)
            )
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(context: Context): List<Alarm> {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Alarm.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Inserts or updates by timestamp; saves. Returns true if it was a duplicate. */
    @Synchronized
    fun save(context: Context, alarm: Alarm): Boolean {
        val list = load(context).toMutableList()
        val existed = list.any { it.timestamp == alarm.timestamp }
        // Keep leadMinutes off on cancel path handled separately; here we upsert.
        list.removeAll { it.timestamp == alarm.timestamp }
        list.add(alarm)
        write(context, list)
        return existed
    }

    @Synchronized
    fun remove(context: Context, timestamp: Long) {
        write(context, load(context).filterNot { it.timestamp == timestamp })
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ITEMS).apply()
    }

    @Synchronized
    private fun write(context: Context, items: List<Alarm>) {
        val arr = JSONArray()
        items.sortedBy { it.timestamp }.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    // ── Alarm sound choice ─────────────────────────────────────────────────

    @Synchronized
    fun getAlarmSoundUri(context: Context): String? =
        prefs(context).getString(KEY_SOUND_URI, null)

    @Synchronized
    fun setAlarmSoundUri(context: Context, uri: String?) {
        val editor = prefs(context).edit()
        if (uri.isNullOrEmpty()) editor.remove(KEY_SOUND_URI) else editor.putString(KEY_SOUND_URI, uri)
        editor.apply()
    }

    // ── Reminder fired guards ──────────────────────────────────────────────
    @Synchronized
    fun isReminderFired(context: Context, timestamp: Long): Boolean =
        prefs(context).getString(KEY_REMIND_FIRED, null)?.split(",")?.contains(timestamp.toString()) == true

    @Synchronized
    fun markReminderFired(context: Context, timestamp: Long) {
        val current = prefs(context).getString(KEY_REMIND_FIRED, null)
            ?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
        if (!current.contains(timestamp.toString())) current.add(timestamp.toString())
        prefs(context).edit().putString(KEY_REMIND_FIRED, current.distinct().joinToString(",")).apply()
    }
}
