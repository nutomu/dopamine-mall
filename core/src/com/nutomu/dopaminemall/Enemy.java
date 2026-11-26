package com.nutomu.dopaminemall;

import java.util.Random;

/**
 * M3：敵（下層の3種類）。
 * - 客：弱い・数が多い・うろつく
 * - 店員：普通・プレイヤーを追う
 * - 警備員：強い・プレイヤーを追う・たまに出現
 * M5：階層テーマで敵の顔ぶれを変える。
 * - 1〜4階：下層（客・店員・警備員）
 * - 5〜9階：着ぐるみ（マスコット）と展示から逃げ出した動物
 * - 10〜14階：UMA
 * - 15階以上：天使や神話の存在
 * 各階層に2種ずつ以上の新しい敵を追加。階層が上がるほど基本ステータスも上がる。
 * ボス：5階ごとにその階層を代表する強力な敵1体（店長／巨大着ぐるみ／大天使）。
 * ステータスは階が上がるほど少し補正される。
 */
public class Enemy {
    /** 敵の種類。 */
    public enum Kind {
        /** 客：HP8／攻撃3、ピンク色、記号 'c'。 */
        CUSTOMER("客", "Customer", 'c', 8, 3, 1.0f, 0.55f, 0.75f),
        /** 店員：HP14／攻撃5、オレンジ色、記号 's'。 */
        CLERK("店員", "Clerk", 's', 14, 5, 1.0f, 0.55f, 0.2f),
        /** 警備員：HP24／攻撃8、赤色、記号 'G'。 */
        GUARD("警備員", "Guard", 'G', 24, 8, 0.9f, 0.15f, 0.15f),
        /** クマの着ぐるみ：HP22／攻撃7、茶色、記号 'b'（5〜9階）。 */
        BEAR_MASCOT("クマの着ぐるみ", "Bear Mascot", 'b', 22, 7, 0.6f, 0.4f, 0.2f),
        /** ウサギの着ぐるみ：HP16／攻撃6、白っぽいピンク、記号 'r'（5〜9階）。 */
        RABBIT_MASCOT("ウサギの着ぐるみ", "Rabbit Mascot", 'r', 16, 6, 0.95f, 0.85f, 0.9f),
        /** 逃げ出した犬：HP18／攻撃8、黄土色、記号 'd'（5〜9階）。 */
        ESCAPED_DOG("逃げ出した犬", "Escaped Dog", 'd', 18, 8, 0.8f, 0.65f, 0.35f),
        /** 逃げ出したサル：HP14／攻撃7、赤茶色、記号 'm'（5〜9階）。 */
        ESCAPED_MONKEY("逃げ出したサル", "Escaped Monkey", 'm', 14, 7, 0.75f, 0.55f, 0.3f),
        /** ツチノコ：HP30／攻撃11、緑色、記号 't'（10〜14階）。 */
        TSUCHINOKO("ツチノコ", "Tsuchinoko", 't', 30, 11, 0.3f, 0.8f, 0.3f),
        /** 雪男：HP38／攻撃13、白っぽい水色、記号 'Y'（10〜14階）。 */
        YETI("雪男", "Yeti", 'Y', 38, 13, 0.85f, 0.9f, 0.95f),
        /** 天使：HP48／攻撃16、金色、記号 'A'（15階以上）。 */
        ANGEL("天使", "Angel", 'A', 48, 16, 1.0f, 0.9f, 0.5f),
        /** 獅子の神：HP60／攻撃20、濃い金色、記号 'L'（15階以上）。 */
        LION_GOD("獅子の神", "Lion God", 'L', 60, 20, 0.9f, 0.7f, 0.2f),
        /** 店長：5Fのボス。基準は店員相当、記号 'B'、紫色。 */
        MANAGER("店長", "Manager", 'B', 14, 5, 0.7f, 0.3f, 0.8f),
        /** 巨大着ぐるみ：10Fのボス。基準はクマの着ぐるみ相当、記号 'B'、橙色。 */
        GIANT_MASCOT("巨大着ぐるみ", "Giant Mascot", 'B', 22, 7, 0.9f, 0.5f, 0.1f),
        /** 大天使：15F以上のボス。基準は天使相当、記号 'B'、明るい金色。 */
        ARCHANGEL("大天使", "Archangel", 'B', 48, 16, 1.0f, 0.95f, 0.3f);

