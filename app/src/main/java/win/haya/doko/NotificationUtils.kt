package win.haya.doko

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal const val NOTIFICATION_CHANNEL_ID = "koko_channel"
private const val NOTIFICATION_ID = 1001

internal const val UPDATE_NOTIFICATION_CHANNEL_ID = "update_channel"
private const val UPDATE_NOTIFICATION_ID = 1003

private const val PREFS_TRACKER = "tracker_state"
private const val KEY_TRACKER_RUNNING = "tracker_running"

internal fun createKokoNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "山岡家接近通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "山岡家の50m以内に入ったときに通知します"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}

internal fun createUpdateNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            UPDATE_NOTIFICATION_CHANNEL_ID,
            "アップデート",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "新しいバージョンがリリースされたときに通知します"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}

/** 新しいバージョンが公開されたことを知らせる通知を出す。 */
@SuppressLint("MissingPermission")
internal fun sendUpdateNotification(context: Context, versionName: String) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: return
    launchIntent.putExtra(UpdateCheckWorker.EXTRA_CHECK_UPDATE, true)
    val pendingIntent = PendingIntent.getActivity(
        context, 1, launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.icon)
        .setContentTitle("Yamaokaya is Doko?")
        .setContentText("新しいバージョン $versionName が利用できます")
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "新しいバージョン $versionName がリリースされました。タップしてアップデートしてください。"
            )
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
}

@SuppressLint("MissingPermission")
internal fun sendKokoNotification(context: Context) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: return
    val pendingIntent = PendingIntent.getActivity(
        context, 0, launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.icon)
        .setContentTitle("Yamaokaya is Koko!!!")
        .setContentText("山岡家の50m以内に入りました！")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
}

@SuppressLint("MissingPermission")
internal fun startDistanceTrackerService(context: Context) {
    val serviceIntent = Intent(context, DistanceTrackerService::class.java)
    ContextCompat.startForegroundService(context, serviceIntent)
}

internal fun isDistanceTrackerRunning(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        .getBoolean(KEY_TRACKER_RUNNING, false)
}

internal fun setTrackerRunningFlag(context: Context, running: Boolean) {
    context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_TRACKER_RUNNING, running)
        .apply()
}
