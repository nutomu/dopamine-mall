package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * シレン風見下ろし用のスプライト集。画像生成モデルで作ったシートを
 * tools/slice_sheets.py で切り出した PNG を android/assets/sprites2d/ から読む。
 * 壁は1マス＝1チップだが、タイルより背の高い壁チップ（幅64×高さ128相当）
 * を使い、下端をタイルの下端に合わせて上に伸ばして描く。
 * Linear フィルタでなめらかに表示する（描画時に縮小するため）。
 * 敵・アイテム・小物は種類ごとのキャッシュで読み込む。
 * キャラクターの表示サイズ正規化：tools/make_norm.py で測った
 * android/assets/sprites2d/norm.json（内容の bounding box・不透明
 * ピクセル数・方向ごとの倍率）を読み、向きによる見た目の大きさの
 * ばらつきを抑えて描く。足元には柔らかい影を描くためのテクスチャも
 * ここで作る（Pixmap で楕円のグラデーションを生成）。
 */
public class Sprites {
    private final List<Texture> all = new ArrayList<Texture>();
    /** 敵・アイテム・小物用テクスチャのキャッシュ（毎フレーム読み込まないため）。 */
    private final Map<String, Texture> cache = new HashMap<String, Texture>();

    /** norm.json の1スプライト分の実測値。 */
    public static class Norm {
        /** 内容の bounding box（x, y は左上起点、ピクセル単位）。 */
        public int x, y, w, h;
        /** 不透明ピクセルの数。 */
        public int area;
        /** 方向ごとの表示サイズ正規化の倍率（1.0 が等倍）。 */
        public float scale = 1f;
    }
    /** スプライト名 → 実測値（norm.json が読めない場合は空）。 */
    private final Map<String, Norm> norm = new HashMap<String, Norm>();

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
    public final Texture fog;
    /** 斬撃エフェクト（白い弧）。 */
    public final Texture slash;
    /** ヒット時の星エフェクト。 */
    public final Texture hitstar;
    /** キャラクターの足元の柔らかい影（Pixmap で生成した楕円グラデーション）。 */
    public final Texture shadow;

    /** 装飾用の小物の種類。 */
    public enum Prop {
        LANTERN, PLANT, VENDING, TRASH, CART, SIGN, BENCH, LAMP, COINS
    }

    public Sprites() {
        loadNorm();
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
        fog = load("fog");
        slash = load("slash");
        hitstar = load("hitstar");
        shadow = makeShadowTexture();
    }

    /** norm.json（キャラクタースプライトの実測値）を読み込む。
     * 読めない場合は倍率 1.0（正規化なし）で描く。 */
    private void loadNorm() {
        try {
            JsonValue root = new JsonReader().parse(
                    Gdx.files.internal("sprites2d/norm.json"));
            for (JsonValue child = root.child; child != null; child = child.next) {
                Norm n = new Norm();
                n.x = child.getInt("x", 0);
                n.y = child.getInt("y", 0);
                n.w = child.getInt("w", 0);
                n.h = child.getInt("h", 0);
                n.area = child.getInt("area", 0);
                n.scale = child.getFloat("scale", 1f);
                norm.put(child.name, n);
            }
        } catch (Exception e) {
            Gdx.app.log("Sprites", "norm.json could not be loaded: " + e.getMessage());
        }
    }

    /** スプライト名に対応する実測値を返す。なければ null（等倍で描く）。 */
    public Norm normOf(String name) {
        return norm.get(name);
    }

