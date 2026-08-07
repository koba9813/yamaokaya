package win.haya.yamaokaya

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

internal fun normalizeDegrees(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

internal fun calculateDistanceMeters(from: Coordinates, to: Coordinates): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.lat, from.lon, to.lat, to.lon, result)
    return result[0]
}

internal fun calculateBearing(from: Coordinates, to: Coordinates): Double {
    val lat1 = Math.toRadians(from.lat)
    val lat2 = Math.toRadians(to.lat)
    val deltaLon = Math.toRadians(to.lon - from.lon)

    val y = kotlin.math.sin(deltaLon) * kotlin.math.cos(lat2)
    val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
        kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(deltaLon)

    val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    return (bearing + 360.0) % 360.0
}

internal fun startHeadingUpdates(
    context: Context,
    onHeadingChanged: (Float) -> Unit
): () -> Unit {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: return { }
    val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: return { }

    val listener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientation = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            onHeadingChanged(normalizeDegrees(azimuthDeg))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
    return { sensorManager.unregisterListener(listener) }
}

@SuppressLint("MissingPermission")
internal fun startLocationUpdates(
    context: Context,
    minIntervalMs: Long = 5000L,
    onLocation: (Location) -> Unit,
    onFailure: (String) -> Unit
): () -> Unit {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onFailure("位置情報サービスが利用できません。")
        return {}
    }

    val hasProvider = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    ).any { locationManager.isProviderEnabled(it) }

    if (!hasProvider) {
        onFailure("位置情報が無効になっています。")
        return {}
    }

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocation(location)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    val minTime = minIntervalMs.coerceAtLeast(1000L)

    try {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (locationManager.allProviders.contains(provider)) {
                locationManager.requestLocationUpdates(
                    provider,
                    minTime,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        }
    } catch (_: SecurityException) {
        onFailure("位置情報の許可が必要です。")
        return {}
    } catch (_: Exception) {
        onFailure("現在地の更新を開始できませんでした。")
        return {}
    }

    return { locationManager.removeUpdates(listener) }
}
