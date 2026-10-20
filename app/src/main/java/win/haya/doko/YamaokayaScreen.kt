package win.haya.doko

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val IMAGE_TOP_OFFSET_DEGREES = 0f

private fun formatDistance(meters: Float): String = if (meters < 1000f) {
    "${"%.0f".format(meters)} m"
} else {
    "${"%.2f".format(meters / 1000f)} km"
}

@Composable
internal fun YamaokayaScreen(
    viewModel: YamaokayaViewModel
) {
    val context = LocalContext.current

    val appSettings by viewModel.appSettings
    val nearestShop by viewModel.nearestShop
    val headingDegrees by viewModel.headingDegrees
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    val stampRepository = remember { StampRepository(context) }
    val photoRepository = remember { PhotoRepository(context) }
    val shopNames = remember { YamaokayaFinder.getShopNames() }

    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showInfoPage by remember { mutableStateOf(false) }
    var showStampRally by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var cameraShop by remember { mutableStateOf<String?>(null) }
    var checkInFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    var updateState by remember { mutableStateOf<UpdateFlowState>(UpdateFlowState.Hidden) }

    val imageNames = remember {
        listOf("yamaokaya", "gyoza", "miso", "shio", "tokusei_miso", "kara_miso")
    }
    val imageDrawableIds = remember(imageNames) {
        imageNames.map { resolveDrawableId(context, it) }.filter { it != 0 }
    }
    val currentDrawableId = remember(imageDrawableIds, selectedImageIndex) {
        if (imageDrawableIds.isNotEmpty()) {
            imageDrawableIds[selectedImageIndex % imageDrawableIds.size]
        } else {
            0
        }
    }

    val isWithinRadius = nearestShop?.distanceMeters?.let { it <= KOKO_RADIUS_METERS } == true

    when {
        showSettings -> {
            SettingsPage(
                appSettings = appSettings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onClose = { showSettings = false }
            )
            return
        }
        showStampRally -> {
            StampRallyScreen(
                stampRepository = stampRepository,
                photoRepository = remember { PhotoRepository(context) },
                currentCoordinates = viewModel.currentCoordinates.value,
                shopNames = shopNames,
                onClose = { showStampRally = false },
                onOpenGallery = { showGallery = true }
            )
            return
        }
        showGallery -> {
            GalleryScreen(
                photoRepository = remember { PhotoRepository(context) },
                onClose = { showGallery = false }
            )
            return
        }
        cameraShop != null -> {
            CameraCaptureScreen(
                shopName = cameraShop!!,
                onCaptured = { file ->
                    PhotoRepository.decodeSampledBitmapFromUri(context, android.net.Uri.fromFile(file), 1024)?.let { bitmap ->
                        photoRepository.savePhoto(cameraShop!!, bitmap)
                    }
                    file.delete()
                    cameraShop = null
                },
                onClose = { cameraShop = null }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val hasPermission = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasPermission) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val suffixText = when {
        nearestShop == null -> "Doko?"
        isWithinRadius -> "Koko"
        else -> nearestShop?.let { formatDistance(it.distanceMeters) } ?: "Doko?"
    }
    val emphasizeSuffix = nearestShop != null && !isWithinRadius

    val isInitialLoading = isLoading && nearestShop == null && errorMessage == null && hasLocationPermission(context)

    LaunchedEffect(isInitialLoading) {
        if (!isInitialLoading && !splashFinished) {
            delay(900)
            splashFinished = true
        }
    }

    LaunchedEffect(splashFinished) {
        if (splashFinished && updateState == UpdateFlowState.Hidden) {
            delay(1200)
            val result = checkForUpdate(context)
            if (result is UpdateFlowState.Available) {
                updateState = result
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (splashFinished) 1f else 0f)
        ) {
            MainHeader(
                suffixText = suffixText,
                emphasizeSuffix = emphasizeSuffix,
                onOpenStampRally = { showStampRally = true },
                onOpenGallery = { showGallery = true },
                onOpenInfo = { showInfoPage = true },
                onOpenSettings = { showSettings = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    !hasLocationPermission(context) -> {
                        PermissionRequestContent(
                            onRequestPermission = {
                                permissionLauncher.launch(
                                    buildList {
                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                )
                            }
                        )
                    }

                    isLoading && nearestShop == null -> {
                        LoadingContent()
                    }

                    errorMessage != null -> {
                        ErrorContent(
                            message = errorMessage ?: "検索に失敗しました。",
                            onRetry = {
                                if (!hasLocationPermission(context)) {
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
                                    viewModel.retryLocation()
                                }
                            }
                        )
                    }

                    isWithinRadius -> {
                        val shop = nearestShop ?: return@Box
                        AtShopContent(
                            shop = shop,
                            stampRepository = stampRepository,
                            photoRepository = photoRepository,
                            checkInFeedbackMessage = checkInFeedbackMessage,
                            onCheckInFeedbackMessage = { checkInFeedbackMessage = it },
                            onOpenMap = { openGoogleMaps(context, shop.coordinates) },
                            onOpenCamera = { name -> cameraShop = name }
                        )
                    }

                    nearestShop != null -> {
                        val shop = nearestShop ?: return@Box
                        val arrowRotation = normalizeDegrees(
                            shop.bearingDegrees.toFloat() - headingDegrees + IMAGE_TOP_OFFSET_DEGREES
                        )

                        ShopContent(
                            shop = shop,
                            currentDrawableId = currentDrawableId,
                            arrowRotation = arrowRotation,
                            checkInFeedbackMessage = checkInFeedbackMessage,
                            onImageClick = {
                                if (imageDrawableIds.size > 1) {
                                    selectedImageIndex = (selectedImageIndex + 1) % imageDrawableIds.size
                                }
                            },
                            onOpenMap = {
                                openGoogleMaps(context, shop.coordinates)
                            },
                            onOpenMenu = { openMenuInAppBrowser(context) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !splashFinished,
            enter = fadeIn(animationSpec = tween(320)),
            exit = fadeOut(animationSpec = tween(700))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                TitleText(
                    prefix = "Yamaokaya is",
                    suffix = suffixText,
                    baseStyle = MaterialTheme.typography.titleLarge,
                    emphasizedStyle = MaterialTheme.typography.displayLarge,
                    baseColor = Color.White.copy(alpha = 0.72f),
                    emphasizedColor = Color.White,
                    emphasize = emphasizeSuffix,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        UpdateFlow(
            state = updateState,
            onStateChange = { updateState = it },
            onDismiss = {
                if (updateState !is UpdateFlowState.Downloading) {
                    updateState = UpdateFlowState.Hidden
                }
            }
        )
    }
}

/* -------------------------------------------------------------------------
 * Header
 * ---------------------------------------------------------------------- */

@Composable
private fun MainHeader(
    suffixText: String,
    emphasizeSuffix: Boolean,
    onOpenStampRally: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = AppSpacing.screen, end = 16.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp, end = 10.dp)
        ) {
            Text(
                text = "Yamaokaya is",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedContent(
                targetState = suffixText,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) +
                        slideInVertically(animationSpec = tween(220)) { it / 7 }) togetherWith
                        fadeOut(animationSpec = tween(140))
                },
                label = "headerDistance"
            ) { suffix ->
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (emphasizeSuffix) {
                        liquidGlassTextAccent()
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
            }
        }

        MainActionRail(
            onOpenStampRally = onOpenStampRally,
            onOpenGallery = onOpenGallery,
            onOpenInfo = onOpenInfo,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun MainActionRail(
    onOpenStampRally: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit
) {
    GlassSurface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        level = GlassLevel.Thin,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                RailIconButton(Icons.Outlined.Star, "スタンプラリー", onOpenStampRally)
                RailIconButton(Icons.Outlined.PhotoLibrary, "ギャラリー", onOpenGallery)
            }
            Row {
                RailIconButton(Icons.Outlined.Info, "情報", onOpenInfo)
                RailIconButton(Icons.Outlined.Settings, "設定", onOpenSettings)
            }
        }
    }
}

@Composable
private fun RailIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp)
        )
    }
}

/* -------------------------------------------------------------------------
 * Main content
 * ---------------------------------------------------------------------- */

@Composable
private fun ShopContent(
    shop: ShopInfo,
    currentDrawableId: Int,
    arrowRotation: Float,
    checkInFeedbackMessage: String?,
    onImageClick: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = AppSpacing.screen, end = AppSpacing.screen, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(
                maxWidth - 20.dp,
                maxHeight - 12.dp,
                400.dp
            ).coerceAtLeast(120.dp)

            if (currentDrawableId != 0) {
                Image(
                    painter = painterResource(id = currentDrawableId),
                    contentDescription = "山岡家の方向。タップすると画像が切り替わります",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(side)
                        .graphicsLayer {
                            rotationZ = arrowRotation
                        }
                        .clickable(onClick = onImageClick)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(side)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "最寄店舗",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = shop.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassPillButton(
                    text = "Google マップ",
                    onClick = onOpenMap,
                    icon = Icons.Outlined.LocationOn,
                    accent = true,
                    modifier = Modifier.weight(1f)
                )
                GlassPillButton(
                    text = "メニュー",
                    onClick = onOpenMenu,
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    modifier = Modifier.weight(1f)
                )
            }

            CheckInFeedback(
                message = checkInFeedbackMessage,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun AtShopContent(
    shop: ShopInfo,
    stampRepository: StampRepository,
    photoRepository: PhotoRepository,
    checkInFeedbackMessage: String?,
    onCheckInFeedbackMessage: (String?) -> Unit,
    onOpenMap: () -> Unit,
    onOpenCamera: (String) -> Unit
) {
    val context = LocalContext.current
    val isCheckedIn = stampRepository.getRemainingCooldownMillis(shop.name) > 0L

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onOpenCamera(shop.name)
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            PhotoRepository.decodeSampledBitmapFromUri(context, uri, 1024)?.let { bitmap ->
                photoRepository.savePhoto(shop.name, bitmap)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.screen, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassSurface(
                shape = AppShapes.card,
                level = GlassLevel.Regular,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "山岡家に到着",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "チェックインして、訪問の記録を残そう。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    if (isCheckedIn) {
                        GlassPillButton(
                            text = "メニューを見る",
                            onClick = { openMenuInAppBrowser(context) },
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            accent = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GlassPillButton(
                            text = "チェックイン",
                            onClick = {
                                val checkedIn = stampRepository.checkIn(shop.name)
                                if (!checkedIn) {
                                    val remainMs = stampRepository.getRemainingCooldownMillis(shop.name)
                                    onCheckInFeedbackMessage(formatCheckInCooldownMessage(remainMs))
                                } else {
                                    onCheckInFeedbackMessage(null)
                                }
                            },
                            icon = Icons.Filled.CheckCircle,
                            accent = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassPillButton(
                            text = "撮影",
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    onOpenCamera(shop.name)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            icon = Icons.Outlined.AddAPhoto,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = "ギャラリー",
                            onClick = {
                                galleryPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            icon = Icons.Outlined.PhotoLibrary,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = "地図",
                            onClick = onOpenMap,
                            icon = Icons.Outlined.LocationOn,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            CheckInFeedback(
                message = checkInFeedbackMessage,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun CheckInFeedback(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(160)),
        modifier = modifier
    ) {
        message?.let { text ->
            GlassSurface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                tint = MaterialTheme.colorScheme.errorContainer,
                tintAlpha = 0.94f,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
 * Empty / permission / loading states
 * ---------------------------------------------------------------------- */

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.screen, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = AppShapes.card,
            level = GlassLevel.Regular,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "位置情報を許可してください",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "現在地から最寄りの山岡家までの距離と方角を表示するために使用します。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                GlassPillButton(
                    text = "位置情報を許可して検索",
                    onClick = onRequestPermission,
                    accent = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "最寄り店舗を検索中…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.screen, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = AppShapes.card,
            level = GlassLevel.Regular,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "検索に失敗しました",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                GlassPillButton(
                    text = "再試行",
                    onClick = onRetry,
                    accent = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------
 * Splash title
 * ---------------------------------------------------------------------- */

@Composable
private fun TitleText(
    prefix: String,
    suffix: String,
    baseStyle: androidx.compose.ui.text.TextStyle,
    emphasizedStyle: androidx.compose.ui.text.TextStyle,
    baseColor: Color,
    emphasizedColor: Color,
    emphasize: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = prefix,
            style = baseStyle,
            color = baseColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        AnimatedContent(
            targetState = suffix,
            transitionSpec = {
                fadeIn(animationSpec = tween(260)) togetherWith
                    fadeOut(animationSpec = tween(200))
            },
            label = "splashSuffix"
        ) { s ->
            Text(
                text = s,
                style = if (emphasize) emphasizedStyle else baseStyle,
                color = emphasizedColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatCheckInCooldownMessage(remainMs: Long): String {
    val totalMinutes = (remainMs + 59_999L) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "同じ店舗のチェックインは3時間に1回までです（あと${hours}時間${minutes}分）"
    } else {
        "同じ店舗のチェックインは3時間に1回までです（あと${minutes}分）"
    }
}

private fun openGoogleMaps(context: android.content.Context, coordinates: Coordinates) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${coordinates.lat},${coordinates.lon}"
        )
    )
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        // ignore
    }
}
