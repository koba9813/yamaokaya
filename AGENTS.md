# Agent Handoff Notes

## プロジェクト概要

**Yamaokaya is Doko?** は、現在地から最寄りの「山岡家」ラーメン店までの距離と方角を表示するフリー・オープンソースの Android アプリです。

- リポジトリ: `https://github.com/koba9813/yamaokaya`
- ブランチ: `main`
- パッケージ名: `win.haya.doko`
- 現在のバージョン: `V.beta3.1`（`app/build.gradle.kts` の `versionName`、`versionCode = 4`）

## 技術スタック

- Kotlin / Jetpack Compose
- Material Design 3（配色テーマはアプリ内で切り替え）
- CameraX（アプリ内カメラ）/ Coil（ギャラリーの画像読み込み）
- Android 標準 LocationManager（Google Play Services 非依存）
- minSdk: 24 / targetSdk: 35 / compileSdk: 35
- AGP: 8.7.3 / Kotlin: 2.0.0 / Gradle: 8.13

## 最近の開発状況

### 直近のコミット

1. `93adc9b` アップデート確認ダイアログをモダンなデザインに刷新
2. `ea08463` ドキュメント: AGENTS.md を V.beta3.1 リリース情報に更新
3. `e15d9f0` V.beta3.1 に更新（versionCode 4）
4. `5e36e4c` データの引き継ぎ（バックアップ書き出し／読み込み）機能を追加
5. `146b2db` 店舗データを更新（152店舗 → 198店舗）
6. `dccc6a1` ダーク/ライト/山岡家のテーマ切り替え機能を追加
7. `508bcf2` カメラとギャラリーのUIを刷新
8. `ff6a909` Add in-app update flow and agent handoff notes（アプリ内アップデート機能）

### 実装済み: テーマ切り替え

- `ThemeMode`（ダーク / ライト / 山岡家）を設定画面から切り替え、`SharedPreferences`（`app_settings` / `theme_mode`）に保存し起動時に復元
- ダーク = 既定。従来どおり端末の壁紙由来カラー（Android 12+ の dynamic color）を使う
- ライト = 白を基調とした配色（`primary` は `#D71920`）
- 山岡家 = 赤 `#D71920` / 黒 `#111111` / 白 / 金 `#F5B82E` / ブラウン `#6B211B`
- 配色は `Color.kt` のトークン → `AppTheme.kt` のカラースキームに集約（`colorSchemeFor(themeMode)`）。設定画面のプレビューも同じスキームを参照
- ステータスバー / ナビゲーションバーのアイコン色もテーマに追従（`WindowCompat`）
- 既知の制約: 山岡家テーマの赤い見出しは黒背景でコントラスト 3.6:1（太字見出しとしては可読だが AA 通常テキスト基準は満たさない）

### 実装済み: カメラ / ギャラリー

- CameraX によるアプリ内カメラ（`CameraCaptureScreen`）と、端末の写真選択（`PickVisualMedia`）の両方に対応
- ギャラリーは Coil + 店舗名検索 / 店舗フィルタ / 複数選択と一括削除 / ピンチズーム表示
- カメラ画面と写真ビューアは意図的に黒背景（メディア表示のためテーマ非依存）

### 実装済み: データの引き継ぎ（バックアップ）

- 写真・スタンプラリー・設定を 1 つの ZIP（`manifest.json` / `stamps.json` / `settings.json` / `photos.json` / `photos/*.webp`）にまとめ、SAF（Storage Access Framework）の `CreateDocument` / `OpenDocument` で保存・復元
- 書き出し後は共有用コピーを `cacheDir/backup_share/` に作成し、共有シートで他端末へ送信可能
- 読み込みは **追加で読み込む（マージ）** と **完全に置き換える** の 2 方式
  - 追加: 重複写真（店舗名＋タイムスタンプ一致）はスキップ、スタンプは回数が多い方／日時が新しい方を採用、設定は変えない
  - 置き換え: 写真を別フォルダー（`shop_photos_staging`）へ書き出してから `shop_photos` と入れ替えるため、途中で失敗しても既存の写真は残る
- 外部バックアップ由来の値は検証してから保存
  - 店舗名の長さ 50 文字以内、パス区切り・ `"."` / `".."` を正規化・除外
  - チェックイン回数は `0..100_000` にクランプ、未来日時は現在時刻にクランプ
  - 位置情報更新間隔は `{5,10,30,60}` 以外なら既定値 10 秒に丸める
  - テーマは不明なら `DARK` にフォールバック
