package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 言語対応（英語・日本語）の翻訳テーブル。
 * ゲーム内のすべての表示文字列はこのクラスにまとめ、コード中に直書きしない。
 * - 対応言語は英語と日本語の2つ。
 * - 起動時は端末の言語設定（Locale）で自動判定する（日本語なら日本語、それ以外は英語）。
 *   ただし保存された言語設定がある場合はそちらを優先する。
 * - 言語はタイトル画面で L キーで切り替えられ、Preferences に保存される。
 * - フォントが読めない環境では英語表示にフォールバックする（Fonts 側で切り替える）。
 */
public final class Localization {
    /** 対応言語。 */
    public enum Language {
        ENGLISH, JAPANESE
    }

    private static final String PREFS_NAME = "dopamine-mall";
    private static final String PREFS_KEY_LANGUAGE = "language";

    private static Language current = Language.ENGLISH;
    private static boolean initialized = false;

    /** 翻訳テーブル：キー → {英語, 日本語}。 */
    private static final Map<String, String[]> STRINGS = new HashMap<String, String[]>();

    static {
        put("title.subtitle",
                "An endless shopping mall dungeon roguelike",
                "永遠に登れるショッピングモールがダンジョンのローグライクRPG");
        put("title.press_start",
                "PRESS ENTER TO START",
                "エンターキーではじめる");
        put("title.keys1",
                "Move: Arrow keys / WASD   [M] MAP   [I] ITEMS   [N] MUSIC",
                "移動：矢印キー／WASD  [M]マップ  [I]持ち物  [N]音楽");
        put("title.keys2",
                "Gamepad / Fire TV remote: D-pad to move, START items, A start",
                "ゲームパッド／Fire TVリモコン：十字キーで移動、STARTで持ち物、Aではじめる");
        put("title.language",
                "Language: English  (L: switch to Japanese)",
                "言語：日本語  （Lキー：英語に切り替え）");

        put("hud.game_name", "DOPAMINE MALL", "ドパミン・モール");
        put("hud.floor", "%dF", "%d階");
        put("hud.atk", "ATK", "攻撃");
        put("hud.def", "DEF", "防御");
        put("hud.wpn", "WPN", "武器");
        put("hud.arm", "ARM", "防具");
        put("hud.turn", "TURN %d", "ターン %d");
        put("hud.keys",
                "[M] MAP   [I]/[START] ITEMS   [N] MUSIC   [SPACE] WAIT",
                "[M]マップ   [I]/[START]持ち物   [N]音楽   [SPACE]待機");
        put("hud.boss_floor", "!! BOSS FLOOR !!", "！！ボスフロア！！");
        put("hud.pa", "PA: ", "放送：");
        put("hud.music_on", "Music: ON", "音楽：オン");
        put("hud.music_off", "Music: OFF", "音楽：オフ");

        put("msg.attacked", "Attacked %s for %d dmg!", "%sに %d ダメージ！");
        put("msg.defeated", "Defeated %s!", "%sを倒した！");
        put("msg.defeated_boss", "Defeated BOSS %s! It dropped %s!",
                "ボス %sを倒した！%sを落とした！");
        put("msg.monster_house", "Monster house! Enemies everywhere!",
                "モンスターハウスだ！敵だらけだ！");
        put("msg.inv_full_pickup", "Inventory is full! Cannot pick up %s.",
                "持ち物がいっぱい！%sは拾えない。");
        put("msg.picked_up", "Picked up %s!", "%sを拾った！");
        put("msg.used", "Used %s! Recovered %d HP!", "%sを使った！HPが %d 回復した！");
        put("msg.equipped", "Equipped %s!", "%sを装備した！");
        put("msg.inv_full_swap", "Inventory is full! Cannot swap equipment.",
                "持ち物がいっぱい！装備を交換できない。");
        put("msg.hit_by", "%s hit you for %d dmg!", "%sの攻撃！%d ダメージ！");
        put("msg.game_over", "GAME OVER...", "ゲームオーバー…");

        put("inv.title", "ITEMS  (%d/%d)", "持ち物  （%d/%d）");
        put("inv.hint1", "D-pad: select  [A] use  [B] close  /  1-0: use  [I] close",
                "十字キー：選択  [A]使う  [B]閉じる");
        put("inv.hint2", "", "1-0：使う  [I]閉じる");
        put("inv.empty", "(empty)", "（からっぽ）");
        put("inv.heal", "Heal +%d", "回復 +%d");
        put("inv.atk", "ATK +%d", "攻撃 +%d");
        put("inv.def", "DEF +%d", "防御 +%d");

        put("over.title", "GAME OVER", "ゲームオーバー");
        put("over.reached", "You reached %dF  /  TURN %d", "%d階まで到達  /  ターン %d");
        put("over.retry", "PRESS ENTER TO RETRY", "エンターキーでもう一度");

        put("announce.welcome", "Welcome to %dF!", "%d階へようこそ！");
    }

    private Localization() {
    }

    private static void put(String key, String english, String japanese) {
        STRINGS.put(key, new String[]{english, japanese});
    }

    /**
     * 言語設定を読み込む。保存された設定があればそれを使い、なければ端末の
     * 言語設定（Locale）で自動判定する。MallScreen.show() の最初に呼ぶこと。
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            String saved = prefs.getString(PREFS_KEY_LANGUAGE, null);
            if ("ja".equals(saved)) {
                current = Language.JAPANESE;
                return;
            }
            if ("en".equals(saved)) {
                current = Language.ENGLISH;
                return;
            }
        } catch (Exception e) {
            Gdx.app.log("Localization", "Could not load language preference: " + e.getMessage());
        }
        // 保存された設定がない場合は端末の言語で自動判定する。
        try {
            String lang = Locale.getDefault().getLanguage();
            current = "ja".equals(lang) ? Language.JAPANESE : Language.ENGLISH;
        } catch (Exception e) {
            current = Language.ENGLISH;
        }
    }

    public static Language getLanguage() {
        return current;
    }

    public static boolean isJapanese() {
        return current == Language.JAPANESE;
    }

    /** 言語を設定し、Preferences に保存する。 */
    public static void setLanguage(Language language) {
        current = language;
        try {
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            prefs.putString(PREFS_KEY_LANGUAGE,
                    language == Language.JAPANESE ? "ja" : "en");
            prefs.flush();
        } catch (Exception e) {
            Gdx.app.log("Localization", "Could not save language preference: " + e.getMessage());
        }
    }

    /** 言語を英語と日本語で切り替える（タイトル画面の L キー用）。 */
    public static void toggleLanguage() {
        setLanguage(current == Language.JAPANESE ? Language.ENGLISH : Language.JAPANESE);
    }

    /**
     * フォントが読めない場合のフォールバック用：保存された設定を変えずに
     * メモリ上の言語だけを英語にする。
     */
    public static void forceEnglishInMemory() {
        current = Language.ENGLISH;
    }

    /** 現在の言語の文字列を返す（%s・%d などの書式指定子付き）。 */
    public static String t(String key, Object... args) {
        String[] pair = STRINGS.get(key);
        String pattern;
        if (pair == null) {
            pattern = key;
        } else {
            pattern = isJapanese() ? pair[1] : pair[0];
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(Locale.ROOT, pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    /** 翻訳テーブル内のすべての日本語文字列を返す（フォントの文字収集用）。 */
    public static List<String> allJapaneseStrings() {
        List<String> result = new ArrayList<String>();
        for (String[] pair : STRINGS.values()) {
            result.add(pair[1]);
        }
        return result;
    }
}
