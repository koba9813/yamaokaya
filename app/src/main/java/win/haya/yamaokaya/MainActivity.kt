package win.haya.yamaokaya

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.format.DateUtils
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.material3.Typography
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Coordinates(val lat: Double, val lon: Double)

data class ShopInfo(
    val name: String,
    val coordinates: Coordinates,
    val distanceMeters: Float,
    val bearingDegrees: Double
)

private const val IMAGE_TOP_OFFSET_DEGREES = 0f
private const val KOKO_RADIUS_METERS = 100f
private const val SPECIAL_EFFECT_RADIUS_METERS = 50f
private const val NOTIFICATION_RADIUS_METERS = 50f
private const val YAMAOKAYA_MENU_URL = "https://www.yamaokaya.com/menus/yamaokaya/regular/"

private val zenMaruGothicFont = FontFamily(
    Font(
        resId = R.font.zen_maru_gothic_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.zen_maru_gothic_bold,
        weight = FontWeight.Bold
    )
)

private val appTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = zenMaruGothicFont),
        displayMedium = base.displayMedium.copy(fontFamily = zenMaruGothicFont),
        displaySmall = base.displaySmall.copy(fontFamily = zenMaruGothicFont),
        headlineLarge = base.headlineLarge.copy(fontFamily = zenMaruGothicFont),
        headlineMedium = base.headlineMedium.copy(fontFamily = zenMaruGothicFont),
        headlineSmall = base.headlineSmall.copy(fontFamily = zenMaruGothicFont),
        titleLarge = base.titleLarge.copy(fontFamily = zenMaruGothicFont),
        titleMedium = base.titleMedium.copy(fontFamily = zenMaruGothicFont),
        titleSmall = base.titleSmall.copy(fontFamily = zenMaruGothicFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = zenMaruGothicFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = zenMaruGothicFont),
        bodySmall = base.bodySmall.copy(fontFamily = zenMaruGothicFont),
        labelLarge = base.labelLarge.copy(fontFamily = zenMaruGothicFont),
        labelMedium = base.labelMedium.copy(fontFamily = zenMaruGothicFont),
        labelSmall = base.labelSmall.copy(fontFamily = zenMaruGothicFont)
    )
}

private const val NOTIFICATION_CHANNEL_ID = "koko_channel"
private const val NOTIFICATION_ID = 1001

private data class ChaosRamenSprite(
    val imageIndex: Int,
    val phase: Float,
    val speedX: Float,
    val speedY: Float,
    val wobbleCycles: Float,
    val wobbleAmplitude: Float,
    val rotationOffset: Float,
    val scale: Float,
    val alpha: Float,
    val orbitRadius: Float,
    val orbitCycles: Float,
    val pulseDepth: Float,
    val jitterSeed: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "山岡家接近通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "山岡家の50m以内に入ったときに通知します"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        setContent {
            MaterialTheme(typography = appTypography) {
                YamaokayaScreen()
            }
        }
    }
}

