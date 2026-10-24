package win.haya.doko

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases を定期的に確認し、新しいバージョンが公開されたら通知するワーカー。
 *
 * WorkManager の定期実行（12時間ごと）で動くため、Google Play Services は使わない。
 * 同じバージョンで何度も通知しないよう、最後に通知したバージョンを SharedPreferences に記録する。
 */
internal class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val release = UpdateChecker.checkForUpdate(applicationContext)
            if (release != null) {
                maybeNotifyUpdate(applicationContext, release.versionName)
            }
            Result.success()
        } catch (_: Exception) {
            // 通信できないときなどは後でリトライする
            Result.retry()
        }
    }

    companion object {
        /** アップデート通知からアプリを起動したことを示す Intent の extra。 */
        const val EXTRA_CHECK_UPDATE = "extra_check_update"

        private const val WORK_NAME = "yamaokaya_update_check"
        private const val UPDATE_INTERVAL_HOURS = 12L

        private const val PREFS_UPDATE_NOTIFY = "update_notify"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private const val KEY_LAST_AUTO_CHECK = "last_auto_check"

        /** 起動時の自動チェックを行う最小間隔（GitHub API を叩きすぎないようにする）。 */
        private const val AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

        /** 起動時の自動チェックをしてよいか（前回から6時間以上経過しているか）。 */
        fun shouldRunAutoCheck(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_UPDATE_NOTIFY, Context.MODE_PRIVATE)
            val last = prefs.getLong(KEY_LAST_AUTO_CHECK, 0L)
            return System.currentTimeMillis() - last > AUTO_CHECK_INTERVAL_MS
        }

        /** 起動時の自動チェックを実行した時刻を記録する。 */
        fun markAutoCheckDone(context: Context) {
            context.getSharedPreferences(PREFS_UPDATE_NOTIFY, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_AUTO_CHECK, System.currentTimeMillis())
                .apply()
        }

        /** アップデートの定期確認を開始する（すでにスケジュール済みなら何もしない）。 */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                UPDATE_INTERVAL_HOURS, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * このバージョンを「ユーザーに案内済み」として記録する。
         * アプリ内のアップデートダイアログを表示したときにも呼ぶことで、
         * 通知とダイアログの重複を防ぐ。
         */
        fun markVersionNotified(context: Context, versionName: String) {
            context.getSharedPreferences(PREFS_UPDATE_NOTIFY, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_NOTIFIED_VERSION, versionName)
                .apply()
        }

        private fun maybeNotifyUpdate(context: Context, versionName: String) {
            val prefs = context.getSharedPreferences(PREFS_UPDATE_NOTIFY, Context.MODE_PRIVATE)
            if (prefs.getString(KEY_LAST_NOTIFIED_VERSION, null) == versionName) return
            sendUpdateNotification(context, versionName)
            prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, versionName).apply()
        }
    }
}
