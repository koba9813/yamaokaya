package win.haya.doko

import android.content.Context

data class AppSettings(
    val proximityNotificationEnabled: Boolean = true,
    val trackerNotificationEnabled: Boolean = true,
    val locationUpdateIntervalSeconds: Int = 10,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val fontMode: FontMode = FontMode.SMART_FONT_UI
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        return AppSettings(
            proximityNotificationEnabled = prefs.getBoolean("proximity_notification", true),
            trackerNotificationEnabled = prefs.getBoolean("tracker_notification", true),
            locationUpdateIntervalSeconds = normalizeLocationUpdateInterval(
                prefs.getInt("location_update_interval", DEFAULT_LOCATION_INTERVAL_SECONDS)
            ),
            themeMode = ThemeMode.fromStorageKey(prefs.getString("theme_mode", null)),
            fontMode = FontMode.fromStorageKey(prefs.getString("font_mode", null))
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean("proximity_notification", settings.proximityNotificationEnabled)
            .putBoolean("tracker_notification", settings.trackerNotificationEnabled)
            .putInt(
                "location_update_interval",
                normalizeLocationUpdateInterval(settings.locationUpdateIntervalSeconds)
            )
            .putString("theme_mode", settings.themeMode.storageKey)
            .putString("font_mode", settings.fontMode.storageKey)
            .apply()
    }

    companion object {
        /** 設定画面で選べる位置情報の更新間隔（秒）。 */
        val LOCATION_UPDATE_INTERVAL_OPTIONS = listOf(5, 10, 30, 60)

        /** 既定の位置情報の更新間隔（秒）。 */
        const val DEFAULT_LOCATION_INTERVAL_SECONDS = 10

        /**
         * 想定外の値（0・負数・巨大な値）は既定値に丸める。
         * 0 秒のままだと位置情報の更新ループが止まらなくなるため。
         */
        fun normalizeLocationUpdateInterval(seconds: Int): Int =
            if (seconds in LOCATION_UPDATE_INTERVAL_OPTIONS) {
                seconds
            } else {
                DEFAULT_LOCATION_INTERVAL_SECONDS
            }
    }
}
