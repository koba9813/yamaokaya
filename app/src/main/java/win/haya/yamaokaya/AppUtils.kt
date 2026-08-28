package win.haya.yamaokaya

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

private const val YAMAOKAYA_MENU_URL = "https://www.yamaokaya.com/menus/yamaokaya/regular/"

internal fun resolveDrawableId(context: Context, name: String): Int {
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

internal fun getAppVersionName(context: Context): String {
    return try {
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}

internal fun openMenuInAppBrowser(context: Context) {
    val menuUri = Uri.parse(YAMAOKAYA_MENU_URL)
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()

    try {
        customTabsIntent.launchUrl(context, menuUri)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, menuUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
