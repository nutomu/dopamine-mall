package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2.5D アイソメトリック用のスプライト集。画像生成モデルで作ったシートを
 * tools/slice_sheets.py で切り出した PNG を android/assets/sprites2d/ から読む。
 * Linear フィルタでなめらかに表示する（描画時に縮小するため）。
 * 敵・アイテム・小物は種類ごとのキャッシュで読み込む。
 */
public class Sprites {
    private final List<Texture> all = new ArrayList<Texture>();
    /** 敵・アイテム・小物用テクスチャのキャッシュ（毎フレーム読み込まないため）。 */
    private final Map<String, Texture> cache = new HashMap<String, Texture>();

    public final Texture floorMall;
    public final Texture floorWood;
    public final Texture floorCarpet;
    public final Texture wall;
    public final Texture bookshelf;
    public final Texture shelf;
    public final Texture counter;
    public final Texture table;
    public final Texture arcade;
    public final Texture escalator;
    public final Texture stairs;
    public final Texture stage;
    public final Texture bench;
    public final Texture hero;
    public final Texture fog;
    /** 壁の上面ダイヤ（継ぎ目なく並べる）。 */
    public final Texture wallTop;
    /** 壁の前面バンド（壁の並びの南側の縁に帯状に描く）。 */
    public final Texture wallBand;
    /** 壁の角（南側の角タイル用）。 */
    public final Texture wallCorner;
    /** 斬撃エフェクト（白い弧）。 */
    public final Texture slash;
    /** ヒット時の星エフェクト。 */
    public final Texture hitstar;

    /** 装飾用の小物の種類。 */
    public enum Prop {
        LANTERN, PLANT, VENDING, TRASH, CART, SIGN, BENCH, LAMP, COINS
    }

    public Sprites() {
        floorMall = load("floor_mall");
        floorWood = load("floor_wood");
        floorCarpet = load("floor_carpet");
        wall = load("wall");
        bookshelf = load("bookshelf");
        shelf = load("shelf");
        counter = load("counter");
        table = load("table");
        arcade = load("arcade");
        escalator = load("escalator");
        stairs = load("stairs");
        stage = load("stage");
        bench = load("bench");
        hero = load("hero");
        fog = load("fog");
        wallTop = load("wall_top");
        wallBand = load("wall_band");
        wallCorner = load("wall_corner");
        slash = load("slash");
        hitstar = load("hitstar");
    }

    private Texture load(String name) {
        Texture t = new Texture(Gdx.files.internal("sprites2d/" + name + ".png"));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        all.add(t);
        return t;
    }

    /** フロアタイプごとの床タイル。 */
    public Texture floorTexture(FloorType type) {
        switch (type) {
            case SUPERMARKET:
            case FOOD_COURT:
                return floorWood;
            case GAME_CENTER:
            case EVENT:
                return floorCarpet;
            case TENANT:
            default:
                return floorMall;
        }
    }

    /** 地形タイルに対応するスプライト（床は floorTexture を使うこと）。 */
    public Texture terrainTexture(Floor.Tile t) {
        switch (t) {
            case WALL:
                return wall;
            case SHELF:
                return shelf;
            case MACHINE:
                return arcade;
            case TABLE:
                return table;
            case COUNTER:
                return counter;
            case STAGE:
                return stage;
            case SEAT:
                return bench;
            case ESCALATOR:
                return escalator;
            case STAIRS:
                return stairs;
            case FLOOR:
            default:
                return floorMall;
        }
    }

    private Texture cached(String name) {
        Texture t = cache.get(name);
        if (t == null) {
            t = load(name);
            cache.put(name, t);
        }
        return t;
    }

    /** 主人公の向きと歩行フレームに対応するスプライト。
     * dir は 0=南〜7=南東、frame は 0/1（歩行フレームA/B）。 */
    public Texture heroTexture(int dir, int frame) {
        return cached("hero_d" + dir + "_f" + frame);
    }

    /** 敵の種類と向きに対応するスプライト。8方向シートがない種類は
     * 従来の1枚絵をそのまま使う（向きは変わらない）。 */
    public Texture enemyTexture(Enemy.Kind kind, int dir) {
        switch (kind) {
            case CUSTOMER:
                return cached("customer_" + dir);
            case CLERK:
                return cached("clerk_" + dir);
            case GUARD:
                return cached("guard_" + dir);
            case BEAR_MASCOT:
                return cached("bear_" + dir);
            case ESCAPED_DOG:
                return cached("dog_" + dir);
            case ANGEL:
                return cached("angel_" + dir);
            default:
                return enemyTexture(kind);
        }
    }

    /** 敵の種類に対応するスプライト。 */
    public Texture enemyTexture(Enemy.Kind kind) {
        switch (kind) {
            case CUSTOMER:
                return cached("customer");
            case CLERK:
                return cached("clerk");
            case GUARD:
                return cached("guard");
            case BEAR_MASCOT:
                return cached("bear");
            case RABBIT_MASCOT:
                return cached("rabbit");
            case ESCAPED_DOG:
                return cached("dog");
            case ESCAPED_MONKEY:
                return cached("monkey");
            case TSUCHINOKO:
                return cached("tsuchinoko");
            case YETI:
                return cached("yeti");
            case ANGEL:
                return cached("angel");
            case LION_GOD:
                return cached("lion");
            case MANAGER:
                return cached("manager");
            case GIANT_MASCOT:
                return cached("giant_bear");
            case ARCHANGEL:
                return cached("archangel");
            default:
                return cached("customer");
        }
    }

    /** アイテムの種類に対応するスプライト。 */
    public Texture itemTexture(Item.Kind kind) {
        switch (kind) {
            case DRINK:
                return cached("drink");
            case ONIGIRI:
                return cached("onigiri");
            case SWEETS:
                return cached("cake");
            case UMBRELLA:
                return cached("umbrella");
            case FRYING_PAN:
                return cached("pan");
            case APRON:
                return cached("apron");
            case JACKET:
                return cached("jacket");
            default:
                return cached("drink");
        }
    }

    /** 装飾用の小物のスプライト。 */
    public Texture propTexture(Prop kind) {
        switch (kind) {
            case LANTERN:
                return cached("lantern");
            case PLANT:
                return cached("plant");
            case VENDING:
                return cached("vending");
            case TRASH:
                return cached("trash");
            case CART:
                return cached("cart");
            case SIGN:
                return cached("sign");
            case BENCH:
                return cached("bench");
            case LAMP:
                return cached("lamp");
            case COINS:
                return cached("coins");
            default:
                return cached("plant");
        }
    }

    public void dispose() {
        for (Texture t : all) {
            t.dispose();
        }
        all.clear();
        cache.clear();
    }
}
