package win.haya.yamaokaya

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases API を使って最新バージョンを取得し、
 * 現在のアプリバージョンと比較するユーティリティ。
 */
object UpdateChecker {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/koba9813/yamaokaya/releases/latest"

    data class UpdateInfo(
        val latestVersion: String,
        val releaseUrl: String,
        val releaseNotes: String
    )

    /**
     * GitHub の最新リリースを取得し、現在のバージョンと異なれば [UpdateInfo] を返す。
     * アップデート不要またはエラー時は null。
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getAppVersionName(context)

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val htmlUrl = json.optString("html_url", "")
            val releaseNotes = json.optString("body", "")

            if (tagName.isBlank() || htmlUrl.isBlank()) return@withContext null

            // 現在のバージョンと違えばアップデートありとみなす
            if (tagName != currentVersion) {
                UpdateInfo(
                    latestVersion = tagName,
                    releaseUrl = htmlUrl,
                    releaseNotes = releaseNotes
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 最新リリース情報を常に返す（バージョン比較なし）。
     * エラー時は null。
     */
    suspend fun fetchLatestRelease(@Suppress("UNUSED_PARAMETER") context: Context): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                val releaseNotes = json.optString("body", "")

                if (tagName.isBlank()) return@withContext null

                UpdateInfo(
                    latestVersion = tagName,
                    releaseUrl = htmlUrl,
                    releaseNotes = releaseNotes
                )
            } catch (_: Exception) {
                null
            }
        }

    private fun getAppVersionName(context: Context): String {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
