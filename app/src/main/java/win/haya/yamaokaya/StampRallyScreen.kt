package win.haya.yamaokaya

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
@Composable
internal fun StampRallyScreen(
    stampRepository: StampRepository,
    shopNames: List<String>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Coordinates?>(null) }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = Coordinates(location.latitude, location.longitude)
                        }
                    }
            } catch (_: SecurityException) { }
        }
    }

    val shopCoordinatesMap = remember {
        YamaokayaFinder.getRegisteredShops().associate { it.name to it.coordinates }
    }

    val prefectures = remember { YamaokayaFinder.getPrefectures() }
    var selectedPrefecture by remember { mutableStateOf<String?>(null) }

    val filteredNames = if (selectedPrefecture == null) {
        shopNames
    } else {
        YamaokayaFinder.getShopsByPrefecture(selectedPrefecture!!).map { it.name }
    }

    val ranking = filteredNames
        .map { name ->
            Triple(name, stampRepository.getCount(name), currentLocation?.let { loc ->
                shopCoordinatesMap[name]?.let { coords -> calculateDistanceMeters(loc, coords) }
            })
        }
        .sortedBy { it.third ?: Float.MAX_VALUE }

    val visitedCount = ranking.count { it.second > 0 }
    val totalCheckIns = ranking.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "スタンプラリー",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$visitedCount / ${filteredNames.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "店舗達成",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCheckIns",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "総チェックイン",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 都道府県フィルター
        Text(
            text = "都道府県で絞り込み",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isAllSelected = selectedPrefecture == null
            Button(
                onClick = { selectedPrefecture = null },
                colors = if (isAllSelected) ButtonDefaults.buttonColors()
                         else ButtonDefaults.outlinedButtonColors(),
                border = if (!isAllSelected) ButtonDefaults.outlinedButtonBorder else null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("全て", style = MaterialTheme.typography.labelMedium)
            }
            prefectures.forEach { pref ->
                val isSelected = selectedPrefecture == pref
                Button(
                    onClick = { selectedPrefecture = pref },
                    colors = if (isSelected) ButtonDefaults.buttonColors()
                             else ButtonDefaults.outlinedButtonColors(),
                    border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(pref, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "店舗一覧",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ranking.forEachIndexed { index, (name, count, distance) ->
                    val isVisited = count > 0
                    val distanceLabel = if (distance != null) {
                        if (distance < 1000f) "${distance.toInt()}m"
                        else "%.1fkm".format(distance / 1000f)
                    } else ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = distanceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(64.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isVisited) FontWeight.Bold else FontWeight.Normal,
                                color = if (isVisited) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (isVisited) {
                                val stampStr = "🍜".repeat(minOf(count, 5)) +
                                    if (count > 5) "+${count - 5}" else ""
                                Text(
                                    text = stampStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        if (isVisited) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${count}回",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "未訪問",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }

                    if (index < ranking.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
