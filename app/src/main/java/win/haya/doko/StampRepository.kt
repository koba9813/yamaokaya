package win.haya.doko

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

    /** バックアップ書き出し用に、チェックイン記録のある店舗をすべて返す。 */
    fun getAllRecords(): List<StampRecord> =
        prefs.all.entries
            .mapNotNull { (key, value) ->
                if (!key.startsWith(KEY_PREFIX_CHECK_IN)) return@mapNotNull null

                val shopName = key.removePrefix(KEY_PREFIX_CHECK_IN)
                val count = (value as? Int) ?: return@mapNotNull null
                if (shopName.isEmpty() || count <= 0) return@mapNotNull null

                StampRecord(
                    shopName = shopName,
                    count = count,
                    lastCheckInMillis = prefs.getLong(keyForLastCheckIn(shopName), 0L)
                )
            }
            .sortedBy { it.shopName }

    /**
     * バックアップからチェックイン記録を取り込む。
     * [replace] が false のときは、回数が多い方・最終チェックインが新しい方を採用してマージする。
     *
     * @return 取り込んだ店舗数
     */
    fun restoreFromBackup(records: List<StampRecord>, replace: Boolean): Int {
        if (records.isEmpty() && !replace) return 0

        val editor = prefs.edit()
        if (replace) {
            editor.clear()
        }

        val now = System.currentTimeMillis()
        records.forEach { record ->
            // 外部ファイル（バックアップ）由来の値をそのまま保存しない
            val imported = StampState(
                count = record.count.coerceIn(0, MAX_CHECK_IN_COUNT),
                lastCheckInMillis = record.lastCheckInMillis.coerceIn(0L, now)
            )
            val merged = if (replace) {
                imported
            } else {
                mergeStampStates(
                    local = StampState(
                        count = getCount(record.shopName),
                        lastCheckInMillis = prefs.getLong(keyForLastCheckIn(record.shopName), 0L)
                    ),
                    imported = imported
                )
            }

            editor.putInt(keyFor(record.shopName), merged.count)
            if (merged.lastCheckInMillis > 0L) {
                editor.putLong(keyForLastCheckIn(record.shopName), merged.lastCheckInMillis)
            }
        }
        editor.apply()
        return records.size
    }

    private fun keyFor(shopName: String) = "checkin_$shopName"

    private fun keyForLastCheckIn(shopName: String) = "last_checkin_$shopName"

    companion object {
        private const val KEY_PREFIX_CHECK_IN = "checkin_"
        private const val CHECK_IN_COOLDOWN_MILLIS = 3 * 60 * 60 * 1000L
    }
}

/** 1 店舗あたりのチェックイン回数の上限（外部ファイル由来の値をそのまま取り込まないための検証用）。 */
internal const val MAX_CHECK_IN_COUNT = 100_000

/** バックアップに含めるチェックイン記録。 */
data class StampRecord(
    val shopName: String,
    val count: Int,
    val lastCheckInMillis: Long
)

/** マージ計算に使うスタンプの状態。 */
data class StampState(
    val count: Int,
    val lastCheckInMillis: Long
)

/** 既存の記録とバックアップの記録をマージする（回数は多い方、最終チェックインは新しい方）。 */
internal fun mergeStampStates(local: StampState, imported: StampState): StampState =
    StampState(
        count = maxOf(local.count, imported.count),
        lastCheckInMillis = maxOf(local.lastCheckInMillis, imported.lastCheckInMillis)
    )
