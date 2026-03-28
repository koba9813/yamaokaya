package win.haya.yamaokaya

import android.content.Context
import android.content.SharedPreferences

class StampRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stamp_rally", Context.MODE_PRIVATE)

    fun checkIn(shopName: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!canCheckIn(shopName, nowMillis)) return false

        val current = getCount(shopName)
        prefs.edit()
            .putInt(keyFor(shopName), current + 1)
            .putLong(keyForLastCheckIn(shopName), nowMillis)
            .apply()
        return true
    }

    fun canCheckIn(shopName: String, nowMillis: Long = System.currentTimeMillis()): Boolean =
        getRemainingCooldownMillis(shopName, nowMillis) <= 0L

    fun getRemainingCooldownMillis(shopName: String, nowMillis: Long = System.currentTimeMillis()): Long {
        val last = prefs.getLong(keyForLastCheckIn(shopName), 0L)
        if (last <= 0L) return 0L

        val elapsed = nowMillis - last
        val remaining = CHECK_IN_COOLDOWN_MILLIS - elapsed
        return remaining.coerceAtLeast(0L)
    }

    fun getCount(shopName: String): Int =
        prefs.getInt(keyFor(shopName), 0)

    fun getRanking(shopNames: List<String>): List<Pair<String, Int>> =
        shopNames
            .map { it to getCount(it) }
            .sortedByDescending { it.second }

    fun getVisitedCount(shopNames: List<String>): Int =
        shopNames.count { getCount(it) > 0 }

    fun getTotalCheckIns(shopNames: List<String>): Int =
        shopNames.sumOf { getCount(it) }

    private fun keyFor(shopName: String) = "checkin_$shopName"

    private fun keyForLastCheckIn(shopName: String) = "last_checkin_$shopName"

    companion object {
        private const val CHECK_IN_COOLDOWN_MILLIS = 3 * 60 * 60 * 1000L
    }
}
