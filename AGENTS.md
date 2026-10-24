# Agent Handoff Notes

## プロジェクト概要

**Yamaokaya is Doko?** は、現在地から最寄りの「山岡家」ラーメン店までの距離と方角を表示するフリー・オープンソースの Android アプリです。

- リポジトリ: `https://github.com/koba9813/yamaokaya`
- ブランチ: `main`
- パッケージ名: `win.haya.doko`
- 現在のバージョン: `V.beta3.2`（`app/build.gradle.kts` の `versionName`、`versionCode = 8`）

## 技術スタック

- Kotlin / Jetpack Compose
- Material Design 3（配色テーマはアプリ内で切り替え）
- CameraX（アプリ内カメラ）/ Coil（ギャラリーの画像読み込み）
- Android 標準 LocationManager（Google Play Services 非依存）
- minSdk: 24 / targetSdk: 35 / compileSdk: 35
- AGP: 8.7.3 / Kotlin: 2.0.0 / Gradle: 8.13

## 最近の開発状況

### 直近のコミット

1. `e15d9f0` V.beta3.1 に更新（versionCode 4）
2. `93adc9b` アップデート確認ダイアログを刷新 → その後のコミット群（`688a2e5` 〜 `a653884`）で Liquid Glass（ベータ）・新デザイン・ウィジェットのバリエーションを追加し、V.beta3.2 / V.beta3.2.1 としてリリース
3. `a14518a` Liquid Glass関連のUI/UXをV.beta3.1の状態に戻す（ウィジェットのバリエーションは維持）
4. `d1dbfbe` V.beta3.1.1 に更新（versionCode 7）
5. V.beta3.2 に更新（versionCode 8）— ギャラリー全面刷新、Instagram ストーリー共有、アップデート通知、公式アプリ導線、Information 刷新、コントラスト修正ほか

### 取り下げ: Liquid Glass（ベータ）

- V.beta3.2 / V.beta3.2.1 で追加した Liquid Glass と新デザインシステムは取り下げました（GitHub Release・タグも削除済み）
- 各画面とアップデートダイアログは V.beta3.1 のデザインに戻っています。ウィジェットのバリエーション（3 種類）のみ維持し、V.beta3.1.1 としてリリース
- ウィジェットはガラス描画を廃止して通常スタイルに固定（Liquid Glass 設定自体が削除されたため）
- V.beta3.2 / V.beta3.2.1 から V.beta3.1.1 へは、アプリ内アップデートでは案内されません（versionName の数値比較のため、手動で APK をインストールする必要がある）

### 実装済み: テーマ切り替え

- `ThemeMode`（ダーク / ライト / 山岡家）を設定画面から切り替え、`SharedPreferences`（`app_settings` / `theme_mode`）に保存し起動時に復元
- ダーク = 既定。従来どおり端末の壁紙由来カラー（Android 12+ の dynamic color）を使う
- ライト = 白を基調とした配色（`primary` は `#D71920`）
- 山岡家 = 赤 `#D71920` / 黒 `#111111` / 白 / 金 `#F5B82E` / ブラウン `#6B211B`
- 配色は `Color.kt` のトークン → `AppTheme.kt` のカラースキームに集約（`colorSchemeFor(themeMode)`）。設定画面のプレビューも同じスキームを参照
- ステータスバー / ナビゲーションバーのアイコン色もテーマに追従（`WindowCompat`）
- 既知の制約: 山岡家テーマの赤い見出しは黒背景でコントラスト 3.6:1（太字見出しとしては可読だが AA 通常テキスト基準は満たさない）

### 実装済み: フォント切り替え

- `FontMode`（Smart Font UI / 丸ゴシック）を設定画面から切り替え、`SharedPreferences`（`app_settings` / `font_mode`）に保存し起動時に復元
- Smart Font UI = 既定。`res/font/smart_font_ui.otf`。1 ウェイトのみのため、太字は Compose の合成太字（FontSynthesis）に任せる
- 丸ゴシック = `res/font/zen_maru_gothic_regular.ttf` / `zen_maru_gothic_bold.ttf`（Zen Maru Gothic）
- フォントは `AppTheme.kt` の `fontFamilyFor` / `typographyFor` に集約し、`YamaokayaTheme(themeMode, fontMode)` でアプリ全体に適用
- 設定画面のフォント選択プレビュー（「あアA」）も同じフォントファミリーを参照
- バックアップの `settings.json` に `font` を保存。旧バックアップに項目がない場合は Smart Font UI にフォールバック

