package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ドット絵スプライト集。tools/generate_tiles.py で生成した 32x32 PNG を
 * android/assets/sprites/ から読み込む。Nearest フィルタでくっきり表示する。
 * 敵・アイテムの色分けは Enemy.Kind / Item.Kind の既存の色と対応している。
 */
public class Sprites {
    private final List<Texture> all = new ArrayList<Texture>();
    /** 敵・アイテム用テクスチャのキャッシュ（毎フレーム読み込まないため）。 */
    private final Map<String, Texture> cache = new HashMap<String, Texture>();

    public final Texture floorA;
    public final Texture floorB;
    public final Texture floorC;
    public final Texture wall;
    public final Texture shelf;
    public final Texture machine;
    public final Texture table;
    public final Texture counter;
    public final Texture stage;
    public final Texture seat;
    public final Texture escalator;
    public final Texture stairs;
    public final Texture player;

    public Sprites() {
        floorA = load("floor_a");
        floorB = load("floor_b");
        floorC = load("floor_c");
        wall = load("wall");
        shelf = load("shelf");
        machine = load("machine");
        table = load("table");
        counter = load("counter");
        stage = load("stage");
        seat = load("seat");
        escalator = load("escalator");
        stairs = load("stairs");
        player = load("player");
    }

    private Texture load(String name) {
        Texture t = new Texture(Gdx.files.internal("sprites/" + name + ".png"));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        all.add(t);
        return t;
    }

    /** フロアの床タイル（位置でバリエーションを変える）。 */
    public Texture floorTile(int x, int y) {
        int v = (x * 7 + y * 13) % 3;
        if (v < 0) {
            v += 3;
        }
        if (v == 0) {
            return floorA;
        }
        if (v == 1) {
            return floorB;
        }
        return floorC;
    }

    /** 地形タイルに対応するスプライト（床は floorTile を使うこと）。 */
    public Texture terrainTexture(Floor.Tile t) {
        switch (t) {
            case WALL:
                return wall;
            case SHELF:
                return shelf;
            case MACHINE:
                return machine;
            case TABLE:
                return table;
            case COUNTER:
                return counter;
            case STAGE:
                return stage;
            case SEAT:
                return seat;
            case ESCALATOR:
                return escalator;
            case STAIRS:
                return stairs;
            case FLOOR:
            default:
                return floorA;
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

    /** 敵の種類に対応するスプライト。 */
    public Texture enemyTexture(Enemy.Kind kind) {
        switch (kind) {
            case CUSTOMER:
                return cached("enemy_customer");
            case CLERK:
                return cached("enemy_clerk");
            case GUARD:
                return cached("enemy_guard");
            case BEAR_MASCOT:
                return cached("enemy_bear_mascot");
            case RABBIT_MASCOT:
                return cached("enemy_rabbit_mascot");
            case ESCAPED_DOG:
                return cached("enemy_dog");
            case ESCAPED_MONKEY:
                return cached("enemy_monkey");
            case TSUCHINOKO:
                return cached("enemy_tsuchinoko");
            case YETI:
                return cached("enemy_yeti");
            case ANGEL:
                return cached("enemy_angel");
            case LION_GOD:
                return cached("enemy_lion_god");
            case MANAGER:
                return cached("boss_manager");
            case GIANT_MASCOT:
                return cached("boss_giant_mascot");
            case ARCHANGEL:
                return cached("boss_archangel");
            default:
                return cached("enemy_customer");
        }
    }

    /** アイテムの種類に対応するスプライト。 */
    public Texture itemTexture(Item.Kind kind) {
        switch (kind) {
            case DRINK:
                return cached("item_drink");
            case ONIGIRI:
                return cached("item_onigiri");
            case SWEETS:
                return cached("item_sweets");
            case UMBRELLA:
                return cached("item_umbrella");
            case FRYING_PAN:
                return cached("item_frying_pan");
            case APRON:
                return cached("item_apron");
            case JACKET:
                return cached("item_jacket");
            default:
                return cached("item_drink");
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
