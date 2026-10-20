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
 * ウィジェットの各レイアウトを PNG に書き出す確認用テスト。
 *
 * RemoteViews を実際に適用してから、壁紙に見立てた背景の上に描画する。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetScreenshotTest {

    @Test
    fun small() = render(R.layout.widget_yamaokaya, 340, 150, "widget_small")

    @Test
    fun wide() = render(R.layout.widget_yamaokaya_wide, 340, 110, "widget_wide")

    @Test
    fun compact() = render(R.layout.widget_yamaokaya_compact, 340, 70, "widget_compact")

    @Test
    fun minimal() = render(R.layout.widget_yamaokaya_minimal, 170, 170, "widget_minimal")

    private fun render(
        layoutId: Int,
        widthDp: Int,
        heightDp: Int,
        name: String
    ) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val remoteViews = RemoteViews(context.packageName, layoutId)
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
