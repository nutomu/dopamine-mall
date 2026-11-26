package com.nutomu.dopaminemall;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** ドパミン・モールのデスクトップ（PC）起動クラス。 */
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("ドパミン・モール");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new DopamineMallGame(), config);
    }
}
