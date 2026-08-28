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

class YamaokayaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, YamaokayaWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            performUpdate(context, appWidgetManager, appWidgetIds)
        }

        private fun performUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val layoutId = selectLayout(context, options)
            val views = RemoteViews(context.packageName, layoutId)

            bindData(context, views, layoutId)

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

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
            views.setTextViewText(R.id.widget_shop_name, message)
            views.setTextViewText(R.id.widget_distance, "--")
            if (layoutId == R.layout.widget_yamaokaya_tall) {
                bindMultipleShops(views, emptyList())
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
}
