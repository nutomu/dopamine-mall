package com.nutomu.dopaminemall;

import java.util.Random;

/**
 * M4：アイテム（3系統）。
 * - 回復：ドリンク（HP+10）、おにぎり（HP+15）、スイーツ（HP+25）。使うと即座に回復する。
 * - 武器：傘（攻撃+2）、フライパン（攻撃+4）。装備すると攻撃力に加算される。
 * - 防具：エプロン（防御+1）、ジャケット（防御+2）。装備すると防御力に加算される。
 * 武器・防具は1つずつ装備できる。
 */
public class Item {
    /** アイテムの系統。 */
    public enum Type {
        CONSUMABLE, WEAPON, ARMOR
    }

    /** アイテムの種類。 */
    public enum Kind {
        /** ドリンク：HP+10、緑色、記号 '!'。 */
        DRINK("ドリンク", "DRINK", '!', Type.CONSUMABLE, 10, 0, 0, 0.3f, 0.9f, 0.4f),
        /** おにぎり：HP+15、白っぽい色、記号 '!'。 */
        ONIGIRI("おにぎり", "ONIGIRI", '!', Type.CONSUMABLE, 15, 0, 0, 0.95f, 0.9f, 0.7f),
        /** スイーツ：HP+25、ピンク色、記号 '!'。 */
        SWEETS("スイーツ", "SWEETS", '!', Type.CONSUMABLE, 25, 0, 0, 1.0f, 0.6f, 0.8f),
        /** 傘：攻撃+2、茶色、記号 '/'。 */
        UMBRELLA("傘", "UMBRELLA", '/', Type.WEAPON, 0, 2, 0, 0.7f, 0.6f, 0.5f),
        /** フライパン：攻撃+4、灰色、記号 '/'。 */
        FRYING_PAN("フライパン", "FRYING PAN", '/', Type.WEAPON, 0, 4, 0, 0.5f, 0.5f, 0.55f),
        /** エプロン：防御+1、白、記号 ']'。 */
        APRON("エプロン", "APRON", ']', Type.ARMOR, 0, 0, 1, 0.95f, 0.95f, 0.9f),
        /** ジャケット：防御+2、青色、記号 ']'。 */
        JACKET("ジャケット", "JACKET", ']', Type.ARMOR, 0, 0, 2, 0.3f, 0.5f, 0.9f);

        /** 日本語名（仕様上の名称）。 */
        public final String japaneseName;
        /** 表示名（デフォルトフォントは ASCII のみのため英字で表示）。 */
        public final String displayName;
        /** 表示記号（回復 '!'、武器 '/'、防具 ']'）。 */
        public final char symbol;
        public final Type type;
        /** 回復量（回復アイテムのみ）。 */
        public final int heal;
        /** 攻撃力ボーナス（武器のみ）。 */
        public final int attackBonus;
        /** 防御力ボーナス（防具のみ）。 */
        public final int defenseBonus;
        public final float r;
        public final float g;
        public final float b;

        Kind(String japaneseName, String displayName, char symbol, Type type,
             int heal, int attackBonus, int defenseBonus,
             float r, float g, float b) {
            this.japaneseName = japaneseName;
            this.displayName = displayName;
            this.symbol = symbol;
            this.type = type;
            this.heal = heal;
            this.attackBonus = attackBonus;
            this.defenseBonus = defenseBonus;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        /** 現在の言語での表示名。 */
        public String label() {
            return Localization.isJapanese() ? japaneseName : displayName;
        }
    }

    public final Kind kind;
    public int x;
    public int y;

    public Item(Kind kind, int x, int y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
    }

    /** ランダムな種類を1つ選ぶ。回復が出やすく、強い武器・防具は出にくい。 */
    public static Kind pickKind(Random random) {
        double r = random.nextDouble();
        if (r < 0.25) return Kind.DRINK;
        if (r < 0.45) return Kind.ONIGIRI;
        if (r < 0.55) return Kind.SWEETS;
        if (r < 0.70) return Kind.UMBRELLA;
        if (r < 0.78) return Kind.FRYING_PAN;
        if (r < 0.90) return Kind.APRON;
        return Kind.JACKET;
    }
}
