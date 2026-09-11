package com.clyxhub6.planetarysigils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restores scheduled planetary-hour alarms/reminders after events that would
 * otherwise wipe them out: device reboot, clock/timezone change, or an app
 * update (package replaced). Records are read from AlarmStore and re-registered
 * through PlanetaryAlarmPlugin, which itself persists them, so the whole
 * operation is idempotent — no duplicates are created.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED) {
            return
        }
        Log.d(TAG, "Received $action — restoring scheduled planetary hours")

        // Direct broadcast call so we don't depend on the Capacitor bridge
        // being running. The plugin object holds no state, so a plain static
        // entry point is safe and always reflects the persisted schedule.
        PlanetaryAlarmPlugin.scheduleFromStore(context.applicationContext)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
