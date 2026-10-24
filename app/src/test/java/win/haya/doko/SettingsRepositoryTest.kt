package win.haya.doko

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = SettingsRepository(context)
    }

    @Test
    fun defaultSettings_useDarkTheme() {
        val settings = repository.getSettings()

        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(FontMode.SMART_FONT_UI, settings.fontMode)
        assertTrue(settings.proximityNotificationEnabled)
        assertTrue(settings.trackerNotificationEnabled)
        assertEquals(10, settings.locationUpdateIntervalSeconds)
    }

    @Test
    fun save_persistsEveryThemeMode() {
        ThemeMode.entries.forEach { mode ->
            repository.save(repository.getSettings().copy(themeMode = mode))

            assertEquals(mode, SettingsRepository(context).getSettings().themeMode)
        }
    }

    @Test
    fun save_persistsEveryFontMode() {
        FontMode.entries.forEach { mode ->
            repository.save(repository.getSettings().copy(fontMode = mode))

            assertEquals(mode, SettingsRepository(context).getSettings().fontMode)
        }
    }

    @Test
    fun save_persistsOtherSettingsWithTheme() {
        repository.save(
            AppSettings(
                proximityNotificationEnabled = false,
                trackerNotificationEnabled = false,
                locationUpdateIntervalSeconds = 60,
                themeMode = ThemeMode.YAMAOKAYA,
                fontMode = FontMode.SMART_FONT_UI
            )
        )

        val restored = SettingsRepository(context).getSettings()
        assertEquals(false, restored.proximityNotificationEnabled)
        assertEquals(false, restored.trackerNotificationEnabled)
        assertEquals(60, restored.locationUpdateIntervalSeconds)
        assertEquals(ThemeMode.YAMAOKAYA, restored.themeMode)
        assertEquals(FontMode.SMART_FONT_UI, restored.fontMode)
    }

    @Test
    fun invalidStoredInterval_fallsBackToDefault() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("location_update_interval", 0)
            .commit()

        assertEquals(
            SettingsRepository.DEFAULT_LOCATION_INTERVAL_SECONDS,
            SettingsRepository(context).getSettings().locationUpdateIntervalSeconds
        )
    }

    @Test
    fun save_ignoresOutOfRangeInterval() {
        repository.save(repository.getSettings().copy(locationUpdateIntervalSeconds = -5))

        assertEquals(
            SettingsRepository.DEFAULT_LOCATION_INTERVAL_SECONDS,
            SettingsRepository(context).getSettings().locationUpdateIntervalSeconds
        )
    }

    @Test
    fun unknownStoredTheme_fallsBackToDark() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("theme_mode", "unknown-theme")
            .commit()

        assertEquals(ThemeMode.DARK, SettingsRepository(context).getSettings().themeMode)
    }

    @Test
    fun unknownStoredFont_fallsBackToSmartFontUi() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("font_mode", "unknown-font")
            .commit()

        assertEquals(FontMode.SMART_FONT_UI, SettingsRepository(context).getSettings().fontMode)
    }

    @Test
    fun fontModeFromStorageKey_resolvesStoredValues() {
        FontMode.entries.forEach { mode ->
            assertEquals(mode, FontMode.fromStorageKey(mode.storageKey))
        }
        assertEquals(FontMode.SMART_FONT_UI, FontMode.fromStorageKey(null))
        assertEquals(FontMode.SMART_FONT_UI, FontMode.fromStorageKey(""))
    }

    @Test
    fun themeModeFromStorageKey_resolvesStoredValues() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorageKey(mode.storageKey))
        }
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey(""))
    }

    @Test
    fun themeMode_isDarkOnlyForLightTheme() {
        assertTrue(ThemeMode.DARK.isDark)
        assertTrue(ThemeMode.YAMAOKAYA.isDark)
        assertFalse(ThemeMode.LIGHT.isDark)
    }
}
