package win.haya.yamaokaya

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

data class FriendInvitePayload(
    val userId: String,
    val name: String,
    val icon: String,
    val bio: String,
    val distanceMeters: Float?,
    val updatedAtMillis: Long
)

object FriendInviteCodec {
    private const val PARAM_DATA = "d"

    fun buildInviteUri(payload: FriendInvitePayload): Uri {
        val json = JSONObject()
            .put("userId", payload.userId)
            .put("name", payload.name)
            .put("icon", payload.icon)
            .put("bio", payload.bio)
            .put("updatedAtMillis", payload.updatedAtMillis)

        if (payload.distanceMeters != null) {
            json.put("distanceMeters", payload.distanceMeters.toDouble())
        } else {
            json.put("distanceMeters", JSONObject.NULL)
        }

        val encoded = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )

        return Uri.Builder()
            .scheme("https")
            .authority("yamaokaya.win")
            .path("friend")
            .appendQueryParameter(PARAM_DATA, encoded)
            .build()
    }

    fun parseFromUri(uri: Uri?): FriendInvitePayload? {
        if (uri == null) return null

        val encoded = uri.getQueryParameter(PARAM_DATA) ?: return null
        return try {
            val decodedBytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
            val distanceAny = json.opt("distanceMeters")
            val distanceMeters = when (distanceAny) {
                is Number -> distanceAny.toFloat()
                else -> null
            }

            val userId = json.optString("userId", "")
            if (userId.isBlank()) return null

            FriendInvitePayload(
                userId = userId,
                name = json.optString("name", "名無しラーメン部員"),
                icon = json.optString("icon", "🍜"),
                bio = json.optString("bio", ""),
                distanceMeters = distanceMeters,
                updatedAtMillis = json.optLong("updatedAtMillis", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }
}
