package win.haya.doko

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class GallerySort(val label: String) {
    DATE("日付順"),
    SHOP("店舗順")
}

private data class PhotoSection(
    val dateLabel: String,
    val entries: List<PhotoEntry>
)

private val headerDateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.JAPAN)
private val sameYearDateFormat = SimpleDateFormat("M月d日 EEEE", Locale.JAPAN)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GalleryScreen(
    photoRepository: PhotoRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    var sortBy by remember { mutableStateOf(GallerySort.DATE) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedShop by remember { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDeleteAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { refreshKey++ }

    val allPhotos = remember(refreshKey, sortBy) {
        when (sortBy) {
            GallerySort.DATE -> photoRepository.loadAllPhotos()
            GallerySort.SHOP -> photoRepository.loadAllPhotos()
                .sortedWith(compareBy({ it.shopName }, { -it.timestampMillis }))
        }
    }

    val shopNames = remember(allPhotos) {
        allPhotos.map { it.shopName }.distinct().sorted()
    }

    val filteredPhotos = remember(allPhotos, searchQuery, selectedShop) {
        allPhotos.filter { entry ->
            (searchQuery.isBlank() || entry.shopName.contains(searchQuery, ignoreCase = true)) &&
                (selectedShop == null || entry.shopName == selectedShop)
        }
    }

    val sections = remember(filteredPhotos, sortBy) {
        if (sortBy == GallerySort.DATE) {
            filteredPhotos.groupBy { getDayStartMillis(it.timestampMillis) }
                .toSortedMap(reverseOrder())
                .map { (dayStart, entries) ->
                    PhotoSection(
                        dateLabel = formatSectionDate(dayStart),
                        entries = entries.sortedByDescending { it.timestampMillis }
                    )
                }
        } else {
            filteredPhotos.groupBy { it.shopName }
                .map { (shopName, entries) ->
                    PhotoSection(
                        dateLabel = shopName,
                        entries = entries.sortedByDescending { it.timestampMillis }
                    )
                }
        }
    }

    if (selectedIndex >= 0 && selectedIndex < filteredPhotos.size) {
        PhotoViewerDialog(
            allPhotos = filteredPhotos,
            initialIndex = selectedIndex,
            photoRepository = photoRepository,
            onDismiss = { selectedIndex = -1 },
            onShare = { entry, target -> sharePhoto(context, entry, target) },
            onDeleted = { refreshKey++; selectedIndex = -1 }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; pendingDeleteAll = false },
            title = { Text(if (pendingDeleteAll) "選択した写真を削除" else "写真を削除") },
            text = {
                Text(
                    if (pendingDeleteAll) "${selected.value.size}枚の写真を削除しますか？"
                    else "この写真を削除しますか？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pendingDeleteAll) {
                        selected.value.forEach { path ->
                            File(path).delete()
                        }
                        selected.value = emptySet()
                        selectionMode = false
                    }
                    refreshKey++
                    showDeleteConfirm = false
                    pendingDeleteAll = false
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; pendingDeleteAll = false }) { Text("キャンセル") }
            }
        )
    }

    ScreenEnter(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = screenContainerColor(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopBar(
                    title = if (selectionMode) "${selected.value.size}枚選択中" else "ギャラリー",
                    onBack = {
                        if (selectionMode) {
                            selectionMode = false
                            selected.value = emptySet()
                        } else {
                            onClose()
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            GlassIconButton(
                                icon = Icons.Outlined.Delete,
                                contentDescription = "選択した写真を削除",
                                onClick = {
                                    if (selected.value.isNotEmpty()) {
                                        pendingDeleteAll = true
                                        showDeleteConfirm = true
                                    }
                                },
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                if (!selectionMode) {
                    GalleryFilters(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        sortBy = sortBy,
                        onSortChange = { sortBy = it },
                        selectedShop = selectedShop,
                        onShopSelected = { selectedShop = it },
                        shopNames = shopNames,
                        resultCount = filteredPhotos.size
                    )
                }

                if (filteredPhotos.isEmpty()) {
                    EmptyGallery(
                        allPhotosEmpty = allPhotos.isEmpty(),
                        hasActiveFilter = searchQuery.isNotBlank() || selectedShop != null,
                        searchQuery = searchQuery,
                        onClearFilters = {
                            searchQuery = ""
                            selectedShop = null
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = if (selectionMode) 12.dp else 4.dp,
                            bottom = 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sections.forEach { section ->
                            item(
                                key = "header_${section.dateLabel}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                DateHeader(label = section.dateLabel)
                            }
                            items(
                                items = section.entries,
                                key = { it.file.absolutePath }
                            ) { entry ->
                                val index = filteredPhotos.indexOf(entry)
                                GalleryThumbnail(
                                    entry = entry,
                                    isSelected = selected.value.contains(entry.file.absolutePath),
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) {
                                            val set = selected.value.toMutableSet()
                                            if (set.contains(entry.file.absolutePath)) {
                                                set.remove(entry.file.absolutePath)
                                            } else {
                                                set.add(entry.file.absolutePath)
                                            }
                                            selected.value = set
                                        } else {
                                            selectedIndex = index
                                        }
                                    },
                                    onLongClick = {
                                        selectionMode = true
                                        selected.value = setOf(entry.file.absolutePath)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: GallerySort,
    onSortChange: (GallerySort) -> Unit,
    selectedShop: String?,
    onShopSelected: (String?) -> Unit,
    shopNames: List<String>,
    resultCount: Int
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        GlassSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "店舗名で検索"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GallerySort.entries.forEach { sort ->
                    FilterPill(
                        label = sort.label,
                        selected = sortBy == sort,
                        onClick = { onSortChange(sort) }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$resultCount 枚",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (shopNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = "すべて",
                    selected = selectedShop == null,
                    onClick = { onShopSelected(null) }
                )
                shopNames.take(6).forEach { shop ->
                    FilterPill(
                        label = shop,
                        selected = selectedShop == shop,
                        onClick = {
                            onShopSelected(if (selectedShop == shop) null else shop)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun EmptyGallery(
    allPhotosEmpty: Boolean,
    hasActiveFilter: Boolean,
    searchQuery: String,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlassSurface(
                modifier = Modifier.size(78.dp),
                shape = CircleShape,
                level = GlassLevel.Thin,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (allPhotosEmpty) "まだ写真がありません" else "条件に一致する写真がありません",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    allPhotosEmpty -> "店舗で撮影した写真が、ここに日付ごとに並びます。"
                    searchQuery.isNotBlank() -> "「$searchQuery」に一致する写真は見つかりませんでした。"
                    else -> "店舗のフィルターを変えると、他の写真が見つかるかもしれません。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (hasActiveFilter) {
                Spacer(modifier = Modifier.height(18.dp))
                GlassPillButton(
                    text = "検索条件をクリア",
                    onClick = onClearFilters,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun DateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryThumbnail(
    entry: PhotoEntry,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AsyncImage(
            model = entry.file,
            contentDescription = "${entry.shopName} で撮影した写真",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(30.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.46f))
                    )
                )
        )
        Text(
            text = entry.shopName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color.Black.copy(alpha = 0.30f) else Color.Transparent)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.30f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "選択中",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoViewerDialog(
    allPhotos: List<PhotoEntry>,
    initialIndex: Int,
    photoRepository: PhotoRepository,
    onDismiss: () -> Unit,
    onShare: (PhotoEntry, ShareTarget) -> Unit,
    onDeleted: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { allPhotos.size }
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val currentEntry = allPhotos.getOrNull(currentPage)

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1
            ) { page ->
                val entry = allPhotos[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = entry.file,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            // 上端のスクリムとヘッダー
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewerGlassIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "閉じる",
                    onClick = onDismiss
                )
                Spacer(modifier = Modifier.weight(1f))
                currentEntry?.let { entry ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = entry.shopName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = photoRepository.formatDate(entry),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = "${currentPage + 1}/${allPhotos.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            // 下端のスクリムと操作バー
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )
            currentEntry?.let { entry ->
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerActionButton(label = "Instagram") { onShare(entry, ShareTarget.INSTAGRAM) }
                    ViewerActionButton(label = "LINE") { onShare(entry, ShareTarget.LINE) }
                    ViewerActionButton(label = "X") { onShare(entry, ShareTarget.X) }
                    IconButton(onClick = { onShare(entry, ShareTarget.OTHER) }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "その他",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = {
                        entry.file.delete()
                        if (allPhotos.size <= 1) onDismiss()
                        onDeleted()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "削除",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerGlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.13f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun ViewerActionButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private enum class ShareTarget { INSTAGRAM, LINE, X, OTHER }

private fun getDayStartMillis(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatSectionDate(dayStart: Long): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
    return if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        sameYearDateFormat.format(cal.time)
    } else {
        headerDateFormat.format(cal.time)
    }
}

private fun sharePhoto(context: Context, entry: PhotoEntry, target: ShareTarget) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        entry.file
    )
    val text = "${entry.shopName} で山岡家を楽しんだよ #YamaokayaIsDoko"

    val intent = when (target) {
        ShareTarget.INSTAGRAM -> Intent(Intent.ACTION_SEND).apply {
            type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); `package` = "com.instagram.android"
        }
        ShareTarget.LINE -> Intent(Intent.ACTION_SEND).apply {
            type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); `package` = "jp.naver.line.android"
        }
        ShareTarget.X -> Intent(Intent.ACTION_SEND).apply {
            type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); `package` = "com.twitter.android"
        }
        ShareTarget.OTHER -> Intent(Intent.ACTION_SEND).apply {
            type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text)
        }
    }.apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }

    val finalIntent = if (target == ShareTarget.OTHER) Intent.createChooser(intent, "写真を共有") else intent

    try {
        if (context !is Activity) finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(finalIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "アプリが見つかりませんでした", Toast.LENGTH_SHORT).show()
    }
}
