package com.nutomu.dopaminemall;

import java.util.ArrayDeque;

/** 1フロア分のタイルデータ。 */
public class Floor {
    /** タイルの種類。 */
    public enum Tile {
        FLOOR, WALL, SHELF, MACHINE, TABLE, COUNTER, STAGE, SEAT, ESCALATOR, STAIRS;

        /** 歩いて通れるかどうか。 */
        public boolean isWalkable() {
            return this == FLOOR || this == ESCALATOR || this == STAIRS;
        }
    }

    public final FloorType type;
    public final int w;
    public final int h;
    public final Tile[][] tiles;
    /** 行ったことのあるタイル（明るく表示する）。 */
    public final boolean[][] visited;
    public int startX;
    public int startY;
    public int escX, escY;
    public int stairX, stairY;
    /** モンスターハウス（M5）：ある場合、敵が詰まった部屋の範囲。 */
    public boolean hasMonsterHouse = false;
    public int mhX0, mhY0, mhX1, mhY1;

    public Floor(FloorType type, int w, int h) {
        this.type = type;
        this.w = w;
        this.h = h;
        tiles = new Tile[w][h];
        visited = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                tiles[x][y] = Tile.FLOOR;
            }
        }
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    public boolean isWalkable(int x, int y) {
        return inBounds(x, y) && tiles[x][y].isWalkable();
    }

    /**
     * スタート地点から歩いて到達できるタイルまでの距離（BFS、8方向）。
     * 斜め移動も1ターンでできるため、視界・到達可能性も8方向で数える。
     * 到達不能なタイルは -1。
     */
    public int[][] distancesFrom(int sx, int sy) {
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                dist[x][y] = -1;
            }
        }
        if (!isWalkable(sx, sy)) {
            return dist;
        }
        ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        dist[sx][sy] = 0;
        queue.add(new int[]{sx, sy});
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {1, 0, 1, 1, -1, 0, -1, -1};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int i = 0; i < 8; i++) {
                int nx = cur[0] + dx[i];
                int ny = cur[1] + dy[i];
                if (isWalkable(nx, ny) && dist[nx][ny] < 0) {
                    dist[nx][ny] = dist[cur[0]][cur[1]] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    /** (x, y) に最も近い歩行可能タイルを探す（スタート地点決め用）。 */
    public int[] nearestWalkable(int x, int y) {
        int bestDist = Integer.MAX_VALUE;
        int bx = 1;
        int by = 1;
        for (int ix = 0; ix < w; ix++) {
            for (int iy = 0; iy < h; iy++) {
                if (isWalkable(ix, iy)) {
                    int d = Math.abs(ix - x) + Math.abs(iy - y);
                    if (d < bestDist) {
                        bestDist = d;
                        bx = ix;
                        by = iy;
                    }
                }
            }
        }
        return new int[]{bx, by};
    }
}
