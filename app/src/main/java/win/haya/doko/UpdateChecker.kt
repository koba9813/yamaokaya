package win.haya.doko

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

internal data class ReleaseInfo(
    val versionName: String,
    val body: String,
    val downloadUrl: String
)

internal object UpdateChecker {

    private const val RELEASE_API_URL =
        "https://api.github.com/repos/koba9813/yamaokaya/releases/latest"

    /** ダウンロードする APK の上限サイズ（壊れた応答や過大なファイル対策）。 */
    private const val MAX_APK_BYTES = 200 * 1024 * 1024

    suspend fun checkForUpdate(context: Context): ReleaseInfo? = withContext(Dispatchers.IO) {
        val currentVersion = getAppVersionName(context)
        val connection = URL(RELEASE_API_URL).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw RuntimeException("GitHub API returned $responseCode")
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val tagName = root.getString("tag_name")
            val body = root.optString("body", "")
            val assets = root.getJSONArray("assets")

            val apkUrl = (0 until assets.length()).firstNotNullOfOrNull { i ->
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    asset.getString("browser_download_url")
                } else null
            } ?: throw RuntimeException("APK アセットが見つかりません")

            if (isNewerVersion(currentVersion, tagName)) {
                ReleaseInfo(versionName = tagName, body = body, downloadUrl = apkUrl)
            } else null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File(outputDir, "update.apk")
        outputFile.delete()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw RuntimeException("ダウンロードに失敗しました（HTTP $responseCode）")
            }

            // 途中で切れた APK をインストールしようとすると失敗するため、サイズを確認する
            val totalBytes = connection.contentLength
            if (totalBytes <= 0) {
                throw RuntimeException("ファイルサイズを確認できませんでした。通信環境を変えて、もう一度お試しください")
            }
            if (totalBytes > MAX_APK_BYTES) {
                throw RuntimeException("ファイルサイズが大きすぎます")
            }

            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded > MAX_APK_BYTES) {
                            throw RuntimeException("ファイルサイズが大きすぎます")
                        }
                        onProgress(downloaded.toFloat() / totalBytes)
                    }
                }
            }

            // 途中で切れた APK をインストールしようとすると失敗するため、サイズを検証する
            if (outputFile.length() != totalBytes.toLong()) {
                outputFile.delete()
                throw RuntimeException("ダウンロードが途中で終了しました。もう一度お試しください")
            }

            outputFile
        } finally {
            connection.disconnect()
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = parseVersion(current)
        val latestParts = parseVersion(latest)
        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        val normalized = version.trim()
            .removePrefix("V")
            .removePrefix("v")
            .removePrefix(".")
            .replace(Regex("[a-zA-Z]"), ".")
            .replace(Regex("""\.+"""), ".")
            .trim('.')
        if (normalized.isBlank()) return emptyList()
        return normalized.split(".").mapNotNull { it.toIntOrNull() }
    }
}
