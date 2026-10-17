package win.haya.doko

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPage(
    appSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(appSettings) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var pendingRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        pendingRelease?.let { release ->
            if (UpdateChecker.canInstallPackages(context)) {
                coroutineScope.launch { downloadAndInstall(context, release) { updateState = it } }
            }
            pendingRelease = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "閉じる"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(title = "位置情報")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("位置情報の権限") },
                        supportingContent = { Text("OS の設定から変更できます") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("開く")
                            }
                        }
                    )
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "位置情報の更新間隔",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${settings.locationUpdateIntervalSeconds} 秒ごとに更新",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 30, 60).forEach { sec ->
                                FilterChip(
                                    selected = settings.locationUpdateIntervalSeconds == sec,
                                    onClick = {
                                        settings = settings.copy(locationUpdateIntervalSeconds = sec)
                                        onSettingsChanged(settings)
                                    },
                                    label = { Text("${sec}s") }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(title = "通知")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    SwitchItem(
                        icon = Icons.Filled.Notifications,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "接近通知",
                        body = "山岡家の 50m 以内に入ると通知",
                        checked = settings.proximityNotificationEnabled,
                        onCheckedChange = {
                            settings = settings.copy(proximityNotificationEnabled = it)
                            onSettingsChanged(settings)
                        }
                    )
                    HorizontalDivider()
                    SwitchItem(
                        icon = Icons.Filled.Notifications,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "常時通知（距離トラッカー）",
                        body = "通知バーに最寄り店舗の距離を常時表示",
                        checked = settings.trackerNotificationEnabled,
                        onCheckedChange = {
                            settings = settings.copy(trackerNotificationEnabled = it)
                            onSettingsChanged(settings)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(title = "アプリ情報")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("バージョン") },
                        trailingContent = {
                            Text(
                                text = getAppVersionName(context).removePrefix("V."),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("登録店舗数") },
                        trailingContent = {
                            Text(
                                text = "${YamaokayaFinder.getRegisteredShops().size} 店舗",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider()
                    LinkItem(
                        icon = Icons.Filled.Star,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "GitHub リポジトリ",
                        onClick = { uriHandler.openUri("https://github.com/koba9813/yamaokaya") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            UpdateSection(
                state = updateState,
                onCheckUpdate = {
                    updateState = UpdateState.Checking
                    coroutineScope.launch {
                        updateState = try {
                            val release = UpdateChecker.checkForUpdate(context)
                            if (release != null) {
                                UpdateState.Available(release)
                            } else {
                                UpdateState.UpToDate
                            }
                        } catch (e: Exception) {
                            UpdateState.Error(e.message ?: "不明なエラー")
                        }
                    }
                },
                onConfirmInstall = { release ->
                if (!UpdateChecker.canInstallPackages(context)) {
                    pendingRelease = release
                    installPermissionLauncher.launch(
                        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } else {
                        coroutineScope.launch {
                            downloadAndInstall(context, release) { updateState = it }
                        }
                    }
                },
                onDismiss = {
                    if (updateState !is UpdateState.Downloading) {
                        updateState = UpdateState.Idle
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(title = "開発者")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    LinkItem(
                        icon = Icons.Filled.Star,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "koba9813",
                        body = "GitHub @koba9813",
                        onClick = { uriHandler.openUri("https://github.com/koba9813") }
                    )
                    HorizontalDivider()
                    LinkItem(
                        icon = Icons.Filled.Info,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Special Thanks",
                        body = "Photo by Sagara",
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(title = "フィードバック")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    LinkItem(
                        icon = Icons.Filled.Info,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "GitHub Issue を作成",
                        body = "バグ報告や機能リクエストはこちらへ",
                        onClick = { uriHandler.openUri("https://github.com/koba9813/yamaokaya/issues/new") }
                    )
                    HorizontalDivider()
                    LinkItem(
                        icon = Icons.Filled.Email,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "メールで送る",
                        body = "Yamaokaya is Doko - 意見・要望",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_SUBJECT, "Yamaokaya is Doko - 意見・要望")
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun LinkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    body: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = body?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val release: ReleaseInfo) : UpdateState()
    data object UpToDate : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Composable
private fun UpdateSection(
    state: UpdateState,
    onCheckUpdate: () -> Unit,
    onConfirmInstall: (ReleaseInfo) -> Unit,
    onDismiss: () -> Unit
) {
    SectionTitle(title = "アップデート")
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "最新版を確認",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GitHub Releases から APK を取得します",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (state) {
                is UpdateState.Checking -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                else -> {
                    OutlinedButton(onClick = onCheckUpdate) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("確認")
                    }
                }
            }
        }
    }

    when (state) {
        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("新しいバージョンがあります") },
                text = {
                    Column {
                        Text(
                            text = "最新: ${state.release.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.release.body.ifBlank { "リリースノートはありません" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { onConfirmInstall(state.release) }) {
                        Text("ダウンロードしてインストール")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                }
            )
        }
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("ダウンロード中") },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(state.progress * 100).toInt()}%")
                    }
                },
                confirmButton = { }
            )
        }
        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("最新版です") },
                text = { Text("現在のバージョンが最新です。") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("エラー") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
        else -> { }
    }
}

private suspend fun downloadAndInstall(
    context: android.content.Context,
    release: ReleaseInfo,
    onState: (UpdateState) -> Unit
) {
    onState(UpdateState.Downloading(0f))
    try {
        val apkFile = UpdateChecker.downloadApk(context, release.downloadUrl) { progress ->
            onState(UpdateState.Downloading(progress))
        }
        onState(UpdateState.Downloading(1f))
        UpdateChecker.installApk(context, apkFile)
        onState(UpdateState.Idle)
    } catch (e: Exception) {
        onState(UpdateState.Error(e.message ?: "ダウンロードに失敗しました"))
    }
}