        /** 日本語名（仕様上の名称）。 */
        public final String japaneseName;
        /** 英語名。 */
        public final String englishName;
        /** 表示記号（デフォルトフォントは ASCII のみのため英字）。 */
        public final char symbol;
        /** 基準 HP。 */
        public final int baseHp;
        /** 基準攻撃力。 */
        public final int baseAttack;
        public final float r;
        public final float g;
        public final float b;

        Kind(String japaneseName, String englishName, char symbol, int baseHp, int baseAttack,
             float r, float g, float b) {
            this.japaneseName = japaneseName;
            this.englishName = englishName;
            this.symbol = symbol;
            this.baseHp = baseHp;
            this.baseAttack = baseAttack;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        /** 現在の言語での表示名。 */
        public String label() {
            return Localization.isJapanese() ? japaneseName : englishName;
        }
    }

    public final Kind kind;
    public int x;
    public int y;
    public int hp;
    public final int maxHp;
    public final int attack;
    /** 防御力（今は全種類 0）。 */
    public final int defense = 0;
    /** ボスかどうか。ボスは HP が大幅に高く、攻撃も強い。 */
    public final boolean isBoss;

    public Enemy(Kind kind, int x, int y, int floorNumber) {
        this(kind, x, y, floorNumber, false);
    }

    public Enemy(Kind kind, int x, int y, int floorNumber, boolean isBoss) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.isBoss = isBoss;
        // 階が上がるほど少し強くなる：HP +2／階、攻撃 +1／2階。
        // ボスは通常敵の約5倍のHP、攻撃は約1.5倍。
        int scaledHp = kind.baseHp + (floorNumber - 1) * 2;
        int scaledAttack = kind.baseAttack + (floorNumber - 1) / 2;
        if (isBoss) {
            this.maxHp = scaledHp * 5;
            this.attack = Math.round(scaledAttack * 1.5f);
        } else {
            this.maxHp = scaledHp;
            this.attack = scaledAttack;
        }
        this.hp = maxHp;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    /** ダメージを受ける。ダメージは攻撃力−防御力、最低1。 */
    public int takeDamage(int rawAttack) {
        int damage = Math.max(1, rawAttack - defense);
        hp -= damage;
        return damage;
    }

    /** 階数に応じた階層テーマ名（HUD 表示用）。現在の言語で返す。 */
    public static String themeName(int floorNumber) {
        if (Localization.isJapanese()) {
            if (floorNumber <= 4) {
                return "下層モール";
            }
            if (floorNumber <= 9) {
                return "着ぐるみ・動物";
            }
            if (floorNumber <= 14) {
                return "UMA";
            }
            return "天界";
        }
        if (floorNumber <= 4) {
            return "LOWER MALL";
        }
        if (floorNumber <= 9) {
            return "MASCOT & ANIMALS";
        }
        if (floorNumber <= 14) {
            return "UMA";
        }
        return "CELESTIAL";
    }

    /** 階数に応じた出現種別を決める。 */
    public static Kind pickKind(int floorNumber, Random random) {
        double r = random.nextDouble();
        if (floorNumber <= 4) {
            // 下層：客・店員・警備員。警備員は階が上がるほど出やすくなる。
            double guardChance = 0.05 + (floorNumber - 1) * 0.02;
            if (r < guardChance) {
                return Kind.GUARD;
            }
            if (floorNumber <= 2) {
                return r < 0.70 ? Kind.CUSTOMER : Kind.CLERK;
            }
            return r < 0.55 ? Kind.CUSTOMER : Kind.CLERK;
        }
        if (floorNumber <= 9) {
            // 着ぐるみ・動物フロア：クマ／ウサギの着ぐるみ、逃げ出した犬／サル。
            if (r < 0.30) return Kind.BEAR_MASCOT;
            if (r < 0.55) return Kind.RABBIT_MASCOT;
            if (r < 0.80) return Kind.ESCAPED_DOG;
            return Kind.ESCAPED_MONKEY;
        }
        if (floorNumber <= 14) {
            // UMAフロア：ツチノコ／雪男。
            return r < 0.55 ? Kind.TSUCHINOKO : Kind.YETI;
        }
        // 天界フロア：天使／獅子の神。
        return r < 0.55 ? Kind.ANGEL : Kind.LION_GOD;
    }

    /** ボスフロア（5階ごと）のボスの種類を決める。 */
    public static Kind bossKind(int floorNumber) {
        if (floorNumber == 5) {
            return Kind.MANAGER;
        }
        if (floorNumber == 10) {
            return Kind.GIANT_MASCOT;
        }
        return Kind.ARCHANGEL;
    }
}
