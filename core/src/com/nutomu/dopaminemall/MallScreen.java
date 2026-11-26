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

    /** 新しい階のフロアを生成してプレイヤーを配置する。 */
    private void loadFloor() {
        floor = FloorGenerator.generate(floorNumber);
        playerX = floor.startX;
        playerY = floor.startY;
        updateVisibility();
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

    private void tryMove(int dx, int dy) {
        int nx = playerX + dx;
        int ny = playerY + dy;
        if (!floor.isWalkable(nx, ny)) {
            return;
        }
        playerX = nx;
        playerY = ny;
        turn++;
        updateVisibility();
        Floor.Tile t = floor.tiles[nx][ny];
        if (t == Floor.Tile.ESCALATOR || t == Floor.Tile.STAIRS) {
            floorNumber++;
            loadFloor();
        }
    }

    private void waitTurn() {
        turn++;
    }

    @Override
    public boolean keyDown(int keycode) {
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
        // プレイヤー（黄色い円）
        shapes.setColor(1f, 0.8f, 0.2f, 1f);
        shapes.circle(playerX * TILE + TILE / 2f, playerY * TILE + TILE / 2f, TILE * 0.32f);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.BLACK);
        font.draw(batch, "@", playerX * TILE + TILE / 2f - 13, playerY * TILE + TILE / 2f + 15);
        batch.end();

        drawHud();
        if (showMinimap) {
            drawMinimap();
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

    /** HUD：階数とフロアタイプ名、ターン数、キー案内。 */
    private void drawHud() {
        float topY = camera.position.y + VIEW_H / 2f;
        float leftX = camera.position.x - VIEW_W / 2f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(leftX, topY - 64, VIEW_W, 64);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "DOPAMINE MALL  " + floorNumber + "F  -  " + floor.type.displayName,
                leftX + 12, topY - 22);
        hudFont.draw(batch, "TURN " + turn + "   [M] MAP   [SPACE] WAIT",
                leftX + 12, topY - 50);
        batch.end();
    }

    /** ミニマップ：オーバーレイ表示。通った道だけ表示する。 */
    private void drawMinimap() {
        float cell = 4f;
        float mapW = floor.w * cell;
        float mapH = floor.h * cell;
        float topY = camera.position.y + VIEW_H / 2f;
        float ox = camera.position.x + VIEW_W / 2f - mapW - 12;
        float oy = topY - 64 - mapH - 12;

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
