package com.nutomu.dopaminemall;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * フロアタイプに応じたフロア生成。
 * 5種類のフロアタイプ（テナント／スーパー／ゲームセンター／フードコート／イベント）を、
 * それぞれのレイアウトのルールで生成する。
 */
public class FloorGenerator {

    /**
     * 階数からフロアタイプを選ぶ。階数で候補を絞ってランダム。
     * - イベントフロアはどの階でも稀（8%）に出現する。
     * - 1〜2階：テナント／スーパー中心。
     * - 3〜4階：テナント／スーパー／ゲームセンター／フードコート。
     * - 5階以上：4タイプ均等寄り。
     */
    public static FloorType selectType(int floorNumber, Random random) {
        if (random.nextDouble() < 0.08) {
            return FloorType.EVENT;
        }
        double r = random.nextDouble();
        if (floorNumber <= 2) {
            return r < 0.55 ? FloorType.TENANT : FloorType.SUPERMARKET;
        } else if (floorNumber <= 4) {
            if (r < 0.30) return FloorType.TENANT;
            if (r < 0.55) return FloorType.SUPERMARKET;
            if (r < 0.80) return FloorType.GAME_CENTER;
            return FloorType.FOOD_COURT;
        } else {
            if (r < 0.20) return FloorType.TENANT;
            if (r < 0.40) return FloorType.SUPERMARKET;
            if (r < 0.65) return FloorType.GAME_CENTER;
            return FloorType.FOOD_COURT;
        }
    }

    /** 指定した階数のフロアを生成する。到達可能性を検証し、ダメなら生成し直す。 */
    public static Floor generate(int floorNumber) {
        for (int attempt = 0; attempt < 50; attempt++) {
            Floor floor = generateOnce(floorNumber);
            if (isPlayable(floor)) {
                return floor;
            }
        }
        return generateOnce(floorNumber);
    }

    /**
     * フロアが遊べるかどうかの検証。スタート地点から両方の階段に到達でき、
     * 歩行可能タイルの95%以上に到達できること。
     * （棚の配置によってはスタート地点が囲まれて孤立することがあるため）
     */
    private static boolean isPlayable(Floor floor) {
        int[][] dist = floor.distancesFrom(floor.startX, floor.startY);
        if (dist[floor.escX][floor.escY] < 0) {
            return false;
        }
        if (dist[floor.stairX][floor.stairY] < 0) {
            return false;
        }
        int walkable = 0;
        int reachable = 0;
        for (int x = 0; x < floor.w; x++) {
            for (int y = 0; y < floor.h; y++) {
                if (floor.tiles[x][y].isWalkable()) {
                    walkable++;
                    if (dist[x][y] >= 0) {
                        reachable++;
                    }
                }
            }
        }
        return walkable > 0 && (double) reachable / walkable >= 0.95;
    }

    private static Floor generateOnce(int floorNumber) {
        Random random = new Random();
        FloorType type = selectType(floorNumber, random);
        Floor floor;
        switch (type) {
            case SUPERMARKET:
                floor = buildSupermarket(random);
                break;
            case GAME_CENTER:
                floor = buildGameCenter(random);
                break;
            case FOOD_COURT:
                floor = buildFoodCourt(random);
                break;
            case EVENT:
                floor = buildEvent(random);
                break;
            case TENANT:
            default:
                floor = buildTenant(random);
                break;
        }
        int[] start = floor.nearestWalkable(2, 2);
        floor.startX = start[0];
        floor.startY = start[1];
        placeStairs(floor, random);
        return floor;
    }

    private static void borderWalls(Floor f) {
        for (int x = 0; x < f.w; x++) {
            f.tiles[x][0] = Floor.Tile.WALL;
            f.tiles[x][f.h - 1] = Floor.Tile.WALL;
        }
        for (int y = 0; y < f.h; y++) {
            f.tiles[0][y] = Floor.Tile.WALL;
            f.tiles[f.w - 1][y] = Floor.Tile.WALL;
        }
    }

