package win.haya.doko

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass の有効状態と、その下地色。
 *
 * 画面側で「ガラス調の装飾を足すかどうか」と、ヘッダーのスクリム色などを
 * 判定するために使う。
 */
internal data class LiquidGlassStyle(
    val enabled: Boolean,
    val isDark: Boolean,
    val canvas: Color
)

internal val LocalLiquidGlass = staticCompositionLocalOf {
    LiquidGlassStyle(enabled = false, isDark = true, canvas = Color(0xFF0C0D10))
}

/** テーマごとの、ガラスの下に敷く落ち着いたベース色。 */
internal fun liquidGlassCanvas(themeMode: ThemeMode): Color = when (themeMode) {
    ThemeMode.DARK -> Color(0xFF0C0E12)
    ThemeMode.LIGHT -> Color(0xFFF6F4F1)
    ThemeMode.YAMAOKAYA -> Color(0xFF111012)
}

/**
 * 通常のカラースキームから、Liquid Glass 用の半透明カラースキームを作る。
 *
 * 背景を透明にし、サーフェス系のトークンを半透明に置き換える。
 * 文字色とアクセント色はテーマのものをそのまま使い、可読性を優先する。
 */
internal fun liquidGlassColorScheme(base: ColorScheme, isDark: Boolean): ColorScheme {
    fun white(alpha: Float) = Color.White.copy(alpha = alpha)
    fun black(alpha: Float) = Color.Black.copy(alpha = alpha)

    return if (isDark) {
        base.copy(
            background = Color.Transparent,
            surface = white(0.06f),
            surfaceDim = black(0.18f),
            surfaceBright = white(0.18f),
            surfaceVariant = white(0.09f),
            surfaceContainerLowest = white(0.035f),
            surfaceContainerLow = white(0.055f),
            surfaceContainer = white(0.075f),
            surfaceContainerHigh = white(0.105f),
            surfaceContainerHighest = white(0.14f),
            outline = white(0.24f),
            outlineVariant = white(0.12f)
        )
    } else {
        base.copy(
            background = Color.Transparent,
            surface = white(0.34f),
            surfaceDim = white(0.24f),
            surfaceBright = white(0.72f),
            surfaceVariant = white(0.46f),
            surfaceContainerLowest = white(0.52f),
            surfaceContainerLow = white(0.62f),
            surfaceContainer = white(0.70f),
            surfaceContainerHigh = white(0.78f),
            surfaceContainerHighest = white(0.86f),
            outline = black(0.22f),
            outlineVariant = black(0.10f)
        )
    }
}

/**
 * アプリ全体の背景。
 *
 * 以前の「赤黒い全面グラデーション」は廃止し、落ち着いたニュートラルな下地に
 * ごく淡いブランド色の光を静的に重ねる。常時動く装飾は使わない。
 * ガラス面が透けたときに、背景の存在と明暗の変化を感じられる程度に留める。
 */
@Composable
internal fun LiquidGlassBackdrop(
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val isDark = themeMode.isDark
    val baseColor = liquidGlassCanvas(themeMode)
    val brandRed = Color(0xFFD71920)
    val brandGold = Color(0xFFF5B82E)
    val brandBrown = Color(0xFF6B211B)
    val schemeAccent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            fun drawGlow(
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
                // 上端にごく淡い赤、右に金、下端にブラウン。全体は黒に近いニュートラル。
                // 赤はブランドの気配を残す程度に抑え、古い「赤黒グラデーション」には戻さない。
                drawGlow(
                    color = if (themeMode == ThemeMode.YAMAOKAYA) brandRed else schemeAccent,
                    alpha = 0.05f,
                    centerX = width * 0.10f,
                    centerY = height * 0.0f,
                    radius = width * 0.82f
                )
                drawGlow(
                    color = brandGold,
                    alpha = if (themeMode == ThemeMode.YAMAOKAYA) 0.028f else 0.045f,
                    centerX = width * 1.0f,
                    centerY = height * 0.30f,
                    radius = width * 0.62f
                )
                drawGlow(
                    color = brandBrown,
                    alpha = if (themeMode == ThemeMode.YAMAOKAYA) 0.04f else 0.08f,
                    centerX = width * 0.62f,
                    centerY = height * 1.04f,
                    radius = width * 1.0f
                )
                drawGlow(
                    color = Color.White,
                    alpha = 0.018f,
                    centerX = width * 0.52f,
                    centerY = height * 0.46f,
                    radius = width * 0.75f
                )
            } else {
                drawGlow(
                    color = brandRed,
                    alpha = 0.030f,
                    centerX = width * 0.10f,
                    centerY = height * 0.04f,
                    radius = width * 0.85f
                )
                drawGlow(
                    color = brandGold,
                    alpha = 0.055f,
                    centerX = width * 1.02f,
                    centerY = height * 0.30f,
                    radius = width * 0.62f
                )
                drawGlow(
                    color = brandBrown,
                    alpha = 0.024f,
                    centerX = width * 0.60f,
                    centerY = height * 1.02f,
                    radius = width * 0.92f
                )
                drawGlow(
                    color = Color.White,
                    alpha = 0.32f,
                    centerX = width * 0.46f,
                    centerY = height * 0.44f,
                    radius = width * 0.72f
                )
            }
        }
    }
}

/**
 * ガラス背景の上でも読めるアクセント色（文字・数値用）。
 *
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
 * 太い白枠にはせず、1dp の光の反射として扱う。
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
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.58f)
            )
        )
    }
    return border(width = width, brush = brush, shape = shape)
}
