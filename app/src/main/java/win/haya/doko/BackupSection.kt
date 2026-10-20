package win.haya.doko

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** バックアップ書き出し・読み込みの進行状態。 */
internal sealed interface BackupUiState {
    data object Hidden : BackupUiState
    data class Working(val message: String, val progress: Float?) : BackupUiState
    data class ExportDone(
        val fileName: String,
        val manifest: BackupManifest,
        /** 共有用コピーの URI。作成できなかった場合は null。 */
        val shareUri: Uri?
    ) : BackupUiState

    data class ConfirmImport(val source: Uri, val manifest: BackupManifest) : BackupUiState
    data class ImportDone(val result: BackupImportResult) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

/**
 * 設定画面の「データの引き継ぎ」セクション。
 *
 * 写真・スタンプラリー・設定を 1 つの ZIP ファイルへ書き出し／読み込みする。
 * サーバーは使わず、保存先は端末の保存先選択（SAF）で指定する。
 */
@Composable
internal fun BackupSection(
    currentSettings: AppSettings,
    onSettingsImported: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf<BackupUiState>(BackupUiState.Hidden) }
    var dataVersion by remember { mutableIntStateOf(0) }
    // 保存ダイアログに渡したファイル名を保持し、共有コピーにも同じ名前を使う
    var pendingExportFileName by remember { mutableStateOf("") }

    val photoRepository = remember { PhotoRepository(context) }
    val stampRepository = remember { StampRepository(context) }

    // 件数の集計はディスクを読むため、UI スレッドではなく IO で行う
    val counts by produceState(initialValue = Triple(0, 0, 0), dataVersion) {
        value = withContext(Dispatchers.IO) {
            val shopNames = YamaokayaFinder.getShopNames()
            Triple(
                photoRepository.loadAllPhotos().size,
                stampRepository.getVisitedCount(shopNames),
                stampRepository.getTotalCheckIns(shopNames)
            )
        }
    }
    val (photoCount, visitedShopCount, totalCheckInCount) = counts

