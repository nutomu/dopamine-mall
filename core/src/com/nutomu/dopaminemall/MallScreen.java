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
 * 最小限のモール画面。
 * 12x8 のタイルグリッドを表示し、プレイヤー「@」を1マスずつ移動できる。
 * 矢印キー / WASD、ゲームパッドや Fire TV リモコンの十字キー
 * （Android では十字キーが矢印キーとして届く）、タップ移動に対応。
 */
public class MallScreen implements Screen, InputProcessor {
    static final int COLS = 12;
    static final int ROWS = 8;
    static final int TILE = 64;
    static final int WORLD_W = COLS * TILE; // 768
    static final int WORLD_H = ROWS * TILE; // 512
    static final int HUD_H = 64;

    private final DopamineMallGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont hudFont;

    private int playerX = 1;
    private int playerY = 1;
    private int floor = 1;

    /** true = 壁（店舗の壁・柱） */
    private final boolean[][] walls = new boolean[COLS][ROWS];

    public MallScreen(DopamineMallGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_W, WORLD_H + HUD_H, camera);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2f);
        hudFont = new BitmapFont();
        hudFont.getData().setScale(1.5f);
        buildWalls();
        Gdx.input.setInputProcessor(this);
    }

    private void buildWalls() {
        for (int x = 0; x < COLS; x++) {
            walls[x][0] = true;
            walls[x][ROWS - 1] = true;
        }
        for (int y = 0; y < ROWS; y++) {
            walls[0][y] = true;
            walls[COLS - 1][y] = true;
        }
        // 店舗の柱（内壁）
        walls[4][3] = true;
        walls[4][4] = true;
        walls[7][2] = true;
        walls[8][5] = true;
    }

    private void tryMove(int dx, int dy) {
        int nx = playerX + dx;
        int ny = playerY + dy;
        if (nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS) return;
        if (walls[nx][ny]) return;
        playerX = nx;
        playerY = ny;
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
        if (dx != 0 && dy != 0) dy = 0;
        if (dx == 0 && dy == 0) return true;
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
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        camera.update();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (walls[x][y]) {
                    shapes.setColor(0.35f, 0.30f, 0.45f, 1f);
                } else if ((x + y) % 2 == 0) {
                    shapes.setColor(0.22f, 0.22f, 0.28f, 1f);
                } else {
                    shapes.setColor(0.18f, 0.18f, 0.24f, 1f);
                }
                shapes.rect(x * TILE, y * TILE, TILE - 1, TILE - 1);
            }
        }
        // プレイヤー（黄色い円＋「@」）
        shapes.setColor(1f, 0.8f, 0.2f, 1f);
        shapes.circle(playerX * TILE + TILE / 2f, playerY * TILE + TILE / 2f, TILE * 0.32f);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.BLACK);
        font.draw(batch, "@", playerX * TILE + TILE / 2f - 13, playerY * TILE + TILE / 2f + 15);
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "DOPAMINE MALL", 8, WORLD_H + 48);
        hudFont.draw(batch, "MALL FLOOR " + floor + "F", 8, WORLD_H + 20);
        batch.end();
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