    // ---- テナントフロア：部屋＝テナント区画、通路＝モール通路 ----

    private static Floor buildTenant(Random random) {
        int w = 40;
        int h = 30;
        Floor f = new Floor(FloorType.TENANT, w, h);
        borderWalls(f);
        int roomW = 8;
        int roomH = 7;
        int gapX = 2;
        int gapY = 3;
        for (int rx = 1; rx + roomW <= w - 1; rx += roomW + gapX) {
            for (int ry = 1; ry + roomH <= h - 1; ry += roomH + gapY) {
                buildRoom(f, rx, ry, rx + roomW - 1, ry + roomH - 1, random);
            }
        }
        return f;
    }

    /** テナント区画を1部屋作る。壁で囲み、通路側に1〜2か所の入口を開け、中に棚を置く。 */
    private static void buildRoom(Floor f, int x0, int y0, int x1, int y1, Random random) {
        for (int x = x0; x <= x1; x++) {
            f.tiles[x][y0] = Floor.Tile.WALL;
            f.tiles[x][y1] = Floor.Tile.WALL;
        }
        for (int y = y0; y <= y1; y++) {
            f.tiles[x0][y] = Floor.Tile.WALL;
            f.tiles[x1][y] = Floor.Tile.WALL;
        }
        // ドア：ランダムな辺に1〜2か所。ただし外壁に面した辺には開けない
        // （外壁側にドアを開けると部屋が密閉されてしまうため）。
        List<Integer> edges = new ArrayList<Integer>();
        if (y0 > 1) {
            edges.add(0); // 下辺：下の通路に開く
        }
        if (y1 < f.h - 2) {
            edges.add(1); // 上辺：上の通路に開く
        }
        if (x0 > 1) {
            edges.add(2); // 左辺：左の通路に開く
        }
        if (x1 < f.w - 2) {
            edges.add(3); // 右辺：右の通路に開く
        }
        List<int[]> doors = new ArrayList<int[]>();
        int doorCount = 1 + random.nextInt(2);
        for (int i = 0; i < doorCount; i++) {
            int edge = edges.get(random.nextInt(edges.size()));
            int dx;
            int dy;
            if (edge == 0) {
                dx = x0 + 1 + random.nextInt(x1 - x0 - 1);
                dy = y0;
            } else if (edge == 1) {
                dx = x0 + 1 + random.nextInt(x1 - x0 - 1);
                dy = y1;
            } else if (edge == 2) {
                dx = x0;
                dy = y0 + 1 + random.nextInt(y1 - y0 - 1);
            } else {
                dx = x1;
                dy = y0 + 1 + random.nextInt(y1 - y0 - 1);
            }
            f.tiles[dx][dy] = Floor.Tile.FLOOR;
            doors.add(new int[]{dx, dy});
        }
        int shelfCount = 1 + random.nextInt(2);
        for (int i = 0; i < shelfCount; i++) {
            int sx = x0 + 1 + random.nextInt(x1 - x0 - 1);
            int sy = y0 + 1 + random.nextInt(y1 - y0 - 1);
            if (f.tiles[sx][sy] != Floor.Tile.FLOOR) {
                continue;
            }
            boolean nearDoor = false;
            for (int[] d : doors) {
                if (Math.abs(d[0] - sx) + Math.abs(d[1] - sy) <= 1) {
                    nearDoor = true;
                    break;
                }
            }
            if (nearDoor) {
                continue;
            }
            if (random.nextBoolean() && sx + 1 <= x1 - 1 && f.tiles[sx + 1][sy] == Floor.Tile.FLOOR) {
                f.tiles[sx][sy] = Floor.Tile.SHELF;
                f.tiles[sx + 1][sy] = Floor.Tile.SHELF;
            } else if (sy + 1 <= y1 - 1 && f.tiles[sx][sy + 1] == Floor.Tile.FLOOR) {
                f.tiles[sx][sy] = Floor.Tile.SHELF;
                f.tiles[sx][sy + 1] = Floor.Tile.SHELF;
            } else {
                f.tiles[sx][sy] = Floor.Tile.SHELF;
            }
        }
    }

