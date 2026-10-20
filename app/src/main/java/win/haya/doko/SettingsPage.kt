package win.haya.doko

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
    var updateState by remember { mutableStateOf<UpdateFlowState>(UpdateFlowState.Hidden) }

    // バックアップの読み込みなど、外から設定が変わったときに表示を追従させる。
    LaunchedEffect(appSettings) {
        settings = appSettings
    }

    ScreenEnter(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = screenContainerColor(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopBar(
                    title = "設定",
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
                    .padding(horizontal = AppSpacing.screen)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SectionTitle(title = "テーマ")
                Spacer(modifier = Modifier.height(8.dp))
                ThemeSection(
                    selected = settings.themeMode,
                    onThemeSelected = { mode ->
                        settings = settings.copy(themeMode = mode)
                        onSettingsChanged(settings)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(title = "Liquid Glass")
                Spacer(modifier = Modifier.height(8.dp))
                LiquidGlassSection(
                    enabled = settings.liquidGlassEnabled,
                    onEnabledChange = { enabled ->
                        settings = settings.copy(liquidGlassEnabled = enabled)
                        onSettingsChanged(settings)
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "位置情報")
                Spacer(modifier = Modifier.height(8.dp))
                LocationSection(
                    intervalSeconds = settings.locationUpdateIntervalSeconds,
                    onIntervalChange = { seconds ->
                        settings = settings.copy(locationUpdateIntervalSeconds = seconds)
                        onSettingsChanged(settings)
                    },
                    onOpenSystemSettings = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "通知")
                Spacer(modifier = Modifier.height(8.dp))
                NotificationSection(
                    proximityEnabled = settings.proximityNotificationEnabled,
                    onProximityChange = {
                        settings = settings.copy(proximityNotificationEnabled = it)
                        onSettingsChanged(settings)
                    },
                    trackerEnabled = settings.trackerNotificationEnabled,
                    onTrackerChange = {
                        settings = settings.copy(trackerNotificationEnabled = it)
                        onSettingsChanged(settings)
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "データの引き継ぎ")
                Spacer(modifier = Modifier.height(8.dp))
                BackupSection(
                    currentSettings = settings,
                    onSettingsImported = { imported ->
                        settings = imported
                        onSettingsChanged(imported)
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "アップデート")
                Spacer(modifier = Modifier.height(8.dp))
                UpdateSection(
                    state = updateState,
                    onCheckUpdate = {
                        updateState = UpdateFlowState.Checking
                        coroutineScope.launch {
                            updateState = checkForUpdate(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "アプリ情報")
                Spacer(modifier = Modifier.height(8.dp))
                AppInfoSection(context = context)

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "開発者")
                Spacer(modifier = Modifier.height(8.dp))
                DeveloperSection(
                    onOpenDeveloper = { uriHandler.openUri("https://github.com/koba9813") }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle(title = "フィードバック")
                Spacer(modifier = Modifier.height(8.dp))
                FeedbackSection(
                    onOpenIssue = { uriHandler.openUri("https://github.com/koba9813/yamaokaya/issues/new") },
                    onSendEmail = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_SUBJECT, "Yamaokaya is Doko - 意見・要望")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            UpdateFlow(
                state = updateState,
                onStateChange = { updateState = it },
                onDismiss = {
                    if (updateState !is UpdateFlowState.Downloading) {
                        updateState = UpdateFlowState.Hidden
                    }
                }
            )
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    SectionLabel(text = title)
}

/* -------------------------------------------------------------------------
 * Appearance
 * ---------------------------------------------------------------------- */

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    SettingsGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "アプリ全体の配色を選びます",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
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
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "${selected.displayName}：${selected.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LiquidGlassSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    SettingsGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconTile(
                icon = Icons.Outlined.BlurOn,
                tint = scheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Liquid Glass",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BetaBadge()
                }
                Text(
                    text = "ナビゲーションと操作部品を、背景になじむガラス表現にします",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }

        Hairline()

        Column(modifier = Modifier.padding(16.dp)) {
            LiquidGlassDemo(enabled = enabled)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (enabled) {
                    "背景が透けるガラス素材を、ヘッダー・ボタン・フィルターなど操作部品にだけ使います。設定行や本文は読みやすさを優先しています。"
                } else {
                    "有効にすると、ナビゲーションやボタンが背景の上に浮かぶガラス表現になります。テーマの配色はそのまま使います。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BetaBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = "BETA",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * 実際の写真の上にガラスのピルを置いた、小さな実演用プレビュー。
 * 装飾用の大きなバナーにはせず、オン / オフの違いだけを示す。
 */
@Composable
private fun LiquidGlassDemo(enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.yamaokaya),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))
                    )
                )
        )
        GlassSurface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp),
            shape = AppShapes.pill,
            level = GlassLevel.Regular,
            tint = if (enabled) null else scheme.surfaceContainerHigh,
            tintAlpha = 1f,
            shadowElevation = if (enabled) 6.dp else 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (enabled) liquidGlassTextAccent() else scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Yamaokaya is 1.24 km",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
            }
        }
    }
}

/**
 * テーマ選択カード。実際に適用される配色をミニプレビューで表示し、
 * 選択中は枠線・チェック・背景の3つで示す（色だけに依存しない）。
 */
@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = colorSchemeFor(mode)
    val shape = RoundedCornerShape(16.dp)
    val current = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) current.primary.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) {
                    current.primary
                } else {
                    current.outlineVariant.copy(alpha = 0.55f)
                },
                shape = shape
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) current.primary else current.onSurface
            )
            if (selected) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "選択中",
                    tint = current.primary,
                    modifier = Modifier.size(13.dp)
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
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.background)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(scheme.onSurface.copy(alpha = 0.72f))
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(scheme.surfaceContainerHigh)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.86f)
                    .clip(CircleShape)
                    .background(scheme.onSurface.copy(alpha = 0.78f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.56f)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.78f))
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.62f)
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

