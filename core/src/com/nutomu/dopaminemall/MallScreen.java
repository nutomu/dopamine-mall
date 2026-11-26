package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * M2：ダンジョン生成とターン制移動のテスト実装。
 * - 5種類のフロアタイプに応じたランダム生成（40x30タイル程度）。
 * - フロアは画面より広く、カメラがプレイヤーを追ってスクロールする。
 * - 視界：一度行った部屋と通路だけ明るく見える（主人公の空間把握）。
 * - ミニマップ：Mキーで切り替えるオーバーレイ表示。通った道だけ表示。
 * - ターン制：1マス移動＝1ターン。スペースキー／ピリオドキーで待機（1ターン）。
 * - 階段：エスカレーター（緑）と非常階段（青）の2か所。乗ると次の階へ。
 * - 操作：矢印キー／WASD、タップ移動。ゲームパッド・Fire TVリモコンの十字キーは
 *   Android側で矢印キーとして届くため特別な対応は不要。
 * M3：敵と戦闘のテスト実装。
 * - 敵は下層の3種類：客（弱い・数が多い）、店員（普通・追う）、警備員（強い・たまに出現）。
 * - 戦闘はぶつかり攻撃。ダメージ＝攻撃力−防御力、最低1。
 * - 敵AI：プレイヤーが5マス以内にいると近づく。それ以外はランダムにうろつく。
 * - プレイヤーのHPが0になるとゲームオーバー。Enterキーで最初からリスタート。
 * M4：アイテムと装備のテスト実装。
 * - 3系統：回復（ドリンク／おにぎり／スイーツ）、武器（傘／フライパン）、
 *   防具（エプロン／ジャケット）。フロアに数個ランダム配置。
 * - アイテムのあるマスに移動すると自動で拾う。持ち物は最大10個。
 * - Iキーで持ち物一覧（オーバーレイ表示）。数字キー（1〜0）で選んで使う／装備する。
 *   武器・防具は1つずつ装備でき、交換時は古い装備が持ち物に戻る。
 * - 使う・装備する・拾う操作は1ターン消費する。一覧の開閉だけでは消費しない。
 * M5：階層テーマとボスのテスト実装。
 * - 階数で敵の顔ぶれが変わる。1〜4階は下層（客・店員・警備員）、5〜9階は着ぐるみと
 *   動物、10〜14階はUMA、15階以上は天使や神話の存在。HUD に階層テーマ名を表示する。
 * - ボスフロア（5階ごと）：その階層を代表するボス1体（5F 店長／10F 巨大着ぐるみ／
 *   15F〜 大天使）。HPは通常敵の約5倍、攻撃は約1.5倍。倒すと良いアイテムを落とす。
 *   倒さなくても階段で次の階へ進める。ボスは金色で大きく表示し、記号 'B'。
 * - モンスターハウス：フロアごとに10%の確率で、ランダムな場所に敵6〜10体を詰め込む。
 *   入るとメッセージで知らせる。中の敵は視界ルール通り、入るまで見えない。
 * ※ グラフィックスは風来のシレン風の見下ろし描画（縦方向が真っすぐ）。
 * - 画像生成モデルで作ったスプライトシートを tools/slice_sheets.py で切り出した
 *   PNG（android/assets/sprites2d/）を描画する。1マス＝1つのマップチップ
 *   （壁チップは上面と前面を1枚に含む）。床・壁は四角いチップをグリッド通りに
 *   並べ、家具・キャラクターは立った姿で描く。
 * - 描画順はペインター法（行ごとに上から下へ）。カメラはプレイヤーを追従する。
 * - 階層別の雰囲気：下層は明るい現代のモール、上層ほど暗く神秘的に（霧・ランタンの光）。
 * - 移動はターン制のまま、マスからマスへ滑らかに補間するアニメーションを入れる。
 * ※ HUD の文字はデフォルトフォントが ASCII のみのため英字表記。
 * 言語対応（英語・日本語）。
 * - 表示文字列はすべて Localization クラス（翻訳テーブル）で管理する。
 * - 起動時は端末の言語設定で自動判定し（日本語なら日本語、それ以外は英語）、
 *   タイトル画面で L キーを押すと切り替えられる。切り替えた言語は
 *   Preferences に保存され、次回起動時も維持される。
 * - 日本語表示のときは Noto Sans JP のサブセットフォントを gdx-freetype で
 *   読み込む。英語表示のときは従来のデフォルトフォントのまま。
 * - タイトル画面とゲームオーバー画面の文字は中央揃えで描く（日本語でも崩れない）。
 * M6：音楽・演出・グラフィックスのテスト実装。
 * - BGM：生成チップチューンの WAV ループを3曲（タイトル／ダンジョン／ボス）。
 *   タイトル画面ではタイトル曲、ダンジョンでは環境音楽、ボスフロアと
 *   モンスターハウスでは緊迫感のある曲に切り替わる。Nキーでミュート切替。
 * - タイトル画面：ゲーム開始前に「DOPAMINE MALL」を表示し、Enterキーで開始。
 * - 店内放送：HUD にフロアタイプごとの放送文言が20ターンごとに流れる。
 *   階を上がったときは「Welcome to ○F!」の特別な放送。
 * - 演出：フロアアップ時のきらめき、攻撃・被弾時のフラッシュ、敵撃破時の
 *   パーティクル、プレイヤーを中心とした柔らかい光。視界ルールは変えない。
 * ゲームパッド・Fire TV リモコンでの持ち物一覧操作。
 * - コントローラー拡張は使わず、ゲームパッド・リモコンのボタンは Android の
 *   キーコード（BUTTON_A／BUTTON_B／BUTTON_Y／BUTTON_START／DPAD_CENTER／
 *   BACK など）として keyDown に届く方式。タイトル画面の BUTTON_A 開始と
 *   同じ仕組みで、キー操作（Iキー・数字キー）と並行して使える。
 * - Start／Y ボタンで一覧を開く。開いているときは十字キー上下で項目を移動し、
 *   A／決定ボタンで使う・装備する、B／Back ボタンで閉じる。選んだ項目は
 *   ハイライト表示される。開閉・項目移動だけではターンを消費しない。
 * - Fire TV リモコンの Back ボタンがアプリに届くよう setCatchKey(BACK) する。
 * 8方向移動と向き・アニメーション、シレン風見下ろし。
 * - 移動は8方向。矢印キー／WASD は画面の方向（上＝北(0,-1)、下＝南(0,1)、
 *   左＝西(-1,0)、右＝東(1,0)）に進み、残りの4方向は Q／E／Z／C キーで
 *   移動する（Q＝北西(-1,-1)、E＝北東(1,-1)、Z＝南西(-1,1)、C＝南東(1,1)）。
 *   ゲームパッド・Fire TV リモコンの十字キーは画面の4方向に対応する。
 *   斜め移動も1ターンで、移動先に壁や敵があれば移動できない。
 * - プレイヤーと敵に向き（0=南〜7=南東、時計回り）を持たせ、移動・攻撃した
 *   方向を向く。向きに合った8方向スプライトを描く（ない種類は1枚絵のまま）。
 *   歩行時は移動の補間（約0.12秒）に合わせて歩行フレームを切り替える
 *   （主人公は4フレームを1歩で2フレームずつ、敵は上下の跳ね＋向きの切り替え）。
 *   攻撃時は攻撃方向へ素早く突進して戻る＋斬撃エフェクト＋対象のフラッシュ。
 * - 壁は1マス＝1チップで描く（上面と前面を1枚に含む壁チップをタイルごとに
 *   そのまま1枚描くだけ）。面分割（上面・前面バンド・角）はやめた。
 * 壁の高さ・重なりとキャラクターのサイズ正規化、スプライト確認デバッグモード。
 * - 壁チップはタイルより背が高い（幅64×高さ128、タイル2枚分）。下端を
 *   タイルの下端に合わせ、上に1タイル分はみ出して描く。描画順は行ごとの
 *   ペインター法のままで、壁のはみ出しが上の行の床の上に、壁より手前
 *   （南）の行のキャラクターが壁の前面の上に描かれる。
 * - キャラクターの表示サイズ正規化：tools/make_norm.py で測った実寸
 *   （norm.json）を使い、方向ごとの見た目の大きさのばらつきを抑える。
 *   同じ方向の全フレームに同じ倍率を使うため歩行中に脈打たない。
 *   足元には柔らかい影（半透明の楕円）を描く。
 * - スプライト確認デバッグモード：タイトル画面のメニューまたは F1 キーで
 *   開く。キャラクター種別ごとに8方向×停止／歩行／武器使用の3段を表示し、
 *   各セルの下に実寸（幅×高さ）とフレーム数を出す。左右キーで種別を切り替える。
 */
public class MallScreen implements Screen, InputProcessor {
    /** 8方向のグリッド移動量。向き 0=南、1=南西、2=西、3=北西、4=北、5=北東、6=東、7=南東。
     * 画面の上＝北（y が減る方向）、下＝南（y が増える方向）。 */
    static final int[][] DIR_DXY = {{0, 1}, {-1, 1}, {-1, 0}, {-1, -1},
            {0, -1}, {1, -1}, {1, 0}, {1, 1}};

    /** グリッドの移動量 (dx, dy) に対応する向き（0〜7）を返す。 */
    static int directionOf(int dx, int dy) {
        for (int d = 0; d < 8; d++) {
            if (DIR_DXY[d][0] == dx && DIR_DXY[d][1] == dy) {
                return d;
            }
        }
        return 0;
    }

