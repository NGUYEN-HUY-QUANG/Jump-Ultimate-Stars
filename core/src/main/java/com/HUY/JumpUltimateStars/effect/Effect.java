package com.HUY.JumpUltimateStars.effect;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Mọi effect skill (phi tiêu, kame...) đều implement interface này.
 * Mỗi effect tự lo hết: load ảnh, di chuyển, vẽ, tính dame, dọn Texture.
 * Fighter chỉ việc tạo ra rồi gọi update/draw.
 */
public interface Effect {

    void update(float delta);

    /** Gọi giữa batch.begin() ... batch.end(). */
    void draw(SpriteBatch batch);

    /** Đưa hurtbox của đối thủ vào. Trả về dame của lần này, 0 nếu không trúng. */
    int checkHit(Rectangle hurtbox);

    /** true = trúng thì đối thủ bị knockout, false = chỉ hit. Effect nào knockout thì override. */
    default boolean isKnockdown() { return false; }

    /** false khi đã xong hoặc bị hủy. */
    boolean isActive();

    /** Hủy ngay (nhân vật bị đánh, thoát trận) và giải phóng Texture. */
    void cancel();

    /** Cách tạo effect. Skill giữ cái này, tới frame spawn thì Fighter gọi create(). */
    interface Factory {
        /**
         * @param centerX tâm ngang của nhân vật
         * @param footY   độ cao chân nhân vật (đã cộng độ cao khi nhảy)
         * @param dir     hướng mặt: -1 trái, 1 phải
         */
        Effect create(float centerX, float footY, int dir);
    }
}
