package com.HUY.JumpUltimateStars.character;

import com.HUY.JumpUltimateStars.character.animation.CharacterAnimationManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Đại diện cho 1 nhân vật đang được HIỂN THỊ ĐỘNG (có animation), khác với
 * CharacterData (chỉ là data tĩnh: id + avatar cho UI slot).
 * Dùng cho preview P1/P2 ở màn chọn nhân vật, và sau này dùng luôn trong trận đấu.
 */
public class Character {
    private final CharacterAnimationManager animManager;
    private final String charId;

    private Animation<TextureRegion> currentAnim;
    private String currentAction;
    private float stateTime = 0f;

    private float x, y;
    private float scale = 1f;
    private boolean flipX;

    public Character(CharacterAnimationManager animManager, String charId) {
        this.animManager = animManager;
        this.charId = charId;
    }

    /** Đổi sang action khác, VD: "idle", "walk", "punch". Không reset nếu đang chạy đúng action đó rồi. */
    public void playAnimation(String action, float frameDuration) {
        if (action.equals(currentAction)) return;
        currentAnim = animManager.get(charId, action, frameDuration);
        currentAction = action;
        stateTime = 0f;
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void draw(SpriteBatch batch) {
        if (currentAnim == null) return;
        TextureRegion frame = currentAnim.getKeyFrame(stateTime);
        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;

        if (flipX) {
            batch.draw(frame, x + w, y, -w, h); // lật khi vẽ, không đụng vào region gốc trong cache
        } else {
            batch.draw(frame, x, y, w, h);
        }
    }

    /** Bề rộng hiện tại của frame đang hiển thị (đã nhân scale) — dùng để canh giữa chính xác. */
    public float getCurrentFrameWidth() {
        if (currentAnim == null) return 0f;
        return currentAnim.getKeyFrame(stateTime).getRegionWidth() * scale;
    }

    /** Chiều cao hiện tại của frame đang hiển thị (đã nhân scale). */
    public float getCurrentFrameHeight() {
        if (currentAnim == null) return 0f;
        return currentAnim.getKeyFrame(stateTime).getRegionHeight() * scale;
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setScale(float scale) { this.scale = scale; }
    public void setFlipX(boolean flip) { this.flipX = flip; }
    public String getCharId() { return charId; }
}
