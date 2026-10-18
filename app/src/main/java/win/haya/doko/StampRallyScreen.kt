package win.haya.doko

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

private enum class StampFilterMode(val label: String) {
    ALL("全店舗"),
    VISITED("訪問済み"),
    UNVISITED("未訪問")
}

@OptIn(ExperimentalMaterial3Api::class)
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
                val lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastLocation?.let {
                    // 注: 実際の現在位置は viewModel から渡される currentCoordinates を主とする
                }
            } catch (_: SecurityException) { }
        }
    }

    var pendingShopName by remember { mutableStateOf<String?>(null) }
    var photoRefresh by remember { mutableIntStateOf(0) }
    var cameraShopName by remember { mutableStateOf<String?>(null) }
    var galleryTargetShop by remember { mutableStateOf<String?>(null) }
    var photoMessage by remember { mutableStateOf<String?>(null) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "スタンプラリー",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "閉じる"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenGallery) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "ギャラリーを開く"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProgressCard(
                    visitedCount = visitedCount,
                    totalCount = filteredNames.size,
                    progress = progress,
                    totalCheckIns = ranking.sumOf { it.second }
                )
            }

            item {
                NearestShopCard(
                    shop = nearestShop,
                    isNearby = isNearestNearby,
                    photoRepository = photoRepository,
                    photoRefresh = photoRefresh,
                    onCameraClick = { name ->
                        if (!isNearestNearby || name != nearestShop?.name) {
                            photoMessage = "店舗に近づくと写真を撮れます"
                            return@NearestShopCard
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
            }

            item {
                AnimatedVisibility(
                    visible = photoMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    photoMessage?.let { message ->
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

            item {
                FilterSection(
                    prefectures = prefectures,
                    selectedPrefecture = selectedPrefecture,
                    onPrefectureSelected = { selectedPrefecture = it },
                    filterMode = filterMode,
                    onFilterModeChanged = { filterMode = it }
                )
            }

            items(ranking, key = { it.first }) { (name, count, distance) ->
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

                ShopRowCard(
                    name = name,
                    count = count,
                    distance = distanceToShop,
                    isNearby = isNearby,
                    photoBitmap = photoBitmap,
                    onCameraClick = {
                        if (!isNearby) {
                            photoMessage = "$name は現在の位置から遠いため、写真を撮れません"
                            return@ShopRowCard
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

@Composable
private fun ProgressCard(
    visitedCount: Int,
    totalCount: Int,
    progress: Float,
    totalCheckIns: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$visitedCount",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "/ $totalCount 店舗",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            Text(
                text = "総チェックイン: ${totalCheckIns} 回",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun NearestShopCard(
    shop: ShopInfo?,
    isNearby: Boolean,
    photoRepository: PhotoRepository,
    photoRefresh: Int,
    onCameraClick: (String) -> Unit,
    onGalleryClick: (String) -> Unit
) {
    val latestPhoto = remember(photoRefresh, shop?.name) {
        shop?.name?.let { photoRepository.getLatestPhoto(it) }
    }
    val photoBitmap = remember(photoRefresh, latestPhoto?.file?.absolutePath) {
        latestPhoto?.let { photoRepository.loadBitmap(it) }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (shop != null) "いま一番近い店舗" else "現在地を取得中…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            if (shop != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDistanceForStamp(shop.distanceMeters),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onGalleryClick(shop.name) }) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = "ギャラリーから選ぶ",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        IconButton(
                            onClick = { onCameraClick(shop.name) },
                            enabled = isNearby
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "写真を撮る",
                                tint = if (isNearby) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
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
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "表示",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StampFilterMode.entries.forEach { mode ->
                FilterChip(
                    selected = filterMode == mode,
                    onClick = { onFilterModeChanged(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        Text(
            text = "都道府県",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPrefecture == null,
                onClick = { onPrefectureSelected(null) },
                label = { Text("全て") }
            )
            prefectures.forEach { pref ->
                FilterChip(
                    selected = selectedPrefecture == pref,
                    onClick = { onPrefectureSelected(pref) },
                    label = { Text(pref) }
                )
            }
        }
    }
}

@Composable
private fun ShopRowCard(
    name: String,
    count: Int,
    distance: Float?,
    isNearby: Boolean,
    photoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val isVisited = count > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isVisited) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distance?.let { formatDistanceForStamp(it) } ?: "--",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isVisited) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${count}回チェックイン",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onGalleryClick) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "$name のギャラリーから選ぶ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCameraClick) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = "$name で写真を撮る",
                        tint = if (isNearby) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    }
}

private fun formatDistanceForStamp(meters: Float): String = when {
    meters < 1000f -> "${meters.toInt()} m"
    else -> "%.1f km".format(meters / 1000f)
}