    /** チェビシェフ距離（8方向移動での距離）。隣接は1。 */
    static int chebyshev(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    /** 1マスの画面サイズ（四角いチップの幅・高さ。シレン風見下ろし）。 */
    static final int TILE = 64;
    /** 壁チップの高さ（タイル2枚分。下端をタイルの下端に合わせ、上に1タイル分はみ出す）。 */
    static final float WALL_H = TILE * 2f;
    static final int VIEW_W = 1280;
    static final int VIEW_H = 800;
    /** 視界の広さ（プレイヤーから歩いて何マス先まで「行った場所」にするか）。 */
    static final int SIGHT = 8;

    /** タイル座標 tx を画面 X 座標（タイル中心）に変換する。 */
    static float tileX(float tx) {
        return tx * TILE;
    }

    /** タイル座標 ty を画面 Y 座標（タイル中心）に変換する。
     * libGDX のワールド座標は Y が上向きなので、グリッドの y が増える
     * （画面の下）ほど Y は小さくなる。 */
    static float tileY(float ty) {
        return -ty * TILE;
    }

    /** 画面座標をタイル座標に逆変換する（タップ移動用）。 */
    static float[] screenToTile(float wx, float wy) {
        return new float[]{wx / TILE, -wy / TILE};
    }

    private final DopamineMallGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont hudFont;
    /** ドット絵スプライト集。 */
    private Sprites sprites;
    /** 中央揃えで文字を描くためのレイアウト（タイトル画面・ゲームオーバー用）。 */
    private final GlyphLayout layout = new GlyphLayout();

    private Floor floor;
    private int playerX;
    private int playerY;
    /** プレイヤーの描画用補間位置（タイル単位）。移動時に目標タイルへ滑らかに寄る。 */
    private float playerRx;
    private float playerRy;
    /** プレイヤーの向き（0=南〜7=南東）。 */
    private int playerFacing = 0;
    /** 歩数のパリティ（0=1歩目、1=2歩目）。歩行フレームの切り替えに使う。 */
    private int walkStep = 0;
    /** 今の移動の距離（タイル単位、直進1・斜め√2）。歩行フレームと跳ねの進行度用。 */
    private float moveDist = 0f;
    /** 攻撃時の突進アニメーション（0 なら攻撃中でない）。 */
    private float attackLungeTimer = 0f;
    /** 突進する方向（向き 0〜7）。 */
    private int attackDir = 0;
    /** 攻撃アニメーションの長さ（秒）。 */
    private static final float ATTACK_LUNGE_TIME = 0.18f;
    /** 斬撃エフェクトの表示時間。 */
    private float slashTimer = 0f;
    private int slashX, slashY;
    private static final float SLASH_TIME = 0.22f;
    private int floorNumber = 1;
    private int turn = 0;
    private boolean showMinimap = false;

    /** プレイヤーのステータス（M3）。 */
    private static final int PLAYER_MAX_HP = 50;
    private static final int PLAYER_ATTACK = 6;
    private static final int PLAYER_DEFENSE = 0;
    private int playerHp = PLAYER_MAX_HP;
    /** 敵がプレイヤーを追い始める距離（マンハッタン距離）。 */
    private static final int CHASE_RANGE = 5;

    private List<Enemy> enemies = new ArrayList<Enemy>();
    private boolean gameOver = false;
    /** HUD 付近に表示する1行メッセージ（攻撃した／されたときなど）。 */
    private String message = "";

    /** アイテム（M4）。 */
    private static final int MAX_INVENTORY = 10;
    private List<Item> items = new ArrayList<Item>();
    private List<Item> inventory = new ArrayList<Item>();
    private Item equippedWeapon = null;
    private Item equippedArmor = null;
    private boolean showInventory = false;
    /** 持ち物一覧で現在選んでいる項目（ハイライト表示用）。 */
    private int selectedItem = 0;

    /** ボスとモンスターハウス（M5）。 */
    private boolean isBossFloor = false;
    private boolean monsterHouseAnnounced = false;

    /** 音楽・タイトル画面・店内放送・演出（M6）。 */
    private MusicManager music = new MusicManager();
    private boolean titleScreen = true;
    /** タイトル画面のメニュー選択（0=ゲームスタート、1=スプライト確認）。 */
    private int titleMenuIndex = 0;
    /** スプライト確認デバッグモード（タイトル画面から開く）。 */
    private boolean spriteDebug = false;
    /** デバッグモードで表示中のキャラクター種別（0=主人公、1〜=敵の種類順）。 */
    private int debugKindIndex = 0;
    /** デバッグモードのアニメーション用タイマー（歩行・武器使用の再生に使う）。 */
    private float debugTime = 0f;
    /** 店内放送：20ターンごとにフロアタイプの文言を切り替える。 */
    private static final int ANNOUNCE_EVERY_TURNS = 20;
    private String announcement = "";
    private int announcementIndex = 0;
    private int announcementTurn = 0;
    /** モンスターハウスに入ったあとボス曲を流しているかどうか。 */
    private boolean monsterHouseMusic = false;
    /** 演出用パーティクル（きらめき・撃破時の破片・霧）。 */
    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        float r, g, b;
        /** 描画時の不透明度の上限（霧は薄くする）。 */
        float alpha = 1f;
        /** テクスチャ付きパーティクル（霧）。null なら円で描く。 */
        com.badlogic.gdx.graphics.Texture tex = null;
        /** テクスチャの描画幅（ピクセル）。 */
        float texW = 0f;
    }

    /** 装飾用の小物（2.5D）。床タイルに置くだけで、移動を妨げない。 */
    private static class Prop {
        int x, y;
        Sprites.Prop kind;

        Prop(int x, int y, Sprites.Prop kind) {
            this.x = x;
            this.y = y;
            this.kind = kind;
        }
    }
    private List<Prop> props = new ArrayList<Prop>();
    private List<Particle> particles = new ArrayList<Particle>();
    /** 攻撃・被弾時の画面フラッシュ（0 になると消える）。 */
    private float flashTimer = 0f;
    private float flashR, flashG, flashB;
    /** 攻撃したタイルのフラッシュ。 */
    private float attackFlashTimer = 0f;
    private int attackFlashX, attackFlashY;
    /** 霧の発生間隔を測るタイマー（上層の雰囲気用）。 */
    private float fogTimer = 0f;

    public MallScreen(DopamineMallGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        // 言語設定を読み込み、現在の言語に合わせたフォントを用意する。
        Localization.init();
        Fonts.load();
        font = Fonts.font;
        hudFont = Fonts.hudFont;
        sprites = new Sprites();
        loadFloor();
        music.load();
        music.playTitle();
        Gdx.input.setInputProcessor(this);
        // Fire TV リモコンの Back ボタンを OS に取られずアプリで受け取る。
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
    }

    /** 新しい階のフロアを生成してプレイヤーと敵・アイテムを配置する。 */
    private void loadFloor() {
        floor = FloorGenerator.generate(floorNumber);
        playerX = floor.startX;
        playerY = floor.startY;
        playerRx = playerX;
        playerRy = playerY;
        playerFacing = 0;
        walkStep = 0;
        moveDist = 0f;
        attackLungeTimer = 0f;
        slashTimer = 0f;
        enemies = FloorGenerator.spawnEnemies(floor, floorNumber);
        items = FloorGenerator.spawnItems(floor, floorNumber, enemies);
        spawnProps();
        isBossFloor = floorNumber % 5 == 0;
        monsterHouseAnnounced = false;
        monsterHouseMusic = false;
        updateVisibility();
        // 店内放送：階を上がったときは特別な放送を流す。
        announcement = Announcements.welcome(floorNumber);
        announcementTurn = turn;
        announcementIndex = 0;
        // 2階以降はフロアアップのきらめきを散らす。
        if (floorNumber > 1) {
            spawnSparkles();
        }
        if (!titleScreen) {
            updateMusic();
        }
    }

    /** 装飾用の小物をフロアの空いている床タイルに数個配置する（移動を妨げない）。 */
    private void spawnProps() {
        props = new ArrayList<Prop>();
        Sprites.Prop[] lowerKinds = {Sprites.Prop.VENDING, Sprites.Prop.CART,
                Sprites.Prop.BENCH, Sprites.Prop.PLANT, Sprites.Prop.SIGN,
                Sprites.Prop.TRASH};
        Sprites.Prop[] upperKinds = {Sprites.Prop.LANTERN, Sprites.Prop.LAMP,
                Sprites.Prop.PLANT, Sprites.Prop.COINS, Sprites.Prop.SIGN};
        Sprites.Prop[] kinds = floorNumber <= 4 ? lowerKinds
                : (floorNumber <= 9 ? lowerKinds : upperKinds);
        int count = 6 + (int) (Math.random() * 5);
        int tries = 0;
        while (props.size() < count && tries < 200) {
            tries++;
            int x = 1 + (int) (Math.random() * (floor.w - 2));
            int y = 1 + (int) (Math.random() * (floor.h - 2));
            if (floor.tiles[x][y] != Floor.Tile.FLOOR) {
                continue;
            }
            if (Math.abs(x - floor.startX) + Math.abs(y - floor.startY) <= 2) {
                continue;
            }
            if (x == floor.escX && y == floor.escY) {
                continue;
            }
            if (x == floor.stairX && y == floor.stairY) {
                continue;
            }
            if (enemyAt(x, y) != null || itemAt(x, y) != null || propAt(x, y) != null) {
                continue;
            }
            props.add(new Prop(x, y, kinds[(int) (Math.random() * kinds.length)]));
        }
    }

    /** 指定したタイルにある装飾小物を返す。なければ null。 */
    private Prop propAt(int x, int y) {
        for (Prop p : props) {
            if (p.x == x && p.y == y) {
                return p;
            }
        }
        return null;
    }

