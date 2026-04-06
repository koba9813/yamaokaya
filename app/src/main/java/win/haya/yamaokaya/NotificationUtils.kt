package win.haya.yamaokaya

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

@SuppressLint("MissingPermission")
internal fun sendKokoNotification(context: Context) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
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
