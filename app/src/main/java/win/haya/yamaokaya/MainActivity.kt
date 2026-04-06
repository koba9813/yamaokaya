package win.haya.yamaokaya

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "山岡家接近通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "山岡家の50m以内に入ったときに通知します"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val settingsRepository = SettingsRepository(this)

        setContent {
            var appSettings by remember { mutableStateOf(settingsRepository.getSettings()) }

            MaterialTheme(typography = appTypography) {
                YamaokayaScreen(
                    appSettings = appSettings,
                    onSettingsChanged = { newSettings ->
                        settingsRepository.save(newSettings)
                        appSettings = newSettings
                    }
                )
            }
        }
    }
}
