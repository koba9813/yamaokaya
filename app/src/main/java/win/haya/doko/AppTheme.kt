package win.haya.doko

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Android のシステムフォント（Roboto / Noto Sans JP）を優先する。
 * 日本語の詰まりを避けるため、字間は詰めず、ウェイトで階層を作る。
 */
internal val appTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.withSystemFont(FontWeight.Bold, (-0.5).sp),
        displayMedium = base.displayMedium.withSystemFont(FontWeight.Bold, (-0.25).sp),
        displaySmall = base.displaySmall.withSystemFont(FontWeight.SemiBold, 0.sp),
        headlineLarge = base.headlineLarge.withSystemFont(FontWeight.SemiBold, 0.sp),
        headlineMedium = base.headlineMedium.withSystemFont(FontWeight.SemiBold, 0.sp),
        headlineSmall = base.headlineSmall.withSystemFont(FontWeight.SemiBold, 0.sp),
        titleLarge = base.titleLarge.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = base.titleMedium.withSystemFont(FontWeight.Medium, 0.sp),
        titleSmall = base.titleSmall.withSystemFont(FontWeight.Medium, 0.sp),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = FontFamily.Default,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = FontFamily.Default,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.sp
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = FontFamily.Default,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    )
}

private fun TextStyle.withSystemFont(weight: FontWeight, spacing: androidx.compose.ui.unit.TextUnit) =
    copy(
        fontFamily = FontFamily.Default,
        fontWeight = weight,
        letterSpacing = spacing
    )

internal val appShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

internal val lightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

internal val darkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

private val yamaokayaColorScheme = darkColorScheme(
    primary = primaryYamaokaya,
    onPrimary = onPrimaryYamaokaya,
    primaryContainer = primaryContainerYamaokaya,
    onPrimaryContainer = onPrimaryContainerYamaokaya,
    secondary = secondaryYamaokaya,
    onSecondary = onSecondaryYamaokaya,
    secondaryContainer = secondaryContainerYamaokaya,
    onSecondaryContainer = onSecondaryContainerYamaokaya,
    tertiary = tertiaryYamaokaya,
    onTertiary = onTertiaryYamaokaya,
    tertiaryContainer = tertiaryContainerYamaokaya,
    onTertiaryContainer = onTertiaryContainerYamaokaya,
    error = errorYamaokaya,
    onError = onErrorYamaokaya,
    errorContainer = errorContainerYamaokaya,
    onErrorContainer = onErrorContainerYamaokaya,
    background = backgroundYamaokaya,
    onBackground = onBackgroundYamaokaya,
    surface = surfaceYamaokaya,
    onSurface = onSurfaceYamaokaya,
    surfaceVariant = surfaceVariantYamaokaya,
    onSurfaceVariant = onSurfaceVariantYamaokaya,
    outline = outlineYamaokaya,
    outlineVariant = outlineVariantYamaokaya,
    scrim = scrimYamaokaya,
    inverseSurface = inverseSurfaceYamaokaya,
    inverseOnSurface = inverseOnSurfaceYamaokaya,
    inversePrimary = inversePrimaryYamaokaya,
    surfaceDim = surfaceDimYamaokaya,
    surfaceBright = surfaceBrightYamaokaya,
    surfaceContainerLowest = surfaceContainerLowestYamaokaya,
    surfaceContainerLow = surfaceContainerLowYamaokaya,
    surfaceContainer = surfaceContainerYamaokaya,
    surfaceContainerHigh = surfaceContainerHighYamaokaya,
    surfaceContainerHighest = surfaceContainerHighestYamaokaya
)

/**
 * テーマごとの配色。設定画面のテーマ選択プレビューからも参照するため、
 * 配色の定義はすべてここに集約する。
 */
@Composable
internal fun colorSchemeFor(themeMode: ThemeMode): ColorScheme {
    return when (themeMode) {
        // 従来のダークテーマ。端末（Android 12 以降）の壁紙由来カラーをそのまま使う。
        ThemeMode.DARK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(LocalContext.current)
        } else {
            darkColorScheme
        }
        ThemeMode.LIGHT -> lightColorScheme
        ThemeMode.YAMAOKAYA -> yamaokayaColorScheme
    }
}

@Composable
fun YamaokayaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    liquidGlassEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = colorSchemeFor(themeMode)
    val colorScheme = if (liquidGlassEnabled) {
        liquidGlassColorScheme(baseColorScheme, themeMode.isDark)
    } else {
        baseColorScheme
    }
    val canvas = liquidGlassCanvas(themeMode)

    // ステータスバー / ナビゲーションバーのアイコン色をテーマに合わせる。
    // （ライトテーマで白アイコンが白背景に埋もれるのを防ぐ）
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    if (window != null && !view.isInEditMode) {
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !themeMode.isDark
            controller.isAppearanceLightNavigationBars = !themeMode.isDark
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlass provides LiquidGlassStyle(
            enabled = liquidGlassEnabled,
            isDark = themeMode.isDark,
            canvas = canvas
        )
    ) {
        ProvideReduceMotion {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = appTypography,
                shapes = appShapes
            ) {
                // MaterialTheme 自体は LocalContentColor を設定しないため、
                // 色を明示しない Text が端末既定の黒になってしまうのを防ぐ。
                CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
                    if (liquidGlassEnabled) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LiquidGlassBackdrop(themeMode = themeMode)
                            content()
                        }
                    } else {
                        content()
                    }
                }
            }
        }
    }
}
