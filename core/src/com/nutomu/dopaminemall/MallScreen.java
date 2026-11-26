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
 * ※ グラフィックスは色分けしたタイルのシンプルな表示（凝った表現は M6 で行う）。
 * ※ グラフィックスは色分けしたタイルのシンプルな表示（凝った表現は M6 で行う）。
 * ※ HUD の文字はデフォルトフォントが ASCII のみのため英字表記。
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
        Gdx.input.setInputProcessor(this);
    }

    /** 新しい階のフロアを生成してプレイヤーと敵を配置する。 */
    private void loadFloor() {
        floor = FloorGenerator.generate(floorNumber);
        playerX = floor.startX;
        playerY = floor.startY;
        enemies = FloorGenerator.spawnEnemies(floor, floorNumber);
        updateVisibility();
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
            int damage = target.takeDamage(PLAYER_ATTACK);
            if (target.isAlive()) {
                message = "Attacked " + target.kind.name() + " for " + damage + " dmg!";
            } else {
                enemies.remove(target);
                message = "Defeated " + target.kind.name() + "!";
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
        enemyTurn();
        Floor.Tile t = floor.tiles[nx][ny];
        if (t == Floor.Tile.ESCALATOR || t == Floor.Tile.STAIRS) {
            floorNumber++;
            loadFloor();
        }
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
        List<Enemy> acting = new ArrayList<Enemy>(enemies);
        for (Enemy e : acting) {
            if (!enemies.contains(e)) {
                continue;
            }
            int dist = Math.abs(e.x - playerX) + Math.abs(e.y - playerY);
            if (dist == 1) {
                int damage = Math.max(1, e.attack - PLAYER_DEFENSE);
                playerHp -= damage;
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

    /** ゲームオーバーからのリスタート。最初からやり直す。 */
    private void restart() {
        floorNumber = 1;
        turn = 0;
        playerHp = PLAYER_MAX_HP;
        gameOver = false;
        message = "";
        loadFloor();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (gameOver) {
            if (keycode == Input.Keys.ENTER) {
                restart();
                return true;
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
            default:
                return false;
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (gameOver) {
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
        // 敵（行った場所にいる敵だけ表示する）
        for (Enemy e : enemies) {
            if (!floor.visited[e.x][e.y]) {
                continue;
            }
            shapes.setColor(e.kind.r, e.kind.g, e.kind.b, 1f);
            shapes.circle(e.x * TILE + TILE / 2f, e.y * TILE + TILE / 2f, TILE * 0.30f);
        }
        // プレイヤー（黄色い円）
        shapes.setColor(1f, 0.8f, 0.2f, 1f);
        shapes.circle(playerX * TILE + TILE / 2f, playerY * TILE + TILE / 2f, TILE * 0.32f);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
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
        if (gameOver) {
            drawGameOver();
        }
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

    /** HUD：階数とフロアタイプ名、HP・ターン数、キー案内、メッセージ。 */
    private void drawHud() {
        float topY = camera.position.y + VIEW_H / 2f;
        float leftX = camera.position.x - VIEW_W / 2f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(leftX, topY - 92, VIEW_W, 92);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "DOPAMINE MALL  " + floorNumber + "F  -  " + floor.type.displayName,
                leftX + 12, topY - 22);
        hudFont.draw(batch, "HP " + playerHp + "/" + PLAYER_MAX_HP + "   TURN " + turn
                        + "   [M] MAP   [SPACE] WAIT",
                leftX + 12, topY - 50);
        if (!message.isEmpty()) {
            hudFont.setColor(Color.YELLOW);
            hudFont.draw(batch, message, leftX + 12, topY - 78);
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
        float oy = topY - 92 - mapH - 12;

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
            shapes.setColor(1f, 0.25f, 0.25f, 1f);
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
        shapes.dispose();
        batch.dispose();
        font.dispose();
        hudFont.dispose();
    }
}
