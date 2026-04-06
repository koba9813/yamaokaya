package win.haya.yamaokaya

import android.content.Context

data class AppSettings(
    val proximityNotificationEnabled: Boolean = true,
    val trackerNotificationEnabled: Boolean = true,
    val locationUpdateIntervalSeconds: Int = 10
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        return AppSettings(
            proximityNotificationEnabled = prefs.getBoolean("proximity_notification", true),
            trackerNotificationEnabled = prefs.getBoolean("tracker_notification", true),
            locationUpdateIntervalSeconds = prefs.getInt("location_update_interval", 10)
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean("proximity_notification", settings.proximityNotificationEnabled)
            .putBoolean("tracker_notification", settings.trackerNotificationEnabled)
            .putInt("location_update_interval", settings.locationUpdateIntervalSeconds)
            .apply()
    }
}
