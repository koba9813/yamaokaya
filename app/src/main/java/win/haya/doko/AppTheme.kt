package win.haya.doko

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

internal val zenMaruGothicFont = FontFamily(
    Font(resId = R.font.zen_maru_gothic_regular, weight = FontWeight.Normal),
    Font(resId = R.font.zen_maru_gothic_bold, weight = FontWeight.Bold)
)

/**
 * Smart Font UI は 1 ウェイトのみのため、太字は Compose の合成太字に任せる
 * （同じファイルを Bold として登録すると太字にならないため）。
 */
internal val smartFontUiFont = FontFamily(
    Font(resId = R.font.smart_font_ui, weight = FontWeight.Normal)
)

/** フォント切り替えで利用するフォントファミリー。設定画面のプレビューからも参照する。 */
internal fun fontFamilyFor(fontMode: FontMode): FontFamily = when (fontMode) {
    FontMode.ZEN_MARU -> zenMaruGothicFont
    FontMode.SMART_FONT_UI -> smartFontUiFont
}

private fun typographyWith(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
    )
}

private val zenMaruGothicTypography = typographyWith(zenMaruGothicFont)
private val smartFontUiTypography = typographyWith(smartFontUiFont)

/** フォント設定に対応するタイポグラフィ。起動時に一度だけ生成したものを使い回す。 */
internal fun typographyFor(fontMode: FontMode): Typography = when (fontMode) {
    FontMode.ZEN_MARU -> zenMaruGothicTypography
    FontMode.SMART_FONT_UI -> smartFontUiTypography
}

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
    fontMode: FontMode = FontMode.SMART_FONT_UI,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeFor(themeMode)

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(fontMode),
        content = content
    )
}
