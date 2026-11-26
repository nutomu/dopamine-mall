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
 * ※ グラフィックスはドット絵スプライト＋最先端の2D演出（サンプルD「霧とランタンの神秘」）。
 * - tools/generate_tiles.py で生成した 32x32 のドット絵タイルをスプライト描画する。
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
 */
public class MallScreen implements Screen, InputProcessor {
    static final int TILE = 48;
    static final int VIEW_W = 1280;
    static final int VIEW_H = 800;
    /** 視界の広さ（プレイヤーから歩いて何マス先まで「行った場所」にするか）。 */
    static final int SIGHT = 8;

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
    /** プレイヤーの描画用補間位置（ピクセル単位）。移動時に目標タイルへ滑らかに寄る。 */
    private float playerRx;
    private float playerRy;
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
    /** 店内放送：20ターンごとにフロアタイプの文言を切り替える。 */
    private static final int ANNOUNCE_EVERY_TURNS = 20;
    private String announcement = "";
    private int announcementIndex = 0;
    private int announcementTurn = 0;
    /** モンスターハウスに入ったあとボス曲を流しているかどうか。 */
    private boolean monsterHouseMusic = false;
    /** 演出用パーティクル（きらめき・撃破時の破片）。 */
    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        float r, g, b;
        /** 描画時の不透明度の上限（霧は薄くする）。 */
        float alpha = 1f;
    }
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
        playerRx = playerX * TILE;
        playerRy = playerY * TILE;
        enemies = FloorGenerator.spawnEnemies(floor, floorNumber);
        items = FloorGenerator.spawnItems(floor, floorNumber, enemies);
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
        float worldW = floor.w * TILE;
        float worldH = floor.h * TILE;
        float cx = playerRx + TILE / 2f;
        float cy = playerRy + TILE / 2f;
        if (worldW <= VIEW_W) {
            cx = worldW / 2f;
        } else {
            cx = Math.max(VIEW_W / 2f, Math.min(worldW - VIEW_W / 2f, cx));
        }
        if (worldH <= VIEW_H) {
            cy = worldH / 2f;
        } else {
            cy = Math.max(VIEW_H / 2f, Math.min(worldH - VIEW_H / 2f, cy));
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
        Enemy target = enemyAt(nx, ny);
        if (target != null) {
            int damage = target.takeDamage(playerAttack());
            flash(1f, 1f, 0.6f);
            attackFlashTimer = 0.2f;
            attackFlashX = nx;
            attackFlashY = ny;
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
        playerX = nx;
        playerY = ny;
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
     * - プレイヤーに隣接していたら攻撃する。
     * - プレイヤーが5マス以内にいるとプレイヤーに向かって1マス近づく。
     * - それ以外はランダムな方向に1マスうろつく（壁や他の敵の方向には動かない）。
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
            int dist = Math.abs(e.x - playerX) + Math.abs(e.y - playerY);
            if (dist == 1) {
                int damage = Math.max(1, e.attack - playerDefense());
                playerHp -= damage;
                flash(1f, 0.2f, 0.2f);
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
                // プレイヤーに近づく：差の大きい軸を優先する
                int gx = Integer.compare(playerX, e.x);
                int gy = Integer.compare(playerY, e.y);
                if (Math.abs(playerX - e.x) >= Math.abs(playerY - e.y)) {
                    dx = gx;
                    dy = 0;
                } else {
                    dx = 0;
                    dy = gy;
                }
                if (!canEnemyMoveTo(e, e.x + dx, e.y + dy)) {
                    // 優先方向に進めなければもう一方の軸を試す
                    dx = 0;
                    dy = 0;
                    if (gx != 0 && canEnemyMoveTo(e, e.x + gx, e.y)) {
                        dx = gx;
                    } else if (gy != 0 && canEnemyMoveTo(e, e.x, e.y + gy)) {
                        dy = gy;
                    }
                }
            } else {
                // うろつく：ランダムな4方向のうち進める方向を1つ選ぶ
                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                int start = (int) (Math.random() * dirs.length);
                for (int i = 0; i < dirs.length; i++) {
                    int[] d = dirs[(start + i) % dirs.length];
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
            p.x = (playerX + (float) (Math.random() * 12 - 6)) * TILE + TILE / 2f;
            p.y = (playerY + (float) (Math.random() * 12 - 6)) * TILE + TILE / 2f;
            p.vx = (float) (Math.random() * 10 - 5);
            p.vy = (float) (Math.random() * 20 + 5);
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
            p.x = e.x * TILE + TILE / 2f;
            p.y = e.y * TILE + TILE / 2f;
            double angle = Math.random() * Math.PI * 2;
            double speed = 40 + Math.random() * 90;
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
            p.vy -= 20 * delta;
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
        // タイトル画面：Enterキーなどでゲーム開始する。Lキーで言語を切り替える。
        if (titleScreen) {
            if (keycode == Input.Keys.L) {
                Localization.toggleLanguage();
                Fonts.reload();
                font = Fonts.font;
                hudFont = Fonts.hudFont;
                return true;
            }
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.CENTER
                    || keycode == Input.Keys.BUTTON_A || keycode == Input.Keys.SPACE
                    || keycode == Input.Keys.BUTTON_START) {
                startGame();
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
            case Input.Keys.UP:
            case Input.Keys.W:
                tryMove(0, 1);
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                tryMove(0, -1);
                return true;
            case Input.Keys.LEFT:
            case Input.Keys.A:
                tryMove(-1, 0);
                return true;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                tryMove(1, 0);
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
        // タイトル画面：タップでゲーム開始する。
        if (titleScreen) {
            startGame();
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
        int tx = (int) (v.x / TILE);
        int ty = (int) (v.y / TILE);
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

        // タイルと登場人物をドット絵スプライトで描く
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        int x0 = Math.max(0, (int) ((camera.position.x - VIEW_W / 2f) / TILE) - 1);
        int x1 = Math.min(floor.w - 1, (int) ((camera.position.x + VIEW_W / 2f) / TILE) + 1);
        int y0 = Math.max(0, (int) ((camera.position.y - VIEW_H / 2f) / TILE) - 1);
        int y1 = Math.min(floor.h - 1, (int) ((camera.position.y + VIEW_H / 2f) / TILE) + 1);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                drawTile(x, y);
            }
        }
        // アイテム（行った場所にあるものだけ表示する）
        for (Item it : items) {
            if (!floor.visited[it.x][it.y]) {
                continue;
            }
            batch.draw(sprites.itemTexture(it.kind),
                    it.x * TILE + TILE * 0.2f, it.y * TILE + TILE * 0.2f,
                    TILE * 0.6f, TILE * 0.6f);
        }
        // 敵（行った場所にいる敵だけ表示する。ボスは大きく表示する）
        for (Enemy e : enemies) {
            if (!floor.visited[e.x][e.y]) {
                continue;
            }
            float size = e.isBoss ? TILE * 1.4f : TILE * 0.85f;
            float ox = e.rx + TILE / 2f - size / 2f;
            float oy = e.ry + TILE / 2f - size / 2f;
            batch.draw(sprites.enemyTexture(e.kind), ox, oy, size, size);
        }
        // プレイヤー（歩行時の上下動つき）
        float bob = playerBob();
        batch.draw(sprites.player, playerRx, playerRy + bob, TILE, TILE);
        batch.end();

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
        float pcx = playerRx + TILE / 2f;
        float pcy = playerRy + TILE / 2f + bob;
        shapes.setColor(1f, 0.85f, 0.45f, 0.10f * ls);
        shapes.circle(pcx, pcy, TILE * 1.3f);
        shapes.setColor(1f, 0.85f, 0.45f, 0.05f * ls);
        shapes.circle(pcx, pcy, TILE * 2.6f);
        shapes.setColor(1f, 0.85f, 0.45f, 0.025f * ls);
        shapes.circle(pcx, pcy, TILE * 4.0f);
        // 攻撃したタイルのフラッシュ
        if (attackFlashTimer > 0) {
            shapes.setColor(1f, 1f, 0.6f, attackFlashTimer / 0.2f * 0.7f);
            shapes.circle(attackFlashX * TILE + TILE / 2f,
                    attackFlashY * TILE + TILE / 2f, TILE * 0.38f);
        }
        // パーティクル（きらめき・撃破時の破片・霧）
        for (Particle p : particles) {
            float alpha = Math.max(0f, p.life / p.maxLife) * p.alpha;
            shapes.setColor(p.r, p.g, p.b, alpha);
            shapes.circle(p.x, p.y, p.size);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

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
        drawCentered(hudFont, Localization.t("title.press_start"), cx, cy - 70);
        hudFont.setColor(Color.LIGHT_GRAY);
        drawCentered(hudFont, Localization.t("title.keys1"), cx, cy - 110);
        drawCentered(hudFont, Localization.t("title.keys2"), cx, cy - 140);
        hudFont.setColor(Color.CYAN);
        drawCentered(hudFont, Localization.t("title.language"), cx, cy - 180);
        batch.end();
    }

    /** テキストを横方向の中央揃えで描く（タイトル画面・ゲームオーバー用）。 */
    private void drawCentered(BitmapFont f, String text, float cx, float y) {
        layout.setText(f, text);
        f.draw(batch, text, cx - layout.width / 2f, y);
    }

    /** タイルをドット絵スプライトで描く（batch.begin() の中で呼ぶこと）。 */
    private void drawTile(int x, int y) {
        float px = x * TILE;
        float py = y * TILE;
        if (!floor.visited[x][y]) {
            // 未踏の場所は暗くする（スプライトをほぼ黒く着色して描く）
            batch.setColor(0.05f, 0.05f, 0.08f, 1f);
            batch.draw(sprites.terrainTexture(floor.tiles[x][y]), px, py, TILE, TILE);
            batch.setColor(1f, 1f, 1f, 1f);
            return;
        }
        Floor.Tile t = floor.tiles[x][y];
        if (t == Floor.Tile.FLOOR) {
            // フロアタイプごとにわずかに色調を変える
            float[] c = floorTint();
            batch.setColor(c[0], c[1], c[2], 1f);
            batch.draw(sprites.floorTile(x, y), px, py, TILE, TILE);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            batch.draw(sprites.terrainTexture(t), px, py, TILE, TILE);
        }
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
        float speed = TILE / 0.12f;
        float step = speed * delta;
        playerRx = approach(playerRx, playerX * TILE, step);
        playerRy = approach(playerRy, playerY * TILE, step);
        for (Enemy e : enemies) {
            e.rx = approach(e.rx, e.x * TILE, step);
            e.ry = approach(e.ry, e.y * TILE, step);
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
        float dx = playerX * TILE - playerRx;
        float dy = playerY * TILE - playerRy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= 0.01f) {
            return 0f;
        }
        float progress = 1f - dist / TILE;
        return (float) Math.sin(progress * Math.PI) * 3f;
    }

    /** 上層の霧：カメラの視野内にゆっくり漂う大きな薄い霧を発生させる。 */
    private void updateFog(float delta) {
        if (floorNumber < 10) {
            return;
        }
        fogTimer -= delta;
        if (fogTimer > 0) {
            return;
        }
        fogTimer = 0.7f + (float) Math.random() * 0.8f;
        Particle p = new Particle();
        float vx = camera.position.x - VIEW_W / 2f;
        float vy = camera.position.y - VIEW_H / 2f;
        p.x = vx + (float) (Math.random() * VIEW_W);
        p.y = vy + (float) (Math.random() * VIEW_H);
        p.vx = 6 + (float) (Math.random() * 10);
        p.vy = 2 + (float) (Math.random() * 4);
        p.maxLife = p.life = 5f + (float) Math.random() * 3f;
        p.size = 60f + (float) (Math.random() * 70f);
        p.r = 0.75f;
        p.g = 0.8f;
        p.b = 0.9f;
        p.alpha = 0.05f;
        particles.add(p);
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
