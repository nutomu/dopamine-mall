package com.nutomu.dopaminemall;

/** フロアタイプ（下層のショッピングモールらしい5種類）。 */
public enum FloorType {
    TENANT("テナントフロア", "TENANT FLOOR"),
    SUPERMARKET("スーパーフロア", "SUPERMARKET"),
    GAME_CENTER("ゲームセンター", "GAME CENTER"),
    FOOD_COURT("フードコート", "FOOD COURT"),
    EVENT("イベントフロア", "EVENT FLOOR");

    /** 日本語名（仕様上の名称）。 */
    public final String japaneseName;
    /** HUD 表示名（デフォルトフォントは ASCII のみのため英字で表示）。 */
    public final String displayName;

    FloorType(String japaneseName, String displayName) {
        this.japaneseName = japaneseName;
        this.displayName = displayName;
    }
}
