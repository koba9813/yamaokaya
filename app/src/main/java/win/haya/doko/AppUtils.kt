package win.haya.doko

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

private const val YAMAOKAYA_MENU_URL = "https://www.yamaokaya.com/menus/yamaokaya/regular/"
private const val YAMAOKAYA_OFFICIAL_SITE_URL = "https://www.yamaokaya.com/"

/** 山岡家公式アプリ（ラーメン山岡家公式アプリ）のパッケージ名。 */
private const val YAMAOKAYA_OFFICIAL_APP_PACKAGE = "li.yapp.appCC40B910"

/** このアプリの GitHub リポジトリ。 */
internal const val GITHUB_REPO_URL = "https://github.com/koba9813/yamaokaya"

@SuppressLint("DiscouragedApi")
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

/** URL をブラウザ（Custom Tabs）で開く。Custom Tabs が無ければ通常のブラウザで開く。 */
internal fun openUrlInAppBrowser(context: Context, url: String) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()

    try {
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** 山岡家公式アプリがインストールされていれば起動する。無ければ Google Play を開く。 */
internal fun openYamaokayaOfficialApp(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(YAMAOKAYA_OFFICIAL_APP_PACKAGE)
    if (launchIntent != null) {
        try {
            context.startActivity(launchIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // 起動に失敗したら Google Play へフォールバック
        } catch (_: SecurityException) {
            // 同様にフォールバック
        }
    }

    openUrlInAppBrowser(
        context,
        "https://play.google.com/store/apps/details?id=$YAMAOKAYA_OFFICIAL_APP_PACKAGE"
    )
}

/**
 * 山岡家のメニューを開く。
 *
 * 公式アプリ（ラーメン山岡家公式アプリ）がインストールされていれば公式アプリを起動し、
 * インストールされていなければブラウザで公式メニューページを開く。
 */
internal fun openYamaokayaMenu(context: Context) {
    // 公式アプリが入っていれば、そちらを開く
    val launchIntent = context.packageManager.getLaunchIntentForPackage(YAMAOKAYA_OFFICIAL_APP_PACKAGE)
    if (launchIntent != null) {
        try {
            context.startActivity(launchIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // 起動に失敗したらブラウザのメニューページへフォールバック
        } catch (_: SecurityException) {
            // 同様にフォールバック
        }
    }

    openUrlInAppBrowser(context, YAMAOKAYA_MENU_URL)
}

/** 山岡家公式サイトをブラウザで開く。 */
internal fun openYamaokayaOfficialSite(context: Context) {
    openUrlInAppBrowser(context, YAMAOKAYA_OFFICIAL_SITE_URL)
}
