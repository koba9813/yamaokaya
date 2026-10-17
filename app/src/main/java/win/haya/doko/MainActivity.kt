package win.haya.doko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        createKokoNotificationChannel(this)

        setContent {
            val viewModel: YamaokayaViewModel = viewModel()

            YamaokayaTheme {
                YamaokayaScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
