package com.HUY.JumpUltimateStars.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.HUY.JumpUltimateStars.character.CharacterData;

public class CharacterSlot {
    public enum State { NORMAL, SELECTED, CONFIRMING, CONFIRMED }

    private float x, y, size;
    private State state = State.NORMAL;
    private final CharacterData data;
    private float flashTimer = 0f;

    private static final float FLASH_DURATION = 1f;
    private static final float BORDER_THICKNESS = 1f; // <-- chỉnh mỏng/dày ở đây

    // ----- cấu hình glow -----
    private static final int GLOW_LAYERS = 3;          // số lớp glow, nhiều hơn = mượt hơn nhưng tốn hiệu năng hơn
    private static final float GLOW_MAX_SPREAD = 5f;  // glow tỏa ra xa bao nhiêu (px) so với viền gốc
    private static final float GLOW_MAX_ALPHA = 0.5f; // độ sáng lớp trong cùng (gần viền nhất)

    public CharacterSlot(CharacterData data) {
        this.data = data;
    }

    public void setPosition(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public void setState(State newState) {
        this.state = newState;
        if (newState == State.CONFIRMING) flashTimer = 0f;
    }

    public void update(float delta) {
        if (state == State.CONFIRMING) {
            flashTimer += delta;
            if (flashTimer >= FLASH_DURATION) {
                state = State.CONFIRMED;
            }
        }
    }

    private Color getCurrentColor() {
        switch (state) {
            case SELECTED:
                return Color.WHITE;
            case CONFIRMING:
                float hue = (flashTimer / FLASH_DURATION) * 360f * 4f;
                return new Color().fromHsv(hue % 360f, 1f, 1f);
            case CONFIRMED:
                return Color.RED;
            default:
                return Color.BLACK;
        }
    }

    /**
     * Vẽ hiệu ứng phát sáng quanh viền, chỉ áp dụng khi ô đang được chú ý
     * (không glow ở trạng thái NORMAL màu đen).
     * Gọi TRƯỚC renderBorder, giữa shapeRenderer.begin(ShapeType.Filled) ... end()
     */
    public void renderGlow(ShapeRenderer sr) {
        if (state == State.NORMAL) return;

        Color base = getCurrentColor();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // additive: chồng màu sáng lên nhau

        for (int i = GLOW_LAYERS; i >= 1; i--) {
            float t = (float) i / GLOW_LAYERS;           // 1 (xa) -> gần 0 (sát viền)
            float spread = GLOW_MAX_SPREAD * t;
            float alpha = GLOW_MAX_ALPHA * (1f - t) + GLOW_MAX_ALPHA * 0.15f; // lớp gần viền sáng hơn lớp xa

            sr.setColor(base.r, base.g, base.b, alpha);

            float gx = x - spread;
            float gy = y - spread;
            float gSize = size + spread * 2f;
            float gThickness = BORDER_THICKNESS + spread;

            sr.rect(gx, gy, gSize, gThickness);                              // dưới
            sr.rect(gx, gy + gSize - gThickness, gSize, gThickness);         // trên
            sr.rect(gx, gy, gThickness, gSize);                              // trái
            sr.rect(gx + gSize - gThickness, gy, gThickness, gSize);         // phải
        }

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); // trả về blend mode bình thường
    }

    /** Gọi giữa shapeRenderer.begin(ShapeType.Filled) ... end() */
    public void renderBorder(ShapeRenderer sr) {
        sr.setColor(getCurrentColor());
        sr.rect(x, y, size, BORDER_THICKNESS);
        sr.rect(x, y + size - BORDER_THICKNESS, size, BORDER_THICKNESS);
        sr.rect(x, y, BORDER_THICKNESS, size);
        sr.rect(x + size - BORDER_THICKNESS, y, BORDER_THICKNESS, size);
    }

    /** Gọi giữa batch.begin() ... end() */
    public void renderAvatar(SpriteBatch batch) {
        if (data.avatar != null) {
            float inset = BORDER_THICKNESS + 4f;
            batch.draw(data.avatar, x + inset, y + inset, size - inset * 2, size - inset * 2);
        }
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + size && py >= y && py <= y + size;
    }

    public State getState() { return state; }
    public CharacterData getData() { return data; }
}
