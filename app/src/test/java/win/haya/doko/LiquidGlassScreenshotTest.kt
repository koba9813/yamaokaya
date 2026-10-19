package win.haya.doko

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * Liquid Glass（ベータ）の見た目を PNG に書き出す確認用テスト。
 *
 * 出力先はテスト実行時の `build/screenshots`。Robolectric のネイティブ描画で
 * DecorView を直接ビットマップへ描画する（`captureToImage()` は使わない）。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class LiquidGlassScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsPage_yamaokaya_liquidGlass() = captureScreen(
        name = "liquid_glass_settings_yamaokaya",
        themeMode = ThemeMode.YAMAOKAYA
    ) {
        SettingsPage(
            appSettings = AppSettings(liquidGlassEnabled = true),
            onSettingsChanged = {},
            onClose = {}
        )
    }

    @Test
    fun settingsPage_light_liquidGlass() = captureScreen(
        name = "liquid_glass_settings_light",
        themeMode = ThemeMode.LIGHT
    ) {
        SettingsPage(
            appSettings = AppSettings(themeMode = ThemeMode.LIGHT, liquidGlassEnabled = true),
            onSettingsChanged = {},
            onClose = {}
        )
    }

    @Test
    fun updateDialog_yamaokaya_liquidGlass() {
        composeRule.setContent {
            YamaokayaTheme(themeMode = ThemeMode.YAMAOKAYA, liquidGlassEnabled = true) {
                UpdateFlow(
                    state = UpdateFlowState.Available(sampleRelease),
                    onStateChange = {},
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        // ダイアログは別ウィンドウなので、実際の重なりに近づけるため
        // アクティビティ（ガラス背景）の上にダイアログを重ねて描画する
        val activityDecor = composeRule.activity.window!!.decorView
        val dialogDecor = requireNotNull(ShadowDialog.getLatestDialog()) {
            "ダイアログが表示されていません"
        }.window!!.decorView
        save("liquid_glass_update_dialog_yamaokaya", drawViews(activityDecor, dialogDecor))
    }

    @Test
    fun mainScreen_yamaokaya_liquidGlass() {
        val application = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = YamaokayaViewModel(application)

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            YamaokayaTheme(themeMode = ThemeMode.YAMAOKAYA, liquidGlassEnabled = true) {
                YamaokayaScreen(viewModel = viewModel)
            }
        }
        // スプラッシュのフェードアウトだけ進める（アップデート確認は走らせない）
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.mainClock.advanceTimeByFrame()
        save("liquid_glass_main_yamaokaya", drawViews(composeRule.activity.window!!.decorView))
    }

    @Test
    fun settingsPage_dark_liquidGlass() = captureScreen(
        name = "liquid_glass_settings_dark",
        themeMode = ThemeMode.DARK
    ) {
        SettingsPage(
            appSettings = AppSettings(liquidGlassEnabled = true),
            onSettingsChanged = {},
            onClose = {}
        )
    }

    @Test
    fun informationPage_yamaokaya_liquidGlass() = captureScreen(
        name = "liquid_glass_information_yamaokaya",
        themeMode = ThemeMode.YAMAOKAYA
    ) {
        InformationPage(onClose = {})
    }

    @Test
    fun stampRally_yamaokaya_liquidGlass() {
        val context: android.content.Context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        captureScreen(
            name = "liquid_glass_stamp_rally_yamaokaya",
            themeMode = ThemeMode.YAMAOKAYA
        ) {
            StampRallyScreen(
                stampRepository = StampRepository(context),
                photoRepository = PhotoRepository(context),
                currentCoordinates = null,
                shopNames = YamaokayaFinder.getShopNames().take(12),
                onClose = {},
                onOpenGallery = {}
            )
        }
    }

    private fun captureScreen(
        name: String,
        themeMode: ThemeMode,
        content: @Composable () -> Unit
    ) {
        composeRule.setContent {
            YamaokayaTheme(themeMode = themeMode, liquidGlassEnabled = true) {
                content()
            }
        }
        composeRule.waitForIdle()
        save(name, drawViews(composeRule.activity.window!!.decorView))
    }

    private fun drawViews(vararg views: View): Bitmap {
        val first = views.first()
        ensureLaidOut(first)

        val bitmap = Bitmap.createBitmap(first.width, first.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        views.forEach { canvas.save(); it.draw(canvas); canvas.restore() }
        return bitmap
    }

    private fun ensureLaidOut(view: View) {
        if (view.width > 0 && view.height > 0) return
        val widthPx = (411 * view.resources.displayMetrics.density).toInt()
        val heightPx = (891 * view.resources.displayMetrics.density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, widthPx, heightPx)
    }

    private fun save(name: String, bitmap: Bitmap) {
        val outputDir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        FileOutputStream(File(outputDir, "$name.png")).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private val sampleRelease = ReleaseInfo(
        versionName = "V.beta3.2",
        body = """
            ### ウィジェットのバリエーションを追加
            - コンパクト / ミニマルの 2 種類を追加

            ### Liquid Glass（ベータ）
            - 設定からオンにすると、すべての画面とウィジェットがすりガラス調に
        """.trimIndent(),
        downloadUrl = "https://example.com/app.apk"
    )
}
