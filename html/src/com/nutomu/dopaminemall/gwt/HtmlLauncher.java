package com.nutomu.dopaminemall.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.nutomu.dopaminemall.DopamineMallGame;

/** ドパミン・モールの Web（HTML5）起動クラス。 */
public class HtmlLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        // デスクトップ版と同じ 1280x720 のキャンバス。
        // ゲーム内部は FitViewport(1280x800) で描画される。
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(1280, 720);
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new DopamineMallGame();
    }
}