/* -------------------------------------------------------------------------
 * Location / notifications
 * ---------------------------------------------------------------------- */

@Composable
private fun LocationSection(
    intervalSeconds: Int,
    onIntervalChange: (Int) -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    SettingsGroup {
        SettingsRow(
            title = "位置情報の権限",
            subtitle = "OSの設定から変更できます",
            icon = Icons.Outlined.LocationOn,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = onOpenSystemSettings,
            trailing = { OpenAffordance(text = "開く") }
        )
        Hairline()
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "位置情報の更新間隔",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "近づいたときの距離を更新する頻度です。短くすると電池を多く使います。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.LOCATION_UPDATE_INTERVAL_OPTIONS.forEach { seconds ->
                    FilterPill(
                        label = "${seconds}秒",
                        selected = intervalSeconds == seconds,
                        onClick = { onIntervalChange(seconds) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSection(
    proximityEnabled: Boolean,
    onProximityChange: (Boolean) -> Unit,
    trackerEnabled: Boolean,
    onTrackerChange: (Boolean) -> Unit
) {
    SettingsGroup {
        SettingsRow(
            title = "接近通知",
            subtitle = "山岡家の 50m 以内に入ると通知します",
            icon = Icons.Outlined.Notifications,
            iconTint = MaterialTheme.colorScheme.primary,
            trailing = {
                Switch(
                    checked = proximityEnabled,
                    onCheckedChange = onProximityChange
                )
            }
        )
        Hairline()
        SettingsRow(
            title = "常時通知（距離トラッカー）",
            subtitle = "通知バーに最寄り店舗の距離を表示します",
            icon = Icons.Outlined.Notifications,
            iconTint = MaterialTheme.colorScheme.secondary,
            trailing = {
                Switch(
                    checked = trackerEnabled,
                    onCheckedChange = onTrackerChange
                )
            }
        )
    }
}

/* -------------------------------------------------------------------------
 * Update / app info / links
 * ---------------------------------------------------------------------- */

@Composable
private fun UpdateSection(
    state: UpdateFlowState,
    onCheckUpdate: () -> Unit
) {
    SettingsGroup {
        SettingsRow(
            title = "最新版を確認",
            subtitle = "GitHub Releases から APK を取得します",
            icon = Icons.Outlined.Download,
            iconTint = MaterialTheme.colorScheme.primary,
            trailing = {
                when (state) {
                    is UpdateFlowState.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        GlassPillButton(
                            text = "確認",
                            onClick = onCheckUpdate,
                            compact = true,
                            icon = Icons.Outlined.Download
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun AppInfoSection(context: android.content.Context) {
    val uriHandler = LocalUriHandler.current
    SettingsGroup {
        SettingsRow(
            title = "バージョン",
            trailing = {
                Text(
                    text = getAppVersionName(context).removePrefix("V."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Hairline()
        SettingsRow(
            title = "登録店舗数",
            trailing = {
                Text(
                    text = "${YamaokayaFinder.getRegisteredShops().size} 店舗",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Hairline()
        SettingsRow(
            title = "GitHub リポジトリ",
            subtitle = "ソースコードとリリース",
            icon = Icons.Outlined.Code,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = { uriHandler.openUri("https://github.com/koba9813/yamaokaya") },
            trailing = { OpenAffordance() }
        )
    }
}

@Composable
private fun DeveloperSection(onOpenDeveloper: () -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = "koba9813",
            subtitle = "GitHub @koba9813",
            icon = Icons.Outlined.Person,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = onOpenDeveloper,
            trailing = { OpenAffordance() }
        )
        Hairline()
        SettingsRow(
            title = "Special Thanks",
            subtitle = "Photo by Sagara",
            icon = Icons.Outlined.Star,
            iconTint = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun FeedbackSection(
    onOpenIssue: () -> Unit,
    onSendEmail: () -> Unit
) {
    SettingsGroup {
        SettingsRow(
            title = "GitHub Issue を作成",
            subtitle = "バグ報告や機能リクエストはこちらへ",
            icon = Icons.Outlined.Info,
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = onOpenIssue,
            trailing = { OpenAffordance() }
        )
        Hairline()
        SettingsRow(
            title = "メールで送る",
            subtitle = "Yamaokaya is Doko - 意見・要望",
            icon = Icons.Outlined.Email,
            iconTint = MaterialTheme.colorScheme.tertiary,
            onClick = onSendEmail,
            trailing = { OpenAffordance() }
        )
    }
}

@Composable
private fun OpenAffordance(text: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}