- パストラバーサル対策：エントリー ID に `/` や `..` を含むものは読み込まない
- ZIP 爆弾対策：JSON 8 MB / 写真 1 枚 32 MB / 合計展開 1 GB の上限
- 写真情報が欠けたバックアップ（`photos.json` にない写真ファイル、または `manifest.json` の `photoCount` と実数が不一致）は、データを触る前にエラーとして弾く
- 単体テスト 13 件追加（合計 42 件、全件成功）
- 関連ファイル:
  - `app/src/main/java/win/haya/doko/BackupManager.kt` — 書き出し／読み込みの核心ロジック
  - `app/src/main/java/win/haya/doko/BackupSection.kt` — 設定画面の UI と SAF 連携
  - `app/src/main/java/win/haya/doko/PhotoRepository.kt` — `containsPhoto` / `saveImportedPhoto` / `replaceAllWith` / `sanitize` 強化
  - `app/src/main/java/win/haya/doko/StampRepository.kt` — `getAllRecords` / `restoreFromBackup` / `mergeStampStates`
  - `app/src/main/java/win/haya/doko/SettingsRepository.kt` — `normalizeLocationUpdateInterval`
  - `app/src/main/res/xml/file_paths.xml` — FileProvider を用途別サブフォルダーに限定
  - `app/src/test/java/win/haya/doko/BackupManagerTest.kt` — 13 件のユニットテスト

### 実装済み: アプリ内アップデート

GitHub Releases から最新 APK を自動確認し、アプリ内でダウンロード・インストールする機能があります。

- 自動チェック: `YamaokayaScreen` 起動時（スプラッシュ後 1.2 秒）に GitHub Releases を確認
- 手動チェック: 設定画面「アップデート」セクションから確認可能
- 新しいバージョンがある場合、ダイアログでリリースノートを表示し、「ダウンロードしてインストール」で APK を取得・インストール
- 必要に応じて「不明なアプリのインストール」権限設定画面を開く
- APK ダウンロードに HTTP ステータスとサイズの検証を追加（途中で切れたダウンロードを検出）
- ダイアログは AlertDialog ではなく、角丸サーフェス + グラデーションのアイコンバッジ + バージョン比較チップ + リリースノートカード（下端フェード付き）のカスタム UI
  - ダウンロード中は円形プログレス + パーセント表示
  - テーマ（ダーク / ライト / 山岡家）に追従

関連ファイル:
- `app/src/main/java/win/haya/doko/UpdateChecker.kt`
- `app/src/main/java/win/haya/doko/UpdateFlow.kt`
- `app/src/main/java/win/haya/doko/YamaokayaScreen.kt`
- `app/src/main/java/win/haya/doko/SettingsPage.kt`

## ビルド環境の注意点

- デバッグ / リリースとも **JBR 21**（`C:\Users\user\.jdks\jbr-21.0.11`）でビルド・実機動作を確認済み（この環境に JDK 17 は未インストール）
- コマンドラインからビルドする場合:
  ```powershell
  $env:JAVA_HOME = "C:\Users\user\.jdks\jbr-21.0.11"
  $env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
  .\gradlew.bat --no-daemon :app:installDebug
  ```
- プロジェクトパスに日本語（`開発`）が含まれているため、`-Dfile.encoding=UTF-8` を指定しないと Kotlin コンパイルでパス解決に失敗することがあります
- **ユニットテストは日本語パスのままだと全件失敗します**（`sun.jnu.encoding=MS932` のため Gradle のテストワーカーがクラスを解決できず `ClassNotFoundException`）。ASCII パス経由で実行してください:
  ```powershell
  subst Y: "C:\Users\user\開発\yamaokaya"
  cd Y:\
  .\gradlew.bat --no-daemon :app:testDebugUnitTest
  ```

## 主要ファイル構成

### Android アプリ

- `MainActivity.kt` — エントリポイント、SplashScreen、テーマ適用
- `AppTheme.kt` — `ThemeMode` ごとのカラースキーム、タイポグラフィ、`YamaokayaTheme`
- `ThemeMode.kt` — テーマ定義（ダーク / ライト / 山岡家）と保存キー
- `Color.kt` — 各テーマのカラートークン
- `YamaokayaScreen.kt` — メイン画面、位置情報取得、各種画面遷移、アップデート自動チェック
- `YamaokayaViewModel.kt` — 位置情報・方位・設定の ViewModel
- `YamaokayaFinder.kt` — 店舗データ（198店舗）と最寄り店舗計算
- `SettingsPage.kt` / `SettingsRepository.kt` — 設定画面（テーマ・通知・アップデート）と `AppSettings` の永続化
- `StampRallyScreen.kt` / `StampRepository.kt` — スタンプラリー機能
- `GalleryScreen.kt` / `PhotoRepository.kt` — 訪問写真ギャラリー
- `CameraScreen.kt` — CameraX によるアプリ内カメラ
- `UpdateChecker.kt` — GitHub Releases API 経由で最新 APK を確認・ダウンロード・インストール
- `UpdateFlow.kt` — アップデートダイアログとインストールフローの共通化 UI
- `NotificationUtils.kt` / `DistanceTrackerService.kt` — 接近通知・距離トラッカー
- `YamaokayaWidgetProvider.kt` — ホーム画面ウィジェット（配色はテーマ非連動）

