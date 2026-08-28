package win.haya.doko

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp

private const val IMAGE_TOP_OFFSET_DEGREES = 0f
private const val SHARE_URL = "https://koba9813.github.io/yamaokaya/"

@Composable
internal fun YamaokayaScreen(
    viewModel: YamaokayaViewModel
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current

    val appSettings by viewModel.appSettings
    val nearestShop by viewModel.nearestShop
    val headingDegrees by viewModel.headingDegrees
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    val stampRepository = remember { StampRepository(context) }

    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showInfoPage by remember { mutableStateOf(false) }
    var showStampRally by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var checkInFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val imageNames = remember {
        listOf("yamaokaya", "gyoza", "miso", "shio", "tokusei_miso", "kara_miso")
    }
    val imageDrawableIds = remember(imageNames) {
        imageNames.map { resolveDrawableId(context, it) }.filter { it != 0 }
    }
    val currentDrawableId = remember(imageDrawableIds, selectedImageIndex) {
        if (imageDrawableIds.isNotEmpty()) imageDrawableIds[selectedImageIndex % imageDrawableIds.size] else 0
    }

    val shopNames = remember { YamaokayaFinder.getShopNames() }

    val isWithinRadius = nearestShop?.distanceMeters?.let { it <= KOKO_RADIUS_METERS } == true
    val isSpecialRange = nearestShop?.distanceMeters?.let { it <= SPECIAL_EFFECT_RADIUS_METERS } == true

    // Sub-page navigation
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
        val hasPermission = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasPermission) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Text(
            text = if (isWithinRadius) "Yamaokaya is Koko!!!" else "Yamaokaya is Doko",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        VerticalActionButtons(
            onStampRally = { showStampRally = true },
            onShare = { showShareDialog = true },
            onInfo = { showInfoPage = true },
            onSettings = { showSettings = true },
            shareEnabled = nearestShop != null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp)
        )

        if (isSpecialRange) {
            ChaosRamenStorm(
                imageDrawableIds = imageDrawableIds,
                modifier = Modifier.fillMaxSize()
            )
        }

        when {
            !hasLocationPermission(context) -> {
                PermissionRequestView(
                    modifier = Modifier.align(Alignment.Center),
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
                LoadingView(modifier = Modifier.align(Alignment.Center))
            }

            errorMessage != null -> {
                ErrorView(
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
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            nearestShop != null -> {
                val shop = nearestShop!!
                val arrowRotation = normalizeDegrees(
                    shop.bearingDegrees.toFloat() - headingDegrees + IMAGE_TOP_OFFSET_DEGREES
                )

                MainContent(
                    shop = shop,
                    currentDrawableId = currentDrawableId,
                    arrowRotation = arrowRotation,
                    isSpecialRange = isSpecialRange,
                    isWithinRadius = isWithinRadius,
                    onImageClick = {
                        if (imageDrawableIds.size > 1) {
                            selectedImageIndex = (selectedImageIndex + 1) % imageDrawableIds.size
                        }
                    },
                    onCheckIn = { shopName ->
                        val checkedIn = stampRepository.checkIn(shopName)
                        if (checkedIn) {
                            checkInFeedbackMessage = null
                        } else {
                            val remainMs = stampRepository.getRemainingCooldownMillis(shopName)
                            val totalMinutes = (remainMs + 59_999L) / 60_000L
                            val hours = totalMinutes / 60
                            val minutes = totalMinutes % 60
                            checkInFeedbackMessage = if (hours > 0) {
                                "同じ店舗のチェックインは3時間に1回までです（あと${hours}時間${minutes}分）"
                            } else {
                                "同じ店舗のチェックインは3時間に1回までです（あと${minutes}分）"
                            }
                        }
                    },
                    checkInFeedbackMessage = checkInFeedbackMessage,
                    onOpenMenu = { openMenuInAppBrowser(context) },
                    stampRepository = stampRepository,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Footer(
            appVersion = appVersion,
            uriHandler = uriHandler,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showShareDialog) {
            ShareDialog(
                shop = nearestShop,
                context = context,
                view = view,
                uriHandler = uriHandler,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}

@Composable
private fun VerticalActionButtons(
    onStampRally: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onSettings: () -> Unit,
    shareEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        CircleIconButton(onClick = onStampRally, contentDescription = "スタンプラリー", icon = Icons.Filled.Star)
        if (shareEnabled) {
            CircleIconButton(onClick = onShare, contentDescription = "シェアする", icon = Icons.Filled.Share)
        }
        CircleIconButton(onClick = onInfo, contentDescription = "インフォメーション", icon = Icons.Filled.Info)
        CircleIconButton(onClick = onSettings, contentDescription = "設定", icon = Icons.Filled.Settings)
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PermissionRequestView(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("位置情報を使って最寄り店舗を検索します。")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequestPermission) {
            Text("位置情報を許可して検索")
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator()
        Text("検索中...")
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("エラー: $message")
        Button(onClick = onRetry) {
            Text("再試行")
        }
    }
}

@Composable
private fun MainContent(
    shop: ShopInfo,
    currentDrawableId: Int,
    arrowRotation: Float,
    isSpecialRange: Boolean,
    isWithinRadius: Boolean,
    onImageClick: () -> Unit,
    onCheckIn: (String) -> Unit,
    checkInFeedbackMessage: String?,
    onOpenMenu: () -> Unit,
    stampRepository: StampRepository,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSpecialRange) {
            SpecialRangeCard(shop = shop)
        } else {
            DistanceText(shop = shop)
            Spacer(modifier = Modifier.height(16.dp))
            if (currentDrawableId != 0) {
                DirectionImage(
                    drawableId = currentDrawableId,
                    rotation = arrowRotation,
                    onClick = onImageClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isCheckedIn = stampRepository.getRemainingCooldownMillis(shop.name) > 0L
        val buttonModifier = if (isSpecialRange) {
            Modifier.fillMaxWidth().height(56.dp)
        } else {
            Modifier
        }

        if (isCheckedIn) {
            Button(onClick = onOpenMenu, modifier = buttonModifier) {
                Text("メニューを見る")
            }
        } else {
            Button(
                onClick = { onCheckIn(shop.name) },
                modifier = buttonModifier
            ) {
                Text("チェックイン")
            }
            if (checkInFeedbackMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = checkInFeedbackMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DistanceText(shop: ShopInfo) {
    val text = if (shop.distanceMeters < 1000f) {
        "${"%.0f".format(shop.distanceMeters)} m"
    } else {
        "${"%.2f".format(shop.distanceMeters / 1000f)} km"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun DirectionImage(
    drawableId: Int,
    rotation: Float,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = "山岡家の方向",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(280.dp)
            .graphicsLayer { rotationZ = rotation }
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SpecialRangeCard(shop: ShopInfo) {
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
}

@Composable
private fun Footer(
    appVersion: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            Text("(c) 2026 ", style = MaterialTheme.typography.labelMedium)
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

@Composable
private fun ShareDialog(
    shop: ShopInfo?,
    context: android.content.Context,
    view: android.view.View,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onDismiss: () -> Unit
) {
    val shareMessage = createShareMessage(shop)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("共有する") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDismiss()
                        shareToLine(context, view, uriHandler, shareMessage, SHARE_URL)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("LINEで共有") }
                Button(
                    onClick = {
                        onDismiss()
                        shareToTwitter(context, uriHandler, view, shareMessage, SHARE_URL)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("X (Twitter) で共有") }
                Button(
                    onClick = {
                        onDismiss()
                        shareToInstagram(context, view, shareMessage, SHARE_URL)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Instagramで共有") }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
