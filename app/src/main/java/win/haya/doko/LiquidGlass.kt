package win.haya.doko

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Liquid Glass（ベータ）の有効状態。
 *
 * 画面側で「ガラス調の装飾を足すかどうか」を判定するために使う。
 */
internal data class LiquidGlassStyle(
    val enabled: Boolean,
    val isDark: Boolean
)

internal val LocalLiquidGlass = staticCompositionLocalOf {
    LiquidGlassStyle(enabled = false, isDark = true)
}

/**
 * 通常のカラースキームから、Liquid Glass 用の半透明カラースキームを作る。
 *
 * 背景を透明にし、サーフェス系のトークンを白（ダーク）/ 白（ライト）の
 * 半透明に置き換えることで、背後のグラデーションが透けて見える。
 * 文字色（onSurface など）とアクセント色はテーマのものをそのまま使う。
 */
internal fun liquidGlassColorScheme(base: ColorScheme, isDark: Boolean): ColorScheme {
    fun white(alpha: Float) = Color.White.copy(alpha = alpha)
    fun black(alpha: Float) = Color.Black.copy(alpha = alpha)

    return if (isDark) {
        base.copy(
            background = Color.Transparent,
            surface = white(0.05f),
            surfaceDim = white(0.03f),
            surfaceBright = white(0.20f),
            surfaceVariant = white(0.10f),
            surfaceContainerLowest = white(0.03f),
            surfaceContainerLow = white(0.06f),
            surfaceContainer = white(0.08f),
            surfaceContainerHigh = white(0.11f),
            surfaceContainerHighest = white(0.15f),
            outline = white(0.34f),
            outlineVariant = white(0.18f)
        )
    } else {
        base.copy(
            background = Color.Transparent,
            surface = white(0.32f),
            surfaceDim = white(0.24f),
            surfaceBright = white(0.72f),
            surfaceVariant = white(0.46f),
            surfaceContainerLowest = white(0.24f),
            surfaceContainerLow = white(0.34f),
            surfaceContainer = white(0.42f),
            surfaceContainerHigh = white(0.52f),
            surfaceContainerHighest = white(0.62f),
            outline = black(0.30f),
            outlineVariant = black(0.13f)
        )
    }
}

/**
 * アプリ全体の背景に敷く、ゆっくり動くグラデーション。
 *
 * すりガラスの「背景が透けて見える」感覚を出すための下地で、
 * 赤・金・ブラウン（山岡家カラー）の光をぼかして重ねている。
 */
@Composable
internal fun LiquidGlassBackdrop(
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val isDark = themeMode.isDark

    val transition = rememberInfiniteTransition(label = "liquidGlassBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing)
        ),
        label = "liquidGlassPhase"
    )

    val baseColor = if (isDark) Color(0xFF0B0708) else Color(0xFFF6F1EF)
    val brandRed = Color(0xFFD71920)
    val brandGold = Color(0xFFF5B82E)
    val brandBrown = Color(0xFF6B211B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val driftX = sin(phase)
            val driftY = cos(phase)
            val blobAlpha = if (isDark) 1f else 0.45f

            fun drawBlob(
                color: Color,
                alpha: Float,
                centerX: Float,
                centerY: Float,
                radius: Float
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }

            if (isDark) {
                drawBlob(
                    color = brandRed,
                    alpha = 0.52f * blobAlpha,
                    centerX = width * (0.20f + 0.06f * driftX),
                    centerY = height * (0.16f + 0.04f * driftY),
                    radius = width * 0.90f
                )
                drawBlob(
                    color = brandGold,
                    alpha = 0.24f * blobAlpha,
                    centerX = width * (0.88f + 0.05f * driftY),
                    centerY = height * (0.32f + 0.05f * driftX),
                    radius = width * 0.62f
                )
                drawBlob(
                    color = brandBrown,
                    alpha = 0.62f * blobAlpha,
                    centerX = width * (0.50f - 0.05f * driftX),
                    centerY = height * (0.86f + 0.04f * driftY),
                    radius = width * 1.00f
                )
            } else {
                drawBlob(
                    color = brandRed,
                    alpha = 0.20f * blobAlpha,
                    centerX = width * (0.18f + 0.06f * driftX),
                    centerY = height * (0.14f + 0.04f * driftY),
                    radius = width * 0.85f
                )
                drawBlob(
                    color = brandGold,
                    alpha = 0.30f * blobAlpha,
                    centerX = width * (0.90f + 0.05f * driftY),
                    centerY = height * (0.34f + 0.05f * driftX),
                    radius = width * 0.60f
                )
                drawBlob(
                    color = brandBrown,
                    alpha = 0.16f * blobAlpha,
                    centerX = width * (0.50f - 0.05f * driftX),
                    centerY = height * (0.88f + 0.04f * driftY),
                    radius = width * 0.95f
                )
            }
        }

        // ステータスバー付近の視認性を確保するための薄いベール。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (isDark) 0.30f else 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * ガラス背景の上でも読めるアクセント色（文字・アイコン用）。
 *
 * 山岡家テーマの赤はガラス背景（赤いグラデーション）に沈むため、
 * ダーク系では primaryContainer 上の明るい色に置き換える。
 * Liquid Glass が無効なときはテーマの primary をそのまま返す。
 */
@Composable
internal fun liquidGlassTextAccent(): Color {
    val style = LocalLiquidGlass.current
    val scheme = MaterialTheme.colorScheme
    return when {
        !style.enabled -> scheme.primary
        style.isDark -> scheme.onPrimaryContainer
        else -> scheme.primary
    }
}

/**
 * Liquid Glass 有効時だけ、ガラスらしいハイライト付きの輪郭を付ける。
 * 無効時は何もせず [this] をそのまま返す。
 */
@Composable
internal fun Modifier.liquidGlassBorder(
    shape: Shape,
    width: Dp = 1.dp
): Modifier {
    val style = LocalLiquidGlass.current
    if (!style.enabled) return this

    val brush = if (style.isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.70f)
            )
        )
    }
    return border(width = width, brush = brush, shape = shape)
}
