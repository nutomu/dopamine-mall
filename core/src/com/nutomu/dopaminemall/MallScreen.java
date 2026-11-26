package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
 * ※ グラフィックスは色分けしたタイルのシンプルな表示（凝った表現は M6 で行う）。
 * ※ HUD の文字はデフォルトフォントが ASCII のみのため英字表記。
 * M6：音楽・演出・グラフィックスのテスト実装。
 * - BGM：生成チップチューンの WAV ループを3曲（タイトル／ダンジョン／ボス）。
 *   タイトル画面ではタイトル曲、ダンジョンでは環境音楽、ボスフロアと
 *   モンスターハウスでは緊迫感のある曲に切り替わる。Nキーでミュート切替。
 * - タイトル画面：ゲーム開始前に「DOPAMINE MALL」を表示し、Enterキーで開始。
 * - 店内放送：HUD にフロアタイプごとの放送文言が20ターンごとに流れる。
 *   階を上がったときは「Welcome to ○F!」の特別な放送。
 * - 演出：フロアアップ時のきらめき、攻撃・被弾時のフラッシュ、敵撃破時の
 *   パーティクル、プレイヤーを中心とした柔らかい光。視界ルールは変えない。
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

    private Floor floor;
    private int playerX;
    private int playerY;
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
    }
    private List<Particle> particles = new ArrayList<Particle>();
    /** 攻撃・被弾時の画面フラッシュ（0 になると消える）。 */
    private float flashTimer = 0f;
    private float flashR, flashG, flashB;
    /** 攻撃したタイルのフラッシュ。 */
    private float attackFlashTimer = 0f;
    private int attackFlashX, attackFlashY;

    public MallScreen(DopamineMallGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);
        hudFont = new BitmapFont();
        hudFont.getData().setScale(1.5f);
        loadFloor();
        music.load();
        music.playTitle();
        Gdx.input.setInputProcessor(this);
    }

    /** 新しい階のフロアを生成してプレイヤーと敵・アイテムを配置する。 */
    private void loadFloor() {
        floor = FloorGenerator.generate(floorNumber);
        playerX = floor.startX;
        playerY = floor.startY;
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

    /** カメラをプレイヤーに追従させる（フロアの端でははみ出さない）。 */
    private void followCamera() {
        float worldW = floor.w * TILE;
        float worldH = floor.h * TILE;
        float cx = playerX * TILE + TILE / 2f;
        float cy = playerY * TILE + TILE / 2f;
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
                message = "Attacked " + target.kind.name() + " for " + damage + " dmg!";
            } else {
                spawnDeathParticles(target);
                enemies.remove(target);
                if (target.isBoss) {
                    // ボスを倒すと良いアイテムをその場に落とす。
                    Item.Kind[] loot = {Item.Kind.FRYING_PAN, Item.Kind.JACKET, Item.Kind.SWEETS};
                    Item.Kind drop = loot[(int) (Math.random() * loot.length)];
                    items.add(new Item(drop, target.x, target.y));
                    message = "Defeated BOSS " + target.kind.name() + "! It dropped "
                            + drop.displayName + "!";
                    updateMusic();
                } else {
                    message = "Defeated " + target.kind.name() + "!";
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
            message = "Monster house! Enemies everywhere!";
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
            message = "Inventory is full! Cannot pick up " + it.kind.displayName + ".";
            return;
        }
        items.remove(it);
        inventory.add(it);
        message = "Picked up " + it.kind.displayName + "!";
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
            message = "Used " + it.kind.displayName + "! Recovered "
                    + (playerHp - before) + " HP!";
            turn++;
            enemyTurn();
            return;
        }
        // 武器・防具の装備（交換）
        boolean isWeapon = it.kind.type == Item.Type.WEAPON;
        Item current = isWeapon ? equippedWeapon : equippedArmor;
        if (current != null && inventory.size() >= MAX_INVENTORY) {
            message = "Inventory is full! Cannot swap equipment.";
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
        message = "Equipped " + it.kind.displayName + "!";
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
                message = e.kind.name() + " hit you for " + damage + " dmg!";
                if (playerHp <= 0) {
                    playerHp = 0;
                    gameOver = true;
                    message = "GAME OVER...";
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
        loadFloor();
    }

    @Override
    public boolean keyDown(int keycode) {
        // タイトル画面：Enterキーなどでゲーム開始する。
        if (titleScreen) {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.CENTER
                    || keycode == Input.Keys.BUTTON_A || keycode == Input.Keys.SPACE) {
                startGame();
            }
            return true;
        }
        if (gameOver) {
            if (keycode == Input.Keys.ENTER) {
                restart();
                return true;
            }
            return true;
        }
        // 持ち物一覧が開いているときは、Iキーで閉じるか数字キーで選ぶだけ。
        // それ以外のキー（移動など）は受け付けない。
        if (showInventory) {
            if (keycode == Input.Keys.I) {
                showInventory = false;
                return true;
            }
            int index = numberKeyIndex(keycode);
            if (index >= 0) {
                useItem(index);
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
                message = music.isMuted() ? "Music: OFF" : "Music: ON";
                return true;
            case Input.Keys.I:
                showInventory = true;
                return true;
            default:
                return false;
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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
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
            shapes.setColor(it.kind.r, it.kind.g, it.kind.b, 1f);
            shapes.circle(it.x * TILE + TILE / 2f, it.y * TILE + TILE / 2f, TILE * 0.22f);
        }
        // 敵（行った場所にいる敵だけ表示する。ボスは金色で大きく表示する）
        for (Enemy e : enemies) {
            if (!floor.visited[e.x][e.y]) {
                continue;
            }
            if (e.isBoss) {
                shapes.setColor(1f, 0.85f, 0.2f, 1f);
                shapes.circle(e.x * TILE + TILE / 2f, e.y * TILE + TILE / 2f, TILE * 0.38f);
            } else {
                shapes.setColor(e.kind.r, e.kind.g, e.kind.b, 1f);
                shapes.circle(e.x * TILE + TILE / 2f, e.y * TILE + TILE / 2f, TILE * 0.30f);
            }
        }
        // プレイヤー（黄色い円）
        shapes.setColor(1f, 0.8f, 0.2f, 1f);
        shapes.circle(playerX * TILE + TILE / 2f, playerY * TILE + TILE / 2f, TILE * 0.32f);
        // プレイヤーを中心とした柔らかい光（同心円の薄い光。視界ルールは変えない）
        float pcx = playerX * TILE + TILE / 2f;
        float pcy = playerY * TILE + TILE / 2f;
        shapes.setColor(1f, 0.9f, 0.5f, 0.10f);
        shapes.circle(pcx, pcy, TILE * 1.3f);
        shapes.setColor(1f, 0.9f, 0.5f, 0.05f);
        shapes.circle(pcx, pcy, TILE * 2.6f);
        shapes.setColor(1f, 0.9f, 0.5f, 0.025f);
        shapes.circle(pcx, pcy, TILE * 4.0f);
        // 攻撃したタイルのフラッシュ
        if (attackFlashTimer > 0) {
            shapes.setColor(1f, 1f, 0.6f, attackFlashTimer / 0.2f * 0.7f);
            shapes.circle(attackFlashX * TILE + TILE / 2f,
                    attackFlashY * TILE + TILE / 2f, TILE * 0.38f);
        }
        // パーティクル（きらめき・撃破時の破片）
        for (Particle p : particles) {
            float alpha = Math.max(0f, p.life / p.maxLife);
            shapes.setColor(p.r, p.g, p.b, alpha);
            shapes.circle(p.x, p.y, p.size);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Item it : items) {
            if (!floor.visited[it.x][it.y]) {
                continue;
            }
            font.setColor(Color.BLACK);
            font.draw(batch, String.valueOf(it.kind.symbol),
                    it.x * TILE + TILE / 2f - 11, it.y * TILE + TILE / 2f + 14);
        }
        for (Enemy e : enemies) {
            if (!floor.visited[e.x][e.y]) {
                continue;
            }
            font.setColor(Color.BLACK);
            font.draw(batch, String.valueOf(e.kind.symbol),
                    e.x * TILE + TILE / 2f - 11, e.y * TILE + TILE / 2f + 14);
        }
        font.setColor(Color.BLACK);
        font.draw(batch, "@", playerX * TILE + TILE / 2f - 13, playerY * TILE + TILE / 2f + 15);
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

    /** タイトル画面。Enterキー／タップでゲーム開始する。 */
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
        font.draw(batch, "DOPAMINE MALL", cx - 140, cy + 20);
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "An endless shopping mall dungeon roguelike",
                cx - 260, cy - 20);
        hudFont.draw(batch, "PRESS ENTER TO START", cx - 145, cy - 70);
        hudFont.setColor(Color.LIGHT_GRAY);
        hudFont.draw(batch, "Move: Arrow keys / WASD   [M] MAP   [I] ITEMS   [N] MUSIC",
                cx - 300, cy - 110);
        hudFont.draw(batch, "Gamepad / Fire TV remote: D-pad to move",
                cx - 260, cy - 140);
        batch.end();
    }

    private void drawTile(int x, int y) {
        float px = x * TILE;
        float py = y * TILE;
        if (!floor.visited[x][y]) {
            // 未踏の場所は暗くする（壁や床も含めて見えない）
            shapes.setColor(0.03f, 0.03f, 0.05f, 1f);
            shapes.rect(px, py, TILE - 1, TILE - 1);
            return;
        }
        Floor.Tile t = floor.tiles[x][y];
        switch (t) {
            case WALL:
                shapes.setColor(0.35f, 0.30f, 0.45f, 1f);
                break;
            case SHELF:
                shapes.setColor(0.45f, 0.32f, 0.18f, 1f);
                break;
            case MACHINE:
                shapes.setColor(0.65f, 0.25f, 0.60f, 1f);
                break;
            case TABLE:
                shapes.setColor(0.75f, 0.50f, 0.20f, 1f);
                break;
            case COUNTER:
                shapes.setColor(0.20f, 0.60f, 0.55f, 1f);
                break;
            case STAGE:
                shapes.setColor(0.80f, 0.65f, 0.20f, 1f);
                break;
            case SEAT:
                shapes.setColor(0.35f, 0.40f, 0.55f, 1f);
                break;
            case ESCALATOR:
                shapes.setColor(0.20f, 0.85f, 0.35f, 1f);
                break;
            case STAIRS:
                shapes.setColor(0.20f, 0.45f, 0.90f, 1f);
                break;
            case FLOOR:
            default:
                float[] c = floorColor();
                if ((x + y) % 2 == 0) {
                    shapes.setColor(c[0], c[1], c[2], 1f);
                } else {
                    shapes.setColor(c[0] * 0.85f, c[1] * 0.85f, c[2] * 0.85f, 1f);
                }
                break;
        }
        shapes.rect(px, py, TILE - 1, TILE - 1);
    }

    /** フロアタイプごとの床の基本色。 */
    private float[] floorColor() {
        switch (floor.type) {
            case SUPERMARKET:
                return new float[]{0.20f, 0.26f, 0.20f};
            case GAME_CENTER:
                return new float[]{0.20f, 0.16f, 0.26f};
            case FOOD_COURT:
                return new float[]{0.28f, 0.22f, 0.16f};
            case EVENT:
                return new float[]{0.16f, 0.18f, 0.28f};
            case TENANT:
            default:
                return new float[]{0.24f, 0.22f, 0.20f};
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
        String bossMark = isBossFloor ? "  !! BOSS FLOOR !!" : "";
        hudFont.draw(batch, "DOPAMINE MALL  " + floorNumber + "F  -  " + floor.type.displayName
                        + "  [" + Enemy.themeName(floorNumber) + "]" + bossMark,
                leftX + 12, topY - 22);
        hudFont.draw(batch, "HP " + playerHp + "/" + PLAYER_MAX_HP
                        + "   ATK " + playerAttack() + "   DEF " + playerDefense()
                        + "   WPN " + (equippedWeapon != null ? equippedWeapon.kind.displayName : "-")
                        + "   ARM " + (equippedArmor != null ? equippedArmor.kind.displayName : "-"),
                leftX + 12, topY - 50);
        hudFont.draw(batch, "TURN " + turn + "   [M] MAP   [I] ITEMS   [N] MUSIC   [SPACE] WAIT",
                leftX + 12, topY - 78);
        if (!announcement.isEmpty()) {
            hudFont.setColor(Color.CYAN);
            hudFont.draw(batch, "PA: " + announcement, leftX + 12, topY - 106);
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
        int rows = inventory.size() + 4;
        float panelH = 30 + rows * lineH + 16;
        float x0 = cx - panelW / 2f;
        float y0 = cy + panelH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.85f);
        shapes.rect(x0, y0 - panelH, panelW, panelH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float ty = y0 - 26;
        hudFont.setColor(Color.YELLOW);
        hudFont.draw(batch, "ITEMS  (" + inventory.size() + "/" + MAX_INVENTORY + ")",
                x0 + 16, ty);
        ty -= lineH;
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "WPN: " + (equippedWeapon != null
                        ? equippedWeapon.kind.displayName + " (ATK+"
                        + equippedWeapon.kind.attackBonus + ")" : "-")
                        + "    ARM: " + (equippedArmor != null
                        ? equippedArmor.kind.displayName + " (DEF+"
                        + equippedArmor.kind.defenseBonus + ")" : "-"),
                x0 + 16, ty);
        ty -= lineH;
        hudFont.setColor(Color.LIGHT_GRAY);
        hudFont.draw(batch, "Press 1-0 to use / equip.  Press [I] to close.",
                x0 + 16, ty);
        for (int i = 0; i < inventory.size(); i++) {
            ty -= lineH;
            Item it = inventory.get(i);
            String desc;
            if (it.kind.type == Item.Type.CONSUMABLE) {
                desc = "Heal +" + it.kind.heal;
            } else if (it.kind.type == Item.Type.WEAPON) {
                desc = "ATK +" + it.kind.attackBonus;
            } else {
                desc = "DEF +" + it.kind.defenseBonus;
            }
            hudFont.setColor(Color.WHITE);
            hudFont.draw(batch, (i + 1) % 10 + ". " + it.kind.symbol + " "
                            + it.kind.displayName + "  -  " + desc,
                    x0 + 16, ty);
        }
        if (inventory.isEmpty()) {
            ty -= lineH;
            hudFont.setColor(Color.LIGHT_GRAY);
            hudFont.draw(batch, "(empty)", x0 + 16, ty);
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
        font.draw(batch, "GAME OVER", cx - 130, cy + 40);
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "You reached " + floorNumber + "F  /  TURN " + turn,
                cx - 170, cy - 10);
        hudFont.draw(batch, "PRESS ENTER TO RETRY", cx - 170, cy - 50);
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
        shapes.dispose();
        batch.dispose();
        font.dispose();
        hudFont.dispose();
    }
}
