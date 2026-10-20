package win.haya.doko

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun InformationPage(
    onClose: () -> Unit
) {
    ScreenEnter(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = screenContainerColor(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopBar(
                    title = "Information",
                    onBack = onClose
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                GlassSurface(
                    shape = CircleShape,
                    level = GlassLevel.Thin,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Yamaokaya is Doko",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "最寄りの山岡家までの距離と方角を、ひと目で確認できるアプリです。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))
                SectionTitle(title = "できること")
                Spacer(modifier = Modifier.height(8.dp))

                SettingsGroup {
                    SettingsRow(
                        title = "ラーメンの向く方向が最寄り店舗",
                        subtitle = "画面中央のラーメン画像の上側が、最寄りの山岡家の方向を指しています。",
                        icon = Icons.Outlined.LocationOn,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    Hairline()
                    SettingsRow(
                        title = "ラーメンをタップしてみよう",
                        subtitle = "タップすると別のラーメン画像に変わるかもしれません。",
                        icon = Icons.Outlined.Star,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                    Hairline()
                    SettingsRow(
                        title = "リアルタイムで更新",
                        subtitle = "距離と方角は位置情報に応じて随時更新されます。",
                        icon = Icons.Outlined.Refresh,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    Hairline()
                    SettingsRow(
                        title = "到着でチェックイン",
                        subtitle = "山岡家に近づくとスタンプラリーでチェックインできます。",
                        icon = Icons.Outlined.Favorite,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    Hairline()
                    SettingsRow(
                        title = "非公認アプリ",
                        subtitle = "本アプリは山岡家の公式アプリではなく、ファン制作の非公認アプリです。",
                        icon = Icons.Outlined.Info,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
