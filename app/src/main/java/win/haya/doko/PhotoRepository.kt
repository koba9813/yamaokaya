package win.haya.doko

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** バックアップから復元する写真 1 枚。 */
internal data class ImportedPhoto(
    val shopName: String,
    val timestampMillis: Long,
    val source: File
)

internal class PhotoRepository(context: Context) {

    private val photoDir = File(context.filesDir, "shop_photos").apply {
        if (!exists()) mkdirs()
    }

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)

    fun savePhoto(shopName: String, bitmap: Bitmap): PhotoEntry {
        val shopDir = ensureShopDir(shopName)
        val timestamp = System.currentTimeMillis()
        val file = File(shopDir, photoFileName(timestamp))
        FileOutputStream(file).use { out ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
            }
        }
        return PhotoEntry(shopName = shopName, file = file, timestampMillis = timestamp)
    }

    fun loadAllPhotos(): List<PhotoEntry> {
        return photoDir.listFiles()?.flatMap { shopDir ->
            if (!shopDir.isDirectory) emptyList()
            else shopDir.listFiles()?.mapNotNull { file -> parseEntry(shopDir.name, file) } ?: emptyList()
        }?.sortedByDescending { it.timestampMillis } ?: emptyList()
    }

    fun loadPhotosForShop(shopName: String): List<PhotoEntry> {
        val shopDir = File(photoDir, sanitize(shopName))
        if (!shopDir.exists() || !shopDir.isDirectory) return emptyList()
        return shopDir.listFiles()
            ?.mapNotNull { file -> parseEntry(shopName, file) }
            ?.sortedByDescending { it.timestampMillis }
            ?: emptyList()
    }

    fun getLatestPhoto(shopName: String): PhotoEntry? = loadPhotosForShop(shopName).firstOrNull()

    fun hasPhoto(shopName: String): Boolean = getPhotoCount(shopName) > 0

    fun getPhotoCount(shopName: String): Int = loadPhotosForShop(shopName).size

    fun loadBitmap(entry: PhotoEntry): ImageBitmap? {
        return BitmapFactory.decodeFile(entry.file.absolutePath)?.asImageBitmap()
    }

    fun formatDate(entry: PhotoEntry): String = dateFormat.format(Date(entry.timestampMillis))

    /** 同じ店舗・同じ撮影日時の写真がすでにあるか（バックアップ読み込み時の重複判定に使用）。 */
    fun containsPhoto(shopName: String, timestampMillis: Long): Boolean =
        File(shopDir(shopName), photoFileName(timestampMillis)).exists()

    /** バックアップから復元した写真を保存する。同じ写真がすでにあれば何もせず false を返す。 */
    fun saveImportedPhoto(shopName: String, timestampMillis: Long, source: File): Boolean {
        val directory = ensureShopDir(shopName)
        val file = File(directory, photoFileName(timestampMillis))
        if (file.exists()) return false

        // 途中で失敗しても壊れた写真が残らないように、一時ファイルへ書いてから置き換える
        val temp = File(directory, "${file.name}.tmp")
        try {
            source.inputStream().use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            }
            if (!temp.renameTo(file)) {
                temp.delete()
                return false
            }
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return true
    }

    /**
     * 写真をすべて入れ替える（置き換え読み込みに使用）。
     *
     * いったん別フォルダーへ書き出してから入れ替えるため、途中で失敗しても現在の写真は残る。
     *
     * @return 書き込んだ枚数
     */
    fun replaceAllWith(photos: List<ImportedPhoto>): Int {
        val parent = photoDir.parentFile
            ?: throw IllegalStateException("写真の保存先が見つかりません")
        val staging = File(parent, "shop_photos_staging")
        val backup = File(parent, "shop_photos_backup")

        staging.deleteRecursively()
        if (!staging.mkdirs()) {
            throw IllegalStateException("写真の保存先を作成できませんでした")
        }

        try {
            photos.forEach { photo ->
                val dir = File(staging, sanitize(photo.shopName)).apply {
                    if (!exists()) mkdirs()
                }
                val target = File(dir, photoFileName(photo.timestampMillis))
                if (!target.exists()) {
                    photo.source.inputStream().use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                }
            }
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }

        backup.deleteRecursively()
        if (!photoDir.renameTo(backup)) {
            staging.deleteRecursively()
            throw IllegalStateException("写真の置き換えに失敗しました")
        }
        if (!staging.renameTo(photoDir)) {
            // 元に戻してからエラーにする
            backup.renameTo(photoDir)
            staging.deleteRecursively()
            throw IllegalStateException("写真の置き換えに失敗しました")
        }
        backup.deleteRecursively()
        return photos.size
    }

    /** すべての写真を削除する（置き換え読み込みに使用）。@return 削除した枚数 */
    fun deleteAllPhotos(): Int {
        val count = loadAllPhotos().size
        photoDir.deleteRecursively()
        photoDir.mkdirs()
        return count
    }

    private fun shopDir(shopName: String) = File(photoDir, sanitize(shopName))

    private fun ensureShopDir(shopName: String) = shopDir(shopName).apply {
        if (!exists()) mkdirs()
    }

    private fun photoFileName(timestampMillis: Long) = "$timestampMillis.webp"

    private fun parseEntry(shopName: String, file: File): PhotoEntry? {
        if (!file.isFile) return null
        val timestamp = file.nameWithoutExtension.toLongOrNull() ?: return null
        return PhotoEntry(shopName = shopName, file = file, timestampMillis = timestamp)
    }

    private fun sanitize(name: String): String {
        // フォルダ名として安全な 1 セグメントにする（パス区切りの混入と、
        // "." / ".." による shop_photos の外への書き込みを防ぐ）。
        // 末尾のドットは OS によって無視されることがあるため落とす。
        val cleaned = name
            .replace("/", "_")
            .replace("\\", "_")
            .trim()
            .trimEnd('.')
            .trim()
            .take(MAX_DIR_NAME_LENGTH)
        return when {
            cleaned.isEmpty() || cleaned == "." || cleaned == ".." -> "unknown"
            else -> cleaned
        }
    }

    companion object {
        /** 店舗フォルダー名の最大長（ファイルシステムの名前長超過を防ぐ）。 */
        private const val MAX_DIR_NAME_LENGTH = 60
        fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }

            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) return null

            val scale = maxOf(1, maxOf(width, height) / maxDimension)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            return resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        }
    }
}
