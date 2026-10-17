package win.haya.doko

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
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
private val yearMonthFormat = SimpleDateFormat("yyyy年M月", Locale.JAPAN)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun GalleryScreen(
    photoRepository: PhotoRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var sortBy by remember { mutableStateOf(GallerySort.DATE) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        refreshKey++
    }

    val allPhotos = remember(refreshKey, sortBy) {
        when (sortBy) {
            GallerySort.DATE -> photoRepository.loadAllPhotos()
            GallerySort.SHOP -> photoRepository.loadAllPhotos()
                .sortedWith(compareBy({ it.shopName }, { -it.timestampMillis }))
        }
    }

    val sections = remember(allPhotos, sortBy) {
        if (sortBy == GallerySort.DATE) {
            allPhotos.groupBy { getDayStartMillis(it.timestampMillis) }
                .toSortedMap(reverseOrder())
                .map { (dayStart, entries) ->
                    PhotoSection(
                        dateLabel = formatSectionDate(dayStart),
                        entries = entries.sortedByDescending { it.timestampMillis }
                    )
                }
        } else {
            allPhotos.groupBy { it.shopName }
                .map { (shopName, entries) ->
                    PhotoSection(
                        dateLabel = shopName,
                        entries = entries.sortedByDescending { it.timestampMillis }
                    )
                }
        }
    }

    if (selectedIndex >= 0 && selectedIndex < allPhotos.size) {
        PhotoViewerDialog(
            allPhotos = allPhotos,
            initialIndex = selectedIndex,
            photoRepository = photoRepository,
            onDismiss = { selectedIndex = -1 },
            onShare = { entry, target -> sharePhoto(context, entry, target) },
            onDeleted = { refreshKey++ }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ギャラリー",
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
                    GallerySort.entries.forEach { sort ->
                        FilterChip(
                            selected = sortBy == sort,
                            onClick = { sortBy = sort },
                            label = { Text(sort.label) },
                            modifier = Modifier.padding(end = if (sort == GallerySort.SHOP) 12.dp else 4.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (allPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "まだ写真がありません\n店舗で撮影するとここに表示されます",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                        val index = allPhotos.indexOf(entry)
                        val bitmap = remember(refreshKey, entry.file.absolutePath) {
                            photoRepository.loadBitmap(entry)
                        }
                        GalleryThumbnail(
                            entry = entry,
                            bitmap = bitmap,
                            onClick = { selectedIndex = index },
                            onShare = { sharePhoto(context, entry, ShareTarget.OTHER) }
                        )
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

@Composable
private fun GalleryThumbnail(
    entry: PhotoEntry,
    bitmap: ImageBitmap?,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "${entry.shopName} で撮影した写真",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = onShare,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(32.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "共有",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
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
                val bitmap = remember(entry.file.absolutePath) {
                    photoRepository.loadBitmap(entry)
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = Color.White
                        )
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
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "その他",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                entry.file.delete()
                                if (allPhotos.size <= 1) {
                                    onDismiss()
                                }
                                onDeleted()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

private enum class ShareTarget {
    INSTAGRAM, LINE, X, OTHER
}

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
        ShareTarget.INSTAGRAM -> {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.instagram.android"
            }
        }
        ShareTarget.LINE -> {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "jp.naver.line.android"
            }
        }
        ShareTarget.X -> {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.twitter.android"
            }
        }
        ShareTarget.OTHER -> {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
    }.apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val finalIntent = if (target == ShareTarget.OTHER) {
        Intent.createChooser(intent, "写真を共有")
    } else {
        intent
    }

    try {
        if (context !is Activity) {
            finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(finalIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "アプリが見つかりませんでした",
            Toast.LENGTH_SHORT
        ).show()
    }
}
