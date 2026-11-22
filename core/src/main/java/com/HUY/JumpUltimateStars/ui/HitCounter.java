package com.HUY.JumpUltimateStars.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Đếm hit liên tiếp của mỗi bên, vẽ "số + hit" (P1 bên trái, P2 bên phải màn hình).
 * Ảnh: HUD/hit/hit.png (chữ hit), HUD/hit/0.png ... 9.png (chữ số).
 * Không có hiệu ứng: chỉ hiện khi đang có combo, quá HOLD giây không gây thêm dame thì ẩn và đếm lại từ 0.
 */
public class HitCounter {

    private static final String FOLDER = "HUD/hit/";

    // ===================== CHỈNH Ở ĐÂY =====================
    private static final float SCALE = 1.8f;        // phóng to / thu nhỏ (nhân với màn 720p)
    private static final float DIGIT_GAP = 2f;    // khoảng cách giữa các chữ số
    private static final float WORD_GAP = 10f;    // khoảng cách giữa số và chữ hit
    private static final float MARGIN_X = 60f;    // cách mép trái (P1) / mép phải (P2) màn hình
    private static final float Y_RATIO = 0.6f;    // độ cao đáy chữ, tính theo chiều cao màn hình (0 = đáy, 1 = đỉnh)
    private static final float HOLD = 2f;       // không gây thêm dame sau bấy nhiêu giây thì ẩn
    private static final int MIN_SHOW = 1;        // hiện từ hit thứ mấy (đổi thành 2 nếu chỉ muốn hiện khi combo)

    private final Texture[] digits = new Texture[10];
    private final Texture hitTex;

    private final OrthographicCamera camera = new OrthographicCamera();
    private float screenW, screenH, scale = 1f;

    private final int[] count = new int[2];      // [0] = P1, [1] = P2
    private final float[] timer = new float[2];
    private final int[] last = new int[2];       // tổng hit lần trước (của Fighter)

    public HitCounter() {
        for (int i = 0; i < 10; i++) digits[i] = load(FOLDER + i + ".png");
        hitTex = load(FOLDER + "hit.png");
    }

    private static Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** Gọi mỗi frame. total1 / total2 = Fighter.getHitsDealt() của mỗi bên (P2 không có thì truyền 0). */
    public void update(int total1, int total2, float delta) {
        step(0, total1, delta);
        step(1, total2, delta);
    }

    private void step(int i, int total, float delta) {
        if (total > last[i]) {
            count[i] += total - last[i];
            timer[i] = HOLD;
        }
        last[i] = total;

        if (timer[i] > 0f) {
            timer[i] -= delta;
            if (timer[i] <= 0f) count[i] = 0;
        }
    }

    /** Về 0 hết (gọi khi sang round mới, vì Fighter tạo lại nên tổng hit cũng về 0). */
    public void reset() {
        for (int i = 0; i < 2; i++) { count[i] = 0; timer[i] = 0f; last[i] = 0; }
    }

    public void resize(int w, int h) {
        screenW = w; screenH = h;
        camera.setToOrtho(false, w, h);
        scale = h / 720f * SCALE;
    }

    /** Vẽ (toạ độ màn hình, tự begin/end). Gọi cùng lúc với HUD. */
    public void render(SpriteBatch batch) {
        if (count[0] < MIN_SHOW && count[1] < MIN_SHOW) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        if (count[0] >= MIN_SHOW) draw(batch, count[0], false);
        if (count[1] >= MIN_SHOW) draw(batch, count[1], true);
        batch.end();
    }

    /** Vẽ "số + hit". P1 canh từ mép trái, P2 canh sát mép phải. */
    private void draw(SpriteBatch batch, int n, boolean right) {
        String s = String.valueOf(n);
        float gap = DIGIT_GAP * scale, wordGap = WORD_GAP * scale;

        float totalW = 0f;
        for (int i = 0; i < s.length(); i++) totalW += digits[s.charAt(i) - '0'].getWidth() * scale + gap;
        totalW += wordGap - gap + hitTex.getWidth() * scale;

        float x = right ? screenW - MARGIN_X * scale - totalW : MARGIN_X * scale;
        float y = screenH * Y_RATIO;

        for (int i = 0; i < s.length(); i++) {
            Texture t = digits[s.charAt(i) - '0'];
            float w = t.getWidth() * scale, h = t.getHeight() * scale;
            batch.draw(t, x, y, w, h);
            x += w + gap;
        }
        x += wordGap - gap;
        batch.draw(hitTex, x, y, hitTex.getWidth() * scale, hitTex.getHeight() * scale);
    }

    public void dispose() {
        for (Texture t : digits) t.dispose();
        hitTex.dispose();
    }
}
