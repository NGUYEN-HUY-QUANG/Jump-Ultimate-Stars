package com.HUY.JumpUltimateStars.hethong;

import com.HUY.JumpUltimateStars.screen.AuthScreen;
import com.badlogic.gdx.Game;

public class MainGame extends Game {

    // Tên tài khoản đang đăng nhập, null = chưa đăng nhập
    private String loggedInUser;

    public MainGame() {
    }

    @Override
    public void create() {
        // Vào game phải đăng nhập trước, xong mới tới menu chính
        setScreen(new AuthScreen(this, AuthScreen.Mode.LOGIN));
    }

    public String getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(String username) {
        this.loggedInUser = username;
    }

    @Override
    public void dispose() {
    }
}
