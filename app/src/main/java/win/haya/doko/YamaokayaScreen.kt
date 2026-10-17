package win.haya.doko

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val IMAGE_TOP_OFFSET_DEGREES = 0f

private fun formatDistance(meters: Float): String = if (meters < 1000f) {
    "${"%.0f".format(meters)} m"
} else {
    "${"%.2f".format(meters / 1000f)} km"
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var checkInFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    var titleMoved by rememberSaveable { mutableStateOf(false) }

    val imageNames = remember {
        listOf("yamaokaya", "gyoza", "miso", "shio", "tokusei_miso", "kara_miso")
    }
    val imageDrawableIds = remember(imageNames) {
        imageNames.map { resolveDrawableId(context, it) }.filter { it != 0 }
    }
    val currentDrawableId = remember(imageDrawableIds, selectedImageIndex) {
        if (imageDrawableIds.isNotEmpty()) imageDrawableIds[selectedImageIndex % imageDrawableIds.size] else 0
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            AnimatedVisibility(
                visible = splashFinished,
                enter = fadeIn(animationSpec = tween(400))
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 64.dp, end = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TitleText(
                                prefix = "Yamaokaya is",
                                suffix = suffixText,
                                baseStyle = MaterialTheme.typography.headlineSmall,
                                emphasizedStyle = MaterialTheme.typography.displayLarge,
                                baseColor = MaterialTheme.colorScheme.onSurface,
                                emphasizedColor = MaterialTheme.colorScheme.primary,
                                emphasize = emphasizeSuffix
                            )
                        }

                        Column(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(onClick = { showStampRally = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "スタンプラリー"
                                )
                            }
                            IconButton(onClick = { showGallery = true }) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = "ギャラリー"
                                )
                            }
                            IconButton(onClick = { showInfoPage = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "情報"
                                )
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "設定"
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = { }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
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
                    val shop = nearestShop ?: return@Scaffold
                    AtShopContent(
                        shop = shop,
                        stampRepository = stampRepository,
                        photoRepository = photoRepository,
                        checkInFeedbackMessage = checkInFeedbackMessage,
                        onCheckInFeedbackMessage = { checkInFeedbackMessage = it },
                        onOpenMap = { openGoogleMaps(context, shop.coordinates) }
                    )
                }

                nearestShop != null -> {
                    val shop = nearestShop ?: return@Scaffold
                    val arrowRotation = normalizeDegrees(
                        shop.bearingDegrees.toFloat() - headingDegrees + IMAGE_TOP_OFFSET_DEGREES
                    )

                    ShopContent(
                        shop = shop,
                        currentDrawableId = currentDrawableId,
                        arrowRotation = arrowRotation,
                        isWithinRadius = isWithinRadius,
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
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(1200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        AnimatedVisibility(
            visible = !titleMoved,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val targetOffsetY = with(density) {
                    -(maxHeight.toPx() / 2f) + (statusBarHeight.toPx() + 44.dp.toPx())
                }
                val offsetY = remember { Animatable(0f) }
                val textAlpha = remember { Animatable(1f) }

                LaunchedEffect(splashFinished) {
                    if (splashFinished && !titleMoved) {
                        launch {
                            offsetY.animateTo(targetOffsetY, tween(900))
                        }
                        textAlpha.animateTo(0f, tween(500))
                        titleMoved = true
                    }
                }

                TitleText(
                    prefix = "Yamaokaya is",
                    suffix = suffixText,
                    baseStyle = MaterialTheme.typography.displayMedium,
                    emphasizedStyle = MaterialTheme.typography.displayLarge,
                    baseColor = Color.White,
                    emphasizedColor = Color.White,
                    emphasize = emphasizeSuffix,
                    modifier = Modifier
                        .offset { IntOffset(0, offsetY.value.toInt()) }
                        .alpha(textAlpha.value)
                        .padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ShopContent(
    shop: ShopInfo,
    currentDrawableId: Int,
    arrowRotation: Float,
    isWithinRadius: Boolean,
    checkInFeedbackMessage: String?,
    onImageClick: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isWithinRadius) {
                SpecialRangeCard(shop = shop)
            } else if (currentDrawableId != 0) {
                Image(
                    painter = painterResource(id = currentDrawableId),
                    contentDescription = "山岡家の方向",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(420.dp)
                        .graphicsLayer {
                            rotationZ = arrowRotation
                        }
                        .clickable(onClick = onImageClick)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "最寄店舗 is ${shop.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("Google マップ")
                }

                if (!isWithinRadius) {
                    FilledTonalButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("メニュー")
                    }
                }
            }

            AnimatedVisibility(
                visible = checkInFeedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                checkInFeedbackMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceCard(shop: ShopInfo) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "最寄り店舗まで",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            val text = formatDistance(shop.distanceMeters)
            Text(
                text = text,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun DirectionCard(
    drawableId: Int,
    rotation: Float,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (drawableId != 0) {
                Image(
                    painter = painterResource(id = drawableId),
                    contentDescription = "山岡家の方向",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .clickable(onClick = onClick)
                )
            }
        }
    }
}

@Composable
private fun ShopNameCard(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpecialRangeCard(shop: ShopInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = shop.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "チェックインして記録しよう",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
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
    onOpenMap: () -> Unit
) {
    val context = LocalContext.current
    val isCheckedIn = stampRepository.getRemainingCooldownMillis(shop.name) > 0L
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = captureUri
        if (success && uri != null) {
            PhotoRepository.decodeSampledBitmapFromUri(context, uri, 1024)?.let { bitmap ->
                photoRepository.savePhoto(shop.name, bitmap)
            }
        }
        captureUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCapture(context, shop.name, { captureUri = it }, cameraLauncher)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        SpecialRangeCard(shop = shop)

        Spacer(modifier = Modifier.height(40.dp))

        if (isCheckedIn) {
            Button(
                onClick = { openMenuInAppBrowser(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "メニューを見る",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Button(
                onClick = {
                    val checkedIn = stampRepository.checkIn(shop.name)
                    if (!checkedIn) {
                        val remainMs = stampRepository.getRemainingCooldownMillis(shop.name)
                        onCheckInFeedbackMessage(formatCheckInCooldownMessage(remainMs))
                    } else {
                        onCheckInFeedbackMessage(null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "チェックイン",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        startCapture(context, shop.name, { captureUri = it }, cameraLauncher)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("写真を撮る")
            }

            OutlinedButton(
                onClick = onOpenMap,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("地図")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = checkInFeedbackMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            checkInFeedbackMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "位置情報を許可してください",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "現在地から最寄りの山岡家を探すために、位置情報を使用します。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("位置情報を許可して検索")
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "最寄り店舗を検索中...",
            style = MaterialTheme.typography.bodyLarge,
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "エラー",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRetry) {
                    Text("再試行")
                }
            }
        }
    }
}

@Composable
private fun TitleText(
    prefix: String,
    suffix: String,
    baseStyle: TextStyle,
    emphasizedStyle: TextStyle,
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
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        AnimatedContent(
            targetState = suffix,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(400)) +
                    scaleIn(
                        initialScale = 0.5f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )) togetherWith
                    (slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        targetOffsetY = { -it }
                    ) + fadeOut(animationSpec = tween(400)) +
                        scaleOut(targetScale = 0.4f, animationSpec = tween(400)))
            },
            label = "suffix"
        ) { s ->
            Text(
                text = s,
                style = if (emphasize) emphasizedStyle else baseStyle,
                color = if (emphasize) emphasizedColor else baseColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
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

private fun startCapture(
    context: android.content.Context,
    shopName: String,
    onUriReady: (Uri) -> Unit,
    launcher: androidx.activity.result.ActivityResultLauncher<Uri>
) {
    val tempFile = File(context.cacheDir, "capture_${shopName}.jpg").apply {
        delete()
        createNewFile()
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    onUriReady(uri)
    launcher.launch(uri)
}
