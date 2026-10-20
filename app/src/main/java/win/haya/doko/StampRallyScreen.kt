package win.haya.doko

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private enum class StampFilterMode(val label: String) {
    ALL("全店舗"),
    VISITED("訪問済み"),
    UNVISITED("未訪問")
}

@SuppressLint("MissingPermission")
@Composable
internal fun StampRallyScreen(
    stampRepository: StampRepository,
    photoRepository: PhotoRepository,
    currentCoordinates: Coordinates?,
    shopNames: List<String>,
    onClose: () -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // 位置情報の権限があれば最新位置を一度取得して補完する
        if (hasLocationPermission(context) && currentCoordinates == null) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) { }
        }
    }

    var pendingShopName by remember { mutableStateOf<String?>(null) }
    var photoRefresh by remember { mutableIntStateOf(0) }
    var cameraShopName by remember { mutableStateOf<String?>(null) }
    var galleryTargetShop by remember { mutableStateOf<String?>(null) }
    var photoMessage by remember { mutableStateOf<String?>(null) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val shop = galleryTargetShop
        if (uri != null && shop != null) {
            PhotoRepository.decodeSampledBitmapFromUri(context, uri, 1024)?.let { bitmap ->
                photoRepository.savePhoto(shop, bitmap)
                photoRefresh++
            }
        }
        galleryTargetShop = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingShopName?.let { cameraShopName = it }
        } else {
            pendingShopName = null
        }
    }

    val shopCoordinatesMap = remember {
        YamaokayaFinder.getRegisteredShops().associate { it.name to it.coordinates }
    }

    val prefectures = remember { YamaokayaFinder.getPrefectures() }
    var selectedPrefecture by remember { mutableStateOf<String?>(null) }
    var filterMode by remember { mutableStateOf(StampFilterMode.ALL) }

    val nearestShop = currentCoordinates?.let { YamaokayaFinder.findNearest(it) }
    val isNearestNearby = nearestShop?.distanceMeters?.let { it <= KOKO_RADIUS_METERS } == true

    val filteredNames = remember(shopNames, selectedPrefecture, filterMode) {
        val base = if (selectedPrefecture == null) {
            shopNames
        } else {
            YamaokayaFinder.getShopsByPrefecture(selectedPrefecture!!).map { it.name }
        }
        base.filter { name ->
            when (filterMode) {
                StampFilterMode.ALL -> true
                StampFilterMode.VISITED -> stampRepository.getCount(name) > 0
                StampFilterMode.UNVISITED -> stampRepository.getCount(name) == 0
            }
        }
    }

    val ranking = remember(filteredNames, currentCoordinates, photoRefresh) {
        filteredNames
            .map { name ->
                Triple(
                    name,
                    stampRepository.getCount(name),
                    currentCoordinates?.let { loc ->
                        shopCoordinatesMap[name]?.let { coords -> calculateDistanceMeters(loc, coords) }
                    }
                )
            }
            .sortedBy { it.third ?: Float.MAX_VALUE }
    }

    val visitedCount = ranking.count { it.second > 0 }
    val progress = if (filteredNames.isEmpty()) 0f else visitedCount.toFloat() / filteredNames.size

    if (cameraShopName != null) {
        CameraCaptureScreen(
            shopName = cameraShopName!!,
            onCaptured = { file ->
                PhotoRepository.decodeSampledBitmapFromUri(context, android.net.Uri.fromFile(file), 1024)?.let { bitmap ->
                    photoRepository.savePhoto(cameraShopName!!, bitmap)
                    photoRefresh++
                }
                file.delete()
                cameraShopName = null
                pendingShopName = null
            },
            onClose = {
                cameraShopName = null
                pendingShopName = null
            }
        )
        return
    }

    ScreenEnter(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = screenContainerColor(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopBar(
                    title = "スタンプラリー",
                    onBack = onClose,
                    actions = {
                        GlassIconButton(
                            icon = Icons.Outlined.PhotoLibrary,
                            contentDescription = "ギャラリーを開く",
                            onClick = onOpenGallery
                        )
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = AppSpacing.screen,
                    end = AppSpacing.screen,
                    top = 6.dp,
                    bottom = 32.dp
                )
            ) {
                item {
                    ProgressPanel(
                        visitedCount = visitedCount,
                        totalCount = filteredNames.size,
                        progress = progress,
                        totalCheckIns = ranking.sumOf { it.second }
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.section))
                }

                item {
                    val latestPhoto = remember(photoRefresh, nearestShop?.name) {
                        nearestShop?.name?.let { photoRepository.getLatestPhoto(it) }
                    }
                    val photoBitmap = remember(photoRefresh, latestPhoto?.file?.absolutePath) {
                        latestPhoto?.let { photoRepository.loadBitmap(it) }
                    }
                    NearestShopSection(
                        shop = nearestShop,
                        isNearby = isNearestNearby,
                        photoBitmap = photoBitmap,
                        onCameraClick = { name ->
                            if (!isNearestNearby || name != nearestShop?.name) {
                                photoMessage = "店舗に近づくと写真を撮れます"
                                return@NearestShopSection
                            }
                            pendingShopName = name
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraShopName = name
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onGalleryClick = { name ->
                            galleryTargetShop = name
                            galleryPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.section))
                }

                item {
                    AnimatedVisibility(
                        visible = photoMessage != null,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(160))
                    ) {
                        photoMessage?.let { message ->
                            GlassSurface(
                                modifier = Modifier.padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
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
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    FilterSection(
                        prefectures = prefectures,
                        selectedPrefecture = selectedPrefecture,
                        onPrefectureSelected = { selectedPrefecture = it },
                        filterMode = filterMode,
                        onFilterModeChanged = { filterMode = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "店舗一覧",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${ranking.size} 店舗",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (ranking.isEmpty()) {
                    item {
                        GlassSurface(
                            modifier = Modifier.padding(top = 12.dp),
                            shape = AppShapes.card,
                            level = GlassLevel.Thin,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "条件に一致する店舗がありません",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "表示や都道府県のフィルターを変えてみてください",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = ranking,
                        key = { _, item -> item.first }
                    ) { index, item ->
                        val (name, count, _) = item
                        val latestPhoto = remember(photoRefresh, name) {
                            photoRepository.getLatestPhoto(name)
                        }
                        val photoBitmap = remember(photoRefresh, latestPhoto?.file?.absolutePath) {
                            latestPhoto?.let { photoRepository.loadBitmap(it) }
                        }
                        val distanceToShop = currentCoordinates?.let { loc ->
                            shopCoordinatesMap[name]?.let { coords -> calculateDistanceMeters(loc, coords) }
                        }
                        val isNearby = distanceToShop != null && distanceToShop <= KOKO_RADIUS_METERS

                        ShopListRow(
                            name = name,
                            count = count,
                            distance = distanceToShop,
                            isNearby = isNearby,
                            photoBitmap = photoBitmap,
                            showDivider = index != ranking.lastIndex,
                            onCameraClick = {
                                if (!isNearby) {
                                    photoMessage = "$name は現在の位置から遠いため、写真を撮れません"
                                    return@ShopListRow
                                }
                                pendingShopName = name
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    cameraShopName = name
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onGalleryClick = {
                                galleryTargetShop = name
                                galleryPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPanel(
    visitedCount: Int,
    totalCount: Int,
    progress: Float,
    totalCheckIns: Int
) {
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (reduceMotion) 0 else 420, easing = FastOutSlowInEasing),
        label = "stampProgress"
    )

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card,
        level = GlassLevel.Regular,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "訪問済み",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$visitedCount",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = liquidGlassTextAccent()
                )
                Text(
                    text = " / $totalCount 店舗",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "総チェックイン $totalCheckIns 回",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NearestShopSection(
    shop: ShopInfo?,
    isNearby: Boolean,
    photoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onCameraClick: (String) -> Unit,
    onGalleryClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = "いま一番近い店舗")
        Spacer(modifier = Modifier.height(10.dp))

        if (shop == null) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.card,
                level = GlassLevel.Thin,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "現在地を取得中…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(AppShapes.card)
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = "${shop.name} で撮影した写真",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "訪れたら写真を残そう",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDistanceForStamp(shop.distanceMeters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isNearby) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "写真の撮影は店舗の近くでできます",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            GlassIconButton(
                icon = Icons.Outlined.PhotoLibrary,
                contentDescription = "ギャラリーから写真を選ぶ",
                onClick = { onGalleryClick(shop.name) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassIconButton(
                icon = Icons.Outlined.AddAPhoto,
                contentDescription = "写真を撮る",
                onClick = { onCameraClick(shop.name) },
                tint = if (isNearby) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                }
            )
        }
    }
}

@Composable
private fun FilterSection(
    prefectures: List<String>,
    selectedPrefecture: String?,
    onPrefectureSelected: (String?) -> Unit,
    filterMode: StampFilterMode,
    onFilterModeChanged: (StampFilterMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "表示")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StampFilterMode.entries.forEach { mode ->
                FilterPill(
                    label = mode.label,
                    selected = filterMode == mode,
                    onClick = { onFilterModeChanged(mode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        SectionLabel(text = "都道府県")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill(
                label = "すべて",
                selected = selectedPrefecture == null,
                onClick = { onPrefectureSelected(null) }
            )
            prefectures.forEach { pref ->
                FilterPill(
                    label = pref,
                    selected = selectedPrefecture == pref,
                    onClick = { onPrefectureSelected(pref) }
                )
            }
        }
    }
}

@Composable
private fun ShopListRow(
    name: String,
    count: Int,
    distance: Float?,
    isNearby: Boolean,
    photoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    showDivider: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val isVisited = count > 0

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(AppShapes.thumbnail)
            ) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        MaterialTheme.colorScheme.surfaceContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (isVisited) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "訪問済み",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isVisited) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = distance?.let { formatDistanceForStamp(it) } ?: "--",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isVisited) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$count 回",
                            style = MaterialTheme.typography.labelSmall,
                            color = liquidGlassTextAccent()
                        )
                    }
                }
            }

            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = "$name のギャラリーから選ぶ",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddAPhoto,
                    contentDescription = "$name で写真を撮る",
                    tint = if (isNearby) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            Hairline(modifier = Modifier.padding(start = 74.dp))
        }
    }
}

private fun formatDistanceForStamp(meters: Float): String = when {
    meters < 1000f -> "${meters.toInt()} m"
    else -> "%.1f km".format(meters / 1000f)
}
