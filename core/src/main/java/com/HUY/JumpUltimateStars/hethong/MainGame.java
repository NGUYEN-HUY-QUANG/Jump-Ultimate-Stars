package com.HUY.JumpUltimateStars.hethong;

import com.HUY.JumpUltimateStars.screen.MainScreen;
import com.badlogic.gdx.Game;

public class MainGame extends Game {

    public MainGame() {
    }

    @Override
    public void create() {
        setScreen(new MainScreen(this));
    }

    @Override
    public void dispose() {
    }
}
