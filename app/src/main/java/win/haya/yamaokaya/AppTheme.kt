package win.haya.yamaokaya

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

internal val zenMaruGothicFont = FontFamily(
    Font(resId = R.font.zen_maru_gothic_regular, weight = FontWeight.Normal),
    Font(resId = R.font.zen_maru_gothic_bold, weight = FontWeight.Bold)
)

internal val appTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = zenMaruGothicFont),
        displayMedium = base.displayMedium.copy(fontFamily = zenMaruGothicFont),
        displaySmall = base.displaySmall.copy(fontFamily = zenMaruGothicFont),
        headlineLarge = base.headlineLarge.copy(fontFamily = zenMaruGothicFont),
        headlineMedium = base.headlineMedium.copy(fontFamily = zenMaruGothicFont),
        headlineSmall = base.headlineSmall.copy(fontFamily = zenMaruGothicFont),
        titleLarge = base.titleLarge.copy(fontFamily = zenMaruGothicFont),
        titleMedium = base.titleMedium.copy(fontFamily = zenMaruGothicFont),
        titleSmall = base.titleSmall.copy(fontFamily = zenMaruGothicFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = zenMaruGothicFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = zenMaruGothicFont),
        bodySmall = base.bodySmall.copy(fontFamily = zenMaruGothicFont),
        labelLarge = base.labelLarge.copy(fontFamily = zenMaruGothicFont),
        labelMedium = base.labelMedium.copy(fontFamily = zenMaruGothicFont),
        labelSmall = base.labelSmall.copy(fontFamily = zenMaruGothicFont)
    )
}
