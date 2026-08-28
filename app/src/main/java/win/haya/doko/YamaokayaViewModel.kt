package win.haya.doko

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class YamaokayaViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val notificationPrefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NOTIFICATION, Context.MODE_PRIVATE)

    private val _appSettings = mutableStateOf(settingsRepository.getSettings())
    val appSettings: State<AppSettings> = _appSettings

    private val _nearestShop = mutableStateOf<ShopInfo?>(null)
    val nearestShop: State<ShopInfo?> = _nearestShop

    private val _headingDegrees = mutableFloatStateOf(0f)
    val headingDegrees: State<Float> = _headingDegrees

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private var stopLocation: (() -> Unit)? = null
    private var stopHeading: (() -> Unit)? = null

    init {
        if (hasLocationPermission(application)) {
            _isLoading.value = true
            subscribeLocationAndHeading()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        val oldInterval = _appSettings.value.locationUpdateIntervalSeconds
        val oldTracker = _appSettings.value.trackerNotificationEnabled

        _appSettings.value = newSettings
        settingsRepository.save(newSettings)

        if (oldInterval != newSettings.locationUpdateIntervalSeconds) {
            restartLocationUpdates()
        }

        if (oldTracker != newSettings.trackerNotificationEnabled) {
            updateTrackerServiceState()
        }
    }

    fun onPermissionGranted() {
        _errorMessage.value = null
        restartLocationUpdates()
    }

    fun onPermissionDenied() {
        _errorMessage.value = "位置情報の許可が必要です。"
        _nearestShop.value = null
        _isLoading.value = false
    }

    fun retryLocation() {
        _errorMessage.value = null
        restartLocationUpdates()
    }

    private fun restartLocationUpdates() {
        stopLocationUpdates()
        if (!hasLocationPermission(getApplication())) return
        _isLoading.value = true
        subscribeLocationAndHeading()
    }

    private fun stopLocationUpdates() {
        stopLocation?.invoke()
        stopHeading?.invoke()
        stopLocation = null
        stopHeading = null
    }

    private fun subscribeLocationAndHeading() {
        val context = getApplication<Application>()
        val intervalMs = _appSettings.value.locationUpdateIntervalSeconds * 1000L

        stopLocation = startLocationUpdates(
            context = context,
            minIntervalMs = intervalMs,
            onLocation = { location -> handleLocationUpdate(location) },
            onFailure = { msg ->
                _errorMessage.value = msg
                _isLoading.value = false
            }
        )

        stopHeading = startHeadingUpdates(context) { heading ->
            _headingDegrees.value = heading
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val current = Coordinates(location.latitude, location.longitude)
        val nearest = YamaokayaFinder.findNearest(current)
        _nearestShop.value = nearest
        _isLoading.value = false

        if (nearest != null && nearest.distanceMeters <= KOKO_RADIUS_METERS) {
            maybeSendKokoNotification(nearest.name)
        }

        if (_appSettings.value.trackerNotificationEnabled) {
            ensureDistanceTrackerRunning(getApplication())
        }

        YamaokayaWidgetProvider.updateAllWidgets(getApplication())
    }

    private fun maybeSendKokoNotification(shopName: String) {
        val now = System.currentTimeMillis()
        val lastShop = notificationPrefs.getString(KEY_LAST_KOKO_SHOP, null)
        val lastTime = notificationPrefs.getLong(KEY_LAST_KOKO_TIME, 0L)

        if (lastShop == shopName && now - lastTime < KOKO_NOTIFICATION_COOLDOWN_MS) {
            return
        }

        sendKokoNotification(getApplication())

        notificationPrefs.edit()
            .putString(KEY_LAST_KOKO_SHOP, shopName)
            .putLong(KEY_LAST_KOKO_TIME, now)
            .apply()
    }

    private fun updateTrackerServiceState() {
        val context = getApplication<Application>()
        if (_appSettings.value.trackerNotificationEnabled) {
            ensureDistanceTrackerRunning(context)
        } else {
            context.stopService(Intent(context, DistanceTrackerService::class.java))
            setTrackerRunningFlag(context, false)
        }
    }

    private fun ensureDistanceTrackerRunning(context: Context) {
        if (!isDistanceTrackerRunning(context)) {
            startDistanceTrackerService(context)
            setTrackerRunningFlag(context, true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    companion object {
        private const val PREFS_NOTIFICATION = "notification_state"
        private const val KEY_LAST_KOKO_SHOP = "last_koko_shop"
        private const val KEY_LAST_KOKO_TIME = "last_koko_time"
        private const val KOKO_NOTIFICATION_COOLDOWN_MS = 5 * 60 * 1000L
    }
}