### 実装済み: カメラ / ギャラリー

- CameraX によるアプリ内カメラ（`CameraCaptureScreen`）と、端末の写真選択（`PickVisualMedia`）の両方に対応
- ギャラリーは Coil + 店舗名検索 / 店舗フィルタ / 複数選択と一括削除 / ピンチズーム表示
- カメラ画面と写真ビューアは意図的に黒背景（メディア表示のためテーマ非依存）

### 実装済み: ウィジェットのバリエーション

- 標準（4×2 ベース、サイズに応じて small / wide / tall を自動切替）— `YamaokayaWidgetProvider`
- コンパクト（4×1、店舗名 + 距離を 1 行表示）— `YamaokayaCompactWidgetProvider`
- ミニマル（2×2、距離を大きく表示）— `YamaokayaMinimalWidgetProvider`
- 更新処理は `WidgetUpdater`（`YamaokayaWidgetProvider.kt` 内）に共通化。`updateAllWidgets` は 3 種類すべてを更新する
- 新レイアウト: `widget_yamaokaya_compact.xml` / `widget_yamaokaya_minimal.xml`（`previewLayout` でウィジェットピッカーにプレビュー表示、Android 12+）
- ウィジェットピッカーの表示名: 標準（サイズで変化）/ コンパクト / ミニマル
- 配色はテーマ非連動のライト固定

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

関連ファイル:
- `app/src/main/java/win/haya/doko/UpdateChecker.kt`
- `app/src/main/java/win/haya/doko/UpdateFlow.kt`
- `app/src/main/java/win/haya/doko/YamaokayaScreen.kt`
- `app/src/main/java/win/haya/doko/SettingsPage.kt`

### 実装済み: アップデート通知

- WorkManager（`work-runtime-ktx` 2.9.1）で 12 時間ごとに GitHub Releases を定期チェックし、新バージョンがあればアプリの通知（チャンネル「アップデート」）でお知らせ
- 通知済みのバージョンを `SharedPreferences` に記録し、同じバージョンで重複通知しない（アプリ内ダイアログ表示時にも記録）
- 通知タップでアプリが起動し、即座にアップデートダイアログを表示（`singleTop` + `onNewIntent`）
- 起動時の自動チェックは 6 時間間隔に間引き（GitHub API を叩きすぎないため）
- 関連ファイル: `UpdateCheckWorker.kt` / `NotificationUtils.kt` / `MainActivity.kt`

### 実装済み: ギャラリーの全面刷新

- フルブリードの3列グリッド＋「写真 / 店舗 / お気に入り」の3タブ（SegmentedButton）
- お気に入り（`PhotoRepository` に永続化）・店舗別アルバム（表紙＋枚数）・月ジャンプ（月チップ＋スクロール）
- 共有はボトムシート形式（Instagram ストーリー / LINE / X / その他）
- メイン画面・スタンプラリーのギャラリーボタンは、写真の取り込み（Photo Picker）ではなくギャラリーページへ遷移するように変更（写真の追加はアプリ内カメラのみに）
- 関連ファイル: `GalleryScreen.kt` / `PhotoRepository.kt` / `ShareWatermark.kt`

### 実装済み: Instagram ストーリー共有と透かし

- 共有画像に「Yamaokaya is Doko?」の透かし（左上から下への縦向き、白文字＋影）
- Instagram ストーリーは 9:16（1080×1920）のキャンバス画像（ぼかし背景＋中央に写真＋透かし）を生成して共有（9:16 トリミングで透かしが切れないように）
- 関連ファイル: `ShareWatermark.kt` / `GalleryScreen.kt`

### 実装済み: 山岡家公式アプリへの導線

- メニューボタン（メイン画面「山岡家公式」・チェックイン後）が公式アプリ（`li.yapp.appCC40B910`）を起動。未インストール時は公式メニューページへフォールバック
- Information ページにも公式サイト・公式アプリ・GitHub へのリンクを追加
- 関連ファイル: `AppUtils.kt` / `InformationPage.kt`

