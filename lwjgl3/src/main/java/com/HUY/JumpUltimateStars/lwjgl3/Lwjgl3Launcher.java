package com.HUY.JumpUltimateStars.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.HUY.JumpUltimateStars.MainGame;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        taoUngDung();
    }

    private static Lwjgl3Application taoUngDung() {
        return new Lwjgl3Application(new MainGame(), layCauHinh());
    }

    private static Lwjgl3ApplicationConfiguration layCauHinh() {
        Lwjgl3ApplicationConfiguration cauHinh = new Lwjgl3ApplicationConfiguration();
        cauHinh.setTitle("Jump Ultimate Stars");
        cauHinh.useVsync(true);
        cauHinh.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        cauHinh.setWindowedMode(1000, 600);
        cauHinh.setWindowIcon("icon.png");
        cauHinh.setResizable(false);   // Không cho kéo giãn cửa sổ
        cauHinh.setMaximized(false);   // Không cho phóng to toàn màn hình
        return cauHinh;
    }
}
