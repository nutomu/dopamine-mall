package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * 言語対応のフォント管理。
 * - 英語表示のときは従来のデフォルトフォント（ASCII）を使う。
 * - 日本語表示のときは、あらかじめ生成したビットマップフォント
 *   （android/assets/fonts/font32.fnt / font24.fnt）を読み込む。
 *   gdx-freetype は HTML5（GWT）で動かないため、実行時のフォント生成は
 *   やめて、同じ FreeType ラスタライザで同じサイズ（32px / 24px）を
 *   事前に生成したものを使う。描画結果は従来と変わらない。
 *   フォントが読めない場合は英語表示にフォールバックする。
 * - フォントの大きさは従来の表示と同じ：大きいフォント 32px、
 *   HUD 用フォント 24px（デフォルトフォントの 2倍／1.5倍に相当）。
 * - ビットマップフォントの再生成方法：tools/GenBitmapFont.java を参照。
 *   新しい日本語文字列を追加した場合は、まず tools/subset_font.py で
 *   TTF と tools/chars.txt を更新し、GenBitmapFont を再実行すること。
 */
public final class Fonts {
    /** 大きいフォント（タイトル・ゲームオーバー・タイル記号用、32px）。 */
    private static final String FONT32_PATH = "fonts/font32.fnt";
    /** HUD 用フォント（24px）。 */
    private static final String FONT24_PATH = "fonts/font24.fnt";
    /** 大きいフォントのサイズ（タイトル・ゲームオーバー・タイル記号用）。 */
    private static final int FONT_SIZE = 32;
    /** HUD 用フォントのサイズ。 */
    private static final int HUD_FONT_SIZE = 24;

    /** 大きいフォント（MallScreen の font）。 */
    public static BitmapFont font;
    /** HUD 用フォント（MallScreen の hudFont）。 */
    public static BitmapFont hudFont;

    private static boolean japaneseFont = false;

    private Fonts() {
    }

    /** 現在の言語に合わせてフォントを読み込む。MallScreen.show() で呼ぶこと。 */
    public static void load() {
        japaneseFont = false;
        if (Localization.isJapanese()) {
            try {
                font = new BitmapFont(Gdx.files.internal(FONT32_PATH));
                hudFont = new BitmapFont(Gdx.files.internal(FONT24_PATH));
                japaneseFont = true;
                return;
            } catch (Exception e) {
                Gdx.app.log("Fonts", "Japanese font could not be loaded, "
                        + "falling back to English: " + e.getMessage());
                disposeFonts();
                Localization.forceEnglishInMemory();
            }
        }
        font = new BitmapFont();
        font.getData().setScale(2f);
        hudFont = new BitmapFont();
        hudFont.getData().setScale(1.5f);
    }

    /** 言語切替時にフォントを作り直す（タイトル画面の L キー用）。 */
    public static void reload() {
        dispose();
        load();
    }

    public static boolean isJapaneseFont() {
        return japaneseFont;
    }

    private static void disposeFonts() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (hudFont != null) {
            hudFont.dispose();
            hudFont = null;
        }
    }

    public static void dispose() {
        disposeFonts();
    }
}