### 実装済み: Information ページの刷新

- アプリアイコン・バージョン表示のヒーローカード、主な機能、権限の説明、リンク（GitHub / 公式サイト / 公式アプリ）、このアプリについて のセクション構成に刷新

### 修正: コントラストと細かな不具合

- ギャラリー・Information のルートを `Surface` でラップし、アイコン（戻る矢印など）が `LocalContentColor` の黒のままで背景に溶ける問題を修正
- 距離トラッカー通知とアップデート通知の通知 ID 衝突（どちらも 1002）を修正（アップデート通知は 1003）
- 非推奨アイコン `Icons.Filled.ExitToApp` → `Icons.AutoMirrored.Filled.ExitToApp` に置換
- スタンプラリー画面の無処理の LaunchedEffect を削除

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

### ワイヤレスデバッグ

- 実機 moto g66j 5G（Android 16 / SDK 36）で「ワイヤレスデバッグ」を有効化・ペアリング済み
- 接続は IP・ポートに依存しない mDNS 名で行う:
  ```powershell
  $adb = "C:\Users\user\AppData\Local\Android\Sdk\platform-tools\adb.exe"
  & $adb connect adb-ZY32MCV2Q7-SZtdPX._adb-tls-connect._tcp
  ```
- 繋がらないときは `& $adb mdns services` で `_adb-tls-connect._tcp` のポートを確認し、`& $adb connect 192.168.11.8:<ポート>` で接続する（ポートはトグルや再起動で変わる）
- adb は mDNS でデバイスを見つけても自動再接続しないため、上記 `adb connect` が必要
- USB とワイヤレスが同時に見えると `more than one device` になるので、`& $adb -s <serial>` か `$env:ANDROID_SERIAL` で指定する
- 新規ペアリングはスマホ側「ワイヤレスデバッグ → ペア設定コードによるデバイスのペア設定」を開き、`& $adb pair <IP>:<ペアリングポート> <6桁コード>` を実行

## 主要ファイル構成

### Android アプリ

- `MainActivity.kt` — エントリポイント、SplashScreen、テーマ適用
- `AppTheme.kt` — `ThemeMode` ごとのカラースキーム、`FontMode` ごとのタイポグラフィ、`YamaokayaTheme`
- `FontMode.kt` — フォント定義（Smart Font UI / 丸ゴシック）と保存キー
- `ThemeMode.kt` — テーマ定義（ダーク / ライト / 山岡家）と保存キー
- `Color.kt` — 各テーマのカラートークン
- `YamaokayaScreen.kt` — メイン画面、位置情報取得、各種画面遷移、アップデート自動チェック
- `YamaokayaViewModel.kt` — 位置情報・方位・設定の ViewModel
- `YamaokayaFinder.kt` — 店舗データ（198店舗）と最寄り店舗計算
- `SettingsPage.kt` / `SettingsRepository.kt` — 設定画面（表示 / 通知と位置情報 / データの引き継ぎ / アプリ / サポートの 5 セクション）と `AppSettings` の永続化
- `StampRallyScreen.kt` / `StampRepository.kt` — スタンプラリー機能
- `GalleryScreen.kt` / `PhotoRepository.kt` — 訪問写真ギャラリー
- `CameraScreen.kt` — CameraX によるアプリ内カメラ
- `UpdateChecker.kt` — GitHub Releases API 経由で最新 APK を確認・ダウンロード・インストール
- `UpdateFlow.kt` — アップデートダイアログとインストールフローの共通化 UI
- `NotificationUtils.kt` / `DistanceTrackerService.kt` — 接近通知・距離トラッカー
- `YamaokayaWidgetProvider.kt` — 3 種類のホーム画面ウィジェット（標準 / コンパクト / ミニマル）と共通更新処理 `WidgetUpdater`

### テスト

