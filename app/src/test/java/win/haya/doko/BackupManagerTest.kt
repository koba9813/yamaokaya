package win.haya.doko

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var photoRepository: PhotoRepository
    private lateinit var stampRepository: StampRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("stamp_rally", Context.MODE_PRIVATE).edit().clear().commit()
        photoRepository = PhotoRepository(context)
        photoRepository.deleteAllPhotos()
        stampRepository = StampRepository(context)
    }

    private fun backupUri(name: String = "backup.zip"): Uri =
        Uri.fromFile(File(tempFolder.root, name))

    private fun addPhoto(shopName: String, timestampMillis: Long, content: String) {
        val source = tempFolder.newFile()
        source.writeText(content)
        photoRepository.saveImportedPhoto(shopName, timestampMillis, source)
    }

    private fun saveStamps(vararg records: StampRecord) {
        stampRepository.restoreFromBackup(records.toList(), replace = true)
    }

    @Test
    fun exportAndImportReplace_restoresPhotosStampsAndSettings() {
        addPhoto("山岡家 テスト店", 1_700_000_000_000L, "aaa")
        addPhoto("山岡家 テスト店", 1_700_000_100_000L, "bbb")
        addPhoto("煮干しラーメン山岡家 テスト店", 1_700_000_200_000L, "ccc")
        saveStamps(
            StampRecord("山岡家 テスト店", 3, 1_700_000_000_000L),
            StampRecord("煮干しラーメン山岡家 テスト店", 1, 1_700_000_200_000L)
        )
        val settings = AppSettings(
            proximityNotificationEnabled = false,
            trackerNotificationEnabled = true,
            locationUpdateIntervalSeconds = 30,
            themeMode = ThemeMode.YAMAOKAYA
        )

        val uri = backupUri()
        val manifest = runBlocking { BackupManager.export(context, uri, settings) }
        assertEquals(3, manifest.photoCount)
        assertEquals(2, manifest.stampShopCount)
        assertEquals(4, manifest.stampTotalCount)
        assertTrue(manifest.hasSettings)

        // 機種変更後（データが空の端末）を再現する
        photoRepository.deleteAllPhotos()
        saveStamps()

        val result = runBlocking { BackupManager.import(context, uri, BackupImportMode.REPLACE) }

        assertEquals(3, result.photosAdded)
        assertEquals(0, result.photosSkipped)
        assertEquals(2, result.stampShopsImported)
        assertEquals(settings, result.importedSettings)

        val photos = photoRepository.loadAllPhotos().sortedBy { it.timestampMillis }
        assertEquals(3, photos.size)
        assertEquals("aaa", photos.first().file.readText())
        assertEquals("山岡家 テスト店", photos.first().shopName)
        assertEquals(3, stampRepository.getCount("山岡家 テスト店"))
        assertEquals(1, stampRepository.getCount("煮干しラーメン山岡家 テスト店"))
    }

    @Test
    fun mergeImport_keepsCurrentDataAndSkipsDuplicatePhotos() {
        // 別端末で作ったバックアップ（A店 5回 / B店 1回、写真2枚）
        addPhoto("A店", 1_000L, "dup")
        addPhoto("B店", 2_000L, "new")
        saveStamps(
            StampRecord("A店", 5, 1_500L),
            StampRecord("B店", 1, 2_500L)
        )
        val uri = backupUri("other-device.zip")
        runBlocking { BackupManager.export(context, uri, AppSettings(themeMode = ThemeMode.LIGHT)) }

        // 現在の端末には A店の同じ写真と、A店 2回・最終チェックイン 1_000 の記録がある
        photoRepository.deleteAllPhotos()
        addPhoto("A店", 1_000L, "dup")
        saveStamps(StampRecord("A店", 2, 1_000L))

        val result = runBlocking { BackupManager.import(context, uri, BackupImportMode.MERGE) }

        assertEquals(1, result.photosAdded)
        assertEquals(1, result.photosSkipped)
        assertEquals(2, photoRepository.loadAllPhotos().size)
        assertEquals(2, result.stampShopsImported)
        assertNull(result.importedSettings)

        val records = stampRepository.getAllRecords().associateBy { it.shopName }
        assertEquals(5, records.getValue("A店").count)
        assertEquals(1_500L, records.getValue("A店").lastCheckInMillis)
        assertEquals(1, records.getValue("B店").count)
    }

    @Test
    fun replaceImport_removesCurrentPhotosAndStamps() {
        // バックアップは B店のみ
        addPhoto("B店", 5_000L, "b")
        saveStamps(StampRecord("B店", 1, 5_000L))
        val uri = backupUri("b-only.zip")
        runBlocking { BackupManager.export(context, uri, AppSettings(themeMode = ThemeMode.LIGHT)) }

        // 現在の端末は A店のデータのみ
        photoRepository.deleteAllPhotos()
        addPhoto("A店", 100L, "a")
        saveStamps(StampRecord("A店", 9, 100L))

        val result = runBlocking { BackupManager.import(context, uri, BackupImportMode.REPLACE) }

        assertEquals(1, result.photosAdded)
        val photos = photoRepository.loadAllPhotos()
        assertEquals(1, photos.size)
        assertEquals("B店", photos.first().shopName)
        assertEquals(0, stampRepository.getCount("A店"))
        assertEquals(1, stampRepository.getCount("B店"))
        assertEquals(ThemeMode.LIGHT, result.importedSettings?.themeMode)
    }

    @Test
    fun readManifest_returnsContentsWithoutChangingData() {
        addPhoto("A店", 1_000L, "a")
        saveStamps(StampRecord("A店", 2, 1_000L))
        val uri = backupUri()
        runBlocking { BackupManager.export(context, uri, AppSettings()) }

        val manifest = runBlocking { BackupManager.readManifest(context, uri) }

        assertEquals(BACKUP_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(1, manifest.photoCount)
        assertEquals(1, manifest.stampShopCount)
        assertEquals(2, manifest.stampTotalCount)
        // 確認だけではデータは変わらない
        assertEquals(1, photoRepository.loadAllPhotos().size)
    }

    @Test
    fun import_failsForNewerFormatVersion() {
        val zipFile = File(tempFolder.root, "future.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"app":"win.haya.doko","format":99,"photoCount":0}""".toByteArray())
            zip.closeEntry()
        }

        val error = runCatching {
            runBlocking { BackupManager.import(context, Uri.fromFile(zipFile), BackupImportMode.MERGE) }
        }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message.orEmpty().contains("新しい形式"))
    }

    @Test
    fun import_failsForAnotherAppBackup() {
        val zipFile = File(tempFolder.root, "other-app.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"app":"com.example.other","format":1}""".toByteArray())
            zip.closeEntry()
        }

        val error = runCatching {
            runBlocking { BackupManager.import(context, Uri.fromFile(zipFile), BackupImportMode.MERGE) }
        }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message.orEmpty().contains("別のアプリ"))
    }

    @Test
    fun import_failsForFileWithoutManifest() {
        val file = File(tempFolder.root, "not-a-backup.zip")
        file.writeText("hello")

        val error = runCatching {
            runBlocking { BackupManager.import(context, Uri.fromFile(file), BackupImportMode.MERGE) }
        }.exceptionOrNull()

        assertNotNull(error)
    }

    @Test
    fun import_handlesAnyEntryOrderAndBackslashSeparators() {
        // 他ツールで作られた ZIP を想定し、写真が先・区切りが \ でも読み込めること
        val zipFile = File(tempFolder.root, "handmade.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            put("photos\\00001.webp", "photo-bytes".toByteArray())
            put(
                "photos.json",
                """{"photos":[{"id":"00001.webp","shop":"A店","timestamp":1234}]}""".toByteArray()
            )
            put("stamps.json", """{"stamps":[{"shop":"A店","count":1,"lastCheckIn":1234}]}""".toByteArray())
            put("manifest.json", """{"app":"win.haya.doko","format":1,"photoCount":1}""".toByteArray())
        }

        val result = runBlocking {
            BackupManager.import(context, Uri.fromFile(zipFile), BackupImportMode.MERGE)
        }

        assertEquals(1, result.photosAdded)
        val photos = photoRepository.loadAllPhotos()
        assertEquals(1, photos.size)
        assertEquals("A店", photos.first().shopName)
        assertEquals("photo-bytes", photos.first().file.readText())
        assertEquals(1, stampRepository.getCount("A店"))
    }

    @Test
    fun import_skipsSuspiciousEntryIdsAndKeepsPhotosInsideShopPhotos() {
        val zipFile = File(tempFolder.root, "slip.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            put("photos/../evil.webp", "evil".toByteArray())
            put("photos/..", "evil2".toByteArray())
            put("photos/00001.webp", "good".toByteArray())
            put(
                "photos.json",
                (
                    """{"photos":[""" +
                        """{"id":"../evil.webp","shop":"A店","timestamp":1},""" +
                        """{"id":"00001.webp","shop":"..","timestamp":2}]}"""
                    ).toByteArray()
            )
            // 不正な ID のエントリーは写真として数えないため、正しい 1 枚だけを申告する
            put("manifest.json", """{"app":"win.haya.doko","format":1,"photoCount":1}""".toByteArray())
        }

        val result = runBlocking {
            BackupManager.import(context, Uri.fromFile(zipFile), BackupImportMode.MERGE)
        }

        assertEquals(1, result.photosAdded)
        val photos = photoRepository.loadAllPhotos()
        assertEquals(1, photos.size)
        assertEquals("good", photos.first().file.readText())
        // 展開した写真は必ず shop_photos の中にあること（"../" で外に出ていないこと）
        val shopPhotosPath = File(context.filesDir, "shop_photos").canonicalPath
        assertTrue(photos.first().file.canonicalPath.startsWith(shopPhotosPath))
        assertFalse(File(context.filesDir, "evil.webp").exists())
        assertFalse(File(context.filesDir, "1.webp").exists())
    }

    @Test
    fun replaceImport_keepsExistingPhotosWhenBackupIsBroken() {
        addPhoto("既存店", 1_600_000_000_000L, "keep")

        // 写真ファイルはあるのに photos.json が無い（壊れた）バックアップ
        val zipFile = File(tempFolder.root, "broken.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            put("manifest.json", """{"app":"win.haya.doko","format":1,"photoCount":1}""".toByteArray())
            put("photos/00001.webp", "xxx".toByteArray())
        }

        var failed = false
        try {
            runBlocking {
                BackupManager.import(context, Uri.fromFile(zipFile), BackupImportMode.REPLACE)
            }
        } catch (e: IllegalStateException) {
            failed = true
        }

        assertTrue(failed)
        // 置き換えに失敗しても、既存の写真は消えていないこと
        assertEquals(1, photoRepository.loadAllPhotos().size)
    }

    @Test
    fun import_clampsOutOfRangeValuesFromBackup() {
        val zipFile = File(tempFolder.root, "bogus.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            put("manifest.json", """{"app":"win.haya.doko","format":1,"photoCount":0}""".toByteArray())
            put(
                "stamps.json",
                (
                    """{"stamps":[{"shop":"A店","count":-5,"lastCheckIn":-1},""" +
                        """{"shop":"B店","count":99999999,"lastCheckIn":123}]}"""
                    ).toByteArray()
            )
            put(
                "settings.json",
                """{"theme":"unknown","proximityNotification":true,"locationUpdateInterval":0}""".toByteArray()
            )
        }

        val result = runBlocking {
            BackupManager.import(context, Uri.fromFile(zipFile), BackupImportMode.REPLACE)
        }

        // 回数が 0 以下の記録は取り込まない
        assertEquals(1, result.stampShopsImported)
        assertEquals(0, stampRepository.getCount("A店"))
        assertEquals(MAX_CHECK_IN_COUNT, stampRepository.getCount("B店"))
        // 不正なテーマ・更新間隔は既定値に丸める
        assertEquals(ThemeMode.DARK, result.importedSettings?.themeMode)
        assertEquals(
            SettingsRepository.DEFAULT_LOCATION_INTERVAL_SECONDS,
            result.importedSettings?.locationUpdateIntervalSeconds
        )
    }

    @Test
    fun mergeStampStates_takesLargerCountAndLaterCheckIn() {
        val local = StampState(count = 2, lastCheckInMillis = 1_000L)

        assertEquals(
            StampState(count = 2, lastCheckInMillis = 1_500L),
            mergeStampStates(local, StampState(count = 1, lastCheckInMillis = 1_500L))
        )
        assertEquals(
            StampState(count = 5, lastCheckInMillis = 1_000L),
            mergeStampStates(local, StampState(count = 5, lastCheckInMillis = 500L))
        )
    }

    @Test
    fun suggestedFileName_containsTimestamp() {
        val name = BackupManager.suggestedFileName(0L)

        assertTrue(name.startsWith("Yamaokaya_backup_"))
        assertTrue(name.endsWith(".zip"))
    }
}
