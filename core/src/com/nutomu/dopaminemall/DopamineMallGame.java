package com.nutomu.dopaminemall;

import com.badlogic.gdx.Game;

/** ドパミン・モール：永遠に登れるショッピングモールがダンジョンのローグライクRPG。 */
public class DopamineMallGame extends Game {
    @Override
    public void create() {
        setScreen(new MallScreen(this));
    }
}