    // 進行中の処理（キャンセル用）
    var runningJob by remember { mutableStateOf<Job?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            runningJob = coroutineScope.launch {
                val fileName = pendingExportFileName.ifEmpty { BackupManager.suggestedFileName() }
                state = BackupUiState.Working("バックアップを書き出しています…", 0f)
                try {
                    val manifest = BackupManager.export(context, uri, currentSettings) { done, total ->
                        val progress = if (total > 0) done.toFloat() / total else null
                        state = BackupUiState.Working("バックアップを書き出しています…", progress)
                    }
                    // 共有用コピーは失敗しても書き出し自体は成功として扱う
                    val shareUri = runCatching {
                        BackupManager.prepareShareCopy(context, uri, fileName)
                    }.getOrNull()
                    state = BackupUiState.ExportDone(fileName, manifest, shareUri)
                    dataVersion++
                } catch (e: CancellationException) {
                    state = BackupUiState.Hidden
                } catch (e: Exception) {
                    state = BackupUiState.Error(e.message ?: "バックアップの書き出しに失敗しました")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runningJob = coroutineScope.launch {
                state = BackupUiState.Working("バックアップの内容を確認しています…", null)
                try {
                    state = BackupUiState.ConfirmImport(uri, BackupManager.readManifest(context, uri))
                } catch (e: CancellationException) {
                    state = BackupUiState.Hidden
                } catch (e: Exception) {
                    state = BackupUiState.Error(e.message ?: "バックアップファイルを読み込めませんでした")
                }
            }
        }
    }

    val startImport: (Uri, BackupImportMode) -> Unit = { uri, mode ->
        runningJob = coroutineScope.launch {
            state = BackupUiState.Working("バックアップを読み込んでいます…", 0f)
            try {
                val result = BackupManager.import(context, uri, mode) { done, total ->
                    val progress = if (total > 0) done.toFloat() / total else null
                    state = BackupUiState.Working("バックアップを読み込んでいます…", progress)
                }
                result.importedSettings?.let(onSettingsImported)
                state = BackupUiState.ImportDone(result)
                dataVersion++
            } catch (e: CancellationException) {
                state = BackupUiState.Hidden
            } catch (e: Exception) {
                state = BackupUiState.Error(e.message ?: "バックアップの読み込みに失敗しました")
            }
        }
    }

    SettingsGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "現在のデータ",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "写真 ${photoCount} 枚 / スタンプ ${visitedShopCount} 店舗・合計 ${totalCheckInCount} 回",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "写真・スタンプラリー・設定を 1 つのファイルに書き出せます。" +
                    "書き出したファイルは共有機能や SD カード、クラウドで別の端末へ移せます（サーバーは使いません）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPillButton(
                    text = "書き出す",
                    onClick = {
                        pendingExportFileName = BackupManager.suggestedFileName()
                        exportLauncher.launch(pendingExportFileName)
                    },
                    icon = Icons.Outlined.FileUpload,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                GlassPillButton(
                    text = "読み込む",
                    onClick = {
                        importLauncher.launch(
                            arrayOf(BACKUP_MIME_TYPE, "application/octet-stream")
                        )
                    },
                    icon = Icons.Outlined.FileDownload,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    when (val current = state) {
        is BackupUiState.Working -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(current.message) },
                text = {
                    if (current.progress != null) {
                        Column {
                            LinearProgressIndicator(
                                progress = { current.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${(current.progress * 100).toInt()}%")
                        }
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            runningJob?.cancel()
                            runningJob = null
                            state = BackupUiState.Hidden
                        }
                    ) {
                        Text("キャンセル")
                    }
                }
            )
        }

        is BackupUiState.ExportDone -> {
            AlertDialog(
                onDismissRequest = { state = BackupUiState.Hidden },
                title = { Text("バックアップを書き出しました") },
                text = {
                    Column {
                        Text(
                            text = current.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "写真 ${current.manifest.photoCount} 枚 / " +
                                "スタンプ ${current.manifest.stampShopCount} 店舗・" +
                                "合計 ${current.manifest.stampTotalCount} 回 / 設定",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (current.shareUri != null) {
                                "「共有」で他の端末へ送れます。"
                            } else {
                                "このファイルを SD カードやクラウド、メッセージアプリなどで別の端末へ移せます。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    val shareUri = current.shareUri
                    if (shareUri != null) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = BACKUP_MIME_TYPE
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val launched = runCatching {
                                    context.startActivity(
                                        Intent.createChooser(intent, "バックアップを共有")
                                    )
                                }.isSuccess
                                if (!launched) {
                                    Toast.makeText(
                                        context,
                                        "共有できるアプリが見つかりませんでした",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                state = BackupUiState.Hidden
                            }
                        ) {
                            Text("共有")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { state = BackupUiState.Hidden }) {
                        Text("閉じる")
                    }
                }
            )
        }

        is BackupUiState.ConfirmImport -> {
            AlertDialog(
                onDismissRequest = { state = BackupUiState.Hidden },
                title = { Text("バックアップを読み込みますか？") },
                text = {
                    Column {
                        Text(
                            text = "作成日時: ${BackupManager.formatExportedAt(current.manifest)}" +
                                if (current.manifest.appVersion.isNotEmpty()) {
                                    "（${current.manifest.appVersion}）"
                                } else {
                                    ""
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "写真 ${current.manifest.photoCount} 枚 / " +
                                "スタンプ ${current.manifest.stampShopCount} 店舗・" +
                                "合計 ${current.manifest.stampTotalCount} 回 / 設定",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "追加: 今のデータを残して取り込みます" +
                                "（スタンプは回数の多い方、写真は重複を除いて追加。設定は今のまま）",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "置き換え: 今の写真とスタンプを削除して、" +
                                "バックアップの内容にします（設定も切り替わります）",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { startImport(current.source, BackupImportMode.MERGE) }) {
                        Text("追加で読み込む")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = { startImport(current.source, BackupImportMode.REPLACE) }
                        ) {
                            Text(
                                text = "置き換え",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(onClick = { state = BackupUiState.Hidden }) {
                            Text("キャンセル")
                        }
                    }
                }
            )
        }

        is BackupUiState.ImportDone -> {
            val result = current.result
            AlertDialog(
                onDismissRequest = { state = BackupUiState.Hidden },
                title = {
                    Text(
                        if (result.mode == BackupImportMode.REPLACE) {
                            "読み込みが完了しました"
                        } else {
                            "追加が完了しました"
                        }
                    )
                },
                text = {
                    Column {
                        Text("写真: ${result.photosAdded} 枚を追加", style = MaterialTheme.typography.bodyMedium)
                        if (result.photosSkipped > 0) {
                            Text(
                                text = "（${result.photosSkipped} 枚は同じ写真のためスキップ）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "スタンプ: ${result.stampShopsImported} 店舗の記録を取り込み",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (result.importedSettings != null) {
                                "設定: 適用しました"
                            } else {
                                "設定: 変更していません"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { state = BackupUiState.Hidden }) {
                        Text("OK")
                    }
                }
            )
        }

        is BackupUiState.Error -> {
            AlertDialog(
                onDismissRequest = { state = BackupUiState.Hidden },
                title = { Text("エラー") },
                text = { Text(current.message) },
                confirmButton = {
                    TextButton(onClick = { state = BackupUiState.Hidden }) {
                        Text("OK")
                    }
                }
            )
        }

        BackupUiState.Hidden -> { }
    }
}
