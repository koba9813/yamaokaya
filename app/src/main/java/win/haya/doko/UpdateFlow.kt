package win.haya.doko

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        is UpdateFlowState.Available -> {
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
                },
                modifier = modifier
            )
        }
        is UpdateFlowState.Downloading -> {
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
        is UpdateFlowState.UpToDate -> {
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
        is UpdateFlowState.Error -> {
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
