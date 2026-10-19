package win.haya.doko

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * ウィジェットの各レイアウト（通常 / Liquid Glass）を PNG に書き出す確認用テスト。
 *
 * RemoteViews を実際に適用してから、壁紙に見立てた背景の上に描画する。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetScreenshotTest {

    @Test
    fun small_normal() = render(R.layout.widget_yamaokaya, glass = false, 340, 150, "widget_small_normal")

    @Test
    fun small_glass() = render(R.layout.widget_yamaokaya, glass = true, 340, 150, "widget_small_glass")

    @Test
    fun wide_normal() = render(R.layout.widget_yamaokaya_wide, glass = false, 340, 110, "widget_wide_normal")

    @Test
    fun wide_glass() = render(R.layout.widget_yamaokaya_wide, glass = true, 340, 110, "widget_wide_glass")

    @Test
    fun compact_normal() = render(R.layout.widget_yamaokaya_compact, glass = false, 340, 70, "widget_compact_normal")

    @Test
    fun compact_glass() = render(R.layout.widget_yamaokaya_compact, glass = true, 340, 70, "widget_compact_glass")

    @Test
    fun minimal_normal() = render(R.layout.widget_yamaokaya_minimal, glass = false, 170, 170, "widget_minimal_normal")

    @Test
    fun minimal_glass() = render(R.layout.widget_yamaokaya_minimal, glass = true, 170, 170, "widget_minimal_glass")

    private fun render(
        layoutId: Int,
        glass: Boolean,
        widthDp: Int,
        heightDp: Int,
        name: String
    ) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val remoteViews = RemoteViews(context.packageName, layoutId)
        WidgetUpdater.applyStyle(context, remoteViews, layoutId, liquidGlass = glass)
        val view = remoteViews.apply(context, FrameLayout(context))

        view.findViewById<TextView>(R.id.widget_shop_name)?.text = "山岡家 すすきの店"
        view.findViewById<TextView>(R.id.widget_distance)?.text = "1.24 km"
        view.findViewById<TextView>(R.id.widget_shop_name_1)?.text = "山岡家 すすきの店"
        view.findViewById<TextView>(R.id.widget_distance_1)?.text = "1.24 km"
        view.findViewById<TextView>(R.id.widget_shop_name_2)?.text = "山岡家 苫小牧店"
        view.findViewById<TextView>(R.id.widget_distance_2)?.text = "12.30 km"
        view.findViewById<TextView>(R.id.widget_shop_name_3)?.text = "山岡家 札幌北店"
        view.findViewById<TextView>(R.id.widget_distance_3)?.text = "24.08 km"
        view.findViewById<View>(R.id.widget_shop_row_1)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.widget_shop_row_2)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.widget_shop_row_3)?.visibility = View.VISIBLE

        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, widthPx, heightPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // 壁紙に見立てた背景（すりガラスの透け方を見るため）
        canvas.drawColor(Color.parseColor("#FF2E4756"))
        view.draw(canvas)

        val outputDir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        FileOutputStream(File(outputDir, "$name.png")).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
