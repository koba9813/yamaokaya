# Yamaokaya is Doko? （V.beta2.0）

現在地から最寄りの「山岡家」までの距離と方角を表示する、フリー・オープンソースの Android アプリです。

---

## インストール

- **Android 限定配信アカウント** からインストール
- または [GitHub Releases](https://github.com/koba9813/yamaokaya/releases) から最新 APK をダウンロードできます。

---

## 主な機能

- 現在地から最寄りの山岡家店舗までの距離・方角をリアルタイム表示
- 画面中央の方向指示画像が店舗方向を指し示す
- 最寄り店舗までの距離を常時通知（バックグラウンド対応）
- 店舗 50m 圏内に入ると特別な演出と通知
- 訪問履歴を残せる「スタンプラリー（チェックイン）」機能
- 店舗までの距離を簡単に共有できるシェア機能
- ホーム画面ウィジェットで最寄り店舗の距離を確認
  - 横長ウィジェット：Google マップのルートボタン表示
  - 縦長ウィジェット：近い順に複数店舗を表示

---

## 必要な権限

- **位置情報（ precise / approximate ）**: 現在地を取得して最寄り店舗を計算するため
- **バックグラウンド位置情報**: 常時通知で距離を更新し続けるために使用
- **通知**: 接近通知・距離トラッカー通知の表示に使用

---

## 注意事項

- 検索対象はアプリ内に登録された店舗座標のみです
- 検索半径は 100km です
- バックグラウンド通知には「常に許可」の位置情報権限が必要です
- Foreground Service を利用するため、距離トラッカー有効時は通知が常時表示されます
- 本アプリは山岡家の公式アプリではありません

---

## 開発・構成技術

- Kotlin / Jetpack Compose
- Material Design 3
- Android 標準 LocationManager（Google Play Services 非依存）

---

## クレジット

- Special Thanks: Photo by Sagara

---

## ライセンス

[MIT License](LICENSE)

ご質問・不具合報告は [GitHub Issues](https://github.com/koba9813/yamaokaya/issues) までお願いします。
