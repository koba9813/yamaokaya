package win.haya.yamaokaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
