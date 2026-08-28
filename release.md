# Yamaokaya is Doko? V.beta1.2 Release Notes

## Overview

「Yamaokaya is Doko?」の V.beta1.2 リリースです。
このバージョンでは、F-Droid への公開に向けてプロプライエタリな依存関係を削除し、Android 標準 API のみで動作するように整理しました。

## Main Features

- 現在地から最寄りの山岡家店舗までの距離・方角をリアルタイム表示
- 画面中央のラーメン画像が店舗方向を指し示す
- 最寄り店舗までの距離を常時通知（バックグラウンド対応）
- 店舗 50m 圏内に入ると特別な演出と通知
- 訪問履歴を残せる「スタンプラリー（チェックイン）」機能
- 店舗までの距離を共有できるシェア機能

## Installation Instructions

1. F-Droid からインストール（掲載後）
2. または [GitHub Releases](https://github.com/koba9813/yamaokaya/releases) から最新 APK をダウンロード
3. Android 端末に転送し、ファイルを開く
4. 「提供元不明のアプリのインストール」を許可
5. インストール後、アプリを起動
6. 位置情報の利用を許可

## Notes

- 検索対象はアプリ内に登録された店舗座標のみです
- 検索半径は 100km です
- バックグラウンド通知には「常に許可」の位置情報権限が必要です
- Foreground Service 利用のため、距離トラッカー有効時は通知が常時表示されます
- Google Play Services / Firebase などのプロプライエタリなライブラリは使用していません
- 本アプリは山岡家の公式アプリではありません

## Changes in V.beta1.2

- Google Play Services Location の依存を削除し、Android 標準 LocationManager に置き換え
- Firebase Analytics と Google Services プラグインを削除
- 組み込みの更新確認機能を削除
- 設定画面から最新リリースノートの自動取得を削除

## Future Plans

- F-Droid への掲載
- 店舗データの更新
- UI の改善
- バグ修正

For questions or bug reports, please visit [GitHub Issues](https://github.com/koba9813/yamaokaya/issues).