    // ---- スーパーフロア：陳列棚の列で通路が格子状 ----

    private static Floor buildSupermarket(Random random) {
        int w = 44;
        int h = 30;
        Floor f = new Floor(FloorType.SUPERMARKET, w, h);
        borderWalls(f);
        for (int y = 6; y <= h - 6; y += 4) {
            for (int x = 3; x <= w - 4; x++) {
                if ((x - 3) % 6 < 4) {
                    f.tiles[x][y] = Floor.Tile.SHELF;
                }
            }
        }
        // 生鮮コーナー：上部にカウンターを並べる
        for (int x = 4; x <= w - 5; x++) {
            if ((x - 4) % 7 < 4) {
                f.tiles[x][h - 3] = Floor.Tile.COUNTER;
            }
        }
        return f;
    }

    // ---- ゲームセンター：大部屋に大小の筐体をランダム配置 ----

    private static Floor buildGameCenter(Random random) {
        int w = 36;
        int h = 28;
        Floor f = new Floor(FloorType.GAME_CENTER, w, h);
        borderWalls(f);
        int placed = 0;
        for (int i = 0; i < 120 && placed < 42; i++) {
            int mw = 1;
            int mh = 1;
            double r = random.nextDouble();
            if (r < 0.6) {
                mw = 1;
                mh = 1;
            } else if (r < 0.9) {
                if (random.nextBoolean()) {
                    mw = 2;
                    mh = 1;
                } else {
                    mw = 1;
                    mh = 2;
                }
            } else {
                mw = 2;
                mh = 2;
            }
            int x = 2 + random.nextInt(w - 3 - mw);
            int y = 2 + random.nextInt(h - 3 - mh);
            if (Math.abs(x - 2) + Math.abs(y - 2) < 4) {
                continue;
            }
            boolean ok = true;
            for (int ix = x; ix < x + mw && ok; ix++) {
                for (int iy = y; iy < y + mh && ok; iy++) {
                    if (f.tiles[ix][iy] != Floor.Tile.FLOOR) {
                        ok = false;
                    }
                }
            }
            if (!ok) {
                continue;
            }
            for (int ix = x; ix < x + mw; ix++) {
                for (int iy = y; iy < y + mh; iy++) {
                    f.tiles[ix][iy] = Floor.Tile.MACHINE;
                }
            }
            placed++;
        }
        return f;
    }

    // ---- フードコート：広い大部屋、カウンター店舗とテーブル ----

    private static Floor buildFoodCourt(Random random) {
        int w = 40;
        int h = 30;
        Floor f = new Floor(FloorType.FOOD_COURT, w, h);
        borderWalls(f);
        // カウンター店舗：上辺に沿って区切って並べる
        for (int x = 3; x <= w - 4; x++) {
            if ((x - 3) % 6 < 4) {
                f.tiles[x][h - 2] = Floor.Tile.COUNTER;
            }
        }
        // 右辺にもカウンター
        for (int y = 4; y <= h - 5; y++) {
            if ((y - 4) % 6 < 4) {
                f.tiles[w - 2][y] = Floor.Tile.COUNTER;
            }
        }
        // テーブル：中央に点在
        int placed = 0;
        for (int i = 0; i < 80 && placed < 16; i++) {
            int x = 6 + random.nextInt(w - 12);
            int y = 5 + random.nextInt(h - 10);
            if (f.tiles[x][y] != Floor.Tile.FLOOR) {
                continue;
            }
            if (Math.abs(x - 2) + Math.abs(y - 2) < 4) {
                continue;
            }
            f.tiles[x][y] = Floor.Tile.TABLE;
            placed++;
        }
        return f;
    }

    // ---- イベントフロア：何もない広い部屋、またはステージ＋観客席 ----

