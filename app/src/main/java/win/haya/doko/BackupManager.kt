package win.haya.doko

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** バックアップファイル（ZIP）の形式バージョン。読み込み時はこれ以下であることを確認する。 */
internal const val BACKUP_FORMAT_VERSION = 1

/** バックアップファイルの MIME タイプ。 */
internal const val BACKUP_MIME_TYPE = "application/zip"

private const val APP_PACKAGE_NAME = "win.haya.doko"
private const val ENTRY_MANIFEST = "manifest.json"
private const val ENTRY_PHOTOS = "photos.json"
private const val ENTRY_STAMPS = "stamps.json"
private const val ENTRY_SETTINGS = "settings.json"
private const val PHOTO_DIR_PREFIX = "photos/"

/** バックアップ読み込み時のサイズ上限（壊れたファイル・ZIP 爆弾対策）。 */
private const val MAX_JSON_ENTRY_BYTES = 8 * 1024 * 1024
private const val MAX_PHOTO_ENTRY_BYTES = 32 * 1024 * 1024
private const val MAX_TOTAL_EXTRACTED_BYTES = 1024L * 1024 * 1024

/** 写真エントリーの ID として許可する形式（パス区切りや ".." を含む ID は読み込まない）。 */
private val PHOTO_ID_PATTERN = Regex("[A-Za-z0-9._-]{1,64}")

/** 取り込む店舗名の最大長（想定外に長い名前で保存先を壊さないため）。 */
private const val MAX_SHOP_NAME_LENGTH = 50

/** 取り込むスタンプ記録の最大件数。 */
private const val MAX_STAMP_RECORDS = 500

/** 使わないエントリーを読み飛ばすときの上限。 */
private const val MAX_UNKNOWN_ENTRY_BYTES = 32 * 1024 * 1024

/** バックアップの概要。読み込み前の内容確認にも使う。 */
internal data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val exportedAtMillis: Long,
    val photoCount: Int,
    val stampShopCount: Int,
    val stampTotalCount: Int,
    val hasSettings: Boolean
)

/** 読み込み方法。 */
internal enum class BackupImportMode {
    /** 現在のデータを残したまま追加する。 */
    MERGE,

    /** 現在のデータを削除して、バックアップの内容に置き換える。 */
    REPLACE
}

/** 読み込み結果。 */
internal data class BackupImportResult(
    val mode: BackupImportMode,
    val photosAdded: Int,
    val photosSkipped: Int,
    val stampShopsImported: Int,
    /** 置き換えモードで適用する設定（マージ時や設定が含まれないときは null）。 */
    val importedSettings: AppSettings?
)

private data class PhotoMeta(val shopName: String, val timestampMillis: Long)

/**
 * 写真・スタンプラリー・設定を 1 つの ZIP ファイルへ書き出し／読み込みする。
 *
 * サーバーは使わず、保存先は端末の保存先選択（SAF）でユーザーが指定する。
 * 書き出したファイルは SD カード・USB・クラウド・共有機能などで自由に別端末へ移せる。
 *
 * ZIP の中身:
 * ```
 * manifest.json    形式バージョン・アプリ版・作成日時・件数
 * stamps.json      店舗ごとのチェックイン回数と最終チェックイン
 * settings.json    テーマ・通知設定・位置情報の更新間隔
 * photos.json      写真のメタデータ（店舗名・撮影日時）
 * photos/00001.webp …（写真本体。番号で管理するので日本語ファイル名の問題を避ける）
 * ```
 */
internal object BackupManager {

    /** 保存先選択ダイアログに出す既定のファイル名。 */
    fun suggestedFileName(nowMillis: Long = System.currentTimeMillis()): String =
        "Yamaokaya_backup_${format("yyyyMMdd_HHmm", nowMillis)}.zip"

    /** バックアップの作成日時の表示用。 */
    fun formatExportedAt(manifest: BackupManifest): String =
        format("yyyy/MM/dd HH:mm", manifest.exportedAtMillis)

    /** SimpleDateFormat はスレッドセーフではないため、呼び出しごとに作る。 */
    private fun format(pattern: String, millis: Long): String =
        SimpleDateFormat(pattern, Locale.JAPAN).format(Date(millis))

