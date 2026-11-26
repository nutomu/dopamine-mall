package com.nutomu.dopaminemall;

import java.util.Random;

/**
 * M3：敵（下層の3種類）。
 * - 客：弱い・数が多い・うろつく
 * - 店員：普通・プレイヤーを追う
 * - 警備員：強い・プレイヤーを追う・たまに出現
 * ステータスは階が上がるほど少し補正される。
 */
public class Enemy {
    /** 敵の種類。 */
    public enum Kind {
        /** 客：HP8／攻撃3、ピンク色、記号 'c'。 */
        CUSTOMER("客", 'c', 8, 3, 1.0f, 0.55f, 0.75f),
        /** 店員：HP14／攻撃5、オレンジ色、記号 's'。 */
        CLERK("店員", 's', 14, 5, 1.0f, 0.55f, 0.2f),
        /** 警備員：HP24／攻撃8、赤色、記号 'G'。 */
        GUARD("警備員", 'G', 24, 8, 0.9f, 0.15f, 0.15f);

        /** 日本語名（仕様上の名称）。 */
        public final String japaneseName;
        /** 表示記号（デフォルトフォントは ASCII のみのため英字）。 */
        public final char symbol;
        /** 基準 HP。 */
        public final int baseHp;
        /** 基準攻撃力。 */
        public final int baseAttack;
        public final float r;
        public final float g;
        public final float b;

        Kind(String japaneseName, char symbol, int baseHp, int baseAttack,
             float r, float g, float b) {
            this.japaneseName = japaneseName;
            this.symbol = symbol;
            this.baseHp = baseHp;
            this.baseAttack = baseAttack;
            this.r = r;
            this.g = g;
            this.b = b;
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

    public Enemy(Kind kind, int x, int y, int floorNumber) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        // 階が上がるほど少し強くなる：HP +2／階、攻撃 +1／2階。
        this.maxHp = kind.baseHp + (floorNumber - 1) * 2;
        this.hp = maxHp;
        this.attack = kind.baseAttack + (floorNumber - 1) / 2;
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

    /** 階数に応じた出現種別を決める。警備員は階が上がるほど出やすくなる。 */
    public static Kind pickKind(int floorNumber, Random random) {
        double guardChance = 0.05 + (floorNumber - 1) * 0.02;
        double r = random.nextDouble();
        if (r < guardChance) {
            return Kind.GUARD;
        }
        if (floorNumber <= 2) {
            return r < 0.70 ? Kind.CUSTOMER : Kind.CLERK;
        }
        return r < 0.55 ? Kind.CUSTOMER : Kind.CLERK;
    }
}