    /** 足元の影テクスチャを作る（黒い楕円が中心から外へ薄くなる）。 */
    private Texture makeShadowTexture() {
        int w = 64;
        int h = 32;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float nx = (px - w / 2f) / (w / 2f);
                float ny = (py - h / 2f) / (h / 2f);
                float d = nx * nx + ny * ny;
                float a = 0f;
                if (d < 1f) {
                    float fall = 1f - d;
                    a = fall * fall * 0.4f;
                }
                pm.drawPixel(px, py, com.badlogic.gdx.graphics.Color.rgba8888(0f, 0f, 0f, a));
            }
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        all.add(t);
        return t;
    }

    /** 足元の柔らかい影を描く（batch.begin() の中で呼ぶこと）。 */
    public void drawShadow(SpriteBatch batch, float cx, float baseY, float width) {
        float h = width * 0.32f;
        batch.draw(shadow, cx - width / 2f, baseY - h / 2f + 2, width, h);
    }

    /** キャラクタースプライトを正規化して描く（batch.begin() の中で呼ぶこと）。
     * norm.json の方向ごとの倍率を掛け、内容の下端（足元）が baseY に、
     * 内容の横方向の中心が cx に来るように描く。 */
    public void drawCharacter(SpriteBatch batch, String name, float cx, float baseY,
                              float baseHeight) {
        Texture tex = cached(name);
        Norm n = norm.get(name);
        float scale = n != null ? n.scale : 1f;
        float drawH = baseHeight * scale;
        float drawW = drawH * tex.getWidth() / (float) tex.getHeight();
        // 内容の下端・横中心のずれをテクスチャ座標から求めて補正する。
        float bottomPad = 0f;
        float centerOff = 0f;
        if (n != null && n.h > 0) {
            bottomPad = tex.getHeight() - (n.y + n.h);
            centerOff = n.x + n.w / 2f - tex.getWidth() / 2f;
        }
        float sx = drawW / tex.getWidth();
        float sy = drawH / tex.getHeight();
        batch.draw(tex, cx - drawW / 2f - centerOff * sx,
                baseY - bottomPad * sy, drawW, drawH);
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
     * dir は 0=南〜7=南東、frame は 0〜3（1歩で2フレーム進む）。 */
    public Texture heroTexture(int dir, int frame) {
        return cached("hero_d" + dir + "_f" + frame);
    }

    /** 主人公の向きと歩行フレームに対応するスプライト名（デバッグ表示用）。 */
    public String heroSpriteName(int dir, int frame) {
        return "hero_d" + dir + "_f" + frame;
    }

    /** 主人公の歩行フレーム数（デバッグ表示用）。 */
    public int heroFrameCount() {
        return 4;
    }

    /** 敵の種類と向きに対応するスプライト名（デバッグ表示用）。 */
    public String enemySpriteName(Enemy.Kind kind, int dir) {
        switch (kind) {
            case CUSTOMER:
                return "customer_" + dir;
            case CLERK:
                return "clerk_" + dir;
            case GUARD:
                return "guard_" + dir;
            case BEAR_MASCOT:
                return "bear_" + dir;
            case ESCAPED_DOG:
                return "dog_" + dir;
            case ANGEL:
                return "angel_" + dir;
            default:
                return enemySpriteName(kind);
        }
    }

    /** 敵の種類に対応するスプライト名（デバッグ表示用）。 */
    public String enemySpriteName(Enemy.Kind kind) {
        switch (kind) {
            case CUSTOMER:
                return "customer";
            case CLERK:
                return "clerk";
            case GUARD:
                return "guard";
            case BEAR_MASCOT:
                return "bear";
            case RABBIT_MASCOT:
                return "rabbit";
            case ESCAPED_DOG:
                return "dog";
            case ESCAPED_MONKEY:
                return "monkey";
            case TSUCHINOKO:
                return "tsuchinoko";
            case YETI:
                return "yeti";
            case ANGEL:
                return "angel";
            case LION_GOD:
                return "lion";
            case MANAGER:
                return "manager";
            case GIANT_MASCOT:
                return "giant_bear";
            case ARCHANGEL:
                return "archangel";
            default:
                return "customer";
        }
    }

    /** 敵の種類に8方向スプライトがあるかどうか。 */
    public boolean hasDirectionSprites(Enemy.Kind kind) {
        switch (kind) {
            case CUSTOMER:
            case CLERK:
            case GUARD:
            case BEAR_MASCOT:
            case ESCAPED_DOG:
            case ANGEL:
                return true;
            default:
                return false;
        }
    }

    /** 敵の種類の歩行フレーム数（デバッグ表示用。8方向の敵も1フレーム）。 */
    public int enemyFrameCount(Enemy.Kind kind) {
        return 1;
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