### テスト

- `app/src/test/java/win/haya/doko/` — Robolectric ベースのユニットテスト（`YamaokayaFinderTest` / `StampRepositoryTest` / `SettingsRepositoryTest` / `LocationUtilsTest` / `BackupManagerTest`）
- `UpdateFlowScreenshotTest` — アップデート関連ダイアログを Robolectric + Compose で描画し、`app/build/screenshots/*.png` に出力する見た目確認用テスト（`@GraphicsMode(NATIVE)` + `unitTests.isIncludeAndroidResources = true` が必要）
  - `captureToImage()` は Robolectric のダイアログ窓では再描画待ちでタイムアウトするため、`ShadowDialog.getLatestDialog()` の DecorView を直接ビットマップへ描画している

### Web

- `web/` — PWA 版（Cloudflare Pages 向け）。`web/shops.json` はアプリと同じ店舗データ

## リリース・署名

- リリース署名は環境変数から読み込みます（`app/build.gradle.kts` 参照）
  - `RELEASE_KEYSTORE_PATH`
  - `RELEASE_KEYSTORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- 実際の値はリポジトリ外の `C:\Users\user\.yamaokaya\keystore.properties` に保存済み（keystore: `release.keystore`、alias: `yamaokaya`）
- `.gitignore` で `*.jks`, `*.crt`, `app/release/` は無視されています

### リリース手順（例: V.beta3.1）

1. `app/build.gradle.kts` の `versionCode` / `versionName` を更新（`V.beta3.1` / `4`）
2. 署名用の環境変数を設定して `:app:assembleRelease` を実行（出力: `app/build/outputs/apk/release/app-release.apk`）
3. GitHub Release を作成（タグ `V.beta3.1`、アセット名 `Yamaokaya_beta3.1.apk`）
4. **必ず通常リリースとして公開する**（prerelease / draft は `/releases/latest` から見えず、アプリ内アップデートが反応しない）
5. アプリ内アップデートが動くのは、公開 APK がインストール済みアプリと同じ鍵で署名されている場合のみ（旧鍵の V.beta2.0 以前からは更新不可）

- 過去のリリース命名: V.beta1.0 Syoyu / V.beta1.1 Miso / V.beta1.2 Shio / V.beta2.0 Karamiso / V.beta3.0 Tokusei Miso / V.beta3.1
- `gh` CLI は未インストール。GitHub API を叩く場合は Windows 資格情報マネージャーのトークンを使用（`"protocol=https\`nhost=github.com\`n" | git credential fill`）

## 既知の課題・次のステップ候補

- [x] デバッグ APK の実機インストール
- [x] アプリ内アップデートの実機動作確認
- [x] テーマ切り替えの実機確認（3テーマ + 再起動後の復元）
- [x] データの引き継ぎ（バックアップ書き出し／読み込み）機能の実装
- [x] バックアップ読み込みの堅牢化（パストラバーサル拒否、サイズ上限、値の検証、置き換え時の原子的入れ替え）
- [x] V.beta3.1 としてリリース（versionCode 4、同一署名）
- [ ] 「このバージョンをスキップ」機能の追加（毎回ダイアログが出るのを防ぐ）
- [ ] 最終アップデート確認日時の保存（起動ごとに API を叩きすぎないように）
- [ ] 山岡家テーマの見出し赤色のコントラスト改善（必要なら `primaryLight` / `primaryYamaokaya` を明るめに調整）
- [ ] ホーム画面ウィジェットをテーマ配色に追従させるか検討
- [ ] Web 側と Android 側のバージョン管理・リリースフローの整理
- [ ] バックアップ機能の実機動作確認（書き出し・読み込み・共有・置き換えの一連の流れ）

## ユーザー情報

- GitHub: `koba9813`
- ローカル Git ユーザー: `koba9813` / `Hayahitokoba@gmail.com`

## 備考

- ユーザーは比較的カジュアルな指示を好みます
- コミットメッセージは日本語（1行目に要約、本文に変更点の箇条書き）
- 大きな変更を行う前に、現状の把握と簡潔な方針確認が推奨されます
- 実機: moto g66j 5G（adb: `C:\Users\user\AppData\Local\Android\Sdk\platform-tools\adb.exe`）
- スクリーンショットは `cmd /c "adb exec-out screencap -p > file.png"`（PowerShell の `>` はバイナリを壊すため）
- UI 操作の自動化は `adb shell uiautomator dump` + `input tap` が確実（座標は 1080x2400 の実ピクセル）
