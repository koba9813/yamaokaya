package win.haya.doko

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun InformationPage(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember { getAppVersionName(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // ヘッダー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "閉じる")
            }
            Text(
                text = "Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            HeroCard(versionName = versionName)

            Spacer(modifier = Modifier.height(20.dp))

            InfoSectionTitle("主な機能")
            InfoSectionCard {
                FeatureItem(
                    icon = Icons.Filled.LocationOn,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "最寄り店舗と方角",
                    body = "現在地から最寄りの山岡家までの距離と方角を表示します。画面中央のラーメン画像の上側が、店舗の方向を指しています。"
                )
                FeatureItem(
                    icon = Icons.Filled.Star,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "スタンプラリー",
                    body = "店舗の近くでチェックインすると訪問記録が貯まり、都道府県ごとの進捗を確認できます。"
                )
                FeatureItem(
                    icon = Icons.Filled.PhotoLibrary,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "写真ギャラリー",
                    body = "アプリ内カメラで撮影した写真を、お気に入りや店舗別アルバムで整理できます。Instagram ストーリーへの共有もできます。"
                )
                FeatureItem(
                    icon = Icons.Filled.Widgets,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "ホーム画面ウィジェット",
                    body = "標準・コンパクト・ミニマルの3種類。ホーム画面から最寄り店舗と距離をひと目で確認できます。"
                )
                FeatureItem(
                    icon = Icons.Filled.Palette,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "テーマとフォント",
                    body = "ダーク / ライト / 山岡家の3テーマと、2種類のフォントを設定画面から選べます。"
                )
                FeatureItem(
                    icon = Icons.Filled.Notifications,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "アップデート通知",
                    body = "新しいバージョンが公開されると、通知でお知らせします。"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            InfoSectionTitle("権限について")
            InfoSectionCard {
                FeatureItem(
                    icon = Icons.Filled.LocationOn,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "位置情報",
                    body = "最寄り店舗の距離・方角の表示と、チェックインの判定に使います。端末の外へ送信しません。"
                )
                FeatureItem(
                    icon = Icons.Filled.CameraAlt,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "カメラ",
                    body = "アプリ内で写真を撮影するためだけに使います。"
                )
                FeatureItem(
                    icon = Icons.Filled.Notifications,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "通知",
                    body = "山岡家への接近と、アプリのアップデートのお知らせに使います。"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            InfoSectionTitle("リンク")
            InfoSectionCard {
                LinkItem(
                    icon = Icons.Filled.Code,
                    label = "GitHub リポジトリ",
                    description = "ソースコード（オープンソース）",
                    onClick = { openUrlInAppBrowser(context, GITHUB_REPO_URL) }
                )
                LinkItem(
                    icon = Icons.Filled.Language,
                    label = "山岡家公式サイト",
                    description = "www.yamaokaya.com",
                    onClick = { openYamaokayaOfficialSite(context) }
                )
                LinkItem(
                    icon = Icons.Filled.Smartphone,
                    label = "山岡家公式アプリ",
                    description = "ポイント・クーポンが使える公式アプリ",
                    onClick = { openYamaokayaOfficialApp(context) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            InfoSectionTitle("このアプリについて")
            InfoSectionCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "本アプリは山岡家の公式アプリではありません。ファンが個人で開発したオープンソースアプリです。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    AboutRow("バージョン", versionName)
                    AboutRow("開発", "koba9813")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
        }
    }
}

@Composable
private fun HeroCard(versionName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "アプリアイコン",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Text(
                text = "Yamaokaya is Doko?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = "最寄りの山岡家までの距離と方角を、ひと目で確認できるアプリです。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoSectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Filled.OpenInNew,
            contentDescription = "開く",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
