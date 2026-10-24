package win.haya.doko

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

/** Instagram ストーリー用のキャンバスサイズ（9:16）。 */
private const val STORY_CANVAS_WIDTH = 1080
private const val STORY_CANVAS_HEIGHT = 1920

/**
 * 共有する写真に「Yamaokaya is Doko?」の透かしを入れた一時ファイルを生成する。
 *
 * 元の写真（shop_photos 配下の webp）は変更せず、cacheDir/share に PNG を書き出す。
 * 透かしは左上から下に向かう縦向きに、白文字＋わずかな影で控えめに描画する。
 *
 * @return 生成した一時ファイル。デコードに失敗した場合は null。
 */
internal fun createWatermarkedShareFile(context: Context, source: File): File? {
    val original = BitmapFactory.decodeFile(source.absolutePath) ?: return null

    val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    drawVerticalWatermark(canvas, bitmap.width)

    val out = writeShareFile(context, bitmap, "share")
    bitmap.recycle()
    original.recycle()
    return out
}

/**
 * Instagram ストーリー用に 9:16（1080x1920）の画像を生成する。
 *
 * Instagram はストーリーへ追加するときに画像を 9:16 に合わせてトリミングするため、
 * 写真の端に描いた透かしが切れてしまう。そこで、あらかじめ 9:16 のキャンバスに
 * 写真（ぼかした背景＋中央にフィット）と透かしを描いてから共有する。
 * 透かしはキャンバスの左上から下へ向かう縦向きで、必ず画面内に収まる。
 *
 * @return 生成した一時ファイル。デコードに失敗した場合は null。
 */
internal fun createStoryShareFile(context: Context, source: File): File? {
    val original = BitmapFactory.decodeFile(source.absolutePath) ?: return null

    val bitmap = Bitmap.createBitmap(STORY_CANVAS_WIDTH, STORY_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 背景: 写真をぼかして 9:16 に引き伸ばし、暗くして中央の写真を際立たせる
    val mini = Bitmap.createScaledBitmap(original, 128, 128, true)
    val blurred = Bitmap.createScaledBitmap(mini, STORY_CANVAS_WIDTH, STORY_CANVAS_HEIGHT, true)
    canvas.drawBitmap(blurred, 0f, 0f, null)
    canvas.drawColor(Color.argb(120, 0, 0, 0))

    // 写真をアスペクト比を保ったまま中央に配置
    val scale = minOf(
        STORY_CANVAS_WIDTH.toFloat() / original.width,
        STORY_CANVAS_HEIGHT.toFloat() / original.height
    )
    val drawWidth = original.width * scale
    val drawHeight = original.height * scale
    val left = (STORY_CANVAS_WIDTH - drawWidth) / 2f
    val top = (STORY_CANVAS_HEIGHT - drawHeight) / 2f
    val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(original, null, RectF(left, top, left + drawWidth, top + drawHeight), photoPaint)

    // 透かし（9:16 のキャンバス内なので Instagram に切られない）
    drawVerticalWatermark(canvas, STORY_CANVAS_WIDTH)

    val out = writeShareFile(context, bitmap, "story")
    mini.recycle()
    blurred.recycle()
    bitmap.recycle()
    original.recycle()
    return out
}

/** 一時ファイルを PNG として書き出す。 */
private fun writeShareFile(context: Context, bitmap: Bitmap, prefix: String): File {
    val dir = File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
    val out = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
    FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return out
}

/** 左上から下へ読める縦向きの透かしを描く。 */
private fun drawVerticalWatermark(canvas: Canvas, canvasWidth: Int) {
    val text = "Yamaokaya is Doko?"
    val fontSize = (canvasWidth / 22f).coerceAtLeast(16f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 200
        textSize = fontSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(fontSize / 5f, 0f, 0f, Color.argb(128, 0, 0, 0))
    }

    // 90°回転させると文字の基線が下向きになる（左上から下へ読める向き）
    val padding = canvasWidth * 0.04f
    val x = padding + fontSize
    val y = padding

    canvas.save()
    canvas.rotate(90f, x, y)
    canvas.drawText(text, x, y, paint)
    canvas.restore()
}
