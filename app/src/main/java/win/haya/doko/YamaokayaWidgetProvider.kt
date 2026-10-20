package win.haya.doko

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

/**
 * 標準ウィジェット。配置されたサイズに応じて small / wide / tall の
 * 3 種類のレイアウトを自動で切り替える。
 */
class YamaokayaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetUpdater.updateAdaptive(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        WidgetUpdater.updateAdaptive(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
    }

    companion object {
        fun updateAllWidgets(context: Context) = WidgetUpdater.updateAll(context)
    }
}

/** 4×1 の横長 1 行ウィジェット。店舗名と距離だけをコンパクトに表示する。 */
class YamaokayaCompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetUpdater.updateFixed(context, appWidgetManager, appWidgetId, R.layout.widget_yamaokaya_compact)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        WidgetUpdater.updateFixed(context, appWidgetManager, appWidgetId, R.layout.widget_yamaokaya_compact)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdater.updateAll(context)
    }
}

/** 2×2 のミニマルウィジェット。距離を大きく表示する。 */
class YamaokayaMinimalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetUpdater.updateFixed(context, appWidgetManager, appWidgetId, R.layout.widget_yamaokaya_minimal)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        WidgetUpdater.updateFixed(context, appWidgetManager, appWidgetId, R.layout.widget_yamaokaya_minimal)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdater.updateAll(context)
    }
}

/** 3 種類のウィジェットで共通の更新処理。 */
internal object WidgetUpdater {

    /** すべてのウィジェット（全種類）を更新する。設定変更時にも呼ぶ。 */
    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateProvider(context, appWidgetManager, YamaokayaWidgetProvider::class.java) { appWidgetId ->
            selectLayout(context, appWidgetManager.getAppWidgetOptions(appWidgetId))
        }
        updateProvider(context, appWidgetManager, YamaokayaCompactWidgetProvider::class.java) {
            R.layout.widget_yamaokaya_compact
        }
        updateProvider(context, appWidgetManager, YamaokayaMinimalWidgetProvider::class.java) {
            R.layout.widget_yamaokaya_minimal
        }
    }

    fun updateAdaptive(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        update(context, appWidgetManager, appWidgetId, selectLayout(context, appWidgetManager.getAppWidgetOptions(appWidgetId)))
    }

    fun updateFixed(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, layoutId: Int) {
        update(context, appWidgetManager, appWidgetId, layoutId)
    }

    private fun updateProvider(
        context: Context,
        appWidgetManager: AppWidgetManager,
        providerClass: Class<*>,
        layoutFor: (Int) -> Int
    ) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
        for (appWidgetId in appWidgetIds) {
            update(context, appWidgetManager, appWidgetId, layoutFor(appWidgetId))
        }
    }

    private fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        layoutId: Int
    ) {
        val views = RemoteViews(context.packageName, layoutId)
        bindData(context, views, layoutId)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent != null) {
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun selectLayout(context: Context, options: Bundle?): Int {
        val density = context.resources.displayMetrics.density
        val minWidthRaw = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val maxWidthRaw = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 0
        val minHeightRaw = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
        val maxHeightRaw = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0

        val minWidthDp = (minWidthRaw / density).toInt()
        val maxWidthDp = (maxWidthRaw / density).toInt()
        val minHeightDp = (minHeightRaw / density).toInt()
        val maxHeightDp = (maxHeightRaw / density).toInt()

        android.util.Log.d(
            "YamaokayaWidget",
            "sizes raw/minW=$minWidthRaw maxW=$maxWidthRaw minH=$minHeightRaw maxH=$maxHeightRaw " +
                "dp/minW=$minWidthDp maxW=$maxWidthDp minH=$minHeightDp maxH=$maxHeightDp"
        )

        val isTall = (minHeightRaw >= 180 || maxHeightRaw >= 220 ||
            minHeightDp >= 120 || maxHeightDp >= 140) &&
            (minWidthRaw >= 150 || maxWidthRaw >= 170 ||
                minWidthDp >= 100 || maxWidthDp >= 110)

        val isWide = minWidthRaw >= 250 || maxWidthRaw >= 300 ||
            minWidthDp >= 170 || maxWidthDp >= 200

        return when {
            isTall -> R.layout.widget_yamaokaya_tall
            isWide -> R.layout.widget_yamaokaya_wide
            else -> R.layout.widget_yamaokaya
        }
    }

    private fun bindData(context: Context, views: RemoteViews, layoutId: Int) {
        if (!hasLocationPermission(context)) {
            setDefaultMessage(views, layoutId, context.getString(R.string.widget_permission_needed))
            return
        }

        val location = getLastKnownLocation(context)
        if (location == null) {
            setDefaultMessage(views, layoutId, context.getString(R.string.widget_locating))
            return
        }

        val current = Coordinates(location.latitude, location.longitude)
        val nearestShops = YamaokayaFinder.getRegisteredShops()
            .map {
                ShopInfo(
                    name = it.name,
                    coordinates = it.coordinates,
                    distanceMeters = calculateDistanceMeters(current, it.coordinates),
                    bearingDegrees = 0.0
                )
            }
            .sortedBy { it.distanceMeters }

        if (nearestShops.isEmpty()) {
            setDefaultMessage(views, layoutId, context.getString(R.string.widget_no_shop))
            return
        }

        val nearest = nearestShops.first()
        views.setTextViewText(R.id.widget_shop_name, nearest.name)
        views.setTextViewText(R.id.widget_distance, formatWidgetDistance(nearest.distanceMeters))

        if (layoutId == R.layout.widget_yamaokaya_wide) {
            bindMapButton(context, views, nearest)
        }

        if (layoutId == R.layout.widget_yamaokaya_tall) {
            bindMultipleShops(views, nearestShops)
        }
    }

    private fun setDefaultMessage(views: RemoteViews, layoutId: Int, message: String) {
        if (layoutId == R.layout.widget_yamaokaya_tall) {
            views.setTextViewText(R.id.widget_title, message)
            bindMultipleShops(views, emptyList())
        } else {
            views.setTextViewText(R.id.widget_shop_name, message)
            views.setTextViewText(R.id.widget_distance, "--")
        }
    }

    private fun bindMapButton(context: Context, views: RemoteViews, shop: ShopInfo) {
        val mapUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${shop.coordinates.lat},${shop.coordinates.lon}"
        )
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
        val mapPendingIntent = PendingIntent.getActivity(
            context,
            1,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_map_button, mapPendingIntent)
    }

    private fun bindMultipleShops(views: RemoteViews, shops: List<ShopInfo>) {
        val rowIds = listOf(
            R.id.widget_shop_row_1,
            R.id.widget_shop_row_2,
            R.id.widget_shop_row_3
        )
        val nameIds = listOf(
            R.id.widget_shop_name_1,
            R.id.widget_shop_name_2,
            R.id.widget_shop_name_3
        )
        val distanceIds = listOf(
            R.id.widget_distance_1,
            R.id.widget_distance_2,
            R.id.widget_distance_3
        )

        for (i in rowIds.indices) {
            if (i < shops.size) {
                views.setViewVisibility(rowIds[i], View.VISIBLE)
                views.setTextViewText(nameIds[i], shops[i].name)
                views.setTextViewText(distanceIds[i], formatWidgetDistance(shops[i].distanceMeters))
            } else {
                views.setViewVisibility(rowIds[i], View.GONE)
            }
        }
    }

    private fun formatWidgetDistance(meters: Float): String {
        return if (meters < 1000f) {
            "${"%.0f".format(meters)} m"
        } else {
            "${"%.2f".format(meters / 1000f)} km"
        }
    }
}
