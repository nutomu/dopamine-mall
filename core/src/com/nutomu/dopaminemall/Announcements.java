package com.nutomu.dopaminemall;

import java.util.ArrayList;
import java.util.List;

/**
 * M6：店内放送の文言。フロアタイプごとに変える。
 * 英語と日本語の両方の文言を持ち、現在の言語に合わせて返す。
 */
public final class Announcements {
    private Announcements() {
    }

    /** フロアタイプごとの放送文言（20ターンごとに切り替わる）。 */
    public static String[] linesFor(FloorType type) {
        if (Localization.isJapanese()) {
            switch (type) {
                case TENANT:
                    return new String[]{
                            "いらっしゃいませ！本日もご来店ありがとうございます！",
                            "新しいお店がオープンしました！",
                            "お買い物をお楽しみください！",
                    };
                case SUPERMARKET:
                    return new String[]{
                            "本日の特売は生鮮コーナーです！",
                            "新鮮な野菜が入荷しました！",
                            "お惣菜コーナーのセールをお見逃しなく！",
                    };
                case GAME_CENTER:
                    return new String[]{
                            "新台入荷しました！",
                            "本日はハイスコアチャレンジ開催中！",
                            "クレーンゲームに挑戦してみてください！",
                    };
                case FOOD_COURT:
                    return new String[]{
                            "フードコートでごゆっくりどうぞ！",
                            "本日のランチスペシャルをご用意しました！",
                            "ドリンクバーはおかわり自由です！",
                    };
                case EVENT:
                default:
                    return new String[]{
                            "ただいまイベント開催中です！",
                            "本日限定のグッズを販売中です！",
                            "ステージ前にお集まりください！",
                    };
            }
        }
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
        return Localization.t("announce.welcome", floorNumber);
    }

    /** すべての日本語の放送文言を返す（フォントの文字収集用）。 */
    public static List<String> allJapaneseLines() {
        List<String> result = new ArrayList<String>();
        for (FloorType type : FloorType.values()) {
            String[] lines = linesForJapanese(type);
            for (String line : lines) {
                result.add(line);
            }
        }
        return result;
    }

    private static String[] linesForJapanese(FloorType type) {
        switch (type) {
            case TENANT:
                return new String[]{
                        "いらっしゃいませ！本日もご来店ありがとうございます！",
                        "新しいお店がオープンしました！",
                        "お買い物をお楽しみください！",
                };
            case SUPERMARKET:
                return new String[]{
                        "本日の特売は生鮮コーナーです！",
                        "新鮮な野菜が入荷しました！",
                        "お惣菜コーナーのセールをお見逃しなく！",
                };
            case GAME_CENTER:
                return new String[]{
                        "新台入荷しました！",
                        "本日はハイスコアチャレンジ開催中！",
                        "クレーンゲームに挑戦してみてください！",
                };
            case FOOD_COURT:
                return new String[]{
                        "フードコートでごゆっくりどうぞ！",
                        "本日のランチスペシャルをご用意しました！",
                        "ドリンクバーはおかわり自由です！",
                };
            case EVENT:
            default:
                return new String[]{
                        "ただいまイベント開催中です！",
                        "本日限定のグッズを販売中です！",
                        "ステージ前にお集まりください！",
                };
        }
    }
}