- `app/src/test/java/win/haya/doko/` — Robolectric ベースのユニットテスト（`YamaokayaFinderTest` / `StampRepositoryTest` / `SettingsRepositoryTest` / `LocationUtilsTest` / `BackupManagerTest`）
- `WidgetScreenshotTest` — ウィジェット各レイアウト（標準 / コンパクト / ミニマル）を `RemoteViews.apply()` で描画して PNG 出力（`@GraphicsMode(NATIVE)` + `unitTests.isIncludeAndroidResources = true` が必要）

### Web

- `web/` — PWA 版（Cloudflare Pages 向け）。`web/shops.json` はアプリと同じ店舗データ

## README の運用

- `README.md` は**バージョン番号・リリース固有の情報を含めない**、純粋なアプリ紹介として保つ
- 機能の追加・変更・削除を行ったときは、README の「主な機能」「必要な権限」「対応環境」「技術スタック」を同じ変更内で更新する
- 画面構成やデザインを変えたときは `docs/screenshots/` のスクリーンショットも撮り直す
  - 撮影対象: メイン画面 / 設定画面 / スタンプラリー / 写真ギャラリー
  - 端末の実画面を `adb exec-out screencap -p` で取得し、README 用に幅 540px 程度へ縮小して保存する
- リリースノートや変更履歴は README ではなく GitHub Releases に書く

## リリース・署名

- リリース署名は環境変数から読み込みます（`app/build.gradle.kts` 参照）
  - `RELEASE_KEYSTORE_PATH`
  - `RELEASE_KEYSTORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- 実際の値はリポジトリ外の `C:\Users\user\.yamaokaya\keystore.properties` に保存済み（keystore: `release.keystore`、alias: `yamaokaya`）
- `.gitignore` で `*.jks`, `*.crt`, `app/release/` は無視されています

### リリース手順（例: V.beta3.1.1）

1. `app/build.gradle.kts` の `versionCode` / `versionName` を更新（`V.beta3.1.1` / `7`）
2. 署名用の環境変数を設定して `:app:assembleRelease` を実行（出力: `app/build/outputs/apk/release/app-release.apk`）
3. GitHub Release を作成（タグ `V.beta3.1.1`、アセット名 `Yamaokaya_beta3.1.1.apk`）
4. **必ず通常リリースとして公開する**（prerelease / draft は `/releases/latest` から見えず、アプリ内アップデートが反応しない）
5. アプリ内アップデートが動くのは、公開 APK がインストール済みアプリと同じ鍵で署名されている場合のみ（旧鍵の V.beta2.0 以前からは更新不可）
6. README の機能一覧・スクリーンショットが最新の実装と一致しているか確認する

- 過去のリリース命名: V.beta1.0 Syoyu / V.beta1.1 Miso / V.beta1.2 Shio / V.beta2.0 Karamiso / V.beta3.0 Tokusei Miso / V.beta3.1 / V.beta3.1.1（V.beta3.2 / V.beta3.2.1 は取り下げ済み）
- `gh` CLI は未インストール。GitHub API を叩く場合は Windows 資格情報マネージャーのトークンを使用（`"protocol=https\`nhost=github.com\`n" | git credential fill`）

## 既知の課題・次のステップ候補

- [x] デバッグ APK の実機インストール
- [x] アプリ内アップデートの実機動作確認
- [x] テーマ切り替えの実機確認（3テーマ + 再起動後の復元）
- [x] データの引き継ぎ（バックアップ書き出し／読み込み）機能の実装
- [x] バックアップ読み込みの堅牢化（パストラバーサル拒否、サイズ上限、値の検証、置き換え時の原子的入れ替え）
- [x] V.beta3.1 としてリリース（versionCode 4、同一署名）
- [x] ウィジェットのバリエーション追加（コンパクト / ミニマル）
- [x] V.beta3.2 / V.beta3.2.1（Liquid Glass・新デザイン）の取り下げと V.beta3.1.1 への収束
- [x] アップデート通知（WorkManager による定期チェック・通知）
- [x] 最終アップデート確認日時の保存（起動時自動チェックを6時間間隔に間引き）
- [x] V.beta3.2 としてリリース（versionCode 8、同一署名）
- [ ] ウィジェット 3 種類の実機確認（配置・サイズ変化・更新）
- [ ] 「このバージョンをスキップ」機能の追加（毎回ダイアログが出るのを防ぐ）
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
