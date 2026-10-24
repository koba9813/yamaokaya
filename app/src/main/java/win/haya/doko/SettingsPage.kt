package win.haya.doko

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val coroutineScope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(appSettings) }
    var updateState by remember { mutableStateOf<UpdateFlowState>(UpdateFlowState.Hidden) }

    // バックアップの読み込みなど、外から設定が変わったときに表示を追従させる。
    LaunchedEffect(appSettings) {
        settings = appSettings
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

            AppearanceSection(
                themeMode = settings.themeMode,
                fontMode = settings.fontMode,
                onThemeSelected = { mode ->
                    settings = settings.copy(themeMode = mode)
                    onSettingsChanged(settings)
                },
                onFontSelected = { mode ->
                    settings = settings.copy(fontMode = mode)
                    onSettingsChanged(settings)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            LocationNotificationSection(
                settings = settings,
                onSettingsChanged = { updated ->
                    settings = updated
                    onSettingsChanged(updated)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            BackupSection(
                currentSettings = settings,
                onSettingsImported = { imported ->
                    settings = imported
                    onSettingsChanged(imported)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            AppSection(
                updateState = updateState,
                onCheckUpdate = {
                    updateState = UpdateFlowState.Checking
                    coroutineScope.launch {
                        val result = checkForUpdate(context)
                        if (result is UpdateFlowState.Available) {
                            // アプリ内で案内済みとして記録し、通知との重複を防ぐ
                            UpdateCheckWorker.markVersionNotified(context, result.release.versionName)
                        }
                        updateState = result
                    }
                }
            )

            UpdateFlow(
                state = updateState,
                onStateChange = { updateState = it },
                onDismiss = {
                    if (updateState !is UpdateFlowState.Downloading) {
                        updateState = UpdateFlowState.Hidden
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SupportSection()

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
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

@Composable
private fun UpdateCard(
    state: UpdateFlowState,
    onCheckUpdate: () -> Unit
) {
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
                is UpdateFlowState.Checking -> {
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
}

@Composable
private fun AppSection(
    updateState: UpdateFlowState,
    onCheckUpdate: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SectionTitle(title = "アプリ")
    UpdateCard(state = updateState, onCheckUpdate = onCheckUpdate)
    Spacer(modifier = Modifier.height(12.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            ListItem(
                headlineContent = { Text("バージョン") },
                trailingContent = {
                    Text(
                        text = getAppVersionName(context),
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
}

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    fontMode: FontMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onFontSelected: (FontMode) -> Unit
) {
    SectionTitle(title = "表示")
    ThemeCard(selected = themeMode, onThemeSelected = onThemeSelected)
    Spacer(modifier = Modifier.height(12.dp))
    FontCard(selected = fontMode, onFontSelected = onFontSelected)
}

@Composable
private fun ThemeCard(
    selected: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "テーマ",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "アプリ全体の配色",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeOption(
                        mode = mode,
                        selected = selected == mode,
                        onClick = { onThemeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = selected.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * フォント選択カード。各フォントで「あアA」を描画し、見た目の違いが分かるようにする。
 */
@Composable
private fun FontCard(
    selected: FontMode,
    onFontSelected: (FontMode) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "フォント",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "アプリ全体の文字フォント",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FontMode.entries.forEach { mode ->
                    FontOption(
                        mode = mode,
                        selected = selected == mode,
                        onClick = { onFontSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = selected.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationNotificationSection(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    val context = LocalContext.current

    SectionTitle(title = "通知と位置情報")
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
                    SettingsRepository.LOCATION_UPDATE_INTERVAL_OPTIONS.forEach { sec ->
                        FilterChip(
                            selected = settings.locationUpdateIntervalSeconds == sec,
                            onClick = {
                                onSettingsChanged(settings.copy(locationUpdateIntervalSeconds = sec))
                            },
                            label = { Text("${sec}s") }
                        )
                    }
                }
            }
            HorizontalDivider()
            SwitchItem(
                icon = Icons.Filled.Notifications,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "接近通知",
                body = "山岡家の 50m 以内に入ると通知",
                checked = settings.proximityNotificationEnabled,
                onCheckedChange = {
                    onSettingsChanged(settings.copy(proximityNotificationEnabled = it))
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
                    onSettingsChanged(settings.copy(trackerNotificationEnabled = it))
                }
            )
        }
    }
}

@Composable
private fun SupportSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SectionTitle(title = "サポート")
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
            HorizontalDivider()
            LinkItem(
                icon = Icons.Filled.Star,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "koba9813",
                body = "開発者 / GitHub @koba9813",
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
}

@Composable
private fun FontOption(
    mode: FontMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    val fontFamily = fontFamilyFor(mode)

    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape
            )
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                }
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "あアA",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = fontFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (selected) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "選択中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * テーマ選択カード。実際に適用される配色をミニプレビューで表示し、
 * 選択中のテーマは枠線とチェックマークで分かるようにする。
 */
@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = colorSchemeFor(mode)
    val shape = MaterialTheme.shapes.large

    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape
            )
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ThemePreview(
            scheme = scheme,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (selected) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "選択中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** テーマの配色（背景・文字・カード・ボタン・アクセント）を示すミニプレビュー。 */
@Composable
private fun ThemePreview(
    scheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.background)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(scheme.onSurface.copy(alpha = 0.75f))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(scheme.surfaceContainerHigh)
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.85f)
                    .clip(CircleShape)
                    .background(scheme.onSurface.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.55f)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.8f))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(scheme.primary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(scheme.secondary)
            )
        }
    }
}
