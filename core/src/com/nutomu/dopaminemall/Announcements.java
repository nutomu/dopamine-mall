package com.nutomu.dopaminemall;

/**
 * M6：店内放送の文言。フロアタイプごとに変える。
 * HUD はデフォルトフォントが ASCII のみのため英字で表示する。
 * （日本語の文言は README.md の「遊び方（M6）」に書いてある）
 */
public final class Announcements {
    private Announcements() {
    }

    /** フロアタイプごとの放送文言（20ターンごとに切り替わる）。 */
    public static String[] linesFor(FloorType type) {
        switch (type) {
            case TENANT:
                return new String[]{
                        "Welcome! Thank you for visiting today!",
                        "New shops are open on this floor!",
                        "Please enjoy your shopping!",
                };
            case SUPERMARKET:
                return new String[]{
                        "Today's special: fresh food corner!",
                        "Fresh vegetables just arrived!",
                        "Don't miss the deli sale!",
                };
            case GAME_CENTER:
                return new String[]{
                        "New machines just arrived!",
                        "High score challenge today!",
                        "Try the new crane game!",
                };
            case FOOD_COURT:
                return new String[]{
                        "Please relax at the food court!",
                        "Today's lunch special is ready!",
                        "Free refills at the drink bar!",
                };
            case EVENT:
            default:
                return new String[]{
                        "Special event is now being held!",
                        "Limited goods on sale today!",
                        "Please gather at the stage!",
                };
        }
    }

    /** 階を上がったときの特別な放送。 */
    public static String welcome(int floorNumber) {
        return "Welcome to " + floorNumber + "F!";
    }
}
