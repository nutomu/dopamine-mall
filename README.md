# ドパミン・モール

永遠に登れるショッピングモールがダンジョンのローグライクRPG（開発中）。

## 世界観

世界は、ほぼ今と一緒の近未来の日本。ダンジョンは、永遠と登れるショッピングモールみたいな施設。
モールは高い階に行くほど、刺激的なアトラクションやアイテムやイベントが発生する。
ドパガキな主人公は、より刺激を求めて短絡的にモールの上の階を目指す。

敵は、モールの下の階層は店員や客。少し上っていくと、着ぐるみや動物展示から逃げ出した動物。
さらに上に行くと、ファンタジーな天界に住まうUMA。上りきると天界になっていて、天使や神話に出てくるような存在が現れる。
天界より上の階はないが、平面方向に無限に広がっている。

## 操作方法

- ゲームパッド（十字キー + A/B/X/Y + L/R + Start/Select）
- Fire TV リモコン（十字キー）

## APK の入手方法（Fire TV Stick で遊ぶ）

1. GitHub の [Releases](../../releases) ページを開く。
2. 「ドパミン・モール 日次ビルド」（タグ `nightly`）の `dopamine-mall-debug.apk` をダウンロードする。
3. Fire TV Stick の「設定」→「マイ Fire TV」→「開発者オプション」で「不明なアプリのインストール」を許可する。
4. Downloader アプリなどで APK をダウンロードしてインストールする。
5. ゲームパッドまたは Fire TV リモコンで遊ぶ。

## 開発

- エンジン：libGDX 1.12.1（Java）
- 対象：Android（minSdk 21 / targetSdk 34）。Fire TV Stick 4K Max（第2世代、32bit・Fire OS 8・RAM 2GB）対応のため armeabi-v7a を含む。
- 日次ビルド：GitHub Actions が毎日 00:00 UTC（日本時間 9:00）に APK をビルドし、`nightly` リリースを更新する。`main` への push 時と手動実行（workflow_dispatch）でもビルドされる。

### ローカルビルド

```sh
# JDK 17 と Android SDK（platform 34 / build-tools 34.0.0）が必要
./gradlew :android:assembleDebug
# APK: android/build/outputs/apk/debug/android-debug.apk
```

`local.properties` に `sdk.dir=/path/to/Android/Sdk` を書くこと。
