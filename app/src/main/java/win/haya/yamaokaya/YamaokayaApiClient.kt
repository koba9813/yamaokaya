package win.haya.yamaokaya

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ServerAuth(
    val userSecret: String,
    val inviteCode: String
)

class YamaokayaApiClient(
    private val baseUrl: String = "https://blackbird.weblike.jp/yamaokaya/php"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun registerOrFetch(profile: AccountProfile): Result<ServerAuth> {
        return runCatching {
            val body = JSONObject()
                .put("user_id", profile.userId)
                .put("name", profile.name)
                .put("icon", profile.icon)
                .put("bio", profile.bio)

            val response = postJson("/auth/register", body, profile.apiKey)
            val secret = response.optString("user_secret", "")
            val inviteCode = response.optString("invite_code", "")
            if (secret.isBlank() || inviteCode.isBlank()) {
                throw IOException("Missing auth fields")
            }
            ServerAuth(secret, inviteCode)
        }
    }

    fun updateProfile(profile: AccountProfile): Result<Unit> {
        val secret = profile.userSecret ?: return Result.failure(IllegalStateException("userSecret missing"))
        return runCatching {
            val body = JSONObject()
                .put("user_id", profile.userId)
                .put("user_secret", secret)
                .put("name", profile.name)
                .put("icon", profile.icon)
                .put("bio", profile.bio)
            postJson("/user/profile/update", body, profile.apiKey)
            Unit
        }
    }

    fun updatePresence(profile: AccountProfile, distanceMeters: Float?, shopName: String?): Result<Unit> {
        val secret = profile.userSecret ?: return Result.failure(IllegalStateException("userSecret missing"))
        return runCatching {
            val body = JSONObject()
                .put("user_id", profile.userId)
                .put("user_secret", secret)
                .put("distance_meters", distanceMeters?.toDouble())
                .put("shop_name", shopName)
            postJson("/presence/update", body, profile.apiKey)
            Unit
        }
    }

    private fun postJson(path: String, body: JSONObject, apiKey: String): JSONObject {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .headers(buildHeaders(apiKey))
            .post(body.toString().toRequestBody(mediaType))
            .build()

        client.newCall(req).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $raw")
            }
            val json = JSONObject(raw)
            if (!json.optBoolean("ok", false)) {
                throw IOException(json.optString("error", "API error"))
            }
            return json
        }
    }

    private fun getJson(path: String, apiKey: String): JSONObject {
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .headers(buildHeaders(apiKey))
            .get()
            .build()

        client.newCall(req).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $raw")
            }
            val json = JSONObject(raw)
            if (!json.optBoolean("ok", false)) {
                throw IOException(json.optString("error", "API error"))
            }
            return json
        }
    }

    private fun buildHeaders(apiKey: String): okhttp3.Headers {
        val builder = okhttp3.Headers.Builder()
        if (apiKey.isNotBlank()) {
            builder.add("X-API-KEY", apiKey)
        }
        return builder.build()
    }

    private fun String.urlEncode(): String {
        return java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
    }

    private fun parseIsoTimeMillis(iso: String): Long {
        if (iso.isBlank()) return 0L
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            try {
                val dt = java.time.LocalDateTime.parse(
                    iso,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
                dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                0L
            }
        }
    }
}