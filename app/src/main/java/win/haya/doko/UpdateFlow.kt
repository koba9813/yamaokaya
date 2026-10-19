package win.haya.doko

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

internal sealed class UpdateFlowState {
    data object Hidden : UpdateFlowState()
    data object Checking : UpdateFlowState()
    data class Available(val release: ReleaseInfo) : UpdateFlowState()
    data object UpToDate : UpdateFlowState()
    data class Downloading(val progress: Float) : UpdateFlowState()
    data class Error(val message: String) : UpdateFlowState()
}

@Composable
internal fun UpdateFlow(
    state: UpdateFlowState,
    onStateChange: (UpdateFlowState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        pendingRelease?.let { release ->
            if (UpdateChecker.canInstallPackages(context)) {
                coroutineScope.launch {
                    downloadAndInstall(context, release, onStateChange)
                }
            }
            pendingRelease = null
        }
    }

    val onConfirmInstall: (ReleaseInfo) -> Unit = { release ->
        if (!UpdateChecker.canInstallPackages(context)) {
            pendingRelease = release
            installPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } else {
            coroutineScope.launch {
                downloadAndInstall(context, release, onStateChange)
            }
        }
    }

    when (state) {
        is UpdateFlowState.Available -> UpdateAvailableDialog(
            release = state.release,
            onInstall = { onConfirmInstall(state.release) },
            onDismiss = onDismiss,
            modifier = modifier
        )

        is UpdateFlowState.Downloading -> UpdateDownloadingDialog(
            progress = state.progress,
            modifier = modifier
        )

        is UpdateFlowState.UpToDate -> UpdateResultDialog(
            icon = Icons.Rounded.CheckCircle,
            accentColor = MaterialTheme.colorScheme.tertiary,
            title = "最新版です",
            message = "現在のバージョン ${getAppVersionName(context)} が最新です。",
            onDismiss = onDismiss,
            modifier = modifier
        )

        is UpdateFlowState.Error -> UpdateResultDialog(
            icon = Icons.Rounded.ErrorOutline,
            accentColor = MaterialTheme.colorScheme.error,
            title = "アップデートできませんでした",
            message = state.message,
            onDismiss = onDismiss,
            modifier = modifier
        )

        else -> { }
    }
}

/**
 * アップデート関連ダイアログの共通の土台。
 * 角丸のサーフェスにアイコンバッジ・タイトル・本文を縦に並べる。
 */
@Composable
private fun UpdateDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
    }
}

/** グラデーションの丸バッジ。ダイアログの主役アイコン。 */
@Composable
private fun UpdateIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    brush: Brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    ),
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val glow = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    Box(modifier = modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .background(
                    Brush.radialGradient(listOf(glow, Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    release: ReleaseInfo,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    UpdateDialog(onDismissRequest = onDismiss, modifier = modifier) {
        UpdateIconBadge(icon = Icons.Rounded.Update)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "新しいバージョンがあります",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "最新版にアップデートできます",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        VersionTransitionRow(
            currentVersion = getAppVersionName(context),
            latestVersion = release.versionName
        )
        Spacer(modifier = Modifier.height(18.dp))
        ReleaseNotesCard(body = release.body)
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onInstall,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "ダウンロードしてインストール", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("あとで")
        }
    }
}

@Composable
private fun UpdateDownloadingDialog(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "updateDownloadProgress"
    )
    val preparingInstall = progress >= 1f

    UpdateDialog(onDismissRequest = { }, modifier = modifier) {
        UpdateIconBadge(icon = Icons.Rounded.Download)
        Spacer(modifier = Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(92.dp),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (preparingInstall) "インストールの準備をしています…" else "ダウンロードしています…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "このままアプリを閉じずにお待ちください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UpdateResultDialog(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    UpdateDialog(onDismissRequest = onDismiss, modifier = modifier) {
        UpdateIconBadge(
            icon = icon,
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.24f),
                    accentColor.copy(alpha = 0.10f)
                )
            ),
            contentColor = accentColor
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "OK", fontWeight = FontWeight.Bold)
        }
    }
}

/** 「現在 → 最新」のバージョン比較チップ。 */
@Composable
private fun VersionTransitionRow(
    currentVersion: String,
    latestVersion: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VersionChip(label = "現在", value = currentVersion, highlighted = false)
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        VersionChip(label = "最新", value = latestVersion, highlighted = true)
    }
}

@Composable
private fun VersionChip(
    label: String,
    value: String,
    highlighted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** リリースノート（簡易 Markdown）を表示するカード。 */
@Composable
private fun ReleaseNotesCard(body: String) {
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "リリースノート",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box {
                Column(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(scrollState)
                ) {
                    ReleaseNotesContent(body.ifBlank { "リリースノートはありません。" })
                }
                // 続きがあることが分かるように、下端をフェードさせる
                if (scrollState.canScrollForward) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, cardColor))
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotesContent(body: String) {
    val scheme = MaterialTheme.colorScheme
    val lines = remember(body) { body.lines() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { rawLine ->
            val indent = rawLine.takeWhile { it == ' ' }.length
            val line = rawLine.trim()
            when {
                line.isEmpty() -> Spacer(modifier = Modifier.height(2.dp))

                line.startsWith("#") -> {
                    Text(
                        text = cleanMarkdown(line.trimStart('#').trim()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = if (indent >= 2) 16.dp else 0.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .background(scheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = cleanMarkdown(line.drop(2).trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Text(
                        text = cleanMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** リリースノートの簡易 Markdown 記法を、プレーンなテキストに整える。 */
private fun cleanMarkdown(text: String): String =
    text.replace("**", "").replace("`", "")

internal suspend fun checkForUpdate(context: Context): UpdateFlowState {
    return try {
        val release = UpdateChecker.checkForUpdate(context)
        if (release != null) {
            UpdateFlowState.Available(release)
        } else {
            UpdateFlowState.UpToDate
        }
    } catch (e: Exception) {
        UpdateFlowState.Error(e.message ?: "アップデートの確認に失敗しました")
    }
}

internal suspend fun downloadAndInstall(
    context: Context,
    release: ReleaseInfo,
    onState: (UpdateFlowState) -> Unit
) {
    onState(UpdateFlowState.Downloading(0f))
    try {
        val apkFile = UpdateChecker.downloadApk(context, release.downloadUrl) { progress ->
            onState(UpdateFlowState.Downloading(progress))
        }
        onState(UpdateFlowState.Downloading(1f))
        UpdateChecker.installApk(context, apkFile)
        onState(UpdateFlowState.Hidden)
    } catch (e: Exception) {
        onState(UpdateFlowState.Error(e.message ?: "ダウンロードに失敗しました"))
    }
}
