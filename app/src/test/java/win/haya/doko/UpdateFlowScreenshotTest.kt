package win.haya.doko

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.io.FileOutputStream

/**
 * アップデート関連ダイアログの見た目を PNG に書き出す確認用テスト。
 *
 * 出力先はテスト実行時の `build/screenshots` で、パスを標準出力に表示する。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class UpdateFlowScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun availableDialog_yamaokayaTheme() =
        capture("update_available_yamaokaya", ThemeMode.YAMAOKAYA, UpdateFlowState.Available(sampleRelease))

    @Test
    fun availableDialog_darkTheme() =
        capture("update_available_dark", ThemeMode.DARK, UpdateFlowState.Available(sampleRelease))

    @Test
    fun availableDialog_lightTheme() =
        capture("update_available_light", ThemeMode.LIGHT, UpdateFlowState.Available(sampleRelease))

    @Test
    fun availableDialog_withoutReleaseNotes() = capture(
        "update_available_no_notes",
        ThemeMode.YAMAOKAYA,
        UpdateFlowState.Available(sampleRelease.copy(body = ""))
    )

    @Test
    fun downloadingDialog() =
        capture("update_downloading", ThemeMode.YAMAOKAYA, UpdateFlowState.Downloading(0.42f))

    @Test
    fun upToDateDialog() =
        capture("update_uptodate", ThemeMode.YAMAOKAYA, UpdateFlowState.UpToDate)

    @Test
    fun errorDialog() = capture(
        "update_error",
        ThemeMode.YAMAOKAYA,
        UpdateFlowState.Error("ファイルサイズを確認できませんでした。通信環境を変えて、もう一度お試しください")
    )

    private fun capture(name: String, themeMode: ThemeMode, state: UpdateFlowState) {
        show(themeMode, state)

        val bitmap = drawDialogWindow()
        val outputDir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        val file = File(outputDir, "$name.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        println("screenshot: ${file.absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    /**
     * Robolectric では `captureToImage()` がダイアログ窓の再描画待ちでタイムアウトするため、
     * ダイアログの DecorView を直接ビットマップへ描画する。
     */
    private fun drawDialogWindow(): Bitmap {
        val dialog = requireNotNull(ShadowDialog.getLatestDialog()) {
            "アップデートのダイアログが表示されていません"
        }
        val decor = dialog.window!!.decorView
        if (decor.width == 0 || decor.height == 0) {
            val widthPx = (411 * decor.resources.displayMetrics.density).toInt()
            val heightPx = (891 * decor.resources.displayMetrics.density).toInt()
            decor.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, widthPx, heightPx)
        }

        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        return bitmap
    }

    private fun show(themeMode: ThemeMode, state: UpdateFlowState) {
        composeRule.setContent {
            YamaokayaTheme(themeMode = themeMode) {
                UpdateFlow(state = state, onStateChange = {}, onDismiss = {})
            }
        }
        composeRule.waitForIdle()
    }

    private val sampleRelease = ReleaseInfo(
        versionName = "V.beta3.1",
        body = """
            ## V.beta3.1

            ### 新機能: データの引き継ぎ（サーバー不要）
            写真・スタンプラリー・設定を 1 つの ZIP にまとめて保存し、別の端末へ復元できます。

            - 設定 → **データの引き継ぎ** から「書き出す / 読み込む」
            - 読み込みは **追加（マージ）** と **完全に置き換える** の 2 方式
            - 写真は重複を検出してスキップ

            ### セキュリティ・堅牢性の改善
            - バックアップ ZIP のパストラバーサルを拒否
            - 壊れたバックアップは読み込み前に検出
        """.trimIndent(),
        downloadUrl = "https://example.com/app.apk"
    )
}
