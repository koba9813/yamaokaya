package win.haya.yamaokaya

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

data class AccountProfile(
    val userId: String,
    val name: String,
    val icon: String,
    val bio: String,
    val userSecret: String?,
    val inviteCode: String?,
    val syncIntervalSeconds: Int,
    val apiKey: String
)

class AccountRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("account_profile", Context.MODE_PRIVATE)

    fun getProfile(): AccountProfile {
        val currentId = prefs.getString(KEY_USER_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_USER_ID, it).apply()
        }

        return AccountProfile(
            userId = currentId,
            name = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME,
            icon = prefs.getString(KEY_ICON, DEFAULT_ICON) ?: DEFAULT_ICON,
            bio = prefs.getString(KEY_BIO, "") ?: "",
            userSecret = prefs.getString(KEY_USER_SECRET, null),
            inviteCode = prefs.getString(KEY_INVITE_CODE, null),
            syncIntervalSeconds = prefs.getInt(KEY_SYNC_INTERVAL_SECONDS, DEFAULT_SYNC_INTERVAL_SECONDS)
                .coerceIn(MIN_SYNC_INTERVAL_SECONDS, MAX_SYNC_INTERVAL_SECONDS),
            apiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        )
    }

    fun saveProfile(
        name: String,
        icon: String,
        bio: String,
        syncIntervalSeconds: Int,
        apiKey: String
    ): AccountProfile {
        val normalizedName = name.trim().ifEmpty { DEFAULT_NAME }
        val normalizedIcon = icon.trim().ifEmpty { DEFAULT_ICON }.take(2)
        val normalizedBio = bio.trim().take(40)
        val normalizedInterval = syncIntervalSeconds.coerceIn(
            MIN_SYNC_INTERVAL_SECONDS,
            MAX_SYNC_INTERVAL_SECONDS
        )
        val normalizedApiKey = apiKey.trim().ifEmpty { DEFAULT_API_KEY }

        val profile = AccountProfile(
            userId = prefs.getString(KEY_USER_ID, null) ?: UUID.randomUUID().toString(),
            name = normalizedName,
            icon = normalizedIcon,
            bio = normalizedBio,
            userSecret = prefs.getString(KEY_USER_SECRET, null),
            inviteCode = prefs.getString(KEY_INVITE_CODE, null),
            syncIntervalSeconds = normalizedInterval,
            apiKey = normalizedApiKey
        )

        prefs.edit()
            .putString(KEY_USER_ID, profile.userId)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_ICON, profile.icon)
            .putString(KEY_BIO, profile.bio)
            .putInt(KEY_SYNC_INTERVAL_SECONDS, profile.syncIntervalSeconds)
            .putString(KEY_API_KEY, profile.apiKey)
            .apply()

        return profile
    }

    fun saveServerAuth(userSecret: String, inviteCode: String): AccountProfile {
        prefs.edit()
            .putString(KEY_USER_SECRET, userSecret)
            .putString(KEY_INVITE_CODE, inviteCode)
            .apply()
        return getProfile()
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_ICON = "icon"
        private const val KEY_BIO = "bio"
        private const val KEY_USER_SECRET = "user_secret"
        private const val KEY_INVITE_CODE = "invite_code"
        private const val KEY_SYNC_INTERVAL_SECONDS = "sync_interval_seconds"
        private const val KEY_API_KEY = "api_key"
        private const val DEFAULT_NAME = "名無しラーメン部員"
        private const val DEFAULT_ICON = "🍜"
        private const val DEFAULT_SYNC_INTERVAL_SECONDS = 15
        private const val MIN_SYNC_INTERVAL_SECONDS = 5
        private const val MAX_SYNC_INTERVAL_SECONDS = 120
        private const val DEFAULT_API_KEY = "yamaokaya_shared_key_change_me"
    }
}
