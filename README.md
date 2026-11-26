# ドパミン・モール

永遠に登れるショッピングモールがダンジョンのローグライクRPG（開発中）。

## 世界観

世界は、ほぼ今と一緒の近未来の日本。ダンジョンは、永遠と登れるショッピングモールみたいな施設。
モールは高い階に行くほど、刺激的なアトラクションやアイテムやイベントが発生する。
ドパガキな主人公は、より刺激を求めて短絡的にモールの上の階を目指す。

敵は、モールの下の階層は店員や客。少し上っていくと、着ぐるみや動物展示から逃げ出した動物が現れる。
さらに上の階には何がいるのか、誰も知らない。上の階には何があるのか、誰も知らない。

## 操作方法

- ゲームパッド（十字キー + A/B/X/Y + L/R + Start/Select）
- Fire TV リモコン（十字キー）

## 遊び方（M2：ダンジョン生成とターン制移動）

- フロアは5種類のタイプ（テナントフロア／スーパーフロア／ゲームセンター／フードコート／イベントフロア）から、階数で候補を絞ってランダムに選ばれる。
- 1フロアは画面より広く、カメラがプレイヤーを追ってスクロールする。
- 緑のエスカレーターと青の非常階段に乗ると、次の階へ進む（基本は一方通行の上り）。
- 行ったことのある部屋と通路だけ明るく見える。未踏の場所は暗いまま。
- Mキーでミニマップ（オーバーレイ表示）の表示／非表示を切り替える。通った道だけ表示される。
- 移動は1マスで1ターン。スペースキー／ピリオドキーで待機（1ターン過ごす）。
- HUD には現在の階数とフロアタイプ名、ターン数が表示される。

## APK の入手方法（Fire TV Stick で遊ぶ）

1. GitHub の [Releases](../../releases) ページを開く。
2. 「ドパミン・モール 日次ビルド」（タグ `nightly`）の `dopamine-mall-debug.apk` をダウンロードする。
3. Fire TV Stick の「設定」→「マイ Fire TV」→「開発者オプション」で「不明なアプリのインストール」を許可する。
4. Downloader アプリなどで APK をダウンロードしてインストールする。
5. ゲームパッドまたは Fire TV リモコンで遊ぶ。

## デスクトップ版の入手方法（PC で遊ぶ）

テストプレイ用の PC 版です。Android 版とゲーム内容は同じソースコード（core モジュール）で、起動部分だけが違います。

1. GitHub の [Releases](../../releases) ページを開く。
2. 「ドパミン・モール 日次ビルド」（タグ `nightly`）の `dopamine-mall-desktop.jar` をダウンロードする。
3. Java 17 が入った PC で、次のコマンドで起動する。

```sh
java -jar dopamine-mall-desktop.jar
```

4. キーボードの矢印キー / WASD で遊ぶ。

## 開発

- エンジン：libGDX 1.12.1（Java）
- 対象：Android（minSdk 21 / targetSdk 34）。Fire TV Stick 4K Max（第2世代、32bit・Fire OS 8・RAM 2GB）対応のため armeabi-v7a を含む。テストプレイ用のデスクトップ版（LWJGL3、ウィンドウ 1280x720）もあり、ゲーム内容は core モジュールを共有している。
- 日次ビルド：GitHub Actions が毎日 00:00 UTC（日本時間 9:00）に APK とデスクトップ版 jar をビルドし、`nightly` リリースを更新する。`main` への push 時と手動実行（workflow_dispatch）でもビルドされる。

### ローカルビルド

```sh
# JDK 17 と Android SDK（platform 34 / build-tools 34.0.0）が必要
./gradlew :android:assembleDebug
# APK: android/build/outputs/apk/debug/android-debug.apk

# デスクトップ版（Android SDK は不要、JDK 17 のみ）
./gradlew :desktop:dist
# jar: desktop/build/libs/dopamine-mall-desktop.jar
# 起動: java -jar desktop/build/libs/dopamine-mall-desktop.jar
```

`local.properties` に `sdk.dir=/path/to/Android/Sdk` を書くこと。
