# Agent Handoff Notes

## プロジェクト概要

**Yamaokaya is Doko?** は、現在地から最寄りの「山岡家」ラーメン店までの距離と方角を表示するフリー・オープンソースの Android アプリです。

- リポジトリ: `https://github.com/koba9813/yamaokaya`
- ブランチ: `main`
- パッケージ名: `win.haya.doko`
- 現在のバージョン: `V.beta3.0`（`app/build.gradle.kts` の `versionName`）

## 技術スタック

- Kotlin / Jetpack Compose
- Material Design 3
- Android 標準 LocationManager（Google Play Services 非依存）
- minSdk: 24 / targetSdk: 35 / compileSdk: 35
- AGP: 8.7.3 / Kotlin: 2.0.0 / Gradle: 8.13

## 最近の開発状況

### 直近のコミット

1. `d8357e7` Apply beta2.0 updates across Android app and web
   - Android アプリの UI・設定・スタンプラリー機能等の大幅改修
   - Web 側（`web/`）の更新
   - リリース成果物・証明書の削除
2. それ以前: パッケージ名変更、beta2.0 リリース準備、README 更新など

### 実装済み: アプリ内アップデート

GitHub Releases から最新 APK を自動確認し、アプリ内でダウンロード・インストールする機能を追加しました。

- 自動チェック: `YamaokayaScreen` 起動時（スプラッシュ後 1.2 秒）に GitHub Releases を確認
- 手動チェック: 設定画面「アップデート」セクションから確認可能
- 新しいバージョンがある場合、ダイアログでリリースノートを表示し、「ダウンロードしてインストール」で APK を取得・インストール
- 必要に応じて「不明なアプリのインストール」権限設定画面を開く

関連ファイル:
- `app/src/main/java/win/haya/doko/UpdateChecker.kt`
- `app/src/main/java/win/haya/doko/UpdateFlow.kt`（新規）
- `app/src/main/java/win/haya/doko/YamaokayaScreen.kt`
- `app/src/main/java/win/haya/doko/SettingsPage.kt`

## ビルド環境の注意点

- このプロジェクトは **JDK 17** が必要です
- Android Studio に同梱されている JDK が Java 25 の場合、Gradle/Kotlin コンパイラが動作しません
- コマンドラインからビルドする場合は JDK 17 を指定してください:
  ```powershell
  $env:JAVA_HOME = "C:\Path\To\jdk-17"
  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
  .\gradlew.bat --no-daemon :app:installDebug
  ```
- プロジェクトパスに日本語（`開発`）が含まれているため、`-Dfile.encoding=UTF-8` を指定しないと Kotlin コンパイルでパス解決に失敗することがあります

## 主要ファイル構成

### Android アプリ

- `MainActivity.kt` — エントリポイント、SplashScreen、Compose のセットアップ
- `YamaokayaScreen.kt` — メイン画面、位置情報取得、各種画面遷移、アップデート自動チェック
- `YamaokayaViewModel.kt` — 位置情報・方位・設定の ViewModel
- `YamaokayaFinder.kt` — 店舗データと最寄り店舗計算
- `SettingsPage.kt` — 設定画面、アップデート手動確認
- `StampRallyScreen.kt` / `StampRepository.kt` — スタンプラリー機能
- `GalleryScreen.kt` / `PhotoRepository.kt` — 訪問写真ギャラリー
- `UpdateChecker.kt` — GitHub Releases API 経由で最新 APK を確認・ダウンロード・インストール
- `UpdateFlow.kt` — アップデートダイアログとインストールフローの共通化 UI
- `NotificationUtils.kt` / `DistanceTrackerService.kt` — 接近通知・距離トラッカー
- `YamaokayaWidgetProvider.kt` — ホーム画面ウィジェット

### Web

- `web/` — PWA 版（Cloudflare Pages 向け）

## リリース・署名

- リリース署名は環境変数から読み込みます（`app/build.gradle.kts` 参照）
  - `RELEASE_KEYSTORE_PATH`
  - `RELEASE_KEYSTORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- `.gitignore` で `*.jks`, `*.crt`, `app/release/` は無視されています

## 既知の課題・次のステップ候補

- [ ] 接続されたデバイスへのデバッグ APK インストールが未完了（ADB 認識待ち）
- [ ] アプリ内アップデートの実機動作確認
- [ ] 「このバージョンをスキップ」機能の追加（毎回ダイアログが出るのを防ぐ）
- [ ] 最終アップデート確認日時の保存（起動ごとに API を叩きすぎないように）
- [ ] Web 側と Android 側のバージョン管理・リリースフローの整理

## ユーザー情報

- GitHub: `koba9813`
- ローカル Git ユーザー: `koba9813` / `Hayahitokoba@gmail.com`

## 備考

- ユーザーは比較的カジュアルな指示を好みます
- コミットメッセージは英語で統一されています
- 大きな変更を行う前に、現状の把握と簡潔な方針確認が推奨されます
