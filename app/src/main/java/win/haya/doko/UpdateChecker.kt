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
        val outputFile = File(context.cacheDir, "update.apk")
        outputFile.delete()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        try {
            val totalBytes = connection.contentLength.takeIf { it > 0 } ?: -1
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress(downloaded.toFloat() / totalBytes)
                        }
                    }
                }
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
        return current.trim() != latest.trim()
    }
}
