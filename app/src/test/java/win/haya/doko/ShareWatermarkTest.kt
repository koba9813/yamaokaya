package win.haya.doko

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28])
class ShareWatermarkTest {

    @Test
    fun watermarkFileIsCreatedAndDecodable() {
        val context: Context = ApplicationProvider.getApplicationContext()

        val source = File.createTempFile("watermark_src", ".png")
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val result = createWatermarkedShareFile(context, source)

        assertNotNull(result)
        assertTrue(result!!.isFile)
        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull(decoded)
        assertTrue(decoded!!.width == 640)
        assertTrue(decoded.height == 480)

        // 透かしが左上から下へ縦向きに描かれていることを確認する。
        // 左端の縦方向の帯（x 40..90, y 40..260）には白系（G > 40）のピクセルがあり、
        // 右下（x > 500, y > 400）は元の赤のまま（G < 10）であること。
        assertTrue("左端に透かしがあること", regionHasColor(decoded, 40, 90, 40, 260) { g -> g > 40 })
        assertTrue("右下には透かしがないこと", !regionHasColor(decoded, 500, 630, 400, 470) { g -> g > 40 })

        source.delete()
        result.delete()
    }

    private fun regionHasColor(
        bitmap: Bitmap,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int,
        predicate: (Int) -> Boolean
    ): Boolean {
        for (y in yStart until yEnd step 2) {
            for (x in xStart until xEnd step 2) {
                if (predicate(Color.green(bitmap.getPixel(x, y)))) return true
            }
        }
        return false
    }

    @Test
    fun returnsNullForMissingSource() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val missing = File(context.cacheDir, "no_such_photo.webp")
        assertTrue(createWatermarkedShareFile(context, missing) == null)
    }

    @Test
    fun storyFileIs9x16WithWatermarkAndPhoto() {
        val context: Context = ApplicationProvider.getApplicationContext()

        val source = File.createTempFile("story_src", ".png")
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val result = createStoryShareFile(context, source)

        assertNotNull(result)
        assertTrue(result!!.isFile)
        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull(decoded)
        // 9:16 (1080x1920) のキャンバスになっている
        assertTrue(decoded!!.width == 1080)
        assertTrue(decoded.height == 1920)
        // 透かしが左上の縦帯にある（白系）
        assertTrue("左上に透かしがあること", regionHasColor(decoded, 60, 170, 40, 550) { it > 40 })
        // 中央に元の写真（赤）がフィットして配置されている
        val center = decoded.getPixel(540, 960)
        assertTrue("中央に写真があること", Color.red(center) > 200 && Color.green(center) < 30)

        source.delete()
        result.delete()
    }
}
