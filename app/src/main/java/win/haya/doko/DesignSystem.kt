package win.haya.doko

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* -------------------------------------------------------------------------
 * Design tokens
 * ---------------------------------------------------------------------- */

internal object AppSpacing {
    val screen = 20.dp
    val section = 28.dp
    val row = 14.dp
    val small = 8.dp
}

internal object AppShapes {
    val group = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(24.dp)
    val control = RoundedCornerShape(16.dp)
    val thumbnail = RoundedCornerShape(14.dp)
    val pill = RoundedCornerShape(50)
}

internal enum class GlassLevel { Thin, Regular, Thick }

/**
 * OS の「アニメーションを減らす」設定。端末設定が変わった直後までは追従しないが、
 * 画面を開き直したときに反映される。
 */
internal val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                !ValueAnimator.areAnimatorsEnabled()
            } else {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) == 0f
            }
        }.getOrDefault(false)
    }
}

/** ガラス無効時は不透明な背景、有効時は背後を透かす。 */
@Composable
internal fun screenContainerColor(): Color =
    if (LocalLiquidGlass.current.enabled) Color.Transparent else MaterialTheme.colorScheme.background

/* -------------------------------------------------------------------------
 * Glass material
 * ---------------------------------------------------------------------- */

/**
 * すりガラスの「素材」。実際の backdrop blur の代わりに、
 * - 背景が透ける半透明のフィル
 * - 上端の細い光沢
 * - 1dp のグラデーション輪郭
 * - 柔らかい影
 * を重ねて光学的な奥行きを作る。画面全体へのフィルター適用は行わない。
 */
@Composable
internal fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.card,
    level: GlassLevel = GlassLevel.Regular,
    tint: Color? = null,
    tintAlpha: Float = 0.18f,
    shadowElevation: Dp = 10.dp,
    showBorder: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlass.current
    val scheme = MaterialTheme.colorScheme
    val enabled = glass.enabled
    val isDark = glass.isDark
    val white = Color.White

    val fillColors: List<Color> = when {
        !enabled -> {
            val base = tint ?: scheme.surfaceContainerLow
            listOf(base, base)
        }
        tint != null -> listOf(
            tint.copy(alpha = tintAlpha.coerceIn(0f, 1f)),
            tint.copy(alpha = (tintAlpha * 0.72f).coerceIn(0f, 1f))
        )
        isDark -> when (level) {
            GlassLevel.Thin -> listOf(white.copy(alpha = 0.075f), white.copy(alpha = 0.035f))
            GlassLevel.Regular -> listOf(white.copy(alpha = 0.120f), white.copy(alpha = 0.055f))
            GlassLevel.Thick -> listOf(white.copy(alpha = 0.170f), white.copy(alpha = 0.080f))
        }
        else -> when (level) {
            GlassLevel.Thin -> listOf(white.copy(alpha = 0.54f), white.copy(alpha = 0.34f))
            GlassLevel.Regular -> listOf(white.copy(alpha = 0.70f), white.copy(alpha = 0.48f))
            GlassLevel.Thick -> listOf(white.copy(alpha = 0.82f), white.copy(alpha = 0.64f))
        }
    }

    val borderBrush = when {
        !enabled -> SolidColor(scheme.outlineVariant.copy(alpha = 0.55f))
        tint != null -> Brush.linearGradient(
            listOf(
                white.copy(alpha = 0.55f),
                white.copy(alpha = 0.12f),
                white.copy(alpha = 0.30f)
            )
        )
        isDark -> Brush.linearGradient(
            listOf(
                white.copy(alpha = 0.40f),
                white.copy(alpha = 0.06f),
                white.copy(alpha = 0.20f)
            )
        )
        else -> Brush.linearGradient(
            listOf(
                white.copy(alpha = 0.96f),
                white.copy(alpha = 0.30f),
                white.copy(alpha = 0.68f)
            )
        )
    }

    val shadowColor = when {
        !enabled -> Color.Black.copy(alpha = 0.06f)
        isDark -> Color.Black.copy(alpha = 0.28f)
        else -> Color.Black.copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) shadowElevation else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(Brush.linearGradient(fillColors))
            .then(
                if (showBorder) {
                    Modifier.border(width = 1.dp, brush = borderBrush, shape = shape)
                } else {
                    Modifier
                }
            )
    ) {
        if (enabled) {
            // 上端から差し込む柔らかな反射。写真や背景の上でも「ガラスの面」を感じさせる。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to white.copy(alpha = if (isDark) 0.10f else 0.30f),
                            0.42f to Color.Transparent,
                            1f to (if (isDark) Color.Black.copy(alpha = 0.035f) else white.copy(alpha = 0.05f))
                        )
                    )
            )
        }
        content()
    }
}

