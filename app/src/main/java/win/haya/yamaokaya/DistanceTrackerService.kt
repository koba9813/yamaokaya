package win.haya.yamaokaya

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class DistanceTrackerService : Service() {

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        setTrackerRunningFlag(this, true)
        startLocationTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.let { locationManager.removeUpdates(it) }
        locationListener = null
        setTrackerRunningFlag(this, false)
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
        if (locationListener != null) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val nearest = YamaokayaFinder.findNearest(
                    Coordinates(location.latitude, location.longitude)
                )
                NotificationManagerCompat.from(this@DistanceTrackerService).notify(
                    DISTANCE_TRACKER_NOTIFICATION_ID,
                    buildTrackerNotification(formatDistanceText(nearest))
                )
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationListener = listener

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (locationManager.allProviders.contains(provider)) {
                locationManager.requestLocationUpdates(
                    provider,
                    10_000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        }
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