    /**
     * 写真・スタンプラリー・設定を [target] へ書き出す。
     *
     * @param onProgress 写真の書き出し進捗（処理済み枚数, 全体枚数）
     */
    suspend fun export(
        context: Context,
        target: Uri,
        settings: AppSettings,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): BackupManifest = withContext(Dispatchers.IO) {
        val photoRepository = PhotoRepository(context)
        val stampRepository = StampRepository(context)

        val photos = photoRepository.loadAllPhotos()
        val stamps = stampRepository.getAllRecords()
        val manifest = BackupManifest(
            formatVersion = BACKUP_FORMAT_VERSION,
            appVersion = getAppVersionName(context),
            exportedAtMillis = System.currentTimeMillis(),
            photoCount = photos.size,
            stampShopCount = stamps.size,
            stampTotalCount = stamps.sumOf { it.count },
            hasSettings = true
        )

        val output = context.contentResolver.openOutputStream(target)
            ?: throw IllegalStateException("保存先を開けませんでした")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.writeJsonEntry(ENTRY_MANIFEST, manifestToJson(manifest))
            zip.writeJsonEntry(ENTRY_STAMPS, stampsToJson(stamps))
            zip.writeJsonEntry(ENTRY_SETTINGS, settingsToJson(settings))
            zip.writeJsonEntry(ENTRY_PHOTOS, photosToJson(photos))

            photos.forEachIndexed { index, entry ->
                onProgress(index, photos.size)
                zip.putNextEntry(ZipEntry(photoEntryName(index)))
                entry.file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
            onProgress(photos.size, photos.size)
        }
        manifest
    }

    /**
     * 読み込み前の内容確認用に、ZIP の manifest.json だけを読む。
     */
    suspend fun readManifest(context: Context, source: Uri): BackupManifest =
        withContext(Dispatchers.IO) {
            openZip(context, source) { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == ENTRY_MANIFEST) {
                        return@openZip parseManifest(zip.readTextEntry(MAX_JSON_ENTRY_BYTES))
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                throw IllegalStateException("バックアップファイルではありません（manifest.json が見つかりません）")
            }
        }

