package win.haya.doko

import android.content.Context

data class AppSettings(
    val proximityNotificationEnabled: Boolean = true,
    val trackerNotificationEnabled: Boolean = true,
    val locationUpdateIntervalSeconds: Int = 10,
    val themeMode: ThemeMode = ThemeMode.DARK
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        return AppSettings(
            proximityNotificationEnabled = prefs.getBoolean("proximity_notification", true),
            trackerNotificationEnabled = prefs.getBoolean("tracker_notification", true),
            locationUpdateIntervalSeconds = prefs.getInt("location_update_interval", 10),
            themeMode = ThemeMode.fromStorageKey(prefs.getString("theme_mode", null))
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean("proximity_notification", settings.proximityNotificationEnabled)
            .putBoolean("tracker_notification", settings.trackerNotificationEnabled)
            .putInt("location_update_interval", settings.locationUpdateIntervalSeconds)
            .putString("theme_mode", settings.themeMode.storageKey)
            .apply()
    }
}
