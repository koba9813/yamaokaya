package win.haya.doko

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
                    } else {
                        // single delete handled in viewer; here for safety
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text(
                            text = "${selected.value.size}枚選択中",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "ギャラリー",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectionMode) {
                            selectionMode = false
                            selected.value = emptySet()
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(
                            imageVector = if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "閉じる"
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                if (selected.value.isNotEmpty()) {
                                    pendingDeleteAll = true
                                    showDeleteConfirm = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        // sort chips
                        GallerySort.entries.forEach { sort ->
                            FilterChip(
                                selected = sortBy == sort,
                                onClick = { sortBy = sort },
                                label = { Text(sort.label) },
                                modifier = Modifier.padding(end = if (sort == GallerySort.SHOP) 8.dp else 4.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Search + filter row
            if (!selectionMode) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("店舗名で検索") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedShop == null,
                            onClick = { selectedShop = null },
                            label = { Text("すべて") }
                        )
                        // show up to a few shop chips
                        shopNames.take(6).forEach { shop ->
                            FilterChip(
                                selected = selectedShop == shop,
                                onClick = { selectedShop = if (selectedShop == shop) null else shop },
                                label = { Text(shop, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }

            if (filteredPhotos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allPhotos.isEmpty()) "まだ写真がありません\n店舗で撮影するとここに表示されます"
                        else "条件に一致する写真がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                        if (set.contains(entry.file.absolutePath)) set.remove(entry.file.absolutePath)
                                        else set.add(entry.file.absolutePath)
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

@Composable
private fun DateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp)
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = entry.file,
            contentDescription = "${entry.shopName} で撮影した写真",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // selection overlay
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "選択中",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                )
            }
        }

        // shop name chip at bottom
        Text(
            text = entry.shopName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// long-press handling via Modifier.combinedClickable would be cleaner; add import for combinedClickable
// We'll instead use Modifier.pointerInput for long press detection below by redefining thumbnail? 
// To keep simple, use combinedClickable. Let's adjust: use androidx.compose.foundation.combinedClickable

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
                .statusBarsPadding()
                .navigationBarsPadding()
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

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color.White)
                    }
                    currentEntry?.let { entry ->
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = entry.shopName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = photoRepository.formatDate(entry),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                currentEntry?.let { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ViewerActionButton(label = "Instagram") { onShare(entry, ShareTarget.INSTAGRAM) }
                        ViewerActionButton(label = "LINE") { onShare(entry, ShareTarget.LINE) }
                        ViewerActionButton(label = "X") { onShare(entry, ShareTarget.X) }
                        IconButton(onClick = { onShare(entry, ShareTarget.OTHER) }) {
                            Icon(Icons.Default.Share, contentDescription = "その他", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = {
                            entry.file.delete()
                            if (allPhotos.size <= 1) onDismiss()
                            onDeleted()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerActionButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
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