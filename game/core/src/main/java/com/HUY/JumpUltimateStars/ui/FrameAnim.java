package com.HUY.JumpUltimateStars.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

/**
 * Chạy 1 lần dãy ảnh folder/prefix0.png, prefix1.png, ... (tự đếm tới khi hết file).
 * Hết thì giữ frame cuối. Dùng cho hiệu ứng ROUND / FIGHT / K.O / WINNER.
 */
public class FrameAnim {

    private final Array<Texture> frames = new Array<>();
    private final float frameDuration;
    private float time;

    public FrameAnim(String folder, String prefix, float frameDuration) {
        this.frameDuration = frameDuration;
        for (int i = 0; ; i++) {
            FileHandle f = Gdx.files.internal(folder + "/" + prefix + i + ".png");
            if (!f.exists()) break;
            Texture t = new Texture(f);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            frames.add(t);
        }
        if (frames.size == 0) throw new IllegalArgumentException("Không có ảnh trong " + folder);
    }

    public void start() { time = 0f; }

    public void update(float delta) { time += delta; }

    public boolean isFinished() { return time >= frames.size * frameDuration; }

    /** Bề rộng frame cuối (dùng để canh chữ ROUND + số). */
    public float lastWidth() { return frames.peek().getWidth(); }

    /** Vẽ frame hiện tại, tâm tại (cx, cy). */
    public void drawCentered(SpriteBatch batch, float cx, float cy, float scale) {
        int i = Math.min((int) (time / frameDuration), frames.size - 1);
        Texture t = frames.get(i);
        float w = t.getWidth() * scale, h = t.getHeight() * scale;
        batch.draw(t, cx - w / 2f, cy - h / 2f, w, h);
    }

    public void dispose() {
        for (Texture t : frames) t.dispose();
    }
}
