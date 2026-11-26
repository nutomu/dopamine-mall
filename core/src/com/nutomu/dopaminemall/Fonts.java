package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import java.util.HashSet;
import java.util.Set;

/**
 * 言語対応のフォント管理。
 * - 英語表示のときは従来のデフォルトフォント（ASCII）を使う。
 * - 日本語表示のときは gdx-freetype で Noto Sans JP のサブセットフォント
 *   （android/assets/fonts/NotoSansJP-subset.ttf）を読み込み、同じ大きさの
 *   フォントを生成する。フォントが読めない場合は英語表示にフォールバックする。
 * - フォントの大きさは従来の表示と同じ：大きいフォント 32px、
 *   HUD 用フォント 24px（デフォルトフォントの 2倍／1.5倍に相当）。
 */
public final class Fonts {
    /** 日本語フォント（Noto Sans JP のサブセット、SIL Open Font License 1.1）。 */
    private static final String FONT_PATH = "fonts/NotoSansJP-subset.ttf";
    /** 大きいフォントのサイズ（タイトル・ゲームオーバー・タイル記号用）。 */
    private static final int FONT_SIZE = 32;
    /** HUD 用フォントのサイズ。 */
    private static final int HUD_FONT_SIZE = 24;

    /** 大きいフォント（MallScreen の font）。 */
    public static BitmapFont font;
    /** HUD 用フォント（MallScreen の hudFont）。 */
    public static BitmapFont hudFont;

    private static FreeTypeFontGenerator generator;
    private static boolean japaneseFont = false;

    private Fonts() {
    }

    /** 現在の言語に合わせてフォントを読み込む。MallScreen.show() で呼ぶこと。 */
    public static void load() {
        japaneseFont = false;
        if (Localization.isJapanese()) {
            try {
                generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
                String characters = allCharacters();
                FreeTypeFontParameter params = new FreeTypeFontParameter();
                params.characters = characters;
                params.size = FONT_SIZE;
                font = generator.generateFont(params);
                params.size = HUD_FONT_SIZE;
                hudFont = generator.generateFont(params);
                japaneseFont = true;
                return;
            } catch (Exception e) {
                Gdx.app.log("Fonts", "Japanese font could not be loaded, "
                        + "falling back to English: " + e.getMessage());
                disposeGenerator();
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

    /** ゲームで使うすべての文字を集める（日本語フォント生成用）。 */
    private static String allCharacters() {
        Set<Character> chars = new HashSet<Character>();
        // ASCII 印字可能文字（数字・英字・記号）はすべて含める。
        for (char c = 32; c <= 126; c++) {
            chars.add(c);
        }
        // 翻訳テーブル内の日本語文字列。
        for (String s : Localization.allJapaneseStrings()) {
            addAll(chars, s);
        }
        // 敵・アイテム・フロアタイプの日本語名。
        for (Enemy.Kind kind : Enemy.Kind.values()) {
            addAll(chars, kind.japaneseName);
        }
        for (Item.Kind kind : Item.Kind.values()) {
            addAll(chars, kind.japaneseName);
        }
        for (FloorType type : FloorType.values()) {
            addAll(chars, type.japaneseName);
        }
        // 階層テーマ名（日本語）。
        addAll(chars, "下層モール着ぐるみ動物天界");
        // 店内放送の日本語文言。
        for (String s : Announcements.allJapaneseLines()) {
            addAll(chars, s);
        }
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c.charValue());
        }
        return sb.toString();
    }

    private static void addAll(Set<Character> chars, String s) {
        if (s == null) {
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            chars.add(s.charAt(i));
        }
    }

    private static void disposeGenerator() {
        if (generator != null) {
            generator.dispose();
            generator = null;
        }
    }

    public static void dispose() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (hudFont != null) {
            hudFont.dispose();
            hudFont = null;
        }
        disposeGenerator();
    }
}
