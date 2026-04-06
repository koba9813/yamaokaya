package win.haya.yamaokaya

import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
internal fun SettingsPage(
    appSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var settings by remember { mutableStateOf(appSettings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 位置情報
        Text(
            text = "位置情報",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("位置情報の権限はOSの設定から変更できます", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("アプリの設定を開く")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("位置情報の更新間隔", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${settings.locationUpdateIntervalSeconds}秒",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(5, 10, 30, 60).forEach { sec ->
                            val isSelected = settings.locationUpdateIntervalSeconds == sec
                            Button(
                                onClick = {
                                    settings = settings.copy(locationUpdateIntervalSeconds = sec)
                                    onSettingsChanged(settings)
                                },
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                },
                                border = if (!isSelected) {
                                    ButtonDefaults.outlinedButtonBorder
                                } else null,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("${sec}s", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 通知
        Text(
            text = "通知",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("接近通知", style = MaterialTheme.typography.bodyLarge)
                        Text("山岡家の50m以内に入ると通知", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.proximityNotificationEnabled,
                        onCheckedChange = {
                            settings = settings.copy(proximityNotificationEnabled = it)
                            onSettingsChanged(settings)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("常時通知（距離トラッカー）", style = MaterialTheme.typography.bodyLarge)
                        Text("通知バーに最寄り店舗の距離を常時表示", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.trackerNotificationEnabled,
                        onCheckedChange = {
                            settings = settings.copy(trackerNotificationEnabled = it)
                            onSettingsChanged(settings)
                            if (it) {
                                startDistanceTrackerService(context)
                            } else {
                                context.stopService(Intent(context, DistanceTrackerService::class.java))
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // アプリの詳細
        Text(
            text = "アプリの詳細",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // リリースノート
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                var releaseNotes by remember { mutableStateOf<String?>(null) }
                var releaseVersion by remember { mutableStateOf<String?>(null) }
                var isLoadingNotes by remember { mutableStateOf(true) }
                var showFullNotes by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val info = UpdateChecker.fetchLatestRelease(context)
                    releaseVersion = info?.latestVersion
                    releaseNotes = info?.releaseNotes
                    isLoadingNotes = false
                }

                Text(
                    text = "最新のリリースノート",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingNotes) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp
                        )
                        Text("読み込み中…", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (releaseNotes.isNullOrBlank()) {
                    Text(
                        "リリースノートを取得できませんでした",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (releaseVersion != null) {
                        Text(
                            text = releaseVersion!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val displayNotes = if (showFullNotes) {
                        releaseNotes!!
                    } else {
                        releaseNotes!!.take(200) +
                            if (releaseNotes!!.length > 200) "…" else ""
                    }

                    AndroidView(
                        factory = { ctx -> TextView(ctx) },
                        update = { textView ->
                            val markwon = Markwon.create(textView.context)
                            markwon.setMarkdown(textView, displayNotes)
                        }
                    )

                    if (releaseNotes!!.length > 200) {
                        TextButton(onClick = { showFullNotes = !showFullNotes }) {
                            Text(if (showFullNotes) "閉じる" else "すべて表示")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "すべてのリリースを見る",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/koba9813/yamaokaya/releases")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // アプリ情報
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "アプリ情報",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("バージョン", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        getAppVersionName(context).removePrefix("V."),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("登録店舗数", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${YamaokayaFinder.getRegisteredShops().size}店舗",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
                Text(
                    text = "GitHub リポジトリ",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/koba9813/yamaokaya")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 開発者情報
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "開発者情報",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("開発者", style = MaterialTheme.typography.bodyMedium)
                    Text("koba9813", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                Text(
                    text = "GitHub @koba9813",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/koba9813")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 意見・要望
        Text(
            text = "意見・要望",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "バグ報告や機能リクエストなどお気軽にどうぞ",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/koba9813/yamaokaya/issues/new")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GitHub Issueを作成")
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_SUBJECT, "Yamaokaya is Doko - 意見・要望")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("メールで送る")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
