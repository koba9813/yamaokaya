package win.haya.yamaokaya

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class FriendEntry(
    val userId: String,
    val name: String,
    val icon: String,
    val bio: String,
    val distanceMeters: Float?,
    val updatedAtMillis: Long
)

enum class FriendUpsertResult {
    Added,
    Updated,
    IgnoredSelf
}

class FriendRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("friends", Context.MODE_PRIVATE)

    fun getFriends(): List<FriendEntry> {
        val raw = prefs.getString(KEY_ITEMS_JSON, "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<FriendEntry>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val distanceAny = item.opt("distanceMeters")
            val distance = when (distanceAny) {
                is Number -> distanceAny.toFloat()
                else -> null
            }

            result += FriendEntry(
                userId = item.optString("userId", ""),
                name = item.optString("name", "名無し"),
                icon = item.optString("icon", "🍜"),
                bio = item.optString("bio", ""),
                distanceMeters = distance,
                updatedAtMillis = item.optLong("updatedAtMillis", 0L)
            )
        }

        return result
            .filter { it.userId.isNotBlank() }
            .sortedByDescending { it.updatedAtMillis }
    }

    fun upsertFromInvite(selfUserId: String, invite: FriendInvitePayload): FriendUpsertResult {
        if (invite.userId == selfUserId) return FriendUpsertResult.IgnoredSelf

        val current = getFriends().toMutableList()
        val idx = current.indexOfFirst { it.userId == invite.userId }

        val updated = FriendEntry(
            userId = invite.userId,
            name = invite.name,
            icon = invite.icon,
            bio = invite.bio,
            distanceMeters = invite.distanceMeters,
            updatedAtMillis = invite.updatedAtMillis
        )

        val result = if (idx >= 0) {
            current[idx] = updated
            FriendUpsertResult.Updated
        } else {
            current += updated
            FriendUpsertResult.Added
        }

        saveFriends(current)
        return result
    }

    fun replaceAll(friends: List<FriendEntry>) {
        saveFriends(friends)
    }

    private fun saveFriends(friends: List<FriendEntry>) {
        val array = JSONArray()
        friends.forEach { friend ->
            val obj = JSONObject()
                .put("userId", friend.userId)
                .put("name", friend.name)
                .put("icon", friend.icon)
                .put("bio", friend.bio)
                .put("updatedAtMillis", friend.updatedAtMillis)

            if (friend.distanceMeters != null) {
                obj.put("distanceMeters", friend.distanceMeters.toDouble())
            } else {
                obj.put("distanceMeters", JSONObject.NULL)
            }

            array.put(obj)
        }

        prefs.edit().putString(KEY_ITEMS_JSON, array.toString()).apply()
    }

    companion object {
        private const val KEY_ITEMS_JSON = "items_json"
    }
}
