package win.haya.yamaokaya

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class YamaokayaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context, appWidgetManager, appWidgetIds)
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
            val views = RemoteViews(context.packageName, R.layout.widget_yamaokaya)

            when {
                !hasLocationPermission(context) -> {
                    views.setTextViewText(R.id.widget_shop_name, context.getString(R.string.widget_permission_needed))
                    views.setTextViewText(R.id.widget_distance, "--")
                }
                else -> {
                    val location = getLastKnownLocation(context)
                    if (location != null) {
                        val current = Coordinates(location.latitude, location.longitude)
                        val nearest = YamaokayaFinder.findNearest(current)
                        if (nearest != null) {
                            views.setTextViewText(R.id.widget_shop_name, nearest.name)
                            views.setTextViewText(R.id.widget_distance, formatWidgetDistance(nearest.distanceMeters))
                        } else {
                            views.setTextViewText(R.id.widget_shop_name, context.getString(R.string.widget_no_shop))
                            views.setTextViewText(R.id.widget_distance, "--")
                        }
                    } else {
                        views.setTextViewText(R.id.widget_shop_name, context.getString(R.string.widget_locating))
                        views.setTextViewText(R.id.widget_distance, "--")
                    }
                }
            }

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

        private fun formatWidgetDistance(meters: Float): String {
            return if (meters < 1000f) {
                "${"%.0f".format(meters)} m"
            } else {
                "${"%.2f".format(meters / 1000f)} km"
            }
        }
    }
}