    private static Floor buildEvent(Random random) {
        int w = 44;
        int h = 32;
        Floor f = new Floor(FloorType.EVENT, w, h);
        borderWalls(f);
        if (random.nextBoolean()) {
            int cx = w / 2;
            for (int x = cx - 6; x <= cx + 5; x++) {
                for (int y = h - 6; y <= h - 4; y++) {
                    f.tiles[x][y] = Floor.Tile.STAGE;
                }
            }
            for (int row = 0; row < 3; row++) {
                int y = h - 9 - row * 2;
                for (int x = cx - 7; x <= cx + 6; x++) {
                    if ((x - (cx - 7)) % 2 == 0) {
                        f.tiles[x][y] = Floor.Tile.SEAT;
                    }
                }
            }
        }
        return f;
    }

    // ---- 敵の配置（M3）：スタート地点の近く（3マス以内）には配置しない ----

    /**
     * フロアに敵を配置する。数は階数に応じて増やす（1階で4〜6体）。
     * スタート地点の近く（3マス以内）や階段の上には配置しない。
     */
    public static List<Enemy> spawnEnemies(Floor floor, int floorNumber) {
        Random random = new Random();
        List<Enemy> enemies = new ArrayList<Enemy>();
        int count = 4 + (floorNumber - 1) + random.nextInt(3);
        int tries = 0;
        while (enemies.size() < count && tries < 500) {
            tries++;
            int x = 1 + random.nextInt(floor.w - 2);
            int y = 1 + random.nextInt(floor.h - 2);
            if (!floor.isWalkable(x, y)) {
                continue;
            }
            if (Math.abs(x - floor.startX) + Math.abs(y - floor.startY) <= 3) {
                continue;
            }
            if ((x == floor.escX && y == floor.escY) || (x == floor.stairX && y == floor.stairY)) {
                continue;
            }
            boolean occupied = false;
            for (Enemy e : enemies) {
                if (e.x == x && e.y == y) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) {
                continue;
            }
            enemies.add(new Enemy(Enemy.pickKind(floorNumber, random), x, y, floorNumber));
        }
        return enemies;
    }

    private static void placeStairs(Floor f, Random random) {
        int[][] dist = f.distancesFrom(f.startX, f.startY);
        int maxDist = 0;
        for (int x = 0; x < f.w; x++) {
            for (int y = 0; y < f.h; y++) {
                if (dist[x][y] > maxDist) {
                    maxDist = dist[x][y];
                }
            }
        }
        int[] esc = farthestTile(f, dist, maxDist, -1, -1, random);
        f.escX = esc[0];
        f.escY = esc[1];
        f.tiles[esc[0]][esc[1]] = Floor.Tile.ESCALATOR;
        int[] st = farthestTile(f, dist, maxDist, esc[0], esc[1], random);
        f.stairX = st[0];
        f.stairY = st[1];
        f.tiles[st[0]][st[1]] = Floor.Tile.STAIRS;
    }

    /**
     * 距離が最大の70%以上の到達可能タイルの中から、avoid から最も遠いものを選ぶ。
     * avoid が (-1, -1) のときはスタートから最も遠いタイルを選ぶ。
     */
    private static int[] farthestTile(Floor f, int[][] dist, int maxDist,
                                      int avoidX, int avoidY, Random random) {
        // フォールバックはスタート地点（必ず歩行可能）。
        int bestX = f.startX;
        int bestY = f.startY;
        int bestScore = -1;
        for (int x = 0; x < f.w; x++) {
            for (int y = 0; y < f.h; y++) {
                if (dist[x][y] < maxDist * 0.7) {
                    continue;
                }
                if (x == avoidX && y == avoidY) {
                    continue;
                }
                int dAvoid = (avoidX < 0) ? 0 : Math.abs(x - avoidX) + Math.abs(y - avoidY);
                int score = dAvoid * 100 + dist[x][y] + random.nextInt(10);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new int[]{bestX, bestY};
    }
}