    /**
     * バックアップを読み込む。
     *
     * いったんキャッシュへ展開してから反映するため、途中で失敗しても現在のデータは壊れない
     * （置き換えモードでも、展開に成功したときだけ削除する）。
     *
     * @param onProgress 写真の読み込み進捗（処理済み枚数, 全体枚数）
     */
    suspend fun import(
        context: Context,
        source: Uri,
        mode: BackupImportMode,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): BackupImportResult = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "backup_import").apply {
            deleteRecursively()
            mkdirs()
        }

        try {
            // 展開した写真（id -> ファイル）とメタデータ。エントリーの並び順に依存しないよう、
            // いったん全部展開してから photos.json の内容と突き合わせる。
            val extractedFiles = linkedMapOf<String, File>()
            val photoMeta = mutableMapOf<String, PhotoMeta>()
            var extractedBytes = 0L
            var photosJsonSeen = false
            var manifest: BackupManifest? = null
            var stamps: List<StampRecord> = emptyList()
            var settings: AppSettings? = null

            // 1) キャッシュへ展開して中身を読む
            openZip(context, source) { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // 他ツールで作られた ZIP は区切りが \ のことがあるため / に正規化する
                    val name = entry.name.replace('\\', '/')
                    when {
                        name == ENTRY_MANIFEST ->
                            manifest = parseManifest(zip.readTextEntry(MAX_JSON_ENTRY_BYTES))

                        name == ENTRY_STAMPS ->
                            stamps = parseStamps(zip.readTextEntry(MAX_JSON_ENTRY_BYTES))

                        name == ENTRY_SETTINGS ->
                            settings = parseSettings(zip.readTextEntry(MAX_JSON_ENTRY_BYTES))

                        name == ENTRY_PHOTOS -> {
                            // photos.json は最初の 1 つだけ読む（大量の JSON でメモリを食い潰さないため）
                            if (!photosJsonSeen) {
                                photosJsonSeen = true
                                photoMetaFromJson(zip.readTextEntry(MAX_JSON_ENTRY_BYTES))
                                    .forEach { (id, meta) -> photoMeta[id] = meta }
                            }
                        }

                        name.startsWith(PHOTO_DIR_PREFIX) -> {
                            val id = name.removePrefix(PHOTO_DIR_PREFIX)
                            if (isSafePhotoId(id)) {
                                val file = File(workDir, id)
                                extractedBytes += zip.copyEntryTo(file, MAX_PHOTO_ENTRY_BYTES)
                                if (extractedBytes > MAX_TOTAL_EXTRACTED_BYTES) {
                                    throw IllegalStateException("バックアップの展開サイズが大きすぎます")
                                }
                                extractedFiles[id] = file
                                onProgress(extractedFiles.size, manifest?.photoCount ?: extractedFiles.size)
                            } else {
                                zip.drainEntryCapped(MAX_UNKNOWN_ENTRY_BYTES)
                            }
                        }

                        else -> zip.drainEntryCapped(MAX_UNKNOWN_ENTRY_BYTES)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val extracted = extractedFiles.mapNotNull { (id, file) ->
                photoMeta[id]?.let { meta -> meta to file }
            }

            val parsed = manifest
                ?: throw IllegalStateException("バックアップファイルではありません（manifest.json が見つかりません）")
            if (parsed.formatVersion > BACKUP_FORMAT_VERSION) {
                throw IllegalStateException(
                    "新しい形式のバックアップです。アプリを最新版に更新してから読み込んでください"
                )
            }

            // 写真本体と写真情報が食い違っているバックアップは、置き換えで既存の写真を
            // 消してしまわないよう、データを触る前に弾く。
            if (parsed.photoCount > extracted.size) {
                throw IllegalStateException(
                    "写真の一部を読み込めませんでした。バックアップファイルが壊れている可能性があります"
                )
            }
            if (extractedFiles.size != extracted.size) {
                throw IllegalStateException("バックアップ内の写真の情報が不足しています")
            }

            // 2) 現在のデータへ反映する
            val photoRepository = PhotoRepository(context)
            val stampRepository = StampRepository(context)

            var photosAdded = 0
            var photosSkipped = 0
            if (mode == BackupImportMode.REPLACE) {
                // 全部書き終わってから入れ替えるため、途中で失敗しても現在の写真は残る
                photosAdded = photoRepository.replaceAllWith(
                    extracted.map { (meta, file) ->
                        ImportedPhoto(
                            shopName = meta.shopName,
                            timestampMillis = meta.timestampMillis,
                            source = file
                        )
                    }
                )
            } else {
                extracted.forEach { (meta, file) ->
                    if (photoRepository.containsPhoto(meta.shopName, meta.timestampMillis)) {
                        photosSkipped++
                    } else if (photoRepository.saveImportedPhoto(meta.shopName, meta.timestampMillis, file)) {
                        photosAdded++
                    } else {
                        photosSkipped++
                    }
                }
            }

            val stampShops = stampRepository.restoreFromBackup(
                records = stamps,
                replace = mode == BackupImportMode.REPLACE
            )

            BackupImportResult(
                mode = mode,
                photosAdded = photosAdded,
                photosSkipped = photosSkipped,
                stampShopsImported = stampShops,
                importedSettings = if (mode == BackupImportMode.REPLACE) settings else null
            )
        } finally {
            workDir.deleteRecursively()
        }
    }

    /**
     * 書き出したバックアップを他の端末へ送れるように、共有用のコピーを作って URI を返す。
     */
    fun prepareShareCopy(context: Context, source: Uri, fileName: String): Uri {
        val shareDir = File(context.cacheDir, "backup_share").apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(shareDir, fileName)
        context.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("共有用のコピーを作成できませんでした")

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun photoEntryName(index: Int) = PHOTO_DIR_PREFIX + "%05d.webp".format(index)

    private inline fun <T> openZip(context: Context, source: Uri, block: (ZipInputStream) -> T): T {
        val input = context.contentResolver.openInputStream(source)
            ?: throw IllegalStateException("ファイルを開けませんでした")
        return ZipInputStream(BufferedInputStream(input)).use { zip -> block(zip) }
    }

    /** 写真エントリーの ID が安全か（"../" などでのディレクトリ脱出を防ぐ）。 */
    private fun isSafePhotoId(id: String): Boolean =
        PHOTO_ID_PATTERN.matches(id) && id != "." && id != ".."

    /** 使わないエントリーを上限付きで読み飛ばす（過大なデータでの時間消費を防ぐ）。 */
    private fun ZipInputStream.drainEntryCapped(limitBytes: Int) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limitBytes) {
                throw IllegalStateException("バックアップファイルに不正なデータが含まれています")
            }
        }
    }

    private fun ZipInputStream.readTextEntry(limitBytes: Int): String =
        readBytesCapped(limitBytes).toString(Charsets.UTF_8)

    /** 1 エントリーを上限付きで読み込む（過大なデータでメモリを食い潰さないため）。 */
    private fun ZipInputStream.readBytesCapped(limitBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (output.size() + read > limitBytes) {
                throw IllegalStateException("バックアップファイルの内容が大きすぎます")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    /** 写真 1 件を [target] へ上限付きでコピーし、書き込んだバイト数を返す。 */
    private fun ZipInputStream.copyEntryTo(target: File, limitBytes: Int): Long {
        var total = 0L
        target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                total += read
                if (total > limitBytes) {
                    throw IllegalStateException("写真のデータが大きすぎます")
                }
                output.write(buffer, 0, read)
            }
        }
        return total
    }

    private fun ZipOutputStream.writeJsonEntry(name: String, json: JSONObject) {
        putNextEntry(ZipEntry(name))
        write(json.toString().toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun manifestToJson(manifest: BackupManifest): JSONObject = JSONObject()
        .put("app", APP_PACKAGE_NAME)
        .put("format", manifest.formatVersion)
        .put("appVersion", manifest.appVersion)
        .put("exportedAt", manifest.exportedAtMillis)
        .put("photoCount", manifest.photoCount)
        .put("stampShopCount", manifest.stampShopCount)
        .put("stampTotalCount", manifest.stampTotalCount)
        .put("hasSettings", manifest.hasSettings)

    private fun parseManifest(text: String): BackupManifest {
        val json = JSONObject(text)
        val app = json.optString("app")
        if (app.isNotEmpty() && app != APP_PACKAGE_NAME) {
            throw IllegalStateException("別のアプリのバックアップファイルです")
        }
        return BackupManifest(
            formatVersion = json.optInt("format", 0),
            appVersion = json.optString("appVersion"),
            exportedAtMillis = json.optLong("exportedAt"),
            photoCount = json.optInt("photoCount"),
            stampShopCount = json.optInt("stampShopCount"),
            stampTotalCount = json.optInt("stampTotalCount"),
            hasSettings = json.optBoolean("hasSettings")
        )
    }

    private fun stampsToJson(records: List<StampRecord>): JSONObject = JSONObject().apply {
        put(
            "stamps",
            JSONArray().apply {
                records.forEach { record ->
                    put(
                        JSONObject()
                            .put("shop", record.shopName)
                            .put("count", record.count)
                            .put("lastCheckIn", record.lastCheckInMillis)
                    )
                }
            }
        )
    }

    private fun parseStamps(text: String): List<StampRecord> {
        val array = JSONObject(text).optJSONArray("stamps") ?: return emptyList()
        val now = System.currentTimeMillis()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val shopName = item.optString("shop")
            // 外部ファイル由来の値なので、そのまま保存せず範囲を検証する
            val count = item.optInt("count").coerceIn(0, MAX_CHECK_IN_COUNT)
            if (shopName.isEmpty() || shopName.length > MAX_SHOP_NAME_LENGTH || count <= 0) {
                return@mapNotNull null
            }

            StampRecord(
                shopName = shopName,
                count = count,
                // 未来の日時は取り込まない（チェックインできない状態になるため）
                lastCheckInMillis = item.optLong("lastCheckIn").coerceIn(0L, now)
            )
        }.take(MAX_STAMP_RECORDS)
    }

    private fun settingsToJson(settings: AppSettings): JSONObject = JSONObject()
        .put("theme", settings.themeMode.storageKey)
        .put("font", settings.fontMode.storageKey)
        .put("proximityNotification", settings.proximityNotificationEnabled)
        .put("trackerNotification", settings.trackerNotificationEnabled)
        .put("locationUpdateInterval", settings.locationUpdateIntervalSeconds)

    private fun parseSettings(text: String): AppSettings {
        val json = JSONObject(text)
        return AppSettings(
            proximityNotificationEnabled = json.optBoolean("proximityNotification", true),
            trackerNotificationEnabled = json.optBoolean("trackerNotification", true),
            // 外部ファイル由来の値なので、想定外の更新間隔は既定値に丸める
            locationUpdateIntervalSeconds = SettingsRepository.normalizeLocationUpdateInterval(
                json.optInt(
                    "locationUpdateInterval",
                    SettingsRepository.DEFAULT_LOCATION_INTERVAL_SECONDS
                )
            ),
            themeMode = ThemeMode.fromStorageKey(json.optString("theme").ifEmpty { null }),
            fontMode = FontMode.fromStorageKey(json.optString("font").ifEmpty { null })
        )
    }

    private fun photosToJson(photos: List<PhotoEntry>): JSONObject = JSONObject().apply {
        put(
            "photos",
            JSONArray().apply {
                photos.forEachIndexed { index, entry ->
                    put(
                        JSONObject()
                            .put("id", photoEntryName(index).removePrefix(PHOTO_DIR_PREFIX))
                            .put("shop", entry.shopName)
                            .put("timestamp", entry.timestampMillis)
                    )
                }
            }
        )
    }

    private fun photoMetaFromJson(text: String): Map<String, PhotoMeta> {
        val array = JSONObject(text).optJSONArray("photos") ?: return emptyMap()
        val result = linkedMapOf<String, PhotoMeta>()
        (0 until array.length()).forEach { index ->
            val item = array.optJSONObject(index) ?: return@forEach
            val id = item.optString("id")
            val shopName = item.optString("shop")
            if (!isSafePhotoId(id) || shopName.isEmpty() || shopName.length > MAX_SHOP_NAME_LENGTH) {
                return@forEach
            }
            result[id] = PhotoMeta(
                shopName = shopName,
                timestampMillis = item.optLong("timestamp").coerceAtLeast(0L)
            )
        }
        return result
    }
}
