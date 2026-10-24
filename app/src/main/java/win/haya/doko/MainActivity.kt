package win.haya.doko

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    /** アップデート通知から起動されたときに増える。YamaokayaScreen が即座にアップデート確認を行う。 */
    private val updateCheckRequest = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        createKokoNotificationChannel(this)
        createUpdateNotificationChannel(this)
        UpdateCheckWorker.schedule(this)

        handleUpdateCheckIntent(intent)

        setContent {
            val viewModel: YamaokayaViewModel = viewModel()
            val appSettings by viewModel.appSettings
            val updateCheckRequestValue by updateCheckRequest

            YamaokayaTheme(
                themeMode = appSettings.themeMode,
                fontMode = appSettings.fontMode
            ) {
                YamaokayaScreen(
                    viewModel = viewModel,
                    updateCheckRequest = updateCheckRequestValue
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateCheckIntent(intent)
    }

    private fun handleUpdateCheckIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateCheckWorker.EXTRA_CHECK_UPDATE, false) == true) {
            updateCheckRequest.intValue++
        }
    }
}