@Composable
internal fun Modifier.pressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f
): Modifier {
    val reduceMotion = LocalReduceMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = tween(if (reduceMotion) 0 else 140, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/* -------------------------------------------------------------------------
 * Controls
 * ---------------------------------------------------------------------- */

@Composable
internal fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    accent: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val glass = LocalLiquidGlass.current
    val backdrop = if (glass.isDark) Color.White else Color.Black
    val pressed by interactionSource.collectIsPressedAsState()
    val pressOverlay by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.08f else 0f,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 120),
        label = "pressOverlay"
    )
    val contentColor = when {
        !enabled -> tint.copy(alpha = 0.36f)
        accent -> MaterialTheme.colorScheme.onPrimary
        else -> tint
    }

    GlassSurface(
        modifier = modifier
            .size(size)
            .pressFeedback(interactionSource, enabled),
        shape = CircleShape,
        level = GlassLevel.Thin,
        tint = if (accent) MaterialTheme.colorScheme.primary else null,
        tintAlpha = if (accent) 0.92f else 0f,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    onClick = onClick
                )
                .background(backdrop.copy(alpha = pressOverlay)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

@Composable
internal fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLiquidGlass.current
    val pressed by interactionSource.collectIsPressedAsState()
    val backdrop = if (glass.isDark) Color.White else Color.Black
    val pressOverlay by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.07f else 0f,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 120),
        label = "pillPress"
    )
    val contentColor = when {
        !enabled -> if (accent) scheme.onPrimary.copy(alpha = 0.55f) else scheme.onSurface.copy(alpha = 0.40f)
        accent -> scheme.onPrimary
        else -> scheme.onSurface
    }

    GlassSurface(
        modifier = modifier
            .height(if (compact) 42.dp else 50.dp)
            .pressFeedback(interactionSource, enabled, pressedScale = 0.97f),
        shape = AppShapes.pill,
        level = GlassLevel.Thin,
        tint = if (accent) scheme.primary else null,
        tintAlpha = if (accent) 0.94f else 0f,
        shadowElevation = if (accent) 8.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    onClick = onClick
                )
                .background(backdrop.copy(alpha = pressOverlay))
                .padding(horizontal = if (compact) 12.dp else 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(if (compact) 17.dp else 19.dp)
                )
                Spacer(modifier = Modifier.width(if (compact) 5.dp else 8.dp))
            }
            Text(
                text = text,
                style = if (compact) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * フィルター用のピル。選択中はテーマの primaryContainer で塗り、
 * 未選択は背景になじむ薄いガラス面にする（太い輪郭線は使わない）。
 */
@Composable
internal fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLiquidGlass.current
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> scheme.primaryContainer
            glass.enabled -> Color.White.copy(alpha = if (glass.isDark) 0.07f else 0.48f)
            else -> scheme.surfaceContainerLow
        },
        animationSpec = tween(if (reduceMotion) 0 else 180, easing = FastOutSlowInEasing),
        label = "filterContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            scheme.primary.copy(alpha = 0.32f)
        } else {
            if (glass.enabled) {
                Color.White.copy(alpha = if (glass.isDark) 0.14f else 0.52f)
            } else {
                scheme.outlineVariant.copy(alpha = 0.55f)
            }
        },
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "filterBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        animationSpec = tween(if (reduceMotion) 0 else 180),
        label = "filterContent"
    )

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressFeedback(interactionSource, pressedScale = 0.96f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(AppShapes.pill)
                .background(containerColor)
                .border(1.dp, borderColor, AppShapes.pill)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.pill,
        level = GlassLevel.Regular,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.82f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "検索文字を消去",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
 * Navigation / structure
 * ---------------------------------------------------------------------- */

/**
 * 不透明な大きなヘッダー面を使わず、戻るボタンとタイトルだけを
 * 背景の上に浮かせる共通ヘッダー。上端にはごく薄いスクリムを敷く。
 */
@Composable
internal fun GlassTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLiquidGlass.current
    val scrim = if (glass.enabled) glass.canvas else scheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to scrim.copy(alpha = if (glass.enabled) 0.86f else 1f),
                    0.62f to scrim.copy(alpha = if (glass.enabled) 0.50f else 1f),
                    1f to Color.Transparent
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "戻る",
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

@Composable
internal fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp)
    )
}

@Composable
internal fun Hairline(
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (glass.enabled) 0.36f else 0.55f)
            )
    )
}

/**
 * 設定画面のグループ。ガラスを多用せず、読みやすさを優先した
 * 控えめなグループ面。区切りはヘアラインのみ。
 */
@Composable
internal fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLiquidGlass.current
    val scheme = MaterialTheme.colorScheme
    val color = when {
        !glass.enabled -> scheme.surfaceContainerLow
        glass.isDark -> Color(0xFF1B1B1E).copy(alpha = 0.88f)
        else -> Color.White.copy(alpha = 0.84f)
    }
    val borderColor = if (glass.enabled) {
        if (glass.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    } else {
        scheme.outlineVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.group,
        color = color,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsIconTile(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val glass = LocalLiquidGlass.current
    val pressed by interactionSource.collectIsPressedAsState()
    val pressOverlay by animateFloatAsState(
        targetValue = if (pressed) 0.055f else 0f,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 120),
        label = "settingsRowPress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .background((if (glass.isDark) Color.White else Color.Black).copy(alpha = pressOverlay))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            SettingsIconTile(icon = icon, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}


/* -------------------------------------------------------------------------
 * Motion
 * ---------------------------------------------------------------------- */

/**
 * 画面を開いたときの短いフェード + 小さな移動。
 * 「アニメーションを減らす」が有効なら即座に表示する。
 */
@Composable
internal fun ScreenEnter(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 12.dp.toPx()
        },
        content = content
    )
}

/** 端末設定に合わせてアニメーションを無効化するためのプロバイダ。 */
@Composable
internal fun ProvideReduceMotion(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        content()
    }
}
