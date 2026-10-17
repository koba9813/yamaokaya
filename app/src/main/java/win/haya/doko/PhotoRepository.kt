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

internal class PhotoRepository(context: Context) {

    private val photoDir = File(context.filesDir, "shop_photos").apply {
        if (!exists()) mkdirs()
    }

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)

    fun savePhoto(shopName: String, bitmap: Bitmap): PhotoEntry {
        val shopDir = File(photoDir, sanitize(shopName)).apply {
            if (!exists()) mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val file = File(shopDir, "$timestamp.webp")
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

    private fun parseEntry(shopName: String, file: File): PhotoEntry? {
        if (!file.isFile) return null
        val timestamp = file.nameWithoutExtension.toLongOrNull() ?: return null
        return PhotoEntry(shopName = shopName, file = file, timestampMillis = timestamp)
    }

    private fun sanitize(name: String): String {
        return name.replace("/", "_").replace("\\", "_")
    }

    companion object {
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