    /** 状況に応じた BGM を選ぶ（ボスフロア／モンスターハウスはボス曲）。 */
    private void updateMusic() {
        if (titleScreen) {
            music.playTitle();
            return;
        }
        if (isBossFloor && bossAlive()) {
            music.playBoss();
            return;
        }
        if (monsterHouseMusic) {
            music.playBoss();
            return;
        }
        music.playDungeon();
    }

    /** ボスがまだ生きているかどうか。 */
    private boolean bossAlive() {
        for (Enemy e : enemies) {
            if (e.isBoss) {
                return true;
            }
        }
        return false;
    }

    /** モンスターハウスの中にまだ敵がいるかどうか。 */
    private boolean monsterHouseHasEnemies() {
        if (!floor.hasMonsterHouse) {
            return false;
        }
        for (Enemy e : enemies) {
            if (e.x >= floor.mhX0 && e.x <= floor.mhX1
                    && e.y >= floor.mhY0 && e.y <= floor.mhY1) {
                return true;
            }
        }
        return false;
    }

    /** プレイヤーの攻撃力（素手の攻撃力＋装備中の武器のボーナス）。 */
    private int playerAttack() {
        return PLAYER_ATTACK + (equippedWeapon != null ? equippedWeapon.kind.attackBonus : 0);
    }

    /** プレイヤーの防御力（素の防御力＋装備中の防具のボーナス）。 */
    private int playerDefense() {
        return PLAYER_DEFENSE + (equippedArmor != null ? equippedArmor.kind.defenseBonus : 0);
    }

    /** 指定したタイルにあるアイテムを返す。なければ null。 */
    private Item itemAt(int x, int y) {
        for (Item it : items) {
            if (it.x == x && it.y == y) {
                return it;
            }
        }
        return null;
    }

    /** 指定したタイルにいる敵を返す。いなければ null。 */
    private Enemy enemyAt(int x, int y) {
        for (Enemy e : enemies) {
            if (e.x == x && e.y == y) {
                return e;
            }
        }
        return null;
    }

