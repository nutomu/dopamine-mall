package com.nutomu.dopaminemall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * M6：BGM 管理。生成チップチューンの OGG ループを再生する（WAV から変換済み）。
 * - title.ogg：タイトル画面の明るくノリのいい曲
 * - dungeon.ogg：ダンジョンの控えめな環境音楽
 * - boss.ogg：ボスフロア／モンスターハウスの緊迫感のある曲
 * アセットが読めない環境でも例外を握りつぶし、無音でゲームを続ける。
 */
public class MusicManager {
    /** BGM の音量（控えめ）。 */
    private static final float VOLUME = 0.35f;

    private Music title;
    private Music dungeon;
    private Music boss;
    private Music current;
    private boolean enabled = false;
    private boolean muted = false;

    /** 3曲を読み込む。失敗した場合は無音モードになる。 */
    public void load() {
        try {
            title = Gdx.audio.newMusic(Gdx.files.internal("music/title.ogg"));
            dungeon = Gdx.audio.newMusic(Gdx.files.internal("music/dungeon.ogg"));
            boss = Gdx.audio.newMusic(Gdx.files.internal("music/boss.ogg"));
            title.setLooping(true);
            dungeon.setLooping(true);
            boss.setLooping(true);
            enabled = true;
        } catch (Exception e) {
            Gdx.app.log("MusicManager", "BGM could not be loaded: " + e.getMessage());
            enabled = false;
        }
    }

    public void playTitle() {
        play(title);
    }

    public void playDungeon() {
        play(dungeon);
    }

    public void playBoss() {
        play(boss);
    }

    private void play(Music next) {
        if (!enabled || next == null) {
            return;
        }
        if (current == next) {
            return;
        }
        if (current != null) {
            current.stop();
        }
        current = next;
        current.setVolume(muted ? 0f : VOLUME);
        current.play();
    }

    /** ミュート切替（Nキー）。再生中の曲の音量だけを変える。 */
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (current != null) {
            current.setVolume(muted ? 0f : VOLUME);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void dispose() {
        if (title != null) {
            title.dispose();
        }
        if (dungeon != null) {
            dungeon.dispose();
        }
        if (boss != null) {
            boss.dispose();
        }
    }
}
