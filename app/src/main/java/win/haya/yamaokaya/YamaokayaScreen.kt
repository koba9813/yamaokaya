package win.haya.yamaokaya

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

private const val KOKO_RADIUS_METERS = 50f
private const val SPECIAL_EFFECT_RADIUS_METERS = 50f
private const val IMAGE_TOP_OFFSET_DEGREES = 0f
private const val SHARE_URL = "https://koba9813.github.io/yamaokaya/"

@SuppressLint("MissingPermission")
@Composable
internal fun YamaokayaScreen(
    appSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var isLoading by remember { mutableStateOf(hasPermission) }
    val stampRepository = remember { StampRepository(context) }

    var nearestShop by remember { mutableStateOf<ShopInfo?>(null) }
    var headingDegrees by remember { mutableFloatStateOf(0f) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showInfoPage by remember { mutableStateOf(false) }
    var showStampRally by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var checkInFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var previouslyInsideRadius by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imageNames = remember {
        listOf("yamaokaya", "gyoza", "miso", "shio", "tokusei_miso", "kara_miso")
    }
    val imageDrawableIds = remember(imageNames) {
        imageNames.map { resolveDrawableId(context, it) }.filter { it != 0 }
    }
    val drawableId = if (imageDrawableIds.isNotEmpty()) {
        imageDrawableIds[selectedImageIndex % imageDrawableIds.size]
    } else 0

    val shopNames = remember { YamaokayaFinder.getShopNames() }

    // Check for updates
    LaunchedEffect(Unit) {
        val updateInfo = UpdateChecker.checkForUpdate(context)
        if (updateInfo != null) {
            showUpdateDialog = updateInfo
        }
    }

    // Location & heading updates
    DisposableEffect(appSettings, hasPermission) {
        var stopLocation: (() -> Unit)? = null
        var stopHeading: (() -> Unit)? = null

        if (hasPermission) {
            stopLocation = fusedClient.startRealtimeLocationUpdates(
                onLocation = { location ->
                    val current = Coordinates(location.latitude, location.longitude)
                    val nearest = YamaokayaFinder.findNearest(current)
                    nearestShop = nearest

                    if (nearest != null && nearest.distanceMeters <= KOKO_RADIUS_METERS) {
                        if (!previouslyInsideRadius && appSettings.proximityNotificationEnabled) {
                            sendKokoNotification(context)
                        }
                        previouslyInsideRadius = true
                    } else {
                        previouslyInsideRadius = false
                    }

                    if (appSettings.trackerNotificationEnabled) {
                        startDistanceTrackerService(context)
                    }
                },
                onFailure = { msg -> errorMessage = msg }
            )

            stopHeading = startHeadingUpdates(context) { heading ->
                headingDegrees = heading
            }
        }

        onDispose {
            stopLocation?.invoke()
            stopHeading?.invoke()
        }
    }

    // Update dialog
    showUpdateDialog?.let { updateInfo ->
        UpdateAvailableDialog(
            updateInfo = updateInfo,
            onUpdate = {
                uriHandler.openUri(updateInfo.releaseUrl)
                showUpdateDialog = null
            },
            onDismiss = { showUpdateDialog = null }
        )
    }

    // Sub-page navigation
    when {
        showSettings -> {
            SettingsPage(
                appSettings = appSettings,
                onSettingsChanged = onSettingsChanged,
                onClose = { showSettings = false }
            )
            return
        }
        showStampRally -> {
            StampRallyScreen(
                stampRepository = stampRepository,
                shopNames = shopNames,
                onClose = { showStampRally = false }
            )
            return
        }
        showInfoPage -> {
            InformationPage(
                onClose = { showInfoPage = false }
            )
            return
        }
    }

    val appVersion = remember { getAppVersionName(context) }

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

    val isWithinRadius = nearestShop != null && nearestShop!!.distanceMeters <= KOKO_RADIUS_METERS

    // Main screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        val titleText = if (isWithinRadius) {
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

        // Vertical action buttons (top-end)
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

            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "設定",
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
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        val isCheckedIn = stampRepository.getRemainingCooldownMillis(shop.name) > 0L
                        if (isCheckedIn) {
                            Button(
                                onClick = { openMenuInAppBrowser(context) },
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
                                    val remainMs = stampRepository.getRemainingCooldownMillis(shop.name)
                                    val totalMinutes = (remainMs + 59_999L) / 60_000L
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
                        val drawableIdFar = if (imageDrawableIds.isNotEmpty()) {
                            imageDrawableIds[selectedImageIndex % imageDrawableIds.size]
                        } else {
                            0
                        }

                        if (drawableIdFar != 0) {
                            Image(
                                painter = painterResource(id = drawableIdFar),
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

        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "山岡家公式サイト",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://www.yamaokaya.com/")
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
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

        // Share dialog
        if (showShareDialog) {
            val shareMessage = createShareMessage(nearestShop)
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("共有する") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showShareDialog = false
                                shareToLine(context, view, uriHandler, shareMessage, SHARE_URL)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("LINEで共有") }
                        Button(
                            onClick = {
                                showShareDialog = false
                                shareToTwitter(context, uriHandler, view, shareMessage, SHARE_URL)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("X (Twitter) で共有") }
                        Button(
                            onClick = {
                                showShareDialog = false
                                shareToInstagram(context, view, shareMessage, SHARE_URL)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Instagramで共有") }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showShareDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}