    /**
     * プレイヤーから歩いて SIGHT マス以内の場所を「行った場所」として記録する。
     * 隣接する壁や障害物も見えるようにする。
     */
    private void updateVisibility() {
        int[][] dist = floor.distancesFrom(playerX, playerY);
        for (int x = 0; x < floor.w; x++) {
            for (int y = 0; y < floor.h; y++) {
                if (dist[x][y] >= 0 && dist[x][y] <= SIGHT) {
                    floor.visited[x][y] = true;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (floor.inBounds(nx, ny)) {
                                floor.visited[nx][ny] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    /** カメラをプレイヤーに追従させる（フロアの端でははみ出さない）。描画用の補間位置を使う。 */
    private void followCamera() {
        float px = tileX(playerRx);
        float py = tileY(playerRy);
        // 見下ろし世界の範囲（タイル中心 ± 半タイル）
        float minX = -TILE / 2f;
        float maxX = (floor.w - 1) * TILE + TILE / 2f;
        float minY = -(floor.h - 1) * TILE - TILE / 2f;
        float maxY = TILE / 2f + 96f; // 上の行の家具・キャラクターが画面上にはみ出す分
        float worldW = maxX - minX;
        float worldH = maxY - minY;
        float cx = px;
        float cy = py;
        if (worldW <= VIEW_W) {
            cx = (minX + maxX) / 2f;
        } else {
            cx = Math.max(minX + VIEW_W / 2f, Math.min(maxX - VIEW_W / 2f, cx));
        }
        if (worldH <= VIEW_H) {
            cy = (minY + maxY) / 2f;
        } else {
            cy = Math.max(minY + VIEW_H / 2f, Math.min(maxY - VIEW_H / 2f, cy));
        }
        camera.position.set(cx, cy, 0);
        camera.update();
    }

    /**
     * 移動を試みる。移動先に敵がいたらぶつかり攻撃をする。
     * プレイヤーの行動のあと、敵が1回ずつ行動する（ターン制）。
     */
    private void tryMove(int dx, int dy) {
        if (gameOver) {
            return;
        }
        int nx = playerX + dx;
        int ny = playerY + dy;
        playerFacing = directionOf(dx, dy);
        Enemy target = enemyAt(nx, ny);
        if (target != null) {
            int damage = target.takeDamage(playerAttack());
            flash(1f, 1f, 0.6f);
            attackFlashTimer = 0.2f;
            attackFlashX = nx;
            attackFlashY = ny;
            // 攻撃アニメーション：攻撃方向へ突進＋斬撃エフェクト＋対象のフラッシュ
            attackLungeTimer = ATTACK_LUNGE_TIME;
            attackDir = playerFacing;
            slashTimer = SLASH_TIME;
            slashX = nx;
            slashY = ny;
            if (target.isAlive()) {
                message = Localization.t("msg.attacked", target.kind.label(), damage);
            } else {
                spawnDeathParticles(target);
                enemies.remove(target);
                if (target.isBoss) {
                    // ボスを倒すと良いアイテムをその場に落とす。
                    Item.Kind[] loot = {Item.Kind.FRYING_PAN, Item.Kind.JACKET, Item.Kind.SWEETS};
                    Item.Kind drop = loot[(int) (Math.random() * loot.length)];
                    items.add(new Item(drop, target.x, target.y));
                    message = Localization.t("msg.defeated_boss",
                            target.kind.label(), drop.label());
                    updateMusic();
                } else {
                    message = Localization.t("msg.defeated", target.kind.label());
                    // モンスターハウスの中の敵を倒しきったらボス曲から戻る。
                    if (monsterHouseMusic && !monsterHouseHasEnemies()) {
                        monsterHouseMusic = false;
                        updateMusic();
                    }
                }
            }
            turn++;
            enemyTurn();
            return;
        }
        if (!floor.isWalkable(nx, ny)) {
            return;
        }
        moveDist = (float) Math.sqrt(dx * dx + dy * dy);
        playerX = nx;
        playerY = ny;
        walkStep++;
        turn++;
        updateVisibility();
        pickUpItem();
        if (gameOver) {
            return;
        }
        enemyTurn();
        if (!gameOver) {
            checkMonsterHouseEntry();
        }
        Floor.Tile t = floor.tiles[nx][ny];
        if (t == Floor.Tile.ESCALATOR || t == Floor.Tile.STAIRS) {
            floorNumber++;
            loadFloor();
        }
    }

    /**
     * モンスターハウスに入ったかどうかを調べる。初めて入ったときにだけ
     * メッセージで知らせる（中の敵は視界ルール通り、入るまで見えない）。
     */
    private void checkMonsterHouseEntry() {
        if (!floor.hasMonsterHouse || monsterHouseAnnounced) {
            return;
        }
        if (playerX >= floor.mhX0 && playerX <= floor.mhX1
                && playerY >= floor.mhY0 && playerY <= floor.mhY1) {
            monsterHouseAnnounced = true;
            message = Localization.t("msg.monster_house");
            // モンスターハウスに入ったらボス曲に切り替わる。
            monsterHouseMusic = true;
            updateMusic();
        }
    }

    /**
     * 今いるマスにアイテムがあれば拾う（移動の一部として1ターンを消費済み）。
     * 持ち物がいっぱいのときは拾わず、その場に残してメッセージで知らせる。
     */
    private void pickUpItem() {
        Item it = itemAt(playerX, playerY);
        if (it == null) {
            return;
        }
        if (inventory.size() >= MAX_INVENTORY) {
            message = Localization.t("msg.inv_full_pickup", it.kind.label());
            return;
        }
        items.remove(it);
        inventory.add(it);
        message = Localization.t("msg.picked_up", it.kind.label());
    }

    /**
     * 持ち物一覧で選んだアイテムを使う／装備する。1ターン消費する。
     * 回復アイテムなら使ってHPを回復する。武器・防具なら装備する。
     * 装備中のものと交換する場合は古い装備が持ち物に戻る。
     * 持ち物がいっぱいで交換できない場合は何もしない（ターンも消費しない）。
     */
    private void useItem(int index) {
        if (gameOver || index < 0 || index >= inventory.size()) {
            return;
        }
        Item it = inventory.get(index);
        if (it.kind.type == Item.Type.CONSUMABLE) {
            int before = playerHp;
            playerHp = Math.min(PLAYER_MAX_HP, playerHp + it.kind.heal);
            inventory.remove(index);
            message = Localization.t("msg.used", it.kind.label(), playerHp - before);
            turn++;
            enemyTurn();
            return;
        }
        // 武器・防具の装備（交換）
        boolean isWeapon = it.kind.type == Item.Type.WEAPON;
        Item current = isWeapon ? equippedWeapon : equippedArmor;
        if (current != null && inventory.size() >= MAX_INVENTORY) {
            message = Localization.t("msg.inv_full_swap");
            return;
        }
        inventory.remove(index);
        if (current != null) {
            inventory.add(current);
        }
        if (isWeapon) {
            equippedWeapon = it;
        } else {
            equippedArmor = it;
        }
        message = Localization.t("msg.equipped", it.kind.label());
        turn++;
        enemyTurn();
    }

    private void waitTurn() {
        if (gameOver) {
            return;
        }
        turn++;
        enemyTurn();
    }

    /**
     * 敵のターン。敵はプレイヤーの1ターンごとに1回行動する。
     * - プレイヤーに隣接（8方向で1マス）していたら攻撃する。
     * - プレイヤーが5マス以内にいるとプレイヤーに向かって1マス近づく。
     * - それ以外はランダムな方向に1マスうろつく（壁や他の敵の方向には動かない）。
     * 移動・攻撃した方向を向く。攻撃時は突進アニメーションと斬撃を見せる。
     */
    private void enemyTurn() {
        if (gameOver) {
            return;
        }
        updateAnnouncement();
        List<Enemy> acting = new ArrayList<Enemy>(enemies);
        for (Enemy e : acting) {
            if (!enemies.contains(e)) {
                continue;
            }
            int dist = chebyshev(e.x, e.y, playerX, playerY);
            if (dist == 1) {
                int damage = Math.max(1, e.attack - playerDefense());
                playerHp -= damage;
                flash(1f, 0.2f, 0.2f);
                e.facing = directionOf(playerX - e.x, playerY - e.y);
                e.lungeTimer = ATTACK_LUNGE_TIME;
                e.lungeDir = e.facing;
                slashTimer = SLASH_TIME;
                slashX = playerX;
                slashY = playerY;
                message = Localization.t("msg.hit_by", e.kind.label(), damage);
                if (playerHp <= 0) {
                    playerHp = 0;
                    gameOver = true;
                    message = Localization.t("msg.game_over");
                }
                continue;
            }
            int dx = 0;
            int dy = 0;
            if (dist <= CHASE_RANGE) {
                // プレイヤーに近づく：8方向のうちチェビシェフ距離が最も縮む方向を選ぶ
                int bestDist = dist;
                for (int d = 0; d < 8; d++) {
                    int ndx = DIR_DXY[d][0];
                    int ndy = DIR_DXY[d][1];
                    if (!canEnemyMoveTo(e, e.x + ndx, e.y + ndy)) {
                        continue;
                    }
                    int nd = chebyshev(e.x + ndx, e.y + ndy, playerX, playerY);
                    if (nd < bestDist) {
                        bestDist = nd;
                        dx = ndx;
                        dy = ndy;
                    }
                }
            } else {
                // うろつく：ランダムな8方向のうち進める方向を1つ選ぶ
                int start = (int) (Math.random() * DIR_DXY.length);
                for (int i = 0; i < DIR_DXY.length; i++) {
                    int[] d = DIR_DXY[(start + i) % DIR_DXY.length];
                    if (canEnemyMoveTo(e, e.x + d[0], e.y + d[1])) {
                        dx = d[0];
                        dy = d[1];
                        break;
                    }
                }
            }
            if ((dx != 0 || dy != 0) && canEnemyMoveTo(e, e.x + dx, e.y + dy)) {
                e.x += dx;
                e.y += dy;
                e.facing = directionOf(dx, dy);
            }
        }
    }

    /** 敵がそのタイルに移動できるか（歩行可能・他の敵なし・プレイヤーの位置でない）。 */
    private boolean canEnemyMoveTo(Enemy self, int x, int y) {
        if (!floor.isWalkable(x, y)) {
            return false;
        }
        if (x == playerX && y == playerY) {
            return false;
        }
        return enemyAt(x, y) == null;
    }

    /** 店内放送：20ターンごとにフロアタイプの文言を切り替える。 */
    private void updateAnnouncement() {
        if (turn - announcementTurn >= ANNOUNCE_EVERY_TURNS) {
            String[] lines = Announcements.linesFor(floor.type);
            announcementIndex = (announcementIndex + 1) % lines.length;
            announcement = lines[announcementIndex];
            announcementTurn = turn;
        }
    }

    /** 攻撃・被弾時の画面フラッシュを設定する。 */
    private void flash(float r, float g, float b) {
        flashTimer = 0.18f;
        flashR = r;
        flashG = g;
        flashB = b;
    }

    /** フロアアップ時のきらめき：プレイヤーの周りに金色の点を散らす。 */
    private void spawnSparkles() {
        for (int i = 0; i < 24; i++) {
            Particle p = new Particle();
            p.x = playerX + (float) (Math.random() * 12 - 6) + 0.5f;
            p.y = playerY + (float) (Math.random() * 12 - 6) + 0.5f;
            p.vx = (float) (Math.random() * 0.2 - 0.1);
            p.vy = (float) (Math.random() * 0.4 + 0.1);
            p.maxLife = p.life = 1.2f + (float) Math.random() * 1.3f;
            p.size = 3f + (float) Math.random() * 4f;
            p.r = 1f;
            p.g = 0.9f;
            p.b = 0.4f;
            particles.add(p);
        }
    }

    /** 敵撃破時のパーティクル：倒れた場所に敵の色の点が散って消える。 */
    private void spawnDeathParticles(Enemy e) {
        for (int i = 0; i < 8; i++) {
            Particle p = new Particle();
            p.x = e.x + 0.5f;
            p.y = e.y + 0.5f;
            double angle = Math.random() * Math.PI * 2;
            double speed = 0.8 + Math.random() * 1.9;
            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed);
            p.maxLife = p.life = 0.4f + (float) Math.random() * 0.3f;
            p.size = 3f + (float) Math.random() * 3f;
            p.r = e.kind.r;
            p.g = e.kind.g;
            p.b = e.kind.b;
            particles.add(p);
        }
    }

    /** パーティクルを1フレーム分進める。 */
    private void updateParticles(float delta) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= delta;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vy -= 0.42f * delta;
        }
    }

    /** ゲームオーバーからのリスタート。最初からやり直す。 */
    private void restart() {
        floorNumber = 1;
        turn = 0;
        playerHp = PLAYER_MAX_HP;
        gameOver = false;
        message = "";
        inventory = new ArrayList<Item>();
        equippedWeapon = null;
        equippedArmor = null;
        showInventory = false;
        selectedItem = 0;
        loadFloor();
    }

    @Override
    public boolean keyDown(int keycode) {
        // スプライト確認デバッグモード：左右で種別切替、B／Back／Esc／F1 でタイトルへ。
        if (spriteDebug) {
            if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
                moveDebugKind(-1);
                return true;
            }
            if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
                moveDebugKind(1);
                return true;
            }
            if (keycode == Input.Keys.B || keycode == Input.Keys.BACK
                    || keycode == Input.Keys.ESCAPE || keycode == Input.Keys.F1
                    || keycode == Input.Keys.BUTTON_B) {
                spriteDebug = false;
                return true;
            }
            if (keycode == Input.Keys.L) {
                toggleLanguage();
                return true;
            }
            return true;
        }
        // タイトル画面：上下でメニュー（ゲームスタート／スプライト確認）を選び、
        // 決定キーで実行する。Lキーで言語を切り替える。F1キーで直接スプライト確認を開く。
        if (titleScreen) {
            if (keycode == Input.Keys.L) {
                toggleLanguage();
                return true;
            }
            if (keycode == Input.Keys.F1) {
                openSpriteDebug();
                return true;
            }
            if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
                titleMenuIndex = (titleMenuIndex + 1) % 2;
                return true;
            }
            if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
                titleMenuIndex = (titleMenuIndex + 1) % 2;
                return true;
            }
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.CENTER
                    || keycode == Input.Keys.BUTTON_A || keycode == Input.Keys.SPACE
                    || keycode == Input.Keys.BUTTON_START) {
                if (titleMenuIndex == 1) {
                    openSpriteDebug();
                } else {
                    startGame();
                }
            }
            return true;
        }
        if (gameOver) {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.BUTTON_A
                    || keycode == Input.Keys.DPAD_CENTER || keycode == Input.Keys.CENTER) {
                restart();
                return true;
            }
            return true;
        }
        // 持ち物一覧が開いているとき：開閉・項目移動はターンを消費しない。
        // 数字キーでの直接選択も残す（キーボード操作）。
        if (showInventory) {
            if (keycode == Input.Keys.I || keycode == Input.Keys.BUTTON_B
                    || keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE
                    || keycode == Input.Keys.BUTTON_START || keycode == Input.Keys.BUTTON_Y) {
                showInventory = false;
                return true;
            }
            if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
                moveSelection(-1);
                return true;
            }
            if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
                moveSelection(1);
                return true;
            }
            if (keycode == Input.Keys.BUTTON_A || keycode == Input.Keys.DPAD_CENTER
                    || keycode == Input.Keys.CENTER || keycode == Input.Keys.ENTER) {
                useItem(selectedItem);
                clampSelectedItem();
                return true;
            }
            int index = numberKeyIndex(keycode);
            if (index >= 0) {
                selectedItem = index;
                useItem(index);
                clampSelectedItem();
            }
            return true;
        }
        switch (keycode) {
            // 矢印キー／WASD は画面の方向に進む（上＝北、下＝南、左＝西、右＝東）。
            case Input.Keys.UP:
            case Input.Keys.W:
                tryMove(0, -1);
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                tryMove(0, 1);
                return true;
            case Input.Keys.LEFT:
            case Input.Keys.A:
                tryMove(-1, 0);
                return true;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                tryMove(1, 0);
                return true;
            // 残りの4方向は Q／E／Z／C キー（Q＝北西、E＝北東、Z＝南西、C＝南東）。
            case Input.Keys.Q:
                tryMove(-1, -1);
                return true;
            case Input.Keys.E:
                tryMove(1, -1);
                return true;
            case Input.Keys.Z:
                tryMove(-1, 1);
                return true;
            case Input.Keys.C:
                tryMove(1, 1);
                return true;
            case Input.Keys.SPACE:
            case Input.Keys.PERIOD:
                waitTurn();
                return true;
            case Input.Keys.M:
                showMinimap = !showMinimap;
                return true;
            case Input.Keys.N:
                music.setMuted(!music.isMuted());
                message = Localization.t(music.isMuted() ? "hud.music_off" : "hud.music_on");
                return true;
            case Input.Keys.I:
            case Input.Keys.BUTTON_START:
            case Input.Keys.BUTTON_Y:
                showInventory = true;
                selectedItem = 0;
                return true;
            default:
                return false;
        }
    }

    /** 持ち物一覧の選択項目を移動する（端で折り返す。空なら何もしない）。 */
    private void moveSelection(int delta) {
        if (inventory.isEmpty()) {
            return;
        }
        selectedItem = (selectedItem + delta + inventory.size()) % inventory.size();
    }

    /** 選択項目が持ち物の範囲に収まるようにする（使って減ったあと用）。 */
    private void clampSelectedItem() {
        if (inventory.isEmpty()) {
            selectedItem = 0;
        } else if (selectedItem >= inventory.size()) {
            selectedItem = inventory.size() - 1;
        }
    }

    /** タイトル画面からゲームを開始する（ダンジョンの BGM に切り替わる）。 */
    private void startGame() {
        titleScreen = false;
        message = "";
        updateMusic();
    }

    /** 言語を切り替え、フォントを作り直す（タイトル画面・デバッグ画面の L キー用）。 */
    private void toggleLanguage() {
        Localization.toggleLanguage();
        Fonts.reload();
        font = Fonts.font;
        hudFont = Fonts.hudFont;
    }

    /** スプライト確認デバッグモードを開く（タイトル画面から）。 */
    private void openSpriteDebug() {
        spriteDebug = true;
        debugTime = 0f;
    }

    /** デバッグモードで表示するキャラクター種別を移動する（端で折り返す）。 */
    private void moveDebugKind(int delta) {
        int count = debugEntryCount();
        debugKindIndex = (debugKindIndex + delta + count) % count;
    }

    /** デバッグモードの表示対象の数（主人公＋敵14種）。 */
    private int debugEntryCount() {
        return 1 + Enemy.Kind.values().length;
    }

    /** デバッグモードで主人公を表示しているかどうか。 */
    private boolean debugIsHero() {
        return debugKindIndex == 0;
    }

    /** デバッグモードで表示中の敵の種類（主人公のときは null）。 */
    private Enemy.Kind debugKind() {
        if (debugIsHero()) {
            return null;
        }
        return Enemy.Kind.values()[debugKindIndex - 1];
    }

    /** デバッグモードで表示中のキャラクター名（現在の言語）。 */
    private String debugName() {
        if (debugIsHero()) {
            return Localization.t("debug.hero");
        }
        return debugKind().label();
    }

    /** デバッグモードで表示中のキャラクターの歩行フレーム数。 */
    private int debugFrameCount() {
        if (debugIsHero()) {
            return sprites.heroFrameCount();
        }
        return sprites.enemyFrameCount(debugKind());
    }

    /** デバッグモードで表示中のキャラクターの描画基準高さ（ボスは大きく）。 */
    private float debugBaseHeight() {
        if (!debugIsHero() && isBossKind(debugKind())) {
            return 100f;
        }
        return 72f;
    }

    /** ボスの種類かどうか（店長／巨大着ぐるみ／大天使）。 */
    private static boolean isBossKind(Enemy.Kind kind) {
        return kind == Enemy.Kind.MANAGER || kind == Enemy.Kind.GIANT_MASCOT
                || kind == Enemy.Kind.ARCHANGEL;
    }

    /** デバッグモードで表示中のキャラクターの、向きと行に応じたスプライト名。
     * row は 0=停止、1=歩行、2=武器使用。歩行は frame で切り替える。 */
    private String debugSpriteName(int dir, int row, int frame) {
        if (debugIsHero()) {
            return sprites.heroSpriteName(dir, frame % sprites.heroFrameCount());
        }
        Enemy.Kind kind = debugKind();
        if (sprites.hasDirectionSprites(kind)) {
            return sprites.enemySpriteName(kind, dir);
        }
        return sprites.enemySpriteName(kind);
    }

    /** 数字キー（1〜9、0）に対応する持ち物の番号を返す。数字キーでなければ -1。 */
    private int numberKeyIndex(int keycode) {
        switch (keycode) {
            case Input.Keys.NUM_1: return 0;
            case Input.Keys.NUM_2: return 1;
            case Input.Keys.NUM_3: return 2;
            case Input.Keys.NUM_4: return 3;
            case Input.Keys.NUM_5: return 4;
            case Input.Keys.NUM_6: return 5;
            case Input.Keys.NUM_7: return 6;
            case Input.Keys.NUM_8: return 7;
            case Input.Keys.NUM_9: return 8;
            case Input.Keys.NUM_0: return 9;
            default: return -1;
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // タイトル画面：スプライト確認のメニュー行をタップしたらデバッグモードを
        // 開き、それ以外のタップでゲーム開始する。
        if (titleScreen) {
            Vector3 v = new Vector3(screenX, screenY, 0);
            viewport.unproject(v);
            float cy = camera.position.y;
            if (v.y > cy - 125 && v.y < cy - 85) {
                openSpriteDebug();
            } else {
                startGame();
            }
            return true;
        }
        if (gameOver) {
            return true;
        }
        if (showInventory) {
            // 持ち物一覧が開いているときはタップ移動しない
            return true;
        }
        // タップした方向へ1マス進む（斜めは横方向を優先）
        Vector3 v = new Vector3(screenX, screenY, 0);
        viewport.unproject(v);
        float[] tile = screenToTile(v.x, v.y);
        int tx = (int) Math.floor(tile[0] + 0.5f);
        int ty = (int) Math.floor(tile[1] + 0.5f);
        if (!floor.inBounds(tx, ty)) {
            return true;
        }
        int dx = Integer.compare(tx, playerX);
        int dy = Integer.compare(ty, playerY);
        if (dx != 0 && dy != 0) {
            dy = 0;
        }
        if (dx == 0 && dy == 0) {
            return true;
        }
        tryMove(dx, dy);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        followCamera();

        // スプライト確認デバッグモード：ダンジョンは描かず、確認画面だけ表示する。
        if (spriteDebug) {
            drawSpriteDebug(delta);
            return;
        }

        // タイトル画面：ダンジョンは描かず、タイトルだけ表示する。
        if (titleScreen) {
            drawTitle();
            return;
        }

        // 演出のタイマーを進める。
        if (flashTimer > 0) {
            flashTimer -= delta;
        }
        if (attackFlashTimer > 0) {
            attackFlashTimer -= delta;
        }
        updateParticles(delta);
        updateAnimation(delta);
        updateFog(delta);

        // タイルと登場人物を見下ろしスプライトで描く（ペインター法：行ごとに上から下へ）
        float bob = playerBob();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int y = 0; y < floor.h; y++) {
            for (int x = 0; x < floor.w; x++) {
                drawTile(x, y);
                Item it = itemAt(x, y);
                if (it != null && floor.visited[x][y]) {
                    drawItem(it);
                }
            }
            // この行にいる敵・プレイヤー（補間位置の行で判定）
            for (Enemy e : enemies) {
                if (!floor.visited[e.x][e.y]) {
                    continue;
                }
                if ((int) Math.floor(e.ry + 0.5f) == y) {
                    drawCharacter(e);
                }
            }
            if ((int) Math.floor(playerRy + 0.5f) == y) {
                drawPlayer(bob);
            }
        }
        batch.end();

        // 斬撃エフェクト（白い弧）とヒット時の星（攻撃したタイルに描く）
        if (slashTimer > 0 || attackFlashTimer > 0) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            if (slashTimer > 0) {
                float alpha = slashTimer / SLASH_TIME;
                batch.setColor(1f, 1f, 1f, alpha);
                float sx = tileX(slashX);
                float sy = tileY(slashY);
                batch.draw(sprites.slash, sx - 32, sy - 32, 64, 64);
            }
            if (attackFlashTimer > 0) {
                float alpha = attackFlashTimer / 0.2f;
                batch.setColor(1f, 1f, 1f, alpha);
                float fx = tileX(attackFlashX);
                float fy = tileY(attackFlashY);
                batch.draw(sprites.hitstar, fx - 24, fy - 24, 48, 48);
            }
            batch.setColor(1f, 1f, 1f, 1f);
            batch.end();
        }

        // 階層別の雰囲気オーバーレイ＋光＋パーティクル
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float[] ov = themeOverlay();
        if (ov[3] > 0) {
            shapes.setColor(ov[0], ov[1], ov[2], ov[3]);
            shapes.rect(camera.position.x - VIEW_W / 2f, camera.position.y - VIEW_H / 2f,
                    VIEW_W, VIEW_H);
        }
        // プレイヤーを中心とした柔らかい光（上層ほど強く。視界ルールは変えない）
        float ls = lightStrength();
        float pcx = tileX(playerRx);
        float pcy = tileY(playerRy) + bob;
        shapes.setColor(1f, 0.85f, 0.45f, 0.10f * ls);
        shapes.circle(pcx, pcy, TILE * 1.3f);
        shapes.setColor(1f, 0.85f, 0.45f, 0.05f * ls);
        shapes.circle(pcx, pcy, TILE * 2.6f);
        shapes.setColor(1f, 0.85f, 0.45f, 0.025f * ls);
        shapes.circle(pcx, pcy, TILE * 4.0f);
        // パーティクル（きらめき・撃破時の破片。霧はテクスチャ付きなので後で描く）
        for (Particle p : particles) {
            if (p.tex != null) {
                continue;
            }
            float alpha = Math.max(0f, p.life / p.maxLife) * p.alpha;
            shapes.setColor(p.r, p.g, p.b, alpha);
            shapes.circle(tileX(p.x), tileY(p.y), p.size);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 霧のテクスチャパーティクル（オーバーレイの上に薄く漂わせる）
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Particle p : particles) {
            if (p.tex == null) {
                continue;
            }
            float alpha = Math.max(0f, p.life / p.maxLife) * p.alpha;
            float w = p.texW;
            float h = w * p.tex.getHeight() / (float) p.tex.getWidth();
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(p.tex, tileX(p.x) - w / 2f, tileY(p.y) - h / 2f, w, h);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        drawHud();
        if (showMinimap) {
            drawMinimap();
        }
        if (showInventory) {
            drawInventory();
        }
        if (gameOver) {
            drawGameOver();
        }
        // 攻撃・被弾時の画面フラッシュ（全体にかかる薄い色）
        if (flashTimer > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(flashR, flashG, flashB, (flashTimer / 0.18f) * 0.22f);
            shapes.rect(camera.position.x - VIEW_W / 2f, camera.position.y - VIEW_H / 2f,
                    VIEW_W, VIEW_H);
            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    /** タイトル画面。Enterキー／タップでゲーム開始する。Lキーで言語を切り替える。 */
    private void drawTitle() {
        float cx = camera.position.x;
        float cy = camera.position.y;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.05f, 1f);
        shapes.rect(cx - VIEW_W / 2f, cy - VIEW_H / 2f, VIEW_W, VIEW_H);
        // タイトルの飾り：金色の丸い看板
        shapes.setColor(1f, 0.85f, 0.2f, 1f);
        shapes.circle(cx, cy + 120, 70);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.BLACK);
        font.draw(batch, "MALL", cx - 55, cy + 135);
        font.setColor(Color.YELLOW);
        drawCentered(font, "DOPAMINE MALL", cx, cy + 20);
        hudFont.setColor(Color.WHITE);
        drawCentered(hudFont, Localization.t("title.subtitle"), cx, cy - 20);
        // メニュー：ゲームスタート／スプライト確認。選んでいる項目に ">" を付ける。
        hudFont.setColor(titleMenuIndex == 0 ? Color.YELLOW : Color.WHITE);
        drawCentered(hudFont, (titleMenuIndex == 0 ? "> " : "  ")
                + Localization.t("title.menu_start"), cx, cy - 70);
        hudFont.setColor(titleMenuIndex == 1 ? Color.YELLOW : Color.WHITE);
        drawCentered(hudFont, (titleMenuIndex == 1 ? "> " : "  ")
                + Localization.t("title.menu_debug"), cx, cy - 105);
        hudFont.setColor(Color.LIGHT_GRAY);
        drawCentered(hudFont, Localization.t("title.menu_hint"), cx, cy - 140);
        drawCentered(hudFont, Localization.t("title.keys1"), cx, cy - 175);
        drawCentered(hudFont, Localization.t("title.keys2"), cx, cy - 205);
        hudFont.setColor(Color.CYAN);
        drawCentered(hudFont, Localization.t("title.language"), cx, cy - 245);
        batch.end();
    }

    /** スプライト確認デバッグモードの画面。
     * キャラクター種別ごとに8方向を列に並べ、行を「停止」「歩行」「武器使用」の
     * 3段にした一覧を表示する。歩行は実際の歩行フレームをアニメーションさせ、
     * 武器使用はゲーム内の攻撃アニメーション（突進＋斬撃＋フラッシュ）を
     * その向きで再生して表示する。各セルの下に実寸（幅×高さ）とフレーム数を出す。
     * 左右キーで種別を切り替える。B／Back／Esc／F1 でタイトル画面に戻る。 */
    private void drawSpriteDebug(float delta) {
        debugTime += delta;
        float cx = camera.position.x;
        float cy = camera.position.y;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.05f, 1f);
        shapes.rect(cx - VIEW_W / 2f, cy - VIEW_H / 2f, VIEW_W, VIEW_H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        float colW = VIEW_W / 8f;
        float baseH = debugBaseHeight();
        int frames = debugFrameCount();
        // 歩行フレーム（0.15秒ごとに切り替える）と攻撃の進行度（0〜1を繰り返す）。
        int walkFrame = (int) (debugTime / 0.15f) % Math.max(1, frames);
        float attackPhase = (debugTime * 1.2f) % 1f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // タイトルと種別名・番号
        hudFont.setColor(Color.YELLOW);
        drawCentered(hudFont, Localization.t("debug.title"), cx, cy + 350);
        hudFont.setColor(Color.WHITE);
        drawCentered(hudFont, debugName() + "  (" + (debugKindIndex + 1)
                + " / " + debugEntryCount() + ")", cx, cy + 315);
        // 方向ごとの表示倍率（正規化の確認用）
        StringBuilder scales = new StringBuilder(Localization.t("debug.scales"));
        for (int d = 0; d < 8; d++) {
            String name = debugSpriteName(d, 0, 0);
            Sprites.Norm n = sprites.normOf(name);
            scales.append(" ").append(n != null ? String.format("%.2f", n.scale) : "-");
        }
        hudFont.setColor(Color.LIGHT_GRAY);
        drawCentered(hudFont, scales.toString(), cx, cy + 288);
        // 方向ラベル
        for (int d = 0; d < 8; d++) {
            float colCx = cx - VIEW_W / 2f + colW * (d + 0.5f);
            drawCentered(hudFont, Localization.t("debug.dir" + d), colCx, cy + 262);
        }
        // 3段の行ラベルと各セル
        String[] rowKeys = {"debug.idle", "debug.walk", "debug.attack"};
        float[] rowFeet = {cy + 165, cy + 35, cy - 95};
        float[] rowLabelY = {cy + 245, cy + 115, cy - 15};
        for (int row = 0; row < 3; row++) {
            hudFont.setColor(Color.CYAN);
            hudFont.draw(batch, Localization.t(rowKeys[row]),
                    cx - VIEW_W / 2f + 12, rowLabelY[row]);
            for (int d = 0; d < 8; d++) {
                float colCx = cx - VIEW_W / 2f + colW * (d + 0.5f);
                float feetY = rowFeet[row];
                // 武器使用の行：攻撃方向へ突進する（ゲーム内の攻撃アニメーションと同じ）
                float drawX = colCx;
                float drawY = feetY;
                if (row == 2) {
                    float mag = (float) Math.sin(attackPhase * Math.PI) * 0.35f * TILE;
                    int[] dir = DIR_DXY[d];
                    drawX += dir[0] * mag;
                    drawY += -dir[1] * mag;
                }
                // 歩行の行：1フレームしかない敵は小さく跳ねて歩行中を示す
                if (row == 1 && frames <= 1) {
                    drawY += (float) Math.abs(Math.sin(debugTime * 6f)) * 3f;
                }
                int frame = row == 1 ? walkFrame : 0;
                String name = debugSpriteName(d, row, frame);
                sprites.drawShadow(batch, drawX, feetY, baseH * 0.62f);
                sprites.drawCharacter(batch, name, drawX, drawY, baseH);
                // 武器使用の行：斬撃エフェクトとフラッシュ（攻撃対象の位置に描く）
                if (row == 2) {
                    int[] dir = DIR_DXY[d];
                    float tx = colCx + dir[0] * TILE * 0.62f;
                    float ty = feetY - dir[1] * TILE * 0.62f;
                    float alpha = 1f - attackPhase;
                    batch.setColor(1f, 1f, 1f, alpha);
                    batch.draw(sprites.slash, tx - 32, ty - 32, 64, 64);
                    batch.setColor(1f, 1f, 0.6f, alpha);
                    batch.draw(sprites.hitstar, tx - 24, ty - 24, 48, 48);
                    batch.setColor(1f, 1f, 1f, 1f);
                }
                // セルの下に実寸（幅×高さ）とフレーム数を表示する
                Sprites.Norm n = sprites.normOf(name);
                String size;
                if (n != null && n.w > 0) {
                    size = Localization.t("debug.cell", n.w, n.h, frames);
                } else {
                    size = Localization.t("debug.cell", 0, 0, frames);
                }
                hudFont.setColor(Color.LIGHT_GRAY);
                drawCentered(hudFont, size, colCx, feetY - 18);
            }
        }
        hudFont.setColor(Color.WHITE);
        drawCentered(hudFont, Localization.t("debug.hint"), cx, cy - 170);
        batch.end();
    }

    /** テキストを横方向の中央揃えで描く（タイトル画面・ゲームオーバー用）。 */
    private void drawCentered(BitmapFont f, String text, float cx, float y) {
        layout.setText(f, text);
        f.draw(batch, text, cx - layout.width / 2f, y);
    }

    /** スプライトを足元中心 (cx, baseY) に、高さ height で描く。 */
    private void drawAnchored(Texture tex, float cx, float baseY, float height) {
        float w = height * tex.getWidth() / (float) tex.getHeight();
        batch.draw(tex, cx - w / 2f, baseY, w, height);
    }

    /** 地形スプライトをタイルの位置に描く（高さつきの立体物として）。 */
    private void drawTerrain(Floor.Tile t, float px, float py) {
        switch (t) {
            case WALL:
                // 壁は drawTile から1チップで描く（ここには来ない）
                break;
            case SHELF:
                // テナントフロア（ブティック・雑貨店）は本棚、スーパーは商品棚
                drawAnchored(floor.type == FloorType.TENANT ? sprites.bookshelf : sprites.shelf,
                        px, py, 80);
                break;
            case MACHINE:
                drawAnchored(sprites.arcade, px, py, 88);
                break;
            case TABLE:
                drawAnchored(sprites.table, px, py, 56);
                break;
            case COUNTER:
                drawAnchored(sprites.counter, px, py, 64);
                break;
            case STAGE:
                drawAnchored(sprites.stage, px, py, 56);
                break;
            case SEAT:
                drawAnchored(sprites.bench, px, py, 56);
                break;
            case ESCALATOR:
                drawAnchored(sprites.escalator, px, py, 72);
                break;
            case STAIRS:
                drawAnchored(sprites.stairs, px, py, 72);
                break;
            case FLOOR:
            default:
                break;
        }
    }

    /** タイルを見下ろしスプライトで描く（batch.begin() の中で呼ぶこと）。
     * 壁は1マス＝1チップ（上面と前面を1枚に含む壁チップをタイルにそのまま1枚描く）。 */
    private void drawTile(int x, int y) {
        float px = tileX(x);
        float py = tileY(y);
        Floor.Tile t = floor.tiles[x][y];
        if (!floor.visited[x][y]) {
            // 未踏の場所は暗くする（スプライトをほぼ黒く着色して描く）
            batch.setColor(0.05f, 0.05f, 0.08f, 1f);
            if (t == Floor.Tile.WALL) {
                drawWall(px, py);
            } else if (t == Floor.Tile.FLOOR) {
                batch.draw(sprites.floorTexture(floor.type),
                        px - TILE / 2f, py - TILE / 2f, TILE, TILE);
            } else {
                batch.draw(sprites.floorTexture(floor.type),
                        px - TILE / 2f, py - TILE / 2f, TILE, TILE);
                drawTerrain(t, px, py);
            }
            batch.setColor(1f, 1f, 1f, 1f);
            return;
        }
        if (t == Floor.Tile.WALL) {
            drawWall(px, py);
        } else if (t == Floor.Tile.FLOOR) {
            // フロアタイプごとにわずかに色調を変える
            float[] c = floorTint();
            batch.setColor(c[0], c[1], c[2], 1f);
            batch.draw(sprites.floorTexture(floor.type),
                    px - TILE / 2f, py - TILE / 2f, TILE, TILE);
            batch.setColor(1f, 1f, 1f, 1f);
            // 装飾小物（床タイルに置いたもの）
            Prop prop = propAt(x, y);
            if (prop != null) {
                drawProp(prop, px, py);
            }
        } else {
            // 床の下に四角いチップを敷いてから立体物を描く
            batch.draw(sprites.floorTexture(floor.type),
                    px - TILE / 2f, py - TILE / 2f, TILE, TILE);
            drawTerrain(t, px, py);
        }
    }

    /** 背の高い壁を描く（batch.begin() の中で呼ぶこと）。
     * 壁チップはタイルより背が高く（幅64×高さ128）、下端をタイルの下端に
     * 合わせて上に1タイル分はみ出す。行ごとのペインター法により、はみ出し
     * 部分が上の行の床の上に、壁より手前（南）の行のものが壁の前面の上に描かれる。 */
    private void drawWall(float px, float py) {
        batch.draw(sprites.wall, px - TILE / 2f, py - TILE / 2f, TILE, WALL_H);
    }

    /** 装飾小物を描く。 */
    private void drawProp(Prop prop, float px, float py) {
        float height;
        switch (prop.kind) {
            case VENDING:
            case LAMP:
                height = 72;
                break;
            case SIGN:
                height = 64;
                break;
            case COINS:
                height = 40;
                break;
            case LANTERN:
            case PLANT:
            case TRASH:
            case CART:
            case BENCH:
            default:
                height = 56;
                break;
        }
        drawAnchored(sprites.propTexture(prop.kind), px, py, height);
    }

    /** アイテムをタイルの中心に描く。 */
    private void drawItem(Item it) {
        float px = tileX(it.x);
        float py = tileY(it.y);
        Texture tex = sprites.itemTexture(it.kind);
        float h = 28;
        float w = h * tex.getWidth() / (float) tex.getHeight();
        batch.draw(tex, px - w / 2f, py - h / 2f, w, h);
    }

    /** 敵を補間位置に描く（ボスは大きく表示する）。向きに合ったスプライトを使う。
     * 方向ごとの表示サイズを正規化し、足元に柔らかい影を描く。 */
    private void drawCharacter(Enemy e) {
        float px = tileX(e.rx);
        float py = tileY(e.ry);
        float height = e.isBoss ? 90 : 64;
        // 影は地面（突進していない位置）に描く
        sprites.drawShadow(batch, px, py, e.isBoss ? 56 : 40);
        // 攻撃時の突進：攻撃方向へ素早く出て戻る
        if (e.lungeTimer > 0) {
            float progress = 1f - e.lungeTimer / ATTACK_LUNGE_TIME;
            float mag = (float) Math.sin(progress * Math.PI) * 0.35f;
            int[] d = DIR_DXY[e.lungeDir];
            px += d[0] * mag * TILE;
            py += -d[1] * mag * TILE;
        }
        sprites.drawCharacter(batch, sprites.enemySpriteName(e.kind, e.facing),
                px, py, height);
    }

    /** プレイヤーを補間位置に描く（歩行時の上下動と歩行フレームつき）。
     * 方向ごとの表示サイズを正規化し、足元に柔らかい影を描く。 */
    private void drawPlayer(float bob) {
        float px = tileX(playerRx);
        float py = tileY(playerRy);
        // 影は地面（上下動・突進していない位置）に描く
        sprites.drawShadow(batch, px, py, 40);
        py += bob;
        // 攻撃時の突進：攻撃方向へ素早く出て戻る
        if (attackLungeTimer > 0) {
            float progress = 1f - attackLungeTimer / ATTACK_LUNGE_TIME;
            float mag = (float) Math.sin(progress * Math.PI) * 0.35f;
            int[] d = DIR_DXY[attackDir];
            px += d[0] * mag * TILE;
            py += -d[1] * mag * TILE;
        }
        sprites.drawCharacter(batch,
                sprites.heroSpriteName(playerFacing, walkFrame()), px, py, 64);
    }

    /** フロアタイプごとの床の色調（スプライトの色に掛ける）。 */
    private float[] floorTint() {
        switch (floor.type) {
            case SUPERMARKET:
                return new float[]{0.9f, 1f, 0.9f};
            case GAME_CENTER:
                return new float[]{0.95f, 0.9f, 1f};
            case FOOD_COURT:
                return new float[]{1f, 0.95f, 0.85f};
            case EVENT:
                return new float[]{0.9f, 0.95f, 1f};
            case TENANT:
            default:
                return new float[]{1f, 1f, 1f};
        }
    }

    /** 階層別の雰囲気：上層ほど暗く神秘的にするオーバーレイの色と不透明度。 */
    private float[] themeOverlay() {
        if (floorNumber <= 4) {
            // 下層：明るい現代のモール（オーバーレイなし）
            return new float[]{1f, 0.96f, 0.85f, 0f};
        }
        if (floorNumber <= 9) {
            // 中層：やや落ち着いた光
            return new float[]{0.85f, 0.88f, 1f, 0.08f};
        }
        if (floorNumber <= 14) {
            // 上層：暗めで霧がかった感じ
            return new float[]{0.10f, 0.14f, 0.28f, 0.20f};
        }
        // 最上層：サンプルD「霧とランタンの神秘」
        return new float[]{0.05f, 0.08f, 0.22f, 0.30f};
    }

    /** プレイヤーを中心とした光の強さ（上層ほど強く）。 */
    private float lightStrength() {
        if (floorNumber <= 4) {
            return 1f;
        }
        if (floorNumber <= 9) {
            return 1.2f;
        }
        if (floorNumber <= 14) {
            return 1.6f;
        }
        return 2f;
    }

    /** 移動アニメーション：描画位置を目標タイルへ滑らかに寄せる（約0.12秒で1マス）。 */
    private void updateAnimation(float delta) {
        float speed = 1f / 0.12f;
        float step = speed * delta;
        playerRx = approach(playerRx, playerX, step);
        playerRy = approach(playerRy, playerY, step);
        for (Enemy e : enemies) {
            e.rx = approach(e.rx, e.x, step);
            e.ry = approach(e.ry, e.y, step);
            if (e.lungeTimer > 0) {
                e.lungeTimer -= delta;
            }
        }
        if (attackLungeTimer > 0) {
            attackLungeTimer -= delta;
        }
        if (slashTimer > 0) {
            slashTimer -= delta;
        }
    }

    /** 値を目標値へ step だけ近づける（行き過ぎたら目標値にする）。 */
    private float approach(float current, float target, float step) {
        if (Math.abs(target - current) <= step) {
            return target;
        }
        return current + Math.signum(target - current) * step;
    }

    /** 歩行時の上下動：移動の補間中だけ小さく跳ねる。 */
    private float playerBob() {
        float dx = playerX - playerRx;
        float dy = playerY - playerRy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= 0.01f || moveDist <= 0f) {
            return 0f;
        }
        float progress = 1f - dist / moveDist;
        if (progress < 0f) {
            progress = 0f;
        }
        if (progress > 1f) {
            progress = 1f;
        }
        return (float) Math.sin(progress * Math.PI) * 3f;
    }

    /** 歩行フレーム（0〜3）：1歩で2フレーム進む。1歩目はフレーム0→1、
     * 2歩目はフレーム2→3。止まっているときは0。 */
    private int walkFrame() {
        float dx = playerX - playerRx;
        float dy = playerY - playerRy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= 0.01f || moveDist <= 0f) {
            return 0;
        }
        float progress = 1f - dist / moveDist;
        int base = (walkStep % 2 == 0) ? 0 : 2;
        return progress < 0.5f ? base : base + 1;
    }

    /** 上層の霧：フロア上に霧スプライトのパーティクルを漂わせる（5Fから）。 */
    private void updateFog(float delta) {
        if (floorNumber <= 4) {
            return;
        }
        float alpha = 0.08f + (floorNumber - 5) * 0.02f;
        if (alpha > 0.15f) {
            alpha = 0.15f;
        }
        fogTimer -= delta;
        if (fogTimer > 0) {
            return;
        }
        fogTimer = 0.2f;
        // 霧が残る量は薄く長持ちするパーティクルで調整する
        int count = 0;
        for (Particle p : particles) {
            if (p.tex != null) {
                count++;
            }
        }
        if (count < 8) {
            Particle p = new Particle();
            p.x = playerX + (float) (Math.random() * 24 - 12) + 0.5f;
            p.y = playerY + (float) (Math.random() * 24 - 12) + 0.5f;
            p.vx = (float) (Math.random() * 0.6 - 0.3);
            p.vy = (float) (Math.random() * 0.6 - 0.3);
            p.maxLife = p.life = 10f;
            p.size = 0f;
            p.r = 1f;
            p.g = 1f;
            p.b = 1f;
            p.alpha = alpha;
            p.tex = sprites.fog;
            p.texW = 240f;
            particles.add(p);
        }
    }

    /** HUD：階数とフロアタイプ名、HP・攻撃力・防御力・装備、ターン数、キー案内、店内放送、メッセージ。 */
    private void drawHud() {
        float topY = camera.position.y + VIEW_H / 2f;
        float leftX = camera.position.x - VIEW_W / 2f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(leftX, topY - 148, VIEW_W, 148);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        hudFont.setColor(Color.WHITE);
        String bossMark = isBossFloor ? "  " + Localization.t("hud.boss_floor") : "";
        hudFont.draw(batch, Localization.t("hud.game_name") + "  "
                        + Localization.t("hud.floor", floorNumber)
                        + "  -  " + floor.type.label()
                        + "  [" + Enemy.themeName(floorNumber) + "]" + bossMark,
                leftX + 12, topY - 22);
        hudFont.draw(batch, "HP " + playerHp + "/" + PLAYER_MAX_HP
                        + "   " + Localization.t("hud.atk") + " " + playerAttack()
                        + "   " + Localization.t("hud.def") + " " + playerDefense()
                        + "   " + Localization.t("hud.wpn") + " "
                        + (equippedWeapon != null ? equippedWeapon.kind.label() : "-")
                        + "   " + Localization.t("hud.arm") + " "
                        + (equippedArmor != null ? equippedArmor.kind.label() : "-"),
                leftX + 12, topY - 50);
        hudFont.draw(batch, Localization.t("hud.turn", turn) + "   " + Localization.t("hud.keys"),
                leftX + 12, topY - 78);
        if (!announcement.isEmpty()) {
            hudFont.setColor(Color.CYAN);
            hudFont.draw(batch, Localization.t("hud.pa") + announcement, leftX + 12, topY - 106);
        }
        if (!message.isEmpty()) {
            hudFont.setColor(Color.YELLOW);
            hudFont.draw(batch, message, leftX + 12, topY - 134);
        }
        batch.end();
    }

    /** 持ち物一覧：オーバーレイ表示。数字キーで選んで使う／装備する。 */
    private void drawInventory() {
        float cx = camera.position.x;
        float cy = camera.position.y;
        float panelW = 700;
        float lineH = 30;
        // 日本語ではキー案内が2行になるため、その分だけ行数を増やす。
        String hint1 = Localization.t("inv.hint1");
        String hint2 = Localization.t("inv.hint2");
        int rows = inventory.size() + 4 + (hint2.isEmpty() ? 0 : 1);
        float panelH = 30 + rows * lineH + 16;
        float x0 = cx - panelW / 2f;
        float y0 = cy + panelH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.85f);
        shapes.rect(x0, y0 - panelH, panelW, panelH);
        // 選んでいる項目のハイライト（項目行の背景を薄い黄色にする）。
        if (!inventory.isEmpty()) {
            float selBaseline = y0 - 26 - (3 + (hint2.isEmpty() ? 0 : 1) + selectedItem) * lineH;
            shapes.setColor(1f, 0.85f, 0.2f, 0.25f);
            shapes.rect(x0 + 8, selBaseline - 21, panelW - 16, lineH);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float ty = y0 - 26;
        hudFont.setColor(Color.YELLOW);
        hudFont.draw(batch, Localization.t("inv.title", inventory.size(), MAX_INVENTORY),
                x0 + 16, ty);
        ty -= lineH;
        hudFont.setColor(Color.WHITE);
        String wpn = equippedWeapon != null
                ? equippedWeapon.kind.label() + " ("
                + Localization.t("inv.atk", equippedWeapon.kind.attackBonus) + ")"
                : "-";
        String arm = equippedArmor != null
                ? equippedArmor.kind.label() + " ("
                + Localization.t("inv.def", equippedArmor.kind.defenseBonus) + ")"
                : "-";
        hudFont.draw(batch, Localization.t("hud.wpn") + ": " + wpn
                        + "    " + Localization.t("hud.arm") + ": " + arm,
                x0 + 16, ty);
        ty -= lineH;
        hudFont.setColor(Color.LIGHT_GRAY);
        hudFont.draw(batch, hint1, x0 + 16, ty);
        if (!hint2.isEmpty()) {
            ty -= lineH;
            hudFont.draw(batch, hint2, x0 + 16, ty);
        }
        for (int i = 0; i < inventory.size(); i++) {
            ty -= lineH;
            Item it = inventory.get(i);
            String desc;
            if (it.kind.type == Item.Type.CONSUMABLE) {
                desc = Localization.t("inv.heal", it.kind.heal);
            } else if (it.kind.type == Item.Type.WEAPON) {
                desc = Localization.t("inv.atk", it.kind.attackBonus);
            } else {
                desc = Localization.t("inv.def", it.kind.defenseBonus);
            }
            if (i == selectedItem) {
                hudFont.setColor(Color.YELLOW);
                hudFont.draw(batch, "> " + (i + 1) % 10 + ". " + it.kind.symbol + " "
                                + it.kind.label() + "  -  " + desc,
                        x0 + 16, ty);
            } else {
                hudFont.setColor(Color.WHITE);
                hudFont.draw(batch, "  " + (i + 1) % 10 + ". " + it.kind.symbol + " "
                                + it.kind.label() + "  -  " + desc,
                        x0 + 16, ty);
            }
        }
        if (inventory.isEmpty()) {
            ty -= lineH;
            hudFont.setColor(Color.LIGHT_GRAY);
            hudFont.draw(batch, Localization.t("inv.empty"), x0 + 16, ty);
        }
        batch.end();
    }

    /** ゲームオーバー画面。Enterキーで最初からリスタート。 */
    private void drawGameOver() {
        float cx = camera.position.x;
        float cy = camera.position.y;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.7f);
        shapes.rect(cx - VIEW_W / 2f, cy - VIEW_H / 2f, VIEW_W, VIEW_H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.RED);
        drawCentered(font, Localization.t("over.title"), cx, cy + 40);
        hudFont.setColor(Color.WHITE);
        drawCentered(hudFont, Localization.t("over.reached", floorNumber, turn), cx, cy - 10);
        drawCentered(hudFont, Localization.t("over.retry"), cx, cy - 50);
        batch.end();
    }

    /** ミニマップ：オーバーレイ表示。通った道だけ表示する。 */
    private void drawMinimap() {
        float cell = 4f;
        float mapW = floor.w * cell;
        float mapH = floor.h * cell;
        float topY = camera.position.y + VIEW_H / 2f;
        float ox = camera.position.x + VIEW_W / 2f - mapW - 12;
        float oy = topY - 148 - mapH - 12;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(ox - 4, oy - 4, mapW + 8, mapH + 8);
        for (int x = 0; x < floor.w; x++) {
            for (int y = 0; y < floor.h; y++) {
                if (!floor.visited[x][y]) {
                    continue;
                }
                Floor.Tile t = floor.tiles[x][y];
                if (t == Floor.Tile.ESCALATOR) {
                    shapes.setColor(0.2f, 0.9f, 0.35f, 1f);
                } else if (t == Floor.Tile.STAIRS) {
                    shapes.setColor(0.2f, 0.45f, 1f, 1f);
                } else if (t == Floor.Tile.WALL) {
                    shapes.setColor(0.45f, 0.42f, 0.55f, 1f);
                } else if (t.isWalkable()) {
                    shapes.setColor(0.85f, 0.85f, 0.85f, 1f);
                } else {
                    shapes.setColor(0.5f, 0.45f, 0.35f, 1f);
                }
                shapes.rect(ox + x * cell, oy + y * cell, cell - 0.5f, cell - 0.5f);
            }
        }
        shapes.setColor(1f, 0.8f, 0.2f, 1f);
        shapes.rect(ox + playerX * cell - 1, oy + playerY * cell - 1, cell + 2, cell + 2);
        for (Enemy e : enemies) {
            if (!floor.visited[e.x][e.y]) {
                continue;
            }
            if (e.isBoss) {
                shapes.setColor(1f, 0.85f, 0.2f, 1f);
            } else {
                shapes.setColor(1f, 0.25f, 0.25f, 1f);
            }
            shapes.rect(ox + e.x * cell - 1, oy + e.y * cell - 1, cell + 1, cell + 1);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        music.dispose();
        if (sprites != null) {
            sprites.dispose();
        }
        shapes.dispose();
        batch.dispose();
        Fonts.dispose();
    }
}
