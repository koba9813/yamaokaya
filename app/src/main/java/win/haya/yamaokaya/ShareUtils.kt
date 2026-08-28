package win.haya.yamaokaya

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream

internal fun createShareMessage(shop: ShopInfo?): String {
    val distanceKm = ((shop?.distanceMeters ?: 0f) / 1000f)
    return "山岡家まで ${"%.2f".format(distanceKm)} kmのところにいます！"
}

internal fun shareToTwitter(
    context: Context,
    uriHandler: UriHandler,
    sourceView: View,
    message: String,
    url: String
) {
    val shareText = "$message\n$url"
    val shared = shareToPackageWithOptionalScreenshot(
        context = context,
        sourceView = sourceView,
        packageName = "com.twitter.android",
        chooserTitle = "Twitterで共有",
        shareText = shareText
    )

    if (!shared) {
        val text = Uri.encode(shareText)
        uriHandler.openUri("https://twitter.com/intent/tweet?text=$text")
    }
}

internal fun shareToLine(
    context: Context,
    sourceView: View,
    uriHandler: UriHandler,
    message: String,
    url: String
) {
    val shareText = "$message\n$url"
    val shared = shareToPackageWithOptionalScreenshot(
        context = context,
        sourceView = sourceView,
        packageName = "jp.naver.line.android",
        chooserTitle = "LINEで共有",
        shareText = shareText
    )

    if (!shared) {
        val text = Uri.encode(shareText)
        uriHandler.openUri("https://line.me/R/msg/text/?$text")
    }
}

internal fun shareToInstagram(context: Context, sourceView: View, message: String, url: String) {
    shareToPackageWithOptionalScreenshot(
        context = context,
        sourceView = sourceView,
        packageName = "com.instagram.android",
        chooserTitle = "Instagramで共有",
        shareText = "$message\n$url"
    )
}

private fun shareToPackageWithOptionalScreenshot(
    context: Context,
    sourceView: View,
    packageName: String,
    chooserTitle: String,
    shareText: String
): Boolean {
    val screenshotUri = captureScreenshotUri(context, sourceView)

    val targetIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (screenshotUri != null) "image/png" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        if (screenshotUri != null) {
            putExtra(Intent.EXTRA_STREAM, screenshotUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setPackage(packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(targetIntent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (screenshotUri != null) "image/png" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (screenshotUri != null) {
                putExtra(Intent.EXTRA_STREAM, screenshotUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(fallbackIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }
}

private fun captureScreenshotUri(context: Context, sourceView: View): Uri? {
    return try {
        val fullBitmap = sourceView.drawToBitmap()

        val cropSize = fullBitmap.width
        val startX = 0
        val startY = Math.max(0, (fullBitmap.height - cropSize) / 2 - 100)
        val actualHeight = Math.min(cropSize, fullBitmap.height - startY)

        val croppedBitmap = Bitmap.createBitmap(fullBitmap, startX, startY, cropSize, actualHeight)

        val finalBitmap = Bitmap.createBitmap(cropSize, actualHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(croppedBitmap, 0f, 0f, null)

        val file = File(context.cacheDir, "share_yamaokaya.png")
        FileOutputStream(file).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        null
    }
}
