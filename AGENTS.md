# Agent Handoff Notes

## プロジェクト概要

**Yamaokaya is Doko?** は、現在地から最寄りの「山岡家」ラーメン店までの距離と方角を表示するフリー・オープンソースの Android アプリです。

- リポジトリ: `https://github.com/koba9813/yamaokaya`
- ブランチ: `main`
- パッケージ名: `win.haya.doko`
- 現在のバージョン: `V.beta3.0`（`app/build.gradle.kts` の `versionName`、`versionCode = 3`）

## 技術スタック

- Kotlin / Jetpack Compose
- Material Design 3（配色テーマはアプリ内で切り替え）
- CameraX（アプリ内カメラ）/ Coil（ギャラリーの画像読み込み）
- Android 標準 LocationManager（Google Play Services 非依存）
- minSdk: 24 / targetSdk: 35 / compileSdk: 35
- AGP: 8.7.3 / Kotlin: 2.0.0 / Gradle: 8.13

## 最近の開発状況

### 直近のコミット

1. `146b2db` 店舗データを更新（152店舗 → 198店舗）
2. `dccc6a1` ダーク/ライト/山岡家のテーマ切り替え機能を追加
3. `508bcf2` カメラとギャラリーのUIを刷新
4. `ff6a909` Add in-app update flow and agent handoff notes（アプリ内アップデート機能）

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

### 実装済み: アプリ内アップデート

GitHub Releases から最新 APK を自動確認し、アプリ内でダウンロード・インストールする機能があります。

- 自動チェック: `YamaokayaScreen` 起動時（スプラッシュ後 1.2 秒）に GitHub Releases を確認
- 手動チェック: 設定画面「アップデート」セクションから確認可能
- 新しいバージョンがある場合、ダイアログでリリースノートを表示し、「ダウンロードしてインストール」で APK を取得・インストール
- 必要に応じて「不明なアプリのインストール」権限設定画面を開く

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

- `app/src/test/java/win/haya/doko/` — Robolectric ベースのユニットテスト（`YamaokayaFinderTest` / `StampRepositoryTest` / `SettingsRepositoryTest` / `LocationUtilsTest`）

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

### リリース手順（例: V.beta3.0）

1. `app/build.gradle.kts` の `versionCode` / `versionName` を更新
2. 署名用の環境変数を設定して `:app:assembleRelease` を実行（出力: `app/build/outputs/apk/release/app-release.apk`）
3. GitHub Release を作成（タグ `V.betaX.Y`、アセット名 `Yamaokaya_betaX.Y_<Flavor>.apk`）
4. **必ず通常リリースとして公開する**（prerelease / draft は `/releases/latest` から見えず、アプリ内アップデートが反応しない）
5. アプリ内アップデートが動くのは、公開 APK がインストール済みアプリと同じ鍵で署名されている場合のみ（旧鍵の V.beta2.0 以前からは更新不可）

- 過去のリリース命名: V.beta1.0 Syoyu / V.beta1.1 Miso / V.beta1.2 Shio / V.beta2.0 Karamiso
- `gh` CLI は未インストール。GitHub API を叩く場合は Windows 資格情報マネージャーのトークンを使用（`"protocol=https\`nhost=github.com\`n" | git credential fill`）

## 既知の課題・次のステップ候補

- [x] デバッグ APK の実機インストール
- [x] アプリ内アップデートの実機動作確認
- [x] テーマ切り替えの実機確認（3テーマ + 再起動後の復元）
- [ ] 「このバージョンをスキップ」機能の追加（毎回ダイアログが出るのを防ぐ）
- [ ] 最終アップデート確認日時の保存（起動ごとに API を叩きすぎないように）
- [ ] 山岡家テーマの見出し赤色のコントラスト改善（必要なら `primaryLight` / `primaryYamaokaya` を明るめに調整）
- [ ] ホーム画面ウィジェットをテーマ配色に追従させるか検討
- [ ] Web 側と Android 側のバージョン管理・リリースフローの整理

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
