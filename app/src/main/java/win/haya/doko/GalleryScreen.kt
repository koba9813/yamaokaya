package win.haya.doko

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class GalleryView(val label: String) {
    PHOTOS("写真"),
    SHOPS("店舗"),
    FAVORITES("お気に入り")
}

private enum class GallerySort(val label: String) {
    DATE("日付順"),
    SHOP("店舗順")
}

private data class PhotoSection(
    val header: String,
    val monthKey: String?,
    val entries: List<PhotoEntry>
)

private data class ShopAlbum(
    val shopName: String,
    val count: Int,
    val cover: PhotoEntry
)

private data class ViewerState(
    val photos: List<PhotoEntry>,
    val initialIndex: Int
)

private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.JAPAN)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun GalleryScreen(
    photoRepository: PhotoRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    var view by remember { mutableStateOf(GalleryView.PHOTOS) }
    var sortBy by remember { mutableStateOf(GallerySort.DATE) }
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var selectedShop by remember { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var favoritesVersion by remember { mutableIntStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var viewerState by remember { mutableStateOf<ViewerState?>(null) }

    val allPhotos = remember(refreshKey) { photoRepository.loadAllPhotos() }
    val favoriteIds = remember(favoritesVersion, refreshKey) { photoRepository.favoriteIds() }

    val filteredPhotos = remember(allPhotos, searchQuery, selectedShop) {
        allPhotos.filter { entry ->
            (searchQuery.isBlank() || entry.shopName.contains(searchQuery, ignoreCase = true)) &&
                (selectedShop == null || entry.shopName == selectedShop)
        }
    }

    val sections = remember(filteredPhotos, sortBy) { buildSections(filteredPhotos, sortBy) }
    val displayedPhotos = remember(sections) { sections.flatMap { it.entries } }

    val shopAlbums = remember(allPhotos) {
        allPhotos.groupBy { it.shopName }
            .map { (shop, entries) ->
                ShopAlbum(shop, entries.size, entries.maxByOrNull { it.timestampMillis }!!)
            }
            .sortedByDescending { it.cover.timestampMillis }
    }

    val favoriteEntries = remember(allPhotos, favoriteIds) {
        allPhotos.filter { favoriteIds.contains(favoriteIdOf(it)) }
            .sortedByDescending { it.timestampMillis }
    }

    viewerState?.let { state ->
        PhotoViewerDialog(
            photos = state.photos,
            initialIndex = state.initialIndex,
            photoRepository = photoRepository,
            favoriteIds = favoriteIds,
            onToggleFavorite = { entry ->
                photoRepository.setFavorite(entry, !favoriteIds.contains(favoriteIdOf(entry)))
                favoritesVersion++
            },
            onShare = { entry, target -> sharePhoto(context, entry, target) },
            onDismiss = { viewerState = null },
            onDeleted = { entry ->
                photoRepository.setFavorite(entry, false)
                favoritesVersion++
                refreshKey++
                viewerState = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("選択した写真を削除") },
            text = { Text("${selected.value.size}枚の写真を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    selected.value.forEach { path ->
                        val file = File(path)
                        file.delete()
                    }
                    selected.value = emptySet()
                    selectionMode = false
                    refreshKey++
                    showDeleteConfirm = false
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        if (selectionMode) {
            SelectionTopBar(
                count = selected.value.size,
                onCancel = {
                    selectionMode = false
                    selected.value = emptySet()
                },
                onDelete = {
                    if (selected.value.isNotEmpty()) showDeleteConfirm = true
                }
            )
            PhotoGrid(
                sections = sections,
                favoriteIds = favoriteIds,
                selectionMode = true,
                selectedPaths = selected.value,
                onPhotoClick = { entry ->
                    val set = selected.value.toMutableSet()
                    if (set.contains(entry.file.absolutePath)) set.remove(entry.file.absolutePath)
                    else set.add(entry.file.absolutePath)
                    selected.value = set
                },
                onPhotoLongClick = {}
            )
        } else {
            GalleryHeader(
                sortBy = sortBy,
                showSort = view == GalleryView.PHOTOS,
                sortMenuOpen = sortMenuOpen,
                onSortMenuToggle = { sortMenuOpen = !sortMenuOpen },
                onSortChange = { sortBy = it; sortMenuOpen = false },
                onSearchToggle = { searchExpanded = !searchExpanded },
                onClose = onClose
            )
            ViewTabs(view = view, onViewChange = { view = it })

            AnimatedContent(
                targetState = view,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "galleryView",
                modifier = Modifier.weight(1f)
            ) { target ->
                when (target) {
                    GalleryView.PHOTOS -> PhotosContent(
                        sections = sections,
                        hasAnyPhotos = allPhotos.isNotEmpty(),
                        favoriteIds = favoriteIds,
                        searchExpanded = searchExpanded,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        sortBy = sortBy,
                        selectedShop = selectedShop,
                        onClearShop = { selectedShop = null },
                        onPhotoClick = { entry ->
                            viewerState = ViewerState(displayedPhotos, displayedPhotos.indexOf(entry))
                        },
                        onPhotoLongClick = { entry ->
                            selectionMode = true
                            selected.value = setOf(entry.file.absolutePath)
                        }
                    )
                    GalleryView.SHOPS -> ShopsContent(
                        albums = shopAlbums,
                        onShopClick = { shop ->
                            selectedShop = shop
                            searchQuery = ""
                            view = GalleryView.PHOTOS
                        }
                    )
                    GalleryView.FAVORITES -> FavoritesContent(
                        entries = favoriteEntries,
                        favoriteIds = favoriteIds,
                        onPhotoClick = { entry ->
                            viewerState = ViewerState(favoriteEntries, favoriteEntries.indexOf(entry))
                        },
                        onOpenPhotos = { view = GalleryView.PHOTOS }
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryHeader(
    sortBy: GallerySort,
    showSort: Boolean,
    sortMenuOpen: Boolean,
    onSortMenuToggle: () -> Unit,
    onSortChange: (GallerySort) -> Unit,
    onSearchToggle: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "閉じる")
        }
        Text(
            text = "ギャラリー",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        if (showSort) {
            Box {
                IconButton(onClick = onSortMenuToggle) {
                    Icon(Icons.Default.MoreVert, contentDescription = "並び替え")
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = onSortMenuToggle) {
                    GallerySort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label) },
                            onClick = { onSortChange(sort) },
                            leadingIcon = {
                                if (sortBy == sort) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onSearchToggle) {
                Icon(Icons.Default.Search, contentDescription = "検索")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTabs(view: GalleryView, onViewChange: (GalleryView) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GalleryView.entries.forEachIndexed { index, item ->
            SegmentedButton(
                selected = view == item,
                onClick = { onViewChange(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = GalleryView.entries.size
                )
            ) {
                Text(item.label, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PhotosContent(
    sections: List<PhotoSection>,
    hasAnyPhotos: Boolean,
    favoriteIds: Set<String>,
    searchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: GallerySort,
    selectedShop: String?,
    onClearShop: () -> Unit,
    onPhotoClick: (PhotoEntry) -> Unit,
    onPhotoLongClick: (PhotoEntry) -> Unit
) {
    val gridState = rememberLazyGridState()

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = searchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("店舗名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "クリア")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (selectedShop != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = onClearShop,
                    label = { Text("店舗: $selectedShop", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "絞り込みを解除", modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        if (sortBy == GallerySort.DATE && sections.size > 1) {
            MonthJumpBar(sections = sections, gridState = gridState)
        }

        if (sections.isEmpty()) {
            EmptyState(
                text = if (!hasAnyPhotos) {
                    "まだ写真がありません\n店舗で撮影するとここに表示されます"
                } else {
                    "条件に一致する写真がありません"
                }
            )
        } else {
            PhotoGrid(
                gridState = gridState,
                sections = sections,
                favoriteIds = favoriteIds,
                selectionMode = false,
                selectedPaths = emptySet(),
                onPhotoClick = onPhotoClick,
                onPhotoLongClick = onPhotoLongClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthJumpBar(sections: List<PhotoSection>, gridState: LazyGridState) {
    val scope = rememberCoroutineScope()
    val monthIndexMap = remember(sections) {
        val map = mutableMapOf<String, Int>()
        var index = 0
        sections.forEach { section ->
            section.monthKey?.let { map[it] = index }
            index += 1 + section.entries.size
        }
        map
    }
    val firstVisibleIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
    val activeMonth = remember(firstVisibleIndex, sections) {
        var index = 0
        var active: String? = null
        for (section in sections) {
            if (section.monthKey != null && firstVisibleIndex >= index) active = section.monthKey
            index += 1 + section.entries.size
        }
        active
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.mapNotNull { it.monthKey }.forEach { monthKey ->
            FilterChip(
                selected = monthKey == activeMonth,
                onClick = {
                    val target = monthIndexMap[monthKey] ?: 0
                    scope.launch { gridState.animateScrollToItem(target) }
                },
                label = { Text(monthChipLabel(monthKey)) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShopsContent(
    albums: List<ShopAlbum>,
    onShopClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState("まだ写真がありません\n店舗で撮影するとここに表示されます")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { it.shopName }) { album ->
            ShopAlbumCard(album = album, onClick = { onShopClick(album.shopName) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesContent(
    entries: List<PhotoEntry>,
    favoriteIds: Set<String>,
    onPhotoClick: (PhotoEntry) -> Unit,
    onOpenPhotos: () -> Unit
) {
    if (entries.isEmpty()) {
        EmptyState(
            text = "お気に入りはまだありません\n写真のハートをタップすると登録できます",
            actionLabel = "写真を見る",
            onAction = onOpenPhotos
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(entries, key = { it.file.absolutePath }) { entry ->
            PhotoTile(
                entry = entry,
                isFavorite = favoriteIds.contains(favoriteIdOf(entry)),
                isSelected = false,
                selectionMode = false,
                onClick = { onPhotoClick(entry) },
                onLongClick = {}
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    gridState: LazyGridState? = null,
    sections: List<PhotoSection>,
    favoriteIds: Set<String>,
    selectionMode: Boolean,
    selectedPaths: Set<String>,
    onPhotoClick: (PhotoEntry) -> Unit,
    onPhotoLongClick: (PhotoEntry) -> Unit
) {
    val state = gridState ?: rememberLazyGridState()
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        sections.forEach { section ->
            item(key = "header_${section.header}", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(section.header, section.entries.size)
            }
            items(section.entries, key = { it.file.absolutePath }) { entry ->
                PhotoTile(
                    entry = entry,
                    isFavorite = favoriteIds.contains(favoriteIdOf(entry)),
                    isSelected = selectedPaths.contains(entry.file.absolutePath),
                    selectionMode = selectionMode,
                    onClick = { onPhotoClick(entry) },
                    onLongClick = { onPhotoLongClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Text(
        text = "$label ・ ${count}枚",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    entry: PhotoEntry,
    isFavorite: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = entry.file,
            contentDescription = "${entry.shopName} で撮影した写真",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isFavorite && !selectionMode) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "お気に入り",
                tint = Color(0xFFFF5470),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }

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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShopAlbumCard(album: ShopAlbum, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = album.cover.file,
                contentDescription = "${album.shopName} の写真",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = album.shopName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.count}枚",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "キャンセル")
        }
        Text(
            text = "${count}枚選択中",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun EmptyState(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoViewerDialog(
    photos: List<PhotoEntry>,
    initialIndex: Int,
    photoRepository: PhotoRepository,
    favoriteIds: Set<String>,
    onToggleFavorite: (PhotoEntry) -> Unit,
    onShare: (PhotoEntry, ShareTarget) -> Unit,
    onDismiss: () -> Unit,
    onDeleted: (PhotoEntry) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val currentEntry = photos.getOrNull(currentPage)
    val isFavorite = currentEntry?.let { favoriteIds.contains(favoriteIdOf(it)) } == true

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showShareSheet by remember { mutableStateOf(false) }

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
                .windowInsetsPadding(WindowInsets.safeGestures.only(WindowInsetsSides.Bottom))
                .padding(bottom = 44.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1
            ) { page ->
                val entry = photos[page]
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

            // 上部バー
            currentEntry?.let { entry ->
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color.White)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.Start
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
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(entry) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "お気に入り",
                                tint = if (isFavorite) Color(0xFFFF5470) else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 下部バー
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showShareSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("共有", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            entry.file.delete()
                            onDeleted(entry)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.White)
                        }
                    }
                }
            }

            // 共有シート
            if (showShareSheet && currentEntry != null) {
                val entry = currentEntry!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showShareSheet = false }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .windowInsetsPadding(WindowInsets.safeGestures.only(WindowInsetsSides.Bottom))
                        .padding(bottom = 44.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "写真を共有",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    ShareOption("Instagram ストーリー") {
                        onShare(entry, ShareTarget.INSTAGRAM_STORY); showShareSheet = false
                    }
                    ShareOption("LINE") {
                        onShare(entry, ShareTarget.LINE); showShareSheet = false
                    }
                    ShareOption("X") {
                        onShare(entry, ShareTarget.X); showShareSheet = false
                    }
                    ShareOption("その他のアプリ") {
                        onShare(entry, ShareTarget.OTHER); showShareSheet = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareOption(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private enum class ShareTarget { INSTAGRAM_STORY, LINE, X, OTHER }

private fun favoriteIdOf(entry: PhotoEntry): String = "${entry.shopName}|${entry.timestampMillis}"

private fun buildSections(photos: List<PhotoEntry>, sort: GallerySort): List<PhotoSection> =
    if (sort == GallerySort.DATE) {
        photos.groupBy { monthKeyFormat.format(Date(it.timestampMillis)) }
            .toSortedMap(reverseOrder())
            .map { (monthKey, entries) ->
                PhotoSection(
                    header = monthHeaderLabel(monthKey),
                    monthKey = monthKey,
                    entries = entries.sortedByDescending { it.timestampMillis }
                )
            }
    } else {
        photos.groupBy { it.shopName }
            .map { (shop, entries) ->
                PhotoSection(
                    header = shop,
                    monthKey = null,
                    entries = entries.sortedByDescending { it.timestampMillis }
                )
            }
            .sortedBy { it.header }
    }

private fun monthHeaderLabel(monthKey: String): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return monthKey
    return "${year}年${month}月"
}

private fun monthChipLabel(monthKey: String): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return monthKey
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return if (year == currentYear) "${month}月" else "${year}年${month}月"
}

private fun sharePhoto(context: Context, entry: PhotoEntry, target: ShareTarget) {
    val text = "${entry.shopName} で山岡家を楽しんだよ #YamaokayaIsDoko"

    val intent: Intent = if (target == ShareTarget.INSTAGRAM_STORY) {
        // Instagram ストーリーは 9:16 にトリミングされるため、あらかじめ 9:16 の
        // キャンバス画像（写真＋透かし）を生成して共有する
        val storyFile = createStoryShareFile(context, entry.file) ?: entry.file
        val storyUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            storyFile
        )
        Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(storyUri, "image/*")
            putExtra(Intent.EXTRA_STREAM, storyUri)
            putExtra("source_application", context.packageName)
            `package` = "com.instagram.android"
        }
    } else {
        // その他の共有先には、元写真に透かしを入れた一時ファイルを使う（失敗時は元写真）
        val shareFile = createWatermarkedShareFile(context, entry.file) ?: entry.file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        when (target) {
            ShareTarget.LINE -> Intent(Intent.ACTION_SEND).apply {
                type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); `package` = "jp.naver.line.android"
            }
            ShareTarget.X -> Intent(Intent.ACTION_SEND).apply {
                type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); `package` = "com.twitter.android"
            }
            ShareTarget.OTHER -> Intent(Intent.ACTION_SEND).apply {
                type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text)
            }
            ShareTarget.INSTAGRAM_STORY -> error("unreachable")
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