@Composable
private fun YamaokayaScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var isLoading by remember { mutableStateOf(hasPermission) }
    var nearestShop by remember { mutableStateOf<ShopInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var headingDegrees by remember { mutableStateOf(0f) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var showInfoPage by remember { mutableStateOf(false) }
    var showStampRally by remember { mutableStateOf(false) }
    var showAccountSettings by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var previouslyInsideRadius by remember { mutableStateOf<Boolean?>(null) }
    val stampRepository = remember { StampRepository(context) }
    val accountRepository = remember { AccountRepository(context) }
    val apiClient = remember { YamaokayaApiClient() }
    var accountProfile by remember { mutableStateOf(accountRepository.getProfile()) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }
    var consecutiveSyncFailures by remember { mutableStateOf(0) }
    var lastSyncSuccessMillis by remember { mutableStateOf(0L) }
    var isSyncingNow by remember { mutableStateOf(false) }
    var checkInFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val imageDrawableIds = remember {
        listOf("yamaokaya", "gyoza", "miso", "shio", "tokusei_miso", "kara_miso")
            .map { resolveDrawableId(context, it) }
            .filter { it != 0 }
    }
    val appVersion = remember { getAppVersionName(context) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(true) }
    val latestShopState = rememberUpdatedState(nearestShop)
    val latestProfileState = rememberUpdatedState(accountProfile)

    suspend fun syncNow() {
        val profile = latestProfileState.value
        if (profile.userSecret.isNullOrBlank()) return
        if (isSyncingNow) return

        isSyncingNow = true
        val nearest = latestShopState.value
        withContext(Dispatchers.IO) {
            apiClient.updateProfile(profile)
            apiClient.updatePresence(profile, nearest?.distanceMeters, nearest?.name)
        }.onSuccess {
            syncErrorMessage = null
            consecutiveSyncFailures = 0
            lastSyncSuccessMillis = System.currentTimeMillis()
        }.onFailure {
            syncErrorMessage = it.message ?: "通信エラー"
            consecutiveSyncFailures += 1
        }
        isSyncingNow = false
    }

    LaunchedEffect(accountProfile.userId) {
        withContext(Dispatchers.IO) {
            apiClient.registerOrFetch(accountProfile)
        }.onSuccess { auth ->
            accountProfile = accountRepository.saveServerAuth(auth.userSecret, auth.inviteCode)
            syncErrorMessage = null
        }.onFailure {
            syncErrorMessage = it.message ?: "unknown"
        }
    }

    LaunchedEffect(accountProfile.userSecret, accountProfile.userId) {
        if (accountProfile.userSecret.isNullOrBlank()) return@LaunchedEffect

        while (true) {
            syncNow()
            val waitMs = latestProfileState.value.syncIntervalSeconds
                .coerceIn(5, 120)
                .times(1000L)
            delay(waitMs)
        }
    }

    // アップデートチェック
    LaunchedEffect(Unit) {
        val info = UpdateChecker.checkForUpdate(context)
        if (info != null) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    // アップデートダイアログ
    if (updateInfo != null && showUpdateDialog) {
        UpdateAvailableDialog(
            updateInfo = updateInfo!!,
            onUpdate = {
                uriHandler.openUri(updateInfo!!.releaseUrl)
                showUpdateDialog = false
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }

    if (showAccountSettings) {
        AccountSettingsPage(
            accountProfile = accountProfile,
            onSave = { name, icon, bio, syncIntervalSeconds, apiKey ->
                accountProfile = accountRepository.saveProfile(
                    name = name,
                    icon = icon,
                    bio = bio,
                    syncIntervalSeconds = syncIntervalSeconds,
                    apiKey = apiKey
                )
                showAccountSettings = false
            },
            onClose = { showAccountSettings = false }
        )
        return
    }

    if (showInfoPage) {
        InformationPage(
            onClose = { showInfoPage = false }
        )
        return
    }

    if (showStampRally) {
        StampRallyScreen(
            stampRepository = stampRepository,
            shopNames = YamaokayaFinder.getShopNames(),
            onClose = { showStampRally = false }
        )
        return
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        hasPermission = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasPermission) {
            isLoading = true
            errorMessage = null
        } else {
            errorMessage = "位置情報の許可が必要です。"
            nearestShop = null
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            context.stopService(Intent(context, DistanceTrackerService::class.java))
            onDispose { }
        } else {
            startDistanceTrackerService(context)
            val stopHeading = startHeadingUpdates(context) { heading ->
                headingDegrees = heading
            }

            val stopLocation = fusedClient.startRealtimeLocationUpdates(
                onLocation = { location ->
                    val current = Coordinates(location.latitude, location.longitude)
                    val nearest = YamaokayaFinder.findNearest(current)
                    nearestShop = nearest
                    isLoading = false
                    errorMessage = if (nearest == null) {
                        "山岡家が見つかりませんでした。"
                    } else {
                        null
                    }
                    val isInside = nearest != null && nearest.distanceMeters <= NOTIFICATION_RADIUS_METERS
                    if (isInside && previouslyInsideRadius == false) {
                        sendKokoNotification(context)
                    }
                    previouslyInsideRadius = isInside
                },
                onFailure = { message ->
                    isLoading = false
                    errorMessage = message
                }
            )

            onDispose {
                stopHeading()
                stopLocation()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        val titleText = if (nearestShop != null && nearestShop!!.distanceMeters <= KOKO_RADIUS_METERS) {
            "Yamaokaya is Koko!!!"
        } else {
            "Yamaokaya is Doko"
        }

        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            IconButton(
                onClick = { showStampRally = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "スタンプラリー",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (nearestShop != null) {
                IconButton(
                    onClick = { showShareDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "シェアする",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = { showInfoPage = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "インフォメーション",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (nearestShop?.distanceMeters?.let { it <= SPECIAL_EFFECT_RADIUS_METERS } == true) {
            ChaosRamenStorm(
                imageDrawableIds = imageDrawableIds,
                modifier = Modifier.fillMaxSize()
            )
        }

        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("位置情報を使って最寄り店舗を検索します。")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        permissionLauncher.launch(
                            buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                        )
                    }) {
                        Text("位置情報を許可して検索")
                    }
                }
            }

            isLoading && nearestShop == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("検索中...")
                }
            }

            nearestShop != null -> {
                val shop = nearestShop!!
                val isSpecialRange = shop.distanceMeters <= SPECIAL_EFFECT_RADIUS_METERS
                val arrowRotation = normalizeDegrees(
                    shop.bearingDegrees.toFloat() - headingDegrees + IMAGE_TOP_OFFSET_DEGREES
                )

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (shop.distanceMeters <= KOKO_RADIUS_METERS) {
                        if (isSpecialRange) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${shop.name} is Koko!!!",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "もう山岡家は目の前！さあ、何を食べる？",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Text(
                                text = "${"%.0f".format(shop.distanceMeters)} m",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val drawableIdM = if (imageDrawableIds.isNotEmpty()) {
                                imageDrawableIds[selectedImageIndex % imageDrawableIds.size]
                            } else {
                                0
                            }

                            if (drawableIdM != 0) {
                                Image(
                                    painter = painterResource(id = drawableIdM),
                                    contentDescription = "山岡家の方向",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(280.dp)
                                        .graphicsLayer { rotationZ = arrowRotation }
                                        .clickable {
                                            if (imageDrawableIds.size > 1) {
                                                selectedImageIndex =
                                                    (selectedImageIndex + 1) % imageDrawableIds.size
                                            }
                                        }
                                )
                            } else {
                                Text("画像が見つかりません (drawable/yamaokaya, gyoza, miso, shio, tokusei_miso, kara_miso)")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        val isCheckedIn = stampRepository.getRemainingCooldownMillis(shop.name) > 0L
                        if (isCheckedIn) {
                            Button(
                                onClick = {
                                    openMenuInAppBrowser(context)
                                },
                                modifier = if (isSpecialRange) {
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                } else {
                                    Modifier
                                }
                            ) {
                                Text("メニューを見る")
                            }
                        } else {
                            Button(onClick = {
                                val checkedIn = stampRepository.checkIn(shop.name)
                                if (checkedIn) {
                                    checkInFeedbackMessage = null
                                } else {
                                    val remainingMillis = stampRepository.getRemainingCooldownMillis(shop.name)
                                    val totalMinutes = (remainingMillis + 59_999L) / 60_000L
                                    val hours = totalMinutes / 60
                                    val minutes = totalMinutes % 60
                                    checkInFeedbackMessage = if (hours > 0) {
                                        "同じ店舗のチェックインは3時間に1回までです（あと${hours}時間${minutes}分）"
                                    } else {
                                        "同じ店舗のチェックインは3時間に1回までです（あと${minutes}分）"
                                    }
                                }
                            }, modifier = if (isSpecialRange) {
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            } else {
                                Modifier
                            }) {
                                Text("チェックイン")
                            }
                            if (checkInFeedbackMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = checkInFeedbackMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Text(
                        text = "${"%.2f".format(shop.distanceMeters / 1000f)} km",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val drawableId = if (imageDrawableIds.isNotEmpty()) {
                            imageDrawableIds[selectedImageIndex % imageDrawableIds.size]
                        } else {
                            0
                        }

                        if (drawableId != 0) {
                            Image(
                                painter = painterResource(id = drawableId),
                                contentDescription = "山岡家の方向",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(280.dp)
                                    .graphicsLayer { rotationZ = arrowRotation }
                                    .clickable {
                                        if (imageDrawableIds.size > 1) {
                                            selectedImageIndex =
                                                (selectedImageIndex + 1) % imageDrawableIds.size
                                        }
                                    }
                            )
                        } else {
                            Text("画像が見つかりません (drawable/yamaokaya, gyoza, miso, shio, tokusei_miso, kara_miso)")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("エラー: ${errorMessage ?: "検索に失敗しました。"}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        hasPermission = hasLocationPermission(context)
                        if (!hasPermission) {
                            permissionLauncher.launch(
                                buildList {
                                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                            )
                        } else {
                            isLoading = true
                        }
                    }) {
                        Text("再試行")
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "山岡家公式サイト",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://www.yamaokaya.com/")
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "(c) 2026 ",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Koba_9813",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://haya.win")
                    }
                )
            }
            Text(
                text = "Ver ${appVersion.removePrefix("V.")}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://github.com/koba9813/yamaokaya/releases")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本アプリは山岡家の公式アプリではなく、非公認のアプリです",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    if (showShareDialog && nearestShop != null) {
        val message = createShareMessage(nearestShop)
        val url = "https://koba9813.github.io/yamaokaya/"
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                Text("距離を共有する", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            shareToLine(context, view, uriHandler, message, url)
                            showShareDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C755)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LINE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            shareToTwitter(context, uriHandler, view, message, url)
                            showShareDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("X (Twitter)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            shareToInstagram(context, view, message, url)
                            showShareDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Instagram", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }
}

private fun openMenuInAppBrowser(context: Context) {
    val menuUri = Uri.parse(YAMAOKAYA_MENU_URL)
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()

    try {
        customTabsIntent.launchUrl(context, menuUri)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, menuUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun StampRallyScreen(
    stampRepository: StampRepository,
    shopNames: List<String>,
    onClose: () -> Unit
) {
    val ranking = shopNames
        .map { name -> name to stampRepository.getCount(name) }
        .sortedByDescending { it.second }

    val visitedCount = ranking.count { it.second > 0 }
    val totalCheckIns = ranking.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "スタンプラリー",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$visitedCount / ${shopNames.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "店舗達成",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCheckIns",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "総チェックイン",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ランキング",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ranking.forEachIndexed { index, (name, count) ->
                    val isVisited = count > 0
                    val rankLabel = when {
                        !isVisited -> "　"
                        index == 0 -> "🥇"
                        index == 1 -> "🥈"
                        index == 2 -> "🥉"
                        else -> "${index + 1}位"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rankLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(44.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isVisited) FontWeight.Bold else FontWeight.Normal,
                                color = if (isVisited) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (isVisited) {
                                val stampStr = "🍜".repeat(minOf(count, 5)) +
                                    if (count > 5) "+${count - 5}" else ""
                                Text(
                                    text = stampStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        if (isVisited) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${count}回",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "未訪問",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }

                    if (index < ranking.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ChaosRamenStorm(
    imageDrawableIds: List<Int>,
    modifier: Modifier = Modifier
) {
    if (imageDrawableIds.isEmpty()) return

    val sprites = remember(imageDrawableIds) {
        val random = Random(9813)
        List(42) {
            ChaosRamenSprite(
                imageIndex = random.nextInt(imageDrawableIds.size),
                phase = random.nextFloat(),
                speedX = 0.25f + random.nextFloat() * 1.9f,
                speedY = 0.22f + random.nextFloat() * 1.8f,
                wobbleCycles = 0.8f + random.nextFloat() * 6.8f,
                wobbleAmplitude = 0.02f + random.nextFloat() * 0.18f,
                rotationOffset = random.nextFloat() * 360f,
                scale = 0.45f + random.nextFloat() * 1.3f,
                alpha = 0.2f + random.nextFloat() * 0.75f,
                orbitRadius = 0.01f + random.nextFloat() * 0.11f,
                orbitCycles = 0.6f + random.nextFloat() * 8.6f,
                pulseDepth = 0.05f + random.nextFloat() * 0.55f,
                jitterSeed = random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "chaos_ramen")
    val progressFast by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chaos_progress_fast"
    )
    val progressSlow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chaos_progress_slow"
    )
    val progressPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chaos_progress_pulse"
    )

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        sprites.forEach { sprite ->
            val localFast = (progressFast + sprite.phase) % 1f
            val localSlow = (progressSlow + sprite.phase * 0.43f) % 1f
            val localPulse = (progressPulse + sprite.phase * 0.77f) % 1f

            val driftX = (((sprite.phase * 0.37f) + localFast * sprite.speedX + localSlow * 0.9f) % 1f) * widthPx
            val driftY = (((sprite.phase * 0.61f) + localFast * sprite.speedY + localSlow * 0.7f) % 1f) * heightPx

            val orbitTheta = (localSlow * sprite.orbitCycles * 2f * PI).toFloat() + sprite.jitterSeed
            val orbitX = cos(orbitTheta) * sprite.orbitRadius * widthPx
            val orbitY = sin(orbitTheta) * sprite.orbitRadius * heightPx

            val wobbleX = sin((localFast * (sprite.wobbleCycles + 1.7f) * 2f * PI).toFloat()) * sprite.wobbleAmplitude * widthPx
            val wobbleY = sin((localFast * sprite.wobbleCycles * 2f * PI).toFloat()) * sprite.wobbleAmplitude * heightPx

            val jitterX = sin(localFast * 39f + sprite.jitterSeed) * 18f
            val jitterY = cos(localFast * 31f + sprite.jitterSeed * 0.7f) * 18f

            val x = (driftX + orbitX + wobbleX + jitterX).coerceIn(-120f, widthPx + 120f)
            val y = (driftY + orbitY + wobbleY + jitterY).coerceIn(-120f, heightPx + 120f)

            val rotation = (
                localFast * 1260f +
                    localSlow * 740f +
                    sin(localPulse * 2f * PI.toFloat()) * 120f +
                    sprite.rotationOffset
                ) % 360f

            val pulseScale = 1f + sin((localPulse * 2f * PI).toFloat() + sprite.jitterSeed) * sprite.pulseDepth
            val finalScale = (sprite.scale * pulseScale).coerceIn(0.28f, 2.2f)
            val flickerAlpha = (
                sprite.alpha * (0.75f + 0.25f * sin((localFast * 2f * PI).toFloat() + sprite.jitterSeed))
                ).coerceIn(0.12f, 1f)

            Image(
                painter = painterResource(id = imageDrawableIds[sprite.imageIndex]),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        translationX = x - 42f
                        translationY = y - 42f
                        rotationZ = rotation
                        scaleX = finalScale
                        scaleY = finalScale
                        alpha = flickerAlpha
                    }
            )
        }
    }
}

@Composable
private fun AccountSettingsPage(
    accountProfile: AccountProfile,
    onSave: (String, String, String, Int, String) -> Unit,
    onClose: () -> Unit
) {
    var icon by remember { mutableStateOf(accountProfile.icon) }
    var name by remember { mutableStateOf(accountProfile.name) }
    var bio by remember { mutableStateOf(accountProfile.bio) }
    var syncIntervalSecondsText by remember {
        mutableStateOf(accountProfile.syncIntervalSeconds.toString())
    }
    var apiKey by remember { mutableStateOf(accountProfile.apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "アカウント設定",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = icon,
            onValueChange = { icon = it.take(2) },
            label = { Text("アイコン（絵文字など）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            label = { Text("表示名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it.take(40) },
            label = { Text("ひとこと") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = syncIntervalSecondsText,
            onValueChange = { syncIntervalSecondsText = it.filter(Char::isDigit).take(3) },
            label = { Text("同期間隔（秒: 5-120）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it.take(100) },
            label = { Text("APIキー") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "サーバーの config.php の api_key と同じ値にしてください",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val interval = syncIntervalSecondsText.toIntOrNull() ?: accountProfile.syncIntervalSeconds
                onSave(name, icon, bio, interval, apiKey)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("キャンセル")
        }
    }
}

@Composable
private fun InformationPage(
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Yamaokaya is Dokoは、最寄りの山岡家までの距離と方角を示すアプリです。",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ラーメン等の上側が向く方向が最寄り店舗です", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ラーメン画像をタップすると何かが起こるかも...?", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("距離表示と方角はリアルタイムで更新されます", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("LINE / Instagram / X で共有できます", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SocialLogoButton(
    logoResId: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(34.dp)
        )
    }
}

private fun createShareMessage(shop: ShopInfo?): String {
    val distanceKm = ((shop?.distanceMeters ?: 0f) / 1000f)
    return "山岡家まで ${"%.2f".format(distanceKm)} kmのところにいます！"
}

private fun formatDistanceMeters(distanceMeters: Float?): String {
    if (distanceMeters == null) return "未共有"
    return if (distanceMeters < 1000f) {
        "${"%.0f".format(distanceMeters)} m"
    } else {
        "${"%.2f".format(distanceMeters / 1000f)} km"
    }
}

private fun formatRelativeTime(updatedAtMillis: Long): String {
    if (updatedAtMillis <= 0L) return "不明"
    return DateUtils.getRelativeTimeSpanString(
        updatedAtMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

private fun shareToTwitter(
    context: Context,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    sourceView: View,
    message: String,
    url: String
) {
    val shareText = "$message\n$url"
    val shared = shareToPackageWithOptionalScreenshot(
        context = context,
        sourceView = sourceView,
        packageName = "com.twitter.android",
        chooserTitle = "Twitterで共有",
        shareText = shareText
    )

    if (!shared) {
        val text = Uri.encode(shareText)
        uriHandler.openUri("https://twitter.com/intent/tweet?text=$text")
    }
}

private fun shareToLine(
    context: Context,
    sourceView: View,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    message: String,
    url: String
) {
    val shareText = "$message\n$url"
    val shared = shareToPackageWithOptionalScreenshot(
        context = context,
        sourceView = sourceView,
        packageName = "jp.naver.line.android",
        chooserTitle = "LINEで共有",
        shareText = shareText
    )

    if (!shared) {
        val text = Uri.encode(shareText)
        uriHandler.openUri("https://line.me/R/msg/text/?$text")
    }
}

private fun shareToInstagram(context: Context, sourceView: View, message: String, url: String) {
    shareToPackageWithOptionalScreenshot(
        context = context,
        sourceView = sourceView,
        packageName = "com.instagram.android",
        chooserTitle = "Instagramで共有",
        shareText = "$message\n$url"
    )
}

private fun shareToPackageWithOptionalScreenshot(
    context: Context,
    sourceView: View,
    packageName: String,
    chooserTitle: String,
    shareText: String
): Boolean {
    val screenshotUri = captureScreenshotUri(context, sourceView)

    val targetIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (screenshotUri != null) "image/png" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        if (screenshotUri != null) {
            putExtra(Intent.EXTRA_STREAM, screenshotUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setPackage(packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(targetIntent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (screenshotUri != null) "image/png" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (screenshotUri != null) {
                putExtra(Intent.EXTRA_STREAM, screenshotUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(fallbackIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }
}

private fun captureScreenshotUri(context: Context, sourceView: View): Uri? {
    return try {
        val fullBitmap = sourceView.drawToBitmap()
        
        // 画面幅に合わせて正方形にトリミング
        val cropSize = fullBitmap.width
        val startX = 0
        // 画面の上下中央よりやや上くらいにコンテンツがあると想定し、少し上から切り出す
        val startY = Math.max(0, (fullBitmap.height - cropSize) / 2 - 100)
        val actualHeight = Math.min(cropSize, fullBitmap.height - startY)
        
        val croppedBitmap = Bitmap.createBitmap(fullBitmap, startX, startY, cropSize, actualHeight)
        
        // 背景を白色に塗りつぶすための新しいBitmap
        val finalBitmap = Bitmap.createBitmap(cropSize, actualHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(croppedBitmap, 0f, 0f, null)

        val file = File(context.cacheDir, "share_yamaokaya.png")
        FileOutputStream(file).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
private fun FusedLocationProviderClient.startRealtimeLocationUpdates(
    onLocation: (Location) -> Unit,
    onFailure: (String) -> Unit
): () -> Unit {
    val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(3000L)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val latest = result.lastLocation ?: return
            onLocation(latest)
        }
    }

    try {
        requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener {
                onFailure("現在地の更新を開始できませんでした。")
            }
    } catch (_: SecurityException) {
        onFailure("位置情報の許可が必要です。")
    }

    return {
        removeLocationUpdates(callback)
    }
}

private fun startHeadingUpdates(
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

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // no-op
        }
    }

    sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)

    return {
        sensorManager.unregisterListener(listener)
    }
}

private fun normalizeDegrees(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

@SuppressLint("MissingPermission")
private fun sendKokoNotification(context: Context) {
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
private fun startDistanceTrackerService(context: Context) {
    val serviceIntent = Intent(context, DistanceTrackerService::class.java)
    ContextCompat.startForegroundService(context, serviceIntent)
}

private fun resolveDrawableId(context: Context, name: String): Int {
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

private fun getAppVersionName(context: Context): String {
    return try {
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}

object YamaokayaFinder {

    private val registeredShops = listOf(
        // 北海道
        Coordinates(43.0143434, 144.3198345) to "山岡家 釧路店",
        Coordinates(43.7981896, 143.8644379) to "山岡家 北見店",
        Coordinates(42.9316136, 143.1932328) to "山岡家 帯広店",
        Coordinates(43.8186571, 144.1164278) to "山岡家 美幌店",
        Coordinates(42.8823215, 143.2006915) to "山岡家 帯広南店",
        Coordinates(43.0133938, 144.4037959) to "山岡家 釧路町店",
        Coordinates(43.9998096, 144.2841253) to "山岡家 網走店",
        Coordinates(42.9721878, 143.2079996) to "山岡家 音更店",
        Coordinates(43.5440312, 144.9864827) to "山岡家 中標津店",
        Coordinates(43.8056026, 142.4256515) to "山岡家 旭川永山店",
        Coordinates(45.3848388, 141.7064461) to "山岡家 稚内店",
        Coordinates(43.7438051, 142.4087292) to "山岡家 東光店",
        Coordinates(44.0646566, 143.5266339) to "山岡家 遠軽店",
        Coordinates(44.1657267, 142.3951252) to "山岡家 士別店",
        Coordinates(44.3676374, 143.3320663) to "山岡家 紋別店",
        Coordinates(43.9348987, 141.6637967) to "山岡家 留萌店",
        Coordinates(43.768393, 142.3250374) to "山岡家 旭川神居店",
        Coordinates(42.8742036, 141.5839741) to "山岡家 恵庭店",
        Coordinates(42.9734088, 141.567744) to "山岡家 北広島店",
        Coordinates(43.2115483, 141.7839946) to "山岡家 岩見沢店",
        Coordinates(43.1439194, 141.2765406) to "山岡家 樽川店",
        Coordinates(43.5410208, 141.9221426) to "山岡家 滝川店",
        Coordinates(42.8221253, 141.6572096) to "山岡家 千歳店",
        Coordinates(43.1743917, 141.0600291) to "山岡家 朝里店",
        Coordinates(43.3431415, 142.384644) to "山岡家 富良野店",
        Coordinates(43.1924884, 140.8209115) to "山岡家 余市店",
        Coordinates(43.108394, 141.5463879) to "山岡家 新文京台店",
        Coordinates(43.0648918, 141.4877182) to "山岡家 江別店",
        Coordinates(42.9895094, 141.4410939) to "山岡家 羊ヶ丘通店",
        Coordinates(43.057967, 141.35621) to "山岡家 南2条店",
        Coordinates(43.1300972, 141.2097702) to "山岡家 手稲店",
        Coordinates(42.9615303, 141.2742336) to "山岡家 藤野店",
        Coordinates(43.131467, 141.347034) to "山岡家 太平店",
        Coordinates(43.0915742, 141.4186061) to "山岡家 東雁来店",
        Coordinates(43.1039984, 141.3780466) to "山岡家 新道店",
        Coordinates(43.0536157, 141.3535782) to "山岡家 新すすきの店",
        Coordinates(43.0185341, 141.4077953) to "山岡家 月寒店",
        Coordinates(43.0280475, 141.448433) to "山岡家 大谷地店",
        Coordinates(43.0570279, 141.3517625) to "山岡家 狸小路4丁目店",
        Coordinates(43.1065132, 141.2737397) to "山岡家 新発寒店",
        Coordinates(43.0570238, 141.3516964) to "味噌ラーメン山岡家 すすきの店",
        Coordinates(42.6167056, 141.5412865) to "山岡家 苫小牧糸井店",
        Coordinates(42.3631776, 141.0568933) to "山岡家 室蘭店",
        Coordinates(42.4696844, 140.8751831) to "山岡家 伊達店",
        Coordinates(42.2571836, 140.2804866) to "山岡家 八雲店",
        Coordinates(42.6471397, 141.6267641) to "山岡家 苫小牧船見店",
        Coordinates(41.810416, 140.7691088) to "山岡家 函館鍛冶店",
        Coordinates(42.890613099999996, 140.7519845) to "山岡家 倶知安店",
        Coordinates(42.3413545, 142.3512688) to "山岡家 新ひだか店",
        Coordinates(41.7886103, 140.7319824) to "山岡家 函館万代店",
        Coordinates(42.6401468, 141.6131202) to "味噌ラーメン山岡家 苫小牧店",
        // 青森県
        Coordinates(40.6262333, 140.4872742) to "山岡家 弘前店",
        Coordinates(40.5087852, 141.5063306) to "山岡家 八戸店",
        Coordinates(40.8284416, 140.7836557) to "山岡家 青森東店",
        // 岩手県
        Coordinates(39.6804366, 141.1555256) to "山岡家 岩手盛岡店",
        Coordinates(39.7100725, 141.10535) to "山岡家 盛岡インター店",
        // 秋田県
        Coordinates(39.6867416, 140.1245236) to "山岡家 秋田仁井田店",
        Coordinates(39.7439016, 140.0896765) to "山岡家 秋田寺内店",
        // 山形県
        Coordinates(38.2212412, 140.3314594) to "山岡家 山形青田店",
        Coordinates(38.2668201, 140.3123049) to "山岡家 山形西田店",
        // 宮城県
        Coordinates(38.1198619, 140.874796) to "山岡家 名取店",
        Coordinates(38.3504563, 140.8747718) to "山岡家 仙台泉区店",
        Coordinates(38.2766083, 140.9904266) to "山岡家 宮城野店",
        Coordinates(38.2655383, 140.869467) to "山岡家 定禅寺通店",
        // 福島県
        Coordinates(36.9865093, 140.9025688) to "山岡家 いわき店",
        Coordinates(37.7888247, 140.4602521) to "山岡家 福島矢野目店",
        Coordinates(37.4260279, 140.344144) to "山岡家 郡山店",
        Coordinates(37.2979485, 140.3679489) to "山岡家 須賀川店",
        // 茨城県
        Coordinates(36.397650673276345, 140.50535377876412) to "山岡家 ひたちなか店",
        Coordinates(36.366183045752884, 140.48297166948979) to "山岡家 水戸城南店",
        Coordinates(36.537558996534784, 140.63643157503827) to "山岡家 日立東金沢店",
        Coordinates(36.537658129508564, 140.41588693877145) to "山岡家 常陸大宮店",
        Coordinates(36.377374656642665, 140.3608206027508) to "山岡家 水戸内原店",
        Coordinates(36.31298564606419, 140.44880778716356) to "山岡家 水戸南店",
        Coordinates(36.19229574055287, 140.2941730730318) to "山岡家 石岡店",
        Coordinates(36.13136315812844, 140.22564799696343) to "山岡家 かすみがうら店",
        Coordinates(36.085492839913535, 140.2060142271692) to "山岡家 土浦店",
        Coordinates(36.07663120071107, 140.1059249155306) to "山岡家 つくば中央店",
        Coordinates(36.04915550750586, 140.0848427526962) to "山岡家 谷田部店",
        Coordinates(35.99932258990889, 140.1533127114314) to "山岡家 牛久店",
        Coordinates(35.915208766063834, 140.63690426791038) to "山岡家 神栖店",
        Coordinates(36.2873896, 139.8835851) to "山岡家 新結城店",
        Coordinates(35.960660, 139.986108) to "山岡家 守谷店",
        Coordinates(36.348665, 140.052345) to "山岡家 岩瀬店",
        Coordinates(36.041131, 140.208893) to "山岡家 阿見店",
        Coordinates(36.040836, 140.2092861) to "味噌ラーメン山岡家 阿見店",
        // 栃木県
        Coordinates(36.2671246, 139.8271714) to "山岡家 小山田間店",
        Coordinates(36.2987547, 139.816505) to "山岡家 小山駅南町店",
        Coordinates(36.5974076, 139.8849839) to "山岡家 宇都宮長岡店",
        Coordinates(36.3078337, 139.4632362) to "山岡家 足利店",
        Coordinates(36.2959774, 139.6014027) to "山岡家 佐野店",
        Coordinates(36.5674311, 139.9932831) to "山岡家 テクノポリスセンター店",
        Coordinates(36.414249, 139.8980075) to "山岡家 上三川店",
        Coordinates(36.394488, 139.7299163) to "山岡家 栃木店",
        Coordinates(36.8424476, 139.9520549) to "山岡家 大田原店",
        Coordinates(36.5462495, 139.86438) to "山岡家 鶴田店",
        // 群馬県
        Coordinates(36.3432355, 139.3831653) to "山岡家 太田店",
        Coordinates(36.3342133, 138.9469017) to "山岡家 高崎西店",
        Coordinates(36.3290349, 139.1671695) to "山岡家 伊勢崎宮子店",
        Coordinates(36.2947057, 139.069438) to "山岡家 高崎倉賀野店",
        Coordinates(36.370088, 139.0234183) to "山岡家 高崎中尾店",
        Coordinates(36.2336972, 139.5431296) to "山岡家 館林店",
        Coordinates(36.266624, 139.3974419) to "山岡家 大泉店",
        Coordinates(36.3811427, 139.1047738) to "山岡家 前橋野中店",
        // 埼玉県
        Coordinates(35.9549972, 139.7263575) to "山岡家 春日部店",
        Coordinates(35.9942227, 139.5764194) to "山岡家 上尾店",
        Coordinates(35.9242078, 139.5834897) to "山岡家 さいたま宮前店",
        Coordinates(35.9583397, 139.6563641) to "山岡家 さいたま丸ヶ崎店",
        Coordinates(35.884252, 139.817204) to "山岡家 越谷レイクタウン店",
        Coordinates(35.9686024, 139.4700418) to "山岡家 川島店",
        Coordinates(36.1505235, 139.5386714) to "山岡家 羽生店",
        Coordinates(35.9341125, 139.3902665) to "山岡家 鶴ヶ島店",
        Coordinates(36.1848042, 139.3170081) to "山岡家 深谷店",
        // 千葉県
        Coordinates(35.794417, 140.3192147) to "山岡家 成田店",
        Coordinates(35.7574649, 140.302621) to "山岡家 成田飯仲店",
        Coordinates(35.5543273, 140.3654611) to "山岡家 東金店",
        Coordinates(35.7135115, 140.2193346) to "山岡家 千葉佐倉店",
        Coordinates(35.5518, 140.1252878) to "山岡家 千葉中央区店",
        Coordinates(35.3952961, 139.9417582) to "山岡家 木更津店",
        Coordinates(35.684051, 140.128222) to "山岡家 千葉花見川区店",
        Coordinates(35.3248443, 139.9246205) to "山岡家 君津店",
        Coordinates(35.7713087, 140.0951749) to "山岡家 八千代店",
        Coordinates(35.6268812, 140.1266747) to "山岡家 東千葉店",
        Coordinates(35.6370453, 140.1667861) to "山岡家 千葉若葉区店",
        Coordinates(35.8937398, 139.9584636) to "山岡家 柏店",
        Coordinates(35.937545, 139.8901699) to "山岡家 野田店",
        Coordinates(35.8273997, 139.9362494) to "山岡家 松戸北小金店",
        // 東京都
        Coordinates(35.7650546, 139.3541323) to "山岡家 瑞穂店",
        Coordinates(35.786256400000006, 139.30666800000003) to "山岡家 青梅店",
        // 神奈川県
        Coordinates(35.488332, 139.3648977) to "山岡家 厚木店",
        Coordinates(35.5629704, 139.3466083) to "山岡家 相模原店",
        Coordinates(35.3563238, 139.357292) to "山岡家 平塚店",
        Coordinates(35.3661472, 139.3619684) to "山岡家 平塚田村店",
        // 新潟県
        Coordinates(37.1290648, 138.2473411) to "山岡家 上越店",
        Coordinates(37.4558185, 138.8110035) to "山岡家 長岡堺店",
        Coordinates(37.9020128, 139.0397342) to "山岡家 新潟新和店",
        Coordinates(37.8187252, 139.0195906) to "山岡家 新潟白根大通店",
        Coordinates(37.9340737, 139.0976758) to "山岡家 新潟藤見店",
        // 山梨県
        Coordinates(35.6428205, 138.6453223) to "山岡家 笛吹店",
        Coordinates(35.6606643, 138.5215455) to "山岡家 山梨甲斐店",
        Coordinates(35.4911451, 138.7444003) to "山岡家 フォレスト河口湖店",
        // 長野県
        Coordinates(36.6419115, 138.237275) to "山岡家 長野南長池店",
        Coordinates(36.184297, 137.9628832) to "山岡家 松本店",
        // 静岡県
        Coordinates(35.1351064, 138.6527572) to "山岡家 富士店",
        Coordinates(35.2435531, 138.6181984) to "山岡家 富士宮店",
        Coordinates(34.8780897, 138.3078201) to "山岡家 焼津店",
        Coordinates(34.7591795, 137.7526378) to "山岡家 浜松有玉店",
        Coordinates(34.7253463, 137.7792771) to "山岡家 浜松薬師店",
        Coordinates(34.6775816, 137.6807203) to "山岡家 浜松南区店",
        // 愛知県
        Coordinates(34.7791405, 137.3847552) to "山岡家 豊橋下地店",
        Coordinates(35.3253727, 136.9172402) to "山岡家 大口店",
        // 岐阜県
        Coordinates(35.3443973, 136.6220762) to "山岡家 新大垣店"
    )

    fun findNearest(current: Coordinates): ShopInfo? {
        var nearest: ShopInfo? = null

        for ((coords, name) in registeredShops) {
            val distance = calculateDistanceMeters(current, coords)
            val bearing = calculateBearing(current, coords)
            val candidate = ShopInfo(
                name = name,
                coordinates = coords,
                distanceMeters = distance,
                bearingDegrees = bearing
            )

            if (nearest == null || candidate.distanceMeters < nearest.distanceMeters) {
                nearest = candidate
            }
        }

        return nearest
    }

    fun getRegisteredShops(): List<Pair<Coordinates, String>> = registeredShops

    fun getShopNames(): List<String> = registeredShops.map { it.second }
}

private fun calculateDistanceMeters(from: Coordinates, to: Coordinates): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.lat, from.lon, to.lat, to.lon, result)
    return result[0]
}

private fun calculateBearing(from: Coordinates, to: Coordinates): Double {
    val lat1 = Math.toRadians(from.lat)
    val lat2 = Math.toRadians(to.lat)
    val deltaLon = Math.toRadians(to.lon - from.lon)

    val y = kotlin.math.sin(deltaLon) * kotlin.math.cos(lat2)
    val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
        kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(deltaLon)

    val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    return (bearing + 360.0) % 360.0
}

private fun bearingToDirection(bearing: Double): String {
    val directions = listOf(
        "北", "北北東", "北東", "東北東",
        "東", "東南東", "南東", "南南東",
        "南", "南南西", "南西", "西南西",
        "西", "西北西", "北西", "北北西"
    )
    val index = (((bearing + 11.25) % 360) / 22.5).toInt()
    return directions[index]
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

@Composable
private fun UpdateAvailableDialog(
    updateInfo: UpdateChecker.UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "アップデートがあります",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "最新バージョン",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = updateInfo.latestVersion,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "リリースノート:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AndroidView(
                        factory = { ctx -> TextView(ctx) },
                        update = { textView ->
                            val markwon = Markwon.create(textView.context)
                            val note = updateInfo.releaseNotes.take(300) +
                                if (updateInfo.releaseNotes.length > 300) "…" else ""
                            markwon.setMarkdown(textView, note)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate
            ) {
                Text("アップデートする")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("あとで")
            }
        }
    )
}
