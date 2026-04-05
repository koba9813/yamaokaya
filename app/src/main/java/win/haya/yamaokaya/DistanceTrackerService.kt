package win.haya.yamaokaya

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class DistanceTrackerService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasForegroundLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            DISTANCE_TRACKER_NOTIFICATION_ID,
            buildTrackerNotification("位置情報を取得中...")
        )
        startLocationTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { callback ->
            fusedClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        if (locationCallback != null) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10_000L // 10秒ごと
        )
            .setMinUpdateIntervalMillis(5_000L) // 最短5秒
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val nearest = YamaokayaFinder.findNearest(
                    Coordinates(location.latitude, location.longitude)
                )
                NotificationManagerCompat.from(this@DistanceTrackerService).notify(
                    DISTANCE_TRACKER_NOTIFICATION_ID,
                    buildTrackerNotification(formatDistanceText(nearest))
                )
            }
        }

        locationCallback = callback
        fusedClient.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun formatDistanceText(nearest: ShopInfo?): String {
        return if (nearest == null) {
            "No shop found"
        } else if (nearest.distanceMeters < 1000f) {
            "${"%.0f".format(nearest.distanceMeters)} m"
        } else {
            "${"%.2f".format(nearest.distanceMeters / 1000f)} km"
        }
    }

    private fun buildTrackerNotification(contentText: String) =
        NotificationCompat.Builder(this, DISTANCE_TRACKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Yamaokaya is...")
            .setContentText(contentText)
            .setContentIntent(buildLaunchPendingIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun buildLaunchPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            DISTANCE_TRACKER_CHANNEL_ID,
            "山岡家距離トラッカー",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "最寄り山岡家までの距離をリアルタイム表示します"
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val DISTANCE_TRACKER_CHANNEL_ID = "distance_tracker_channel"
        private const val DISTANCE_TRACKER_NOTIFICATION_ID = 1002
    }
}
