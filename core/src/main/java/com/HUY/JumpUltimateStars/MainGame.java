package com.HUY.JumpUltimateStars;

import com.badlogic.gdx.Game;

public class MainGame extends Game {

    public MainGame() {
    }

    @Override
    public void create() {
        setScreen(new Man_Hinh_Chinh(this));
    }

    @Override
    public void dispose() {
    }
}
